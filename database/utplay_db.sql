-- =============================================================
-- UTPlay - Script completo para MySQL 8.0+
-- Ejecutar todo el archivo en MySQL Workbench.
-- =============================================================

CREATE DATABASE IF NOT EXISTS utplay_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE utplay_db;

-- 1. Estudiantes
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    ciclo_actual    INT NOT NULL,
    puntos_totales  INT NOT NULL DEFAULT 0,
    nivel           INT NOT NULL DEFAULT 1,
    fecha_registro  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT chk_usuario_ciclo CHECK (ciclo_actual BETWEEN 1 AND 10),
    CONSTRAINT chk_usuario_puntos CHECK (puntos_totales >= 0),
    CONSTRAINT chk_usuario_nivel CHECK (nivel >= 1)
) ENGINE=InnoDB;

-- 2. Cursos. El prerrequisito es una FK hacia otro curso.
CREATE TABLE IF NOT EXISTS curso (
    id_curso          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_curso      VARCHAR(120) NOT NULL,
    descripcion       VARCHAR(500) NULL,
    ciclo             INT NOT NULL,
    id_prerrequisito  BIGINT NULL,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_curso_nombre UNIQUE (nombre_curso),
    CONSTRAINT chk_curso_ciclo CHECK (ciclo BETWEEN 1 AND 10),
    CONSTRAINT fk_curso_prerrequisito
        FOREIGN KEY (id_prerrequisito) REFERENCES curso(id_curso)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- 3. Quizzes
CREATE TABLE IF NOT EXISTS quiz (
    id_quiz         BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo          VARCHAR(150) NOT NULL,
    descripcion     VARCHAR(600) NULL,
    id_curso        BIGINT NOT NULL,
    puntaje_minimo  INT NOT NULL DEFAULT 60,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_quiz_titulo UNIQUE (titulo),
    CONSTRAINT chk_quiz_puntaje CHECK (puntaje_minimo BETWEEN 0 AND 100),
    CONSTRAINT fk_quiz_curso
        FOREIGN KEY (id_curso) REFERENCES curso(id_curso)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_quiz_curso (id_curso)
) ENGINE=InnoDB;

-- 4. Preguntas y alternativas
CREATE TABLE IF NOT EXISTS pregunta (
    id_pregunta        BIGINT AUTO_INCREMENT PRIMARY KEY,
    enunciado          VARCHAR(1000) NOT NULL,
    alternativa_a      VARCHAR(500) NOT NULL,
    alternativa_b      VARCHAR(500) NOT NULL,
    alternativa_c      VARCHAR(500) NOT NULL,
    alternativa_d      VARCHAR(500) NOT NULL,
    respuesta_correcta VARCHAR(1) NOT NULL,
    id_quiz            BIGINT NOT NULL,
    CONSTRAINT chk_pregunta_respuesta CHECK (respuesta_correcta IN ('A', 'B', 'C', 'D')),
    CONSTRAINT fk_pregunta_quiz
        FOREIGN KEY (id_quiz) REFERENCES quiz(id_quiz)
        ON UPDATE CASCADE ON DELETE CASCADE,
    INDEX idx_pregunta_quiz (id_quiz)
) ENGINE=InnoDB;

-- 5. Intentos y resultados
CREATE TABLE IF NOT EXISTS resultado (
    id_resultado          BIGINT AUTO_INCREMENT PRIMARY KEY,
    puntaje               INT NOT NULL,
    respuestas_correctas  INT NOT NULL,
    total_preguntas       INT NOT NULL,
    puntos_ganados        INT NOT NULL DEFAULT 0,
    fecha                 DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    id_usuario            BIGINT NOT NULL,
    id_quiz               BIGINT NOT NULL,
    CONSTRAINT chk_resultado_puntaje CHECK (puntaje BETWEEN 0 AND 100),
    CONSTRAINT chk_resultado_cantidades CHECK (
        respuestas_correctas >= 0
        AND total_preguntas > 0
        AND respuestas_correctas <= total_preguntas
        AND puntos_ganados >= 0
    ),
    CONSTRAINT fk_resultado_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_resultado_quiz
        FOREIGN KEY (id_quiz) REFERENCES quiz(id_quiz)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_resultado_usuario_fecha (id_usuario, fecha),
    INDEX idx_resultado_usuario_quiz (id_usuario, id_quiz)
) ENGINE=InnoDB;

-- 6. Catalogo de logros
CREATE TABLE IF NOT EXISTS logro (
    id_logro          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(100) NOT NULL,
    descripcion       VARCHAR(500) NOT NULL,
    puntos_requeridos INT NOT NULL,
    CONSTRAINT uk_logro_nombre UNIQUE (nombre),
    CONSTRAINT chk_logro_puntos CHECK (puntos_requeridos >= 0)
) ENGINE=InnoDB;

-- 7. Logros obtenidos por estudiante (N:M)
CREATE TABLE IF NOT EXISTS usuario_logro (
    id_usuario     BIGINT NOT NULL,
    id_logro       BIGINT NOT NULL,
    fecha_obtenido DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id_usuario, id_logro),
    CONSTRAINT fk_usuario_logro_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_usuario_logro_logro
        FOREIGN KEY (id_logro) REFERENCES logro(id_logro)
        ON UPDATE CASCADE ON DELETE CASCADE,
    INDEX idx_usuario_logro_fecha (id_usuario, fecha_obtenido)
) ENGINE=InnoDB;

-- =============================================================
-- DATOS INICIALES DE DEMOSTRACION
-- Los cursos y preguntas son editables y no representan una malla oficial.
-- =============================================================

INSERT INTO logro (id_logro, nombre, descripcion, puntos_requeridos) VALUES
    (1, 'Primer paso', 'Obtuviste tus primeros puntos en UTPlay.', 50),
    (2, 'Estudiante constante', 'Alcanzaste 250 puntos practicando.', 250),
    (3, 'Explorador academico', 'Llegaste a 500 puntos y al nivel 2.', 500),
    (4, 'Especialista', 'Superaste la barrera de los 1000 puntos.', 1000),
    (5, 'Maestro UTPlay', 'Acumulaste 2000 puntos de aprendizaje.', 2000)
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    puntos_requeridos = VALUES(puntos_requeridos);

INSERT INTO curso (id_curso, nombre_curso, descripcion, ciclo, id_prerrequisito, activo) VALUES
    (1, 'Fundamentos de Programacion', 'Algoritmos, variables, operadores y estructuras basicas.', 1, NULL, TRUE),
    (2, 'Matematica Discreta', 'Logica, conjuntos y conceptos iniciales de grafos.', 1, NULL, TRUE),
    (3, 'Programacion Orientada a Objetos', 'Clases, objetos, encapsulamiento y herencia.', 2, 1, TRUE),
    (4, 'Base de Datos', 'Modelo relacional, SQL y normalizacion.', 3, 3, TRUE),
    (5, 'Desarrollo Web Integrado', 'HTML, CSS, JavaScript, HTTP y aplicaciones web.', 4, 4, TRUE),
    (6, 'Ingenieria de Software', 'Requisitos, diseno, pruebas y gestion de versiones.', 5, 5, TRUE)
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    ciclo = VALUES(ciclo),
    id_prerrequisito = VALUES(id_prerrequisito),
    activo = VALUES(activo);

INSERT INTO quiz (id_quiz, titulo, descripcion, id_curso, puntaje_minimo, activo) VALUES
    (1, 'Reto de fundamentos', 'Comprueba tus conocimientos iniciales de programacion.', 1, 60, TRUE),
    (2, 'Reto de matematica discreta', 'Repasa logica, conjuntos y grafos.', 2, 60, TRUE),
    (3, 'Reto de POO', 'Pon a prueba los conceptos de orientacion a objetos.', 3, 60, TRUE),
    (4, 'Reto de base de datos', 'Evalua fundamentos del modelo relacional y SQL.', 4, 60, TRUE),
    (5, 'Reto de desarrollo web', 'Repasa tecnologias esenciales de la web.', 5, 60, TRUE),
    (6, 'Reto de ingenieria de software', 'Valida conceptos del ciclo de desarrollo.', 6, 60, TRUE)
ON DUPLICATE KEY UPDATE
    descripcion = VALUES(descripcion),
    id_curso = VALUES(id_curso),
    puntaje_minimo = VALUES(puntaje_minimo),
    activo = VALUES(activo);

INSERT INTO pregunta
    (id_pregunta, enunciado, alternativa_a, alternativa_b, alternativa_c, alternativa_d, respuesta_correcta, id_quiz)
VALUES
    (1, '¿Que es una variable en programacion?', 'Una instruccion que siempre se repite', 'Un espacio identificado para almacenar un valor', 'Un error de compilacion', 'Un tipo de diagrama', 'B', 1),
    (2, '¿Que estructura ejecuta instrucciones en un orden definido?', 'Secuencial', 'Selectiva', 'Repetitiva', 'Recursiva', 'A', 1),
    (3, '¿Que palabra se usa normalmente para ingresar un dato en pseudocodigo?', 'ESCRIBIR', 'FIN', 'LEER', 'MIENTRAS', 'C', 1),

    (4, 'Si A = {1,2,3} y B = {2,3,4}, ¿cual es A interseccion B?', '{1,4}', '{2,3}', '{1,2,3,4}', 'Conjunto vacio', 'B', 2),
    (5, '¿Cual de las siguientes expresiones es una proposicion?', 'Abre la puerta', '¿Que hora es?', '5 es un numero primo', 'x + 2', 'C', 2),
    (6, 'En un grafo, ¿como se llama cada punto que representa un elemento?', 'Arista', 'Vertice', 'Camino', 'Peso', 'B', 2),

    (7, '¿Que principio oculta el estado interno de un objeto?', 'Herencia', 'Polimorfismo', 'Encapsulamiento', 'Sobrecarga', 'C', 3),
    (8, '¿Que permite crear una clase a partir de otra clase existente?', 'Abstraccion', 'Herencia', 'Composicion', 'Iteracion', 'B', 3),
    (9, '¿Que es un objeto?', 'Una instancia de una clase', 'Unicamente un metodo', 'Un tipo de bucle', 'Una base de datos', 'A', 3),

    (10, '¿Cual es la funcion principal de una clave primaria?', 'Duplicar filas', 'Identificar univocamente cada registro', 'Ordenar siempre de mayor a menor', 'Cifrar la tabla', 'B', 4),
    (11, '¿Que clausula SQL combina filas relacionadas de dos tablas?', 'GROUP', 'DELETE', 'JOIN', 'ALTER', 'C', 4),
    (12, '¿Que busca reducir la normalizacion de una base de datos?', 'La seguridad', 'La redundancia y las anomalías', 'El numero de usuarios', 'La longitud de las consultas', 'B', 4),

    (13, '¿Que protocolo se utiliza para transferir paginas y recursos web?', 'FTP solamente', 'HTTP/HTTPS', 'SMTP', 'SSH', 'B', 5),
    (14, '¿Para que se utiliza CSS?', 'Para estructurar tablas SQL', 'Para compilar Java', 'Para definir la presentacion visual de una pagina', 'Para administrar servidores', 'C', 5),
    (15, '¿Que representa el DOM en una pagina web?', 'La estructura de objetos del documento', 'Un motor de base de datos', 'Un lenguaje de estilos', 'Un servidor DNS', 'A', 5),

    (16, '¿Que describe un requisito funcional?', 'El color favorito del equipo', 'Una funcion que el sistema debe realizar', 'El precio del hardware', 'La marca del servidor', 'B', 6),
    (17, '¿Cual es el objetivo principal de una prueba de software?', 'Agregar mas lineas de codigo', 'Encontrar defectos y verificar el comportamiento', 'Evitar documentar', 'Reemplazar los requisitos', 'B', 6),
    (18, '¿Para que sirve un sistema de control de versiones como Git?', 'Para editar imagenes', 'Para registrar y coordinar cambios en el codigo', 'Para ejecutar consultas SQL', 'Para disenar redes fisicas', 'B', 6)
ON DUPLICATE KEY UPDATE
    enunciado = VALUES(enunciado),
    alternativa_a = VALUES(alternativa_a),
    alternativa_b = VALUES(alternativa_b),
    alternativa_c = VALUES(alternativa_c),
    alternativa_d = VALUES(alternativa_d),
    respuesta_correcta = VALUES(respuesta_correcta),
    id_quiz = VALUES(id_quiz);

-- Verificacion rapida: deben aparecer 7 tablas.
SHOW TABLES;
