package tdx;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la conexión y operaciones con MySQL para TDX.
 * Requiere MySQL Workbench 8.0 CE + mysql-connector-j.jar en el classpath.
 */
public class DatabaseManager {

    // =====================================================================
    // CONFIGURACIÓN — editar según tu instalación local
    // =====================================================================
    private static final String DB_HOST = "localhost";
    private static final int    DB_PORT = 3306;
    private static final String DB_NAME = "tdx_game";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "123456789"; // ← CAMBIAR

    private static final String URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=false&useUnicode=true&characterEncoding=UTF-8";

    private static DatabaseManager instance;
    private Connection connection;
    private boolean connected = false;

    // =====================================================================
    // SINGLETON
    // =====================================================================
    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    private DatabaseManager() { tryConnect(); }

    // =====================================================================
    // CONEXIÓN
    // =====================================================================
    public boolean tryConnect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            connected = true;
            initSchema();
            System.out.println("[DB] Conectado a MySQL correctamente.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver MySQL no encontrado. Agrega mysql-connector-java-5.1.49.jar al classpath.");
            connected = false;
        } catch (SQLException e) {
            System.err.println("[DB] Error de conexión: " + e.getMessage());
            connected = false;
        }
        return connected;
    }

    public boolean isConnected() { return connected; }

    // =====================================================================
    // ESQUEMA — crea tablas si no existen
    // =====================================================================
    private void initSchema() throws SQLException {
        Statement st = connection.createStatement();

        // ── Tabla admins ──────────────────────────────────────────────────
        // Si ya existe con estructura vieja (sin columna 'username') la
        // reconstruimos para evitar el error "Unknown column 'username'".
        boolean adminsExists = false;
        boolean hasUsername  = false;
        ResultSet rs0 = connection.getMetaData().getTables(null, null, "admins", null);
        if (rs0.next()) adminsExists = true;
        rs0.close();

        if (adminsExists) {
            // Revisar si la columna 'username' ya existe
            ResultSet cols = connection.getMetaData().getColumns(null, null, "admins", "username");
            if (cols.next()) hasUsername = true;
            cols.close();

            if (!hasUsername) {
                // Tabla vieja: renombrar y recrear para no perder datos
                System.out.println("[DB] Tabla 'admins' con estructura anterior detectada. Migrando...");
                st.executeUpdate("ALTER TABLE admins RENAME TO admins_old");
                adminsExists = false; // forzar recreación
            }
        }

        if (!adminsExists) {
            st.executeUpdate(
                "CREATE TABLE admins (" +
                "  id         INT AUTO_INCREMENT PRIMARY KEY," +
                "  username   VARCHAR(50)  NOT NULL UNIQUE," +
                "  password   VARCHAR(255) NOT NULL," +
                "  created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            System.out.println("[DB] Tabla 'admins' creada.");
        }

        // ── Tabla scores ──────────────────────────────────────────────────
        st.executeUpdate(
            "CREATE TABLE IF NOT EXISTS scores (" +
            "  id           INT AUTO_INCREMENT PRIMARY KEY," +
            "  player_name  VARCHAR(5)  NOT NULL," +
            "  score        INT         NOT NULL," +
            "  difficulty   TINYINT     NOT NULL COMMENT '0=EASY 1=NORMAL 2=HARD 3=NIGHTMARE'," +
            "  role         VARCHAR(10) NOT NULL DEFAULT 'usuario' COMMENT 'usuario|admin'," +
            "  admin_name   VARCHAR(50) NULL     COMMENT 'nombre del admin si aplica'," +
            "  wave_reached INT         NOT NULL DEFAULT 1," +
            "  map_played   TINYINT     NOT NULL DEFAULT 0 COMMENT '0=Recto 1=Serpentina'," +
            "  created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // ── Admin por defecto si la tabla está vacía ──────────────────────
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM admins");
        rs.next();
        if (rs.getInt(1) == 0) {
            String defaultHash = sha256("admin123");
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO admins (username, password) VALUES (?, ?)"
            );
            ps.setString(1, "admin");
            ps.setString(2, defaultHash);
            ps.executeUpdate();
            System.out.println("[DB] Admin por defecto creado: usuario=admin / contraseña=admin123");
        }
        rs.close();
        st.close();
    }

    // =====================================================================
    // AUTENTICACIÓN ADMIN
    // =====================================================================
    public boolean loginAdmin(String username, String password) {
        if (!connected) return false;
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM admins WHERE username = ? AND password = ?"
            );
            ps.setString(1, username.trim());
            ps.setString(2, sha256(password));
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DB] loginAdmin error: " + e.getMessage());
            return false;
        }
    }

    // =====================================================================
    // PUNTAJES — guardar
    // =====================================================================
    public boolean saveScore(String playerName, int score, int difficulty,
                             String role, String adminName,
                             int waveReached, int mapPlayed) {
        if (!connected) return false;
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO scores " +
                "(player_name, score, difficulty, role, admin_name, wave_reached, map_played) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, playerName.toUpperCase());
            ps.setInt   (2, score);
            ps.setInt   (3, difficulty);
            ps.setString(4, role);
            ps.setString(5, adminName);
            ps.setInt   (6, waveReached);
            ps.setInt   (7, mapPlayed);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] saveScore error: " + e.getMessage());
            return false;
        }
    }

    // =====================================================================
    // PUNTAJES — leer top 10
    // =====================================================================
    public static class ScoreEntry {
        public String playerName;
        public int    score;
        public int    difficulty;
        public String role;        // "usuario" | "admin"
        public String adminName;
        public int    waveReached;
        public int    mapPlayed;
        public String createdAt;
    }

    public List<ScoreEntry> getTopScores(int limit) {
        List<ScoreEntry> list = new ArrayList<>();
        if (!connected) return list;
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT player_name, score, difficulty, role, admin_name, " +
                "wave_reached, map_played, created_at " +
                "FROM scores ORDER BY score DESC LIMIT ?"
            );
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ScoreEntry e = new ScoreEntry();
                e.playerName  = rs.getString("player_name");
                e.score       = rs.getInt   ("score");
                e.difficulty  = rs.getInt   ("difficulty");
                e.role        = rs.getString("role");
                e.adminName   = rs.getString("admin_name");
                e.waveReached = rs.getInt   ("wave_reached");
                e.mapPlayed   = rs.getInt   ("map_played");
                e.createdAt   = rs.getString("created_at");
                list.add(e);
            }
        } catch (SQLException e) {
            System.err.println("[DB] getTopScores error: " + e.getMessage());
        }
        return list;
    }

    // =====================================================================
    // SHA-256
    // =====================================================================
    public static String sha256(String input) {
        try {
            java.security.MessageDigest md =
                java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }

    // =====================================================================
    // CERRAR CONEXIÓN
    // =====================================================================
    public void close() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException ignored) {}
    }
}