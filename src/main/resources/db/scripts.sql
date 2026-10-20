-- ==========================================================================
-- Clínica Dental · esquema y datos iniciales
--
-- Este script es la vía documentada para preparar MySQL.
-- La aplicación también puede crear la base con createDatabaseIfNotExist=true
-- y Hibernate puede crear las tablas con ddl-auto=update, pero ejecutar este
-- archivo deja el esquema explícito para la entrega.
--
-- Ejecutar con:
--   mysql -u root -p < src/main/resources/db/scripts.sql
-- ==========================================================================

CREATE DATABASE IF NOT EXISTS clinica_dental
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE clinica_dental;

SET NAMES utf8mb4;

-- --------------------------------------------------------------------------
-- Tablas
-- --------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS rol (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rol_nombre (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS usuario (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(120) NOT NULL,
    telefono VARCHAR(20) NULL,
    password VARCHAR(255) NOT NULL,
    rol_id INT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    activo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_correo (correo),
    KEY idx_usuario_rol (rol_id),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS odontologo (
    id INT NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    especialidad VARCHAR(80) NOT NULL,
    correo VARCHAR(120) NOT NULL,
    telefono VARCHAR(20) NULL,
    consultorio VARCHAR(20) NULL,
    activo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    UNIQUE KEY uk_odontologo_correo (correo)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS servicio (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(300) NULL,
    duracion_minutos INT NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    activo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    UNIQUE KEY uk_servicio_nombre (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS horario (
    id INT NOT NULL AUTO_INCREMENT,
    odontologo_id INT NOT NULL,
    dia_semana INT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    UNIQUE KEY uk_horario (odontologo_id, dia_semana, hora_inicio, hora_fin),
    KEY idx_horario_odontologo_dia (odontologo_id, dia_semana),
    CONSTRAINT fk_horario_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologo (id),
    CONSTRAINT ck_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_rango CHECK (hora_fin > hora_inicio)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS cita (
    id INT NOT NULL AUTO_INCREMENT,
    paciente_id INT NOT NULL,
    odontologo_id INT NOT NULL,
    servicio_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    estado VARCHAR(20) NOT NULL,
    motivo VARCHAR(300) NULL,
    observaciones VARCHAR(500) NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_cita_fecha_hora (fecha, hora),
    KEY idx_cita_paciente (paciente_id),
    KEY idx_cita_odontologo (odontologo_id),
    KEY idx_cita_servicio (servicio_id),
    KEY idx_cita_estado (estado),
    CONSTRAINT fk_cita_paciente FOREIGN KEY (paciente_id) REFERENCES usuario (id),
    CONSTRAINT fk_cita_odontologo FOREIGN KEY (odontologo_id) REFERENCES odontologo (id),
    CONSTRAINT fk_cita_servicio FOREIGN KEY (servicio_id) REFERENCES servicio (id),
    CONSTRAINT ck_cita_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'ATENDIDA', 'CANCELADA'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS auditoria_evento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fecha DATETIME NOT NULL,
    accion VARCHAR(50) NOT NULL,
    usuario VARCHAR(120) NULL,
    ip VARCHAR(45) NULL,
    detalle VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_auditoria_fecha (fecha),
    KEY idx_auditoria_accion (accion)
) ENGINE = InnoDB;

-- --------------------------------------------------------------------------
-- Roles
-- --------------------------------------------------------------------------

INSERT INTO rol (nombre)
VALUES ('ADMIN'), ('PACIENTE'), ('ODONTOLOGO')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- --------------------------------------------------------------------------
-- Catálogo de odontólogos
-- --------------------------------------------------------------------------

INSERT INTO odontologo (nombres, apellidos, especialidad, correo, telefono, consultorio, activo)
VALUES
    ('Valeria', 'Ríos Mendoza', 'Odontología general', 'valeria.rios@dentalcare.pe', '987 654 321', 'C-01', b'1'),
    ('Mateo', 'Salazar Paredes', 'Ortodoncia', 'mateo.salazar@dentalcare.pe', '987 654 322', 'C-02', b'1'),
    ('Camila', 'Torres León', 'Endodoncia', 'camila.torres@dentalcare.pe', '987 654 323', 'C-03', b'1'),
    ('Diego', 'Fernández Rojas', 'Odontopediatría', 'diego.fernandez@dentalcare.pe', '987 654 324', 'C-04', b'1')
ON DUPLICATE KEY UPDATE
    nombres = VALUES(nombres),
    apellidos = VALUES(apellidos),
    especialidad = VALUES(especialidad),
    telefono = VALUES(telefono),
    consultorio = VALUES(consultorio);

-- --------------------------------------------------------------------------
-- Catálogo de tratamientos
-- --------------------------------------------------------------------------

INSERT INTO servicio (nombre, descripcion, duracion_minutos, precio, activo)
VALUES
    ('Consulta general', 'Evaluación inicial y plan de tratamiento.', 30, 60.00, b'1'),
    ('Limpieza dental', 'Profilaxis y eliminación de sarro.', 45, 120.00, b'1'),
    ('Blanqueamiento', 'Blanqueamiento en consultorio con control profesional.', 60, 350.00, b'1'),
    ('Control de ortodoncia', 'Ajuste y seguimiento del tratamiento de ortodoncia.', 30, 150.00, b'1'),
    ('Endodoncia', 'Tratamiento de conducto con aislamiento absoluto.', 90, 450.00, b'1'),
    ('Extracción simple', 'Extracción de pieza dental con indicación profesional.', 45, 180.00, b'1')
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    duracion_minutos = VALUES(duracion_minutos),
    precio = VALUES(precio);

-- --------------------------------------------------------------------------
-- Horarios semanales
--
-- Lunes a viernes: 08:00 a 13:00 y 15:00 a 19:00.
-- Sábado: 08:00 a 13:00.
-- Domingo: sin atención.
-- --------------------------------------------------------------------------

INSERT IGNORE INTO horario (odontologo_id, dia_semana, hora_inicio, hora_fin, activo)
SELECT o.id,
       d.dia,
       b.inicio,
       b.fin,
       b'1'
FROM odontologo o
         CROSS JOIN (SELECT 1 AS dia
                     UNION ALL SELECT 2
                     UNION ALL SELECT 3
                     UNION ALL SELECT 4
                     UNION ALL SELECT 5
                     UNION ALL SELECT 6) d
         CROSS JOIN (SELECT CAST('08:00:00' AS TIME) AS inicio,
                            CAST('13:00:00' AS TIME) AS fin
                     UNION ALL
                     SELECT CAST('15:00:00' AS TIME), CAST('19:00:00' AS TIME)) b
WHERE o.activo = b'1'
  AND (d.dia BETWEEN 1 AND 5
    OR (d.dia = 6 AND b.inicio = CAST('08:00:00' AS TIME)));

-- --------------------------------------------------------------------------
-- Cuentas de demostración
--
-- No se insertan aquí porque el hash Argon2 se genera al arrancar la
-- aplicación, con el PasswordEncoder configurado. DataInitializer crea:
--
--   Administrador: admin@dentalcare.pe
--   Paciente demo: paciente@demo.pe
--
-- Las contraseñas de demostración están en COMO-EJECUTAR.md.
-- Cambiarlas antes de cualquier despliegue real.
-- --------------------------------------------------------------------------

-- Para reiniciar la demo desde cero, ejecutar manualmente:
-- DROP DATABASE IF EXISTS clinica_dental;
