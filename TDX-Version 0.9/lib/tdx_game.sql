-- 1. Creación de la base de datos
CREATE DATABASE IF NOT EXISTS tdx_game;
USE tdx_game;

-- 2. Creación de la tabla de administradores
CREATE TABLE IF NOT EXISTS admins (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 3. Creación de la tabla de puntajes (scores)
CREATE TABLE IF NOT EXISTS scores (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    player_name  VARCHAR(5)  NOT NULL,
    score        INT         NOT NULL,
    difficulty   TINYINT     NOT NULL COMMENT '0=EASY 1=NORMAL 2=HARD 3=NIGHTMARE',
    role         VARCHAR(10) NOT NULL DEFAULT 'usuario' COMMENT 'usuario|admin',
    admin_name   VARCHAR(50) NULL     COMMENT 'nombre del admin si aplica',
    wave_reached INT         NOT NULL DEFAULT 1,
    map_played   TINYINT     NOT NULL DEFAULT 0 COMMENT '0=Recto 1=Serpentina',
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- 4. Insertar el administrador por defecto si la tabla está vacía
-- Usamos la función SHA2 de MySQL para encriptar 'admin123' en SHA-256 como hace el código Java
INSERT INTO admins (username, password)
SELECT 'admin', SHA2('admin123', 256)
WHERE NOT EXISTS (SELECT 1 FROM admins);