package tdx;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * AdminSettingsPanel — Panel de configuración exclusivo para administradores.
 *
 *  ┌────────────────────────────────────────────────────┐
 *  │  ⚙  AJUSTES DE ADMINISTRADOR                       │
 *  │                                                    │
 *  │  [🌊 Ronda inicial]     [  01  ◂  ▸  ]            │
 *  │   Dinero bonus: $10,000                            │
 *  │                                                    │
 *  │  ℹ  Los anuncios de virus aparecerán               │
 *  │     automáticamente cada 3 rondas.                 │
 *  │                                                    │
 *  │            [ GUARDAR ]   [ CANCELAR ]              │
 *  └────────────────────────────────────────────────────┘
 *
 *  Integración en TDX:
 *      AdminConfig adminCfg = new AdminConfig();
 *      // Solo visible si adminLoggedIn == true:
 *      AdminSettingsPanel dlg = new AdminSettingsPanel(mainFrame, adminUsername, adminCfg);
 *      dlg.showDialog();
 *      if (dlg.isSaved()) { aplicar adminCfg.startWave }
 *
 *  Los anuncios de virus se disparan internamente en TDX cada 3 rondas (wave % 3 == 0).
 */
public class AdminSettingsPanel extends JDialog {

    // ── Paleta ───────────────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(10,  14,  26);
    private static final Color BG_PANEL     = new Color(18,  24,  42);
    private static final Color BG_CARD      = new Color(24,  34,  58);
    private static final Color ACCENT_BLUE  = new Color(0,  170, 255);
    private static final Color ACCENT_GOLD  = new Color(255, 200,  50);
    private static final Color ACCENT_GREEN = new Color( 50, 220, 120);
    private static final Color ACCENT_CYAN  = new Color(  0, 210, 200);
    private static final Color TEXT_PRIMARY = new Color(220, 230, 255);
    private static final Color TEXT_MUTED   = new Color(100, 120, 160);
    private static final Color BORDER_GLOW  = new Color(0,  170, 255, 80);

    // ── Estado ───────────────────────────────────────────────────────────
    private int     startWave = 1;
    private boolean saved     = false;

    private final String      adminName;
    private final AdminConfig config;

    // ── Componentes UI ───────────────────────────────────────────────────
    private SpinnerLabel startWaveSpinner;

    // ─────────────────────────────────────────────────────────────────────
    public AdminSettingsPanel(Frame owner, String adminName, AdminConfig config) {
        super(owner, "Ajustes — Administrador", true);
        this.adminName = adminName;
        this.config    = config;

        if (config != null) {
            startWave = config.startWave;
        }

        buildUI();
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    // ── API pública ───────────────────────────────────────────────────────
    public void    showDialog()  { setVisible(true); }
    public boolean isSaved()     { return saved; }
    public int     getStartWave(){ return startWave; }

    // ─────────────────────────────────────────────────────────────────────
    // BUILD UI
    // ─────────────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, BG_DARK, 0, getHeight(), BG_PANEL);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createLineBorder(BORDER_GLOW, 2));
        root.setPreferredSize(new Dimension(480, 360));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(),   BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        getContentPane().setBackground(BG_DARK);
    }

    // ── Encabezado ────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT_BLUE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(480, 70));
        p.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel icon   = makeLabel("⚙", 28, ACCENT_BLUE);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(makeLabel("AJUSTES DE ADMINISTRADOR", 15, TEXT_PRIMARY, Font.BOLD));
        titles.add(makeLabel("Admin: " + adminName, 11, ACCENT_GOLD, Font.PLAIN));

        p.add(icon,   BorderLayout.WEST);
        p.add(titles, BorderLayout.CENTER);
        return p;
    }

    // ── Cuerpo ────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(20, 24, 10, 24));

        // ── Sección 1: Ronda inicial ──────────────────────────────────────
        p.add(buildSectionCard(
            "🌊  RONDA DE INICIO",
            ACCENT_BLUE,
            "Elige la ronda en la que comenzará tu partida.",
            () -> {
                startWaveSpinner = new SpinnerLabel(startWave, 1, 30, ACCENT_BLUE);
                return startWaveSpinner;
            },
            "💰  Dinero bonus al iniciar: $10,000",
            ACCENT_GOLD
        ));

        p.add(Box.createVerticalStrut(16));

        // ── Sección 2: Nota sobre anuncios automáticos ───────────────────
        p.add(buildInfoCard());

        return p;
    }

    // ── Card de sección reutilizable ──────────────────────────────────────
    private JPanel buildSectionCard(
            String title, Color accent,
            String subtitle,
            java.util.function.Supplier<JComponent> spinnerFactory,
            String footNote, Color footColor) {

        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
                // barra lateral izquierda
                g2.setColor(accent);
                g2.fillRect(0, 0, 4, getHeight());
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // Textos
        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);
        textBlock.add(makeLabel(title,    13, accent,      Font.BOLD));
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(makeLabel(subtitle, 11, TEXT_MUTED,  Font.PLAIN));
        textBlock.add(Box.createVerticalStrut(6));
        textBlock.add(makeLabel(footNote, 11, footColor,   Font.PLAIN));

        JComponent spinner = spinnerFactory.get();
        card.add(textBlock, BorderLayout.CENTER);
        card.add(spinner,   BorderLayout.EAST);
        return card;
    }

    // ── Tarjeta informativa (anuncios automáticos) ────────────────────────
    private JPanel buildInfoCard() {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(10, 30, 40));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(ACCENT_CYAN.getRed(), ACCENT_CYAN.getGreen(), ACCENT_CYAN.getBlue(), 50));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
                g2.setColor(ACCENT_CYAN);
                g2.fillRect(0, 0, 4, getHeight());
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel ico = makeLabel("🦠", 22, ACCENT_CYAN);
        ico.setBorder(new EmptyBorder(0, 4, 0, 8));

        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setOpaque(false);
        textBlock.add(makeLabel("ANUNCIOS DE VIRUS — AUTOMÁTICOS", 13, ACCENT_CYAN, Font.BOLD));
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(makeLabel("El Dr. Byte aparecerá cada 3 rondas durante la partida.", 11, TEXT_MUTED, Font.PLAIN));
        textBlock.add(Box.createVerticalStrut(2));
        textBlock.add(makeLabel("Rondas 3, 6, 9, 12... — Podrás omitir el anuncio con [OMITIR].", 11, TEXT_MUTED, Font.PLAIN));

        card.add(ico,       BorderLayout.WEST);
        card.add(textBlock, BorderLayout.CENTER);
        return card;
    }

    // ── Pie con botones ───────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0, 100, 160, 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(0, 0, getWidth(), 0);
            }
        };
        p.setOpaque(false);

        JButton btnSave   = buildButton("✔  GUARDAR",   ACCENT_GREEN, BG_DARK);
        JButton btnCancel = buildButton("✕  CANCELAR",  new Color(220, 60, 60), BG_DARK);

        btnSave.addActionListener(e -> {
            // Leer valores del spinner
            startWave = startWaveSpinner.getValue();

            // Advertencia si la ronda inicio es alta
            if (startWave > 25) {
                int opt = JOptionPane.showConfirmDialog(this,
                    "Iniciar en la ronda " + startWave + " puede hacer el juego muy difícil.\n¿Continuar?",
                    "⚠ Confirmación", JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION) return;
            }

            if (config != null) {
                config.startWave  = startWave;
                config.bonusMoney = 10_000;
                config.active     = true;
            }
            saved = true;
            dispose();
        });

        btnCancel.addActionListener(e -> { saved = false; dispose(); });

        p.add(btnSave);
        p.add(btnCancel);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers de UI
    // ─────────────────────────────────────────────────────────────────────
    private static JLabel makeLabel(String text, int size, Color color) {
        return makeLabel(text, size, color, Font.PLAIN);
    }
    private static JLabel makeLabel(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private static JButton buildButton(String text, Color border, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getModel().isPressed()
                    ? border.darker()
                    : getModel().isRollover()
                        ? new Color(border.getRed(), border.getGreen(), border.getBlue(), 60)
                        : new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 200);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(border);
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(150, 36));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ─────────────────────────────────────────────────────────────────────
    // SpinnerLabel — selector numérico personalizado ◂  N  ▸
    // ─────────────────────────────────────────────────────────────────────
    static class SpinnerLabel extends JPanel {
        private int value;
        private final int min, max;
        private final Color accent;
        private final JLabel valueLabel;

        SpinnerLabel(int initial, int min, int max, Color accent) {
            this.value  = initial;
            this.min    = min;
            this.max    = max;
            this.accent = accent;

            setLayout(new FlowLayout(FlowLayout.CENTER, 6, 0));
            setOpaque(false);
            setPreferredSize(new Dimension(110, 48));

            JButton dec = arrow("◂");
            valueLabel  = makeValueLabel();
            JButton inc = arrow("▸");

            dec.addActionListener(e -> { if (value > min) { value--; refresh(); } });
            inc.addActionListener(e -> { if (value < max) { value++; refresh(); } });

            add(dec);
            add(valueLabel);
            add(inc);
        }

        private JLabel makeValueLabel() {
            JLabel l = new JLabel(String.format("%02d", value), SwingConstants.CENTER);
            l.setFont(new Font("Monospaced", Font.BOLD, 22));
            l.setForeground(accent);
            l.setPreferredSize(new Dimension(46, 36));
            return l;
        }

        private JButton arrow(String sym) {
            JButton b = new JButton(sym) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color fill = getModel().isRollover()
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50)
                        : new Color(0, 0, 0, 0);
                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(accent);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
            b.setFont(new Font("SansSerif", Font.BOLD, 16));
            b.setPreferredSize(new Dimension(28, 36));
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        private void refresh() {
            valueLabel.setText(String.format("%02d", value));
            valueLabel.repaint();
        }

        int getValue() { return value; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // AdminConfig — objeto compartido con TDX
    // ─────────────────────────────────────────────────────────────────────
    /**
     * Mantén UNA instancia en TDX:
     *
     *   AdminConfig adminCfg = new AdminConfig();
     *
     * Al iniciar la partida:
     *   if (adminLoggedIn && adminCfg.active) {
     *       wave  = adminCfg.startWave;
     *       money = adminCfg.bonusMoney;
     *   }
     *
     * Los anuncios de virus se controlan directamente en el game-loop de TDX:
     *   if (wave % 3 == 0 && waitingToStart) { mostrar VirusAnnounceDialog }
     */
    public static class AdminConfig {
        /** Ronda de inicio (1–30). Por defecto 1. */
        public int     startWave  = 1;
        /** Dinero inicial extra cuando startWave > 1. */
        public int     bonusMoney = 10_000;
        /** ¿Aplicar la config en la próxima partida? */
        public boolean active     = false;
    }

    // ── Demo ─────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            AdminConfig cfg = new AdminConfig();
            AdminSettingsPanel dlg = new AdminSettingsPanel(null, "admin", cfg);
            dlg.showDialog();
            if (dlg.isSaved()) {
                System.out.println("Ronda inicio : " + dlg.getStartWave());
                System.out.println("Dinero bonus : $" + cfg.bonusMoney);
            } else {
                System.out.println("Cancelado.");
            }
            System.exit(0);
        });
    }
}