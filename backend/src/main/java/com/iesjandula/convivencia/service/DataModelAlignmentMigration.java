package com.iesjandula.convivencia.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DataModelAlignmentMigration implements ApplicationRunner {

    private static final String MIGRATION_KEY = "2026-03-22-unified-model-alignment-v10";

    private final JdbcTemplate jdbcTemplate;

    public DataModelAlignmentMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureMigrationTable();
        if (isApplied()) {
            return;
        }

        ensureCoreColumnsAndDefaults();
        alignProfesoresRoleEnum();
        alignReferenceColumnsToProfesorEmail();
        ensureMissingProfesoresForReferences();
        deduplicateTutorAssignments();
        deduplicateGroupsByCourseAndLetter();

        ensureAlumnosGrupoIdColumn();
        backfillAlumnosGrupoId();

        deduplicateForeignKeys();
        ensureConstraints();
        backfillMissingSubjects();
        seedTestSubjectsForTeachers();
        backfillTaskSubjectsFromImparte();
        markApplied();
    }

    private void backfillTaskSubjectsFromImparte() {
        if (!tableExists("tareas_expulsion") || !tableExists("imparte")) {
            return;
        }

        jdbcTemplate.update("""
                UPDATE tareas_expulsion t
                JOIN (
                    SELECT i.profesor_email, MIN(i.id) AS imparte_id
                    FROM imparte i
                    WHERE i.activo = b'1'
                      AND i.asignatura IS NOT NULL
                      AND TRIM(i.asignatura) <> ''
                      AND LOWER(TRIM(i.asignatura)) <> 'pendiente asignatura'
                    GROUP BY i.profesor_email
                ) pick ON pick.profesor_email = t.profesor_email
                JOIN imparte i2 ON i2.id = pick.imparte_id
                SET t.asignatura = i2.asignatura
                WHERE t.asignatura IS NULL
                   OR TRIM(t.asignatura) = ''
                   OR LOWER(TRIM(t.asignatura)) = 'pendiente asignatura'
                """);
    }

    private void seedTestSubjectsForTeachers() {
        if (!tableExists("profesores") || !tableExists("grupos") || !tableExists("imparte")) {
            return;
        }

        Integer grupoId = jdbcTemplate.query(
                "SELECT id FROM grupos WHERE activo = b'1' ORDER BY id LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null
        );

        if (grupoId == null) {
            jdbcTemplate.update("INSERT INTO grupos (curso, letra, tutor_email, activo) VALUES ('1 ESO', 'A', NULL, b'1')");
            grupoId = jdbcTemplate.query(
                    "SELECT id FROM grupos WHERE activo = b'1' ORDER BY id LIMIT 1",
                    rs -> rs.next() ? rs.getInt(1) : null
            );
        }

        if (grupoId == null) {
            return;
        }

        List<String> materias = List.of(
                "Matemáticas (Prueba)",
                "Lengua (Prueba)",
                "Inglés (Prueba)",
                "Biología (Prueba)",
                "Geografía e Historia (Prueba)",
                "Física y Química (Prueba)",
                "Educación Física (Prueba)",
                "Tecnología (Prueba)"
        );

        List<String> profesores = jdbcTemplate.query(
                "SELECT email FROM profesores WHERE activo = b'1' ORDER BY email",
                (rs, rowNum) -> rs.getString("email")
        );

        int idx = 0;
        for (String email : profesores) {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM imparte WHERE profesor_email = ? AND activo = b'1'",
                    Integer.class,
                    email
            );

            Integer conAsignaturaValida = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM imparte
                    WHERE profesor_email = ?
                      AND activo = b'1'
                      AND asignatura IS NOT NULL
                      AND TRIM(asignatura) <> ''
                      AND LOWER(TRIM(asignatura)) <> 'pendiente asignatura'
                    """,
                    Integer.class,
                    email
            );

            String materia = materias.get(idx % materias.size());

            if (conAsignaturaValida != null && conAsignaturaValida > 0) {
                idx++;
                continue;
            }

            if (existe != null && existe > 0) {
                Integer imparteId = jdbcTemplate.query(
                        "SELECT id FROM imparte WHERE profesor_email = ? AND activo = b'1' ORDER BY id LIMIT 1",
                        rs -> rs.next() ? rs.getInt(1) : null,
                        email
                );

                if (imparteId != null) {
                    jdbcTemplate.update("UPDATE imparte SET asignatura = ? WHERE id = ?", materia, imparteId);
                    idx++;
                    continue;
                }
            }

            jdbcTemplate.update(
                "INSERT INTO imparte (profesor_email, grupo_id, asignatura, activo) VALUES (?, ?, ?, b'1')",
                email,
                grupoId,
                materia
            );
            idx++;
        }
    }

    private void ensureCoreColumnsAndDefaults() {
        ensureBaseTablesIfMissing();

        dropProfesoresAsignaturaIfExists();

        ensureProfesoresColumns();
        ensureAlumnosColumns();
        ensureGruposColumns();
        ensureImparteColumns();
        ensureConductasColumns();
        ensurePartesColumns();
        ensureExpulsionesColumns();
        ensureParteExpulsionColumns();
        ensureSesionesColumns();
        ensureTareasExpulsionColumns();

        backfillCoreDefaults();
    }

    private void ensureBaseTablesIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS profesores (
                    email VARCHAR(100) PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    rol VARCHAR(20) NULL,
                    es_guardia TINYINT(1) NOT NULL DEFAULT 0,
                    activo TINYINT(1) NOT NULL DEFAULT 1,
                    created_at DATETIME NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS alumnos (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    apellidos VARCHAR(100) NOT NULL,
                    curso VARCHAR(50) NOT NULL,
                    grupo VARCHAR(10) NOT NULL,
                    activo TINYINT(1) NOT NULL DEFAULT 1,
                    created_at DATETIME NULL,
                    grupo_id INT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS grupos (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    curso VARCHAR(50) NOT NULL,
                    letra VARCHAR(10) NOT NULL,
                    tutor_email VARCHAR(100) NULL,
                    activo TINYINT(1) NOT NULL DEFAULT 1
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conductas_convivencia (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    codigo VARCHAR(10) NOT NULL,
                    descripcion TEXT NOT NULL,
                    created_at DATETIME NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS partes_disciplinarios (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    profesor_email VARCHAR(100) NULL,
                    fecha DATE NULL,
                    curso VARCHAR(50) NULL,
                    alumno_id INT NULL,
                    descripcion TEXT NULL,
                    gravedad VARCHAR(10) NULL,
                    medida_tomada VARCHAR(30) NULL,
                    tareas TEXT NULL,
                    archivo_url VARCHAR(500) NULL,
                    conducta_id INT NULL,
                    estado VARCHAR(20) NULL,
                    estado_computo VARCHAR(12) NULL,
                    activo TINYINT(1) NOT NULL DEFAULT 1,
                    created_at DATETIME NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS expulsiones (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    alumno_id INT NULL,
                    jefatura_email VARCHAR(100) NULL,
                    fecha_inicio DATE NULL,
                    fecha_fin DATE NULL,
                    fecha_creacion DATETIME NULL,
                    activo TINYINT(1) NOT NULL DEFAULT 1
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parte_expulsion (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    parte_id INT NULL,
                    expulsion_id INT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS imparte (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    profesor_email VARCHAR(100) NULL,
                    grupo_id INT NULL,
                    asignatura VARCHAR(100) NULL,
                    activo TINYINT(1) NOT NULL DEFAULT 1
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sesiones_convivencia (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    parte_id INT NULL,
                    profesor_guardia_email VARCHAR(100) NULL,
                    fecha DATE NULL,
                    tramo_horario VARCHAR(20) NULL,
                    comportamiento VARCHAR(20) NULL,
                    trabaja TINYINT(1) NULL,
                    observaciones TEXT NULL,
                    created_at DATETIME NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS tareas_expulsion (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    expulsion_id INT NULL,
                    profesor_email VARCHAR(100) NULL,
                    asignatura VARCHAR(100) NULL,
                    descripcion_tarea TEXT NULL,
                    estado VARCHAR(20) NULL
                )
                """);
    }

    private void ensureProfesoresColumns() {
        if (!tableExists("profesores")) return;

        addColumnIfMissing("profesores", "email", "VARCHAR(100)");
        addColumnIfMissing("profesores", "nombre", "VARCHAR(100) NULL");
        addColumnIfMissing("profesores", "rol", "VARCHAR(20) NULL");
        addColumnIfMissing("profesores", "es_guardia", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("profesores", "activo", "TINYINT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("profesores", "created_at", "DATETIME NULL");
    }

    private void ensureAlumnosColumns() {
        if (!tableExists("alumnos")) return;

        addColumnIfMissing("alumnos", "nombre", "VARCHAR(100) NULL");
        addColumnIfMissing("alumnos", "apellidos", "VARCHAR(100) NULL");
        addColumnIfMissing("alumnos", "curso", "VARCHAR(50) NULL");
        addColumnIfMissing("alumnos", "grupo", "VARCHAR(10) NULL");
        addColumnIfMissing("alumnos", "activo", "TINYINT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("alumnos", "created_at", "DATETIME NULL");
        addColumnIfMissing("alumnos", "grupo_id", "INT NULL");
    }

    private void ensureGruposColumns() {
        if (!tableExists("grupos")) return;

        addColumnIfMissing("grupos", "activo", "TINYINT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("grupos", "curso", "VARCHAR(50) NULL");
        addColumnIfMissing("grupos", "letra", "VARCHAR(10) NULL");
        addColumnIfMissing("grupos", "tutor_email", "VARCHAR(100) NULL");
    }

    private void ensureImparteColumns() {
        if (!tableExists("imparte")) return;

        addColumnIfMissing("imparte", "profesor_email", "VARCHAR(100) NULL");
        addColumnIfMissing("imparte", "grupo_id", "INT NULL");
        addColumnIfMissing("imparte", "asignatura", "VARCHAR(100) NULL");
        addColumnIfMissing("imparte", "activo", "TINYINT(1) NOT NULL DEFAULT 1");
    }

    private void ensureConductasColumns() {
        if (!tableExists("conductas_convivencia")) return;

        addColumnIfMissing("conductas_convivencia", "codigo", "VARCHAR(10) NULL");
        addColumnIfMissing("conductas_convivencia", "descripcion", "TEXT NULL");
        addColumnIfMissing("conductas_convivencia", "created_at", "DATETIME NULL");
    }

    private void ensurePartesColumns() {
        if (!tableExists("partes_disciplinarios")) return;

        addColumnIfMissing("partes_disciplinarios", "gravedad", "VARCHAR(10) NULL");
        addColumnIfMissing("partes_disciplinarios", "estado", "VARCHAR(20) NULL");
        addColumnIfMissing("partes_disciplinarios", "estado_computo", "VARCHAR(12) NULL");
        addColumnIfMissing("partes_disciplinarios", "activo", "TINYINT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("partes_disciplinarios", "created_at", "DATETIME NULL");
        addColumnIfMissing("partes_disciplinarios", "archivo_url", "VARCHAR(500) NULL");
        addColumnIfMissing("partes_disciplinarios", "tareas", "TEXT NULL");
        addColumnIfMissing("partes_disciplinarios", "medida_tomada", "VARCHAR(30) NULL");
        addColumnIfMissing("partes_disciplinarios", "profesor_email", "VARCHAR(100) NULL");
        addColumnIfMissing("partes_disciplinarios", "alumno_id", "INT NULL");
        addColumnIfMissing("partes_disciplinarios", "conducta_id", "INT NULL");
        addColumnIfMissing("partes_disciplinarios", "fecha", "DATE NULL");
        addColumnIfMissing("partes_disciplinarios", "curso", "VARCHAR(50) NULL");
        addColumnIfMissing("partes_disciplinarios", "descripcion", "TEXT NULL");
    }

    private void ensureExpulsionesColumns() {
        if (!tableExists("expulsiones")) return;

        addColumnIfMissing("expulsiones", "activo", "TINYINT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("expulsiones", "fecha_creacion", "DATETIME NULL");
        addColumnIfMissing("expulsiones", "jefatura_email", "VARCHAR(100) NULL");
        addColumnIfMissing("expulsiones", "alumno_id", "INT NULL");
        addColumnIfMissing("expulsiones", "fecha_inicio", "DATE NULL");
        addColumnIfMissing("expulsiones", "fecha_fin", "DATE NULL");
    }

    private void ensureParteExpulsionColumns() {
        if (!tableExists("parte_expulsion")) return;

        addColumnIfMissing("parte_expulsion", "parte_id", "INT NULL");
        addColumnIfMissing("parte_expulsion", "expulsion_id", "INT NULL");
    }

    private void ensureSesionesColumns() {
        if (!tableExists("sesiones_convivencia")) return;

        addColumnIfMissing("sesiones_convivencia", "fecha", "DATE NULL");
        addColumnIfMissing("sesiones_convivencia", "comportamiento", "VARCHAR(20) NULL");
        addColumnIfMissing("sesiones_convivencia", "trabaja", "TINYINT(1) NULL");
        addColumnIfMissing("sesiones_convivencia", "observaciones", "TEXT NULL");
        addColumnIfMissing("sesiones_convivencia", "created_at", "DATETIME NULL");
        addColumnIfMissing("sesiones_convivencia", "parte_id", "INT NULL");
        addColumnIfMissing("sesiones_convivencia", "profesor_guardia_email", "VARCHAR(100) NULL");
        addColumnIfMissing("sesiones_convivencia", "tramo_horario", "VARCHAR(20) NULL");
    }

    private void ensureTareasExpulsionColumns() {
        if (!tableExists("tareas_expulsion")) return;

        addColumnIfMissing("tareas_expulsion", "asignatura", "VARCHAR(100) NULL");
        addColumnIfMissing("tareas_expulsion", "descripcion_tarea", "TEXT NULL");
        addColumnIfMissing("tareas_expulsion", "estado", "VARCHAR(20) NULL");
        addColumnIfMissing("tareas_expulsion", "expulsion_id", "INT NULL");
        addColumnIfMissing("tareas_expulsion", "profesor_email", "VARCHAR(100) NULL");
    }

    private void backfillCoreDefaults() {
        if (tableExists("profesores")) {
            jdbcTemplate.update("UPDATE profesores SET nombre = 'Pendiente nombre' WHERE nombre IS NULL OR TRIM(nombre) = ''");
            jdbcTemplate.update("UPDATE profesores SET rol = 'PROFESOR' WHERE rol IS NULL OR TRIM(rol) = ''");
            jdbcTemplate.update("UPDATE profesores SET es_guardia = b'0' WHERE es_guardia IS NULL");
            jdbcTemplate.update("UPDATE profesores SET activo = b'1' WHERE activo IS NULL");
            jdbcTemplate.update("UPDATE profesores SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        }

        if (tableExists("alumnos")) {
            jdbcTemplate.update("UPDATE alumnos SET nombre = 'Pendiente nombre' WHERE nombre IS NULL OR TRIM(nombre) = ''");
            jdbcTemplate.update("UPDATE alumnos SET apellidos = 'Pendiente apellidos' WHERE apellidos IS NULL OR TRIM(apellidos) = ''");
            jdbcTemplate.update("UPDATE alumnos SET curso = 'Sin curso' WHERE curso IS NULL OR TRIM(curso) = ''");
            jdbcTemplate.update("UPDATE alumnos SET grupo = 'S' WHERE grupo IS NULL OR TRIM(grupo) = ''");
            jdbcTemplate.update("UPDATE alumnos SET activo = b'1' WHERE activo IS NULL");
            jdbcTemplate.update("UPDATE alumnos SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        }

        if (tableExists("grupos")) {
            jdbcTemplate.update("UPDATE grupos SET curso = 'Sin curso' WHERE curso IS NULL OR TRIM(curso) = ''");
            jdbcTemplate.update("UPDATE grupos SET letra = 'S' WHERE letra IS NULL OR TRIM(letra) = ''");
            jdbcTemplate.update("UPDATE grupos SET activo = b'1' WHERE activo IS NULL");
        }

        if (tableExists("imparte")) {
            jdbcTemplate.update("UPDATE imparte SET activo = b'1' WHERE activo IS NULL");
        }

        if (tableExists("conductas_convivencia")) {
            jdbcTemplate.update("UPDATE conductas_convivencia SET codigo = 'N/A' WHERE codigo IS NULL OR TRIM(codigo) = ''");
            jdbcTemplate.update("UPDATE conductas_convivencia SET descripcion = 'Pendiente descripción' WHERE descripcion IS NULL OR TRIM(descripcion) = ''");
            jdbcTemplate.update("UPDATE conductas_convivencia SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        }

        if (tableExists("partes_disciplinarios")) {
            jdbcTemplate.update("UPDATE partes_disciplinarios SET fecha = CURRENT_DATE WHERE fecha IS NULL");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET curso = 'Sin curso' WHERE curso IS NULL OR TRIM(curso) = ''");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET descripcion = 'Pendiente descripción' WHERE descripcion IS NULL OR TRIM(descripcion) = ''");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET gravedad = 'LEVE' WHERE gravedad IS NULL OR TRIM(gravedad) = ''");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET estado = 'PENDIENTE' WHERE estado IS NULL OR TRIM(estado) = ''");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET estado_computo = 'ACTIVO' WHERE estado_computo IS NULL OR TRIM(estado_computo) = ''");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET activo = b'1' WHERE activo IS NULL");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
            jdbcTemplate.update("UPDATE partes_disciplinarios SET medida_tomada = 'SE_QUEDA_EN_CLASE' WHERE medida_tomada IS NULL OR TRIM(medida_tomada) = ''");
        }

        if (tableExists("expulsiones")) {
            jdbcTemplate.update("UPDATE expulsiones SET fecha_inicio = CURRENT_DATE WHERE fecha_inicio IS NULL");
            jdbcTemplate.update("UPDATE expulsiones SET fecha_fin = CURRENT_DATE WHERE fecha_fin IS NULL");
            jdbcTemplate.update("UPDATE expulsiones SET activo = b'1' WHERE activo IS NULL");
            jdbcTemplate.update("UPDATE expulsiones SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL");
        }

        if (tableExists("sesiones_convivencia")) {
            jdbcTemplate.update("UPDATE sesiones_convivencia SET fecha = CURRENT_DATE WHERE fecha IS NULL");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET tramo_horario = '1' WHERE tramo_horario IS NULL OR TRIM(tramo_horario) = ''");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET tramo_horario = '1' WHERE UPPER(TRIM(tramo_horario)) = 'PRIMERA'");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET tramo_horario = '2' WHERE UPPER(TRIM(tramo_horario)) = 'SEGUNDA'");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET tramo_horario = '3' WHERE UPPER(TRIM(tramo_horario)) = 'TERCERA'");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET tramo_horario = '4' WHERE UPPER(TRIM(tramo_horario)) = 'CUARTA'");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET tramo_horario = '5' WHERE UPPER(TRIM(tramo_horario)) = 'QUINTA'");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET tramo_horario = '6' WHERE UPPER(TRIM(tramo_horario)) = 'SEXTA'");
            jdbcTemplate.update("UPDATE sesiones_convivencia SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        }

        if (tableExists("tareas_expulsion")) {
            jdbcTemplate.update("UPDATE tareas_expulsion SET estado = 'PENDIENTE' WHERE estado IS NULL OR TRIM(estado) = ''");
        }
    }

    private void backfillMissingSubjects() {
        jdbcTemplate.update("""
                UPDATE imparte
                SET asignatura = 'Pendiente asignatura'
                WHERE asignatura IS NULL OR TRIM(asignatura) = ''
                """);

        jdbcTemplate.update("""
                UPDATE tareas_expulsion
                SET asignatura = 'Pendiente asignatura'
                WHERE asignatura IS NULL OR TRIM(asignatura) = ''
                """);
    }

    private void ensureMigrationTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_schema_migrations (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    migration_key VARCHAR(200) NOT NULL UNIQUE,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private boolean isApplied() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_schema_migrations WHERE migration_key = ?",
                Integer.class,
                MIGRATION_KEY
        );
        return count != null && count > 0;
    }

    private void markApplied() {
        jdbcTemplate.update("INSERT INTO app_schema_migrations (migration_key) VALUES (?)", MIGRATION_KEY);
    }

    private void alignProfesoresRoleEnum() {
        jdbcTemplate.execute("""
                ALTER TABLE profesores
                MODIFY COLUMN rol ENUM('PROFESOR','TUTOR','JEFATURA','ADMIN') NULL
                """);
    }

    private void alignReferenceColumnsToProfesorEmail() {
        String charset = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profesores' AND COLUMN_NAME = 'email'",
                String.class
        );
        String collation = jdbcTemplate.queryForObject(
                "SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profesores' AND COLUMN_NAME = 'email'",
                String.class
        );

        alterEmailRefColumn("expulsiones", "jefatura_email", false, charset, collation);
        alterEmailRefColumn("grupos", "tutor_email", true, charset, collation);
        alterEmailRefColumn("imparte", "profesor_email", false, charset, collation);
        alterEmailRefColumn("tareas_expulsion", "profesor_email", false, charset, collation);
        alterEmailRefColumn("sesiones_convivencia", "profesor_guardia_email", false, charset, collation);
    }

    private void alterEmailRefColumn(String table, String column, boolean nullable, String charset, String collation) {
        String nullSpec = nullable ? "NULL" : "NOT NULL";
        jdbcTemplate.execute(String.format(
                "ALTER TABLE %s MODIFY COLUMN %s VARCHAR(100) CHARACTER SET %s COLLATE %s %s",
                table,
                column,
                charset,
                collation,
                nullSpec
        ));
    }

    private void ensureMissingProfesoresForReferences() {
        insertMissingProfesoresFromTable("expulsiones", "jefatura_email");
        insertMissingProfesoresFromTable("grupos", "tutor_email");
        insertMissingProfesoresFromTable("imparte", "profesor_email");
        insertMissingProfesoresFromTable("tareas_expulsion", "profesor_email");
        insertMissingProfesoresFromTable("sesiones_convivencia", "profesor_guardia_email");
    }

    private void insertMissingProfesoresFromTable(String table, String column) {
        jdbcTemplate.execute(String.format("""
                INSERT INTO profesores (email, nombre, rol, es_guardia, activo, created_at)
                SELECT DISTINCT t.%s, 'Pendiente nombre', 'PROFESOR', 0, b'1', CURRENT_TIMESTAMP
                FROM %s t
                LEFT JOIN profesores p ON p.email = t.%s
                WHERE t.%s IS NOT NULL
                  AND p.email IS NULL
                """, column, table, column, column));
    }

    private void deduplicateTutorAssignments() {
        jdbcTemplate.execute("""
                UPDATE grupos g
                JOIN (
                    SELECT tutor_email, MIN(id) keep_id
                    FROM grupos
                    WHERE tutor_email IS NOT NULL
                    GROUP BY tutor_email
                    HAVING COUNT(*) > 1
                ) d ON d.tutor_email = g.tutor_email AND g.id <> d.keep_id
                SET g.tutor_email = NULL
                """);
    }

        private void deduplicateGroupsByCourseAndLetter() {
                if (!tableExists("grupos")) {
                        return;
                }

                jdbcTemplate.execute("""
                                UPDATE grupos keep_g
                                JOIN (
                                        SELECT MIN(g.id) keep_id,
                                                     MIN(CASE WHEN g.tutor_email IS NOT NULL THEN g.tutor_email END) tutor_email,
                                                     MAX(CASE WHEN g.activo = b'1' THEN 1 ELSE 0 END) has_active
                                        FROM grupos g
                                        GROUP BY UPPER(TRIM(g.curso)), UPPER(TRIM(g.letra))
                                        HAVING COUNT(*) >= 1
                                ) m ON m.keep_id = keep_g.id
                                SET keep_g.tutor_email = COALESCE(keep_g.tutor_email, m.tutor_email),
                                        keep_g.activo = CASE WHEN m.has_active = 1 THEN b'1' ELSE keep_g.activo END
                                WHERE (keep_g.tutor_email IS NULL AND m.tutor_email IS NOT NULL)
                                     OR (m.has_active = 1 AND keep_g.activo <> b'1')
                                """);

                if (tableExists("alumnos") && columnExists("alumnos", "grupo_id")) {
                        jdbcTemplate.execute("""
                                        UPDATE alumnos a
                                        JOIN grupos g_dup ON a.grupo_id = g_dup.id
                                        JOIN (
                                                SELECT MIN(id) keep_id,
                                                             UPPER(TRIM(curso)) curso_key,
                                                             UPPER(TRIM(letra)) letra_key
                                                FROM grupos
                                                GROUP BY UPPER(TRIM(curso)), UPPER(TRIM(letra))
                                        ) keep_map
                                            ON UPPER(TRIM(g_dup.curso)) = keep_map.curso_key
                                         AND UPPER(TRIM(g_dup.letra)) = keep_map.letra_key
                                        SET a.grupo_id = keep_map.keep_id
                                        WHERE a.grupo_id IS NOT NULL
                                            AND a.grupo_id <> keep_map.keep_id
                                        """);
                }

                if (tableExists("imparte") && columnExists("imparte", "grupo_id")) {
                        jdbcTemplate.execute("""
                                        UPDATE imparte i
                                        JOIN grupos g_dup ON i.grupo_id = g_dup.id
                                        JOIN (
                                                SELECT MIN(id) keep_id,
                                                             UPPER(TRIM(curso)) curso_key,
                                                             UPPER(TRIM(letra)) letra_key
                                                FROM grupos
                                                GROUP BY UPPER(TRIM(curso)), UPPER(TRIM(letra))
                                        ) keep_map
                                            ON UPPER(TRIM(g_dup.curso)) = keep_map.curso_key
                                         AND UPPER(TRIM(g_dup.letra)) = keep_map.letra_key
                                        SET i.grupo_id = keep_map.keep_id
                                        WHERE i.grupo_id IS NOT NULL
                                            AND i.grupo_id <> keep_map.keep_id
                                        """);
                }

                jdbcTemplate.execute("""
                                DELETE g
                                FROM grupos g
                                JOIN (
                                        SELECT MIN(id) keep_id,
                                                     UPPER(TRIM(curso)) curso_key,
                                                     UPPER(TRIM(letra)) letra_key
                                        FROM grupos
                                        GROUP BY UPPER(TRIM(curso)), UPPER(TRIM(letra))
                                        HAVING COUNT(*) > 1
                                ) d
                                    ON UPPER(TRIM(g.curso)) = d.curso_key
                                 AND UPPER(TRIM(g.letra)) = d.letra_key
                                WHERE g.id <> d.keep_id
                                """);
        }

    private void ensureAlumnosGrupoIdColumn() {
        if (!columnExists("alumnos", "grupo_id")) {
            jdbcTemplate.execute("ALTER TABLE alumnos ADD COLUMN grupo_id INT NULL");
        }

        if (!indexExists("alumnos", "idx_alumnos_grupo_id")) {
            jdbcTemplate.execute("CREATE INDEX idx_alumnos_grupo_id ON alumnos (grupo_id)");
        }
    }

    private void backfillAlumnosGrupoId() {
        List<Map<String, Object>> grupos = jdbcTemplate.queryForList(
                "SELECT id, curso, letra FROM grupos WHERE activo = b'1'"
        );

        Map<String, Integer> grupoIndex = new HashMap<>();
        for (Map<String, Object> grupo : grupos) {
            Integer id = ((Number) grupo.get("id")).intValue();
            String curso = (String) grupo.get("curso");
            String letra = (String) grupo.get("letra");
            grupoIndex.put(normalizeKey(curso, letra), id);
        }

        List<Map<String, Object>> alumnos = jdbcTemplate.queryForList(
                "SELECT id, curso, grupo, grupo_id FROM alumnos"
        );

        for (Map<String, Object> alumno : alumnos) {
            Object grupoId = alumno.get("grupo_id");
            if (grupoId != null) {
                continue;
            }

            Integer alumnoId = ((Number) alumno.get("id")).intValue();
            String curso = (String) alumno.get("curso");
            String grupo = (String) alumno.get("grupo");

            Integer targetGrupoId = grupoIndex.get(normalizeKey(curso, grupo));
            if (targetGrupoId != null) {
                jdbcTemplate.update("UPDATE alumnos SET grupo_id = ? WHERE id = ?", targetGrupoId, alumnoId);
            }
        }
    }

    private String normalizeKey(String curso, String grupo) {
        return normalizeText(curso) + "|" + normalizeText(grupo);
    }

    private String normalizeText(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("Â", "")
                .replace("º", "")
                .replace("ª", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase(Locale.ROOT)
                .trim();

        return normalized;
    }

    private void ensureConstraints() {
        addForeignKeyIfMissing("expulsiones", "FK_expulsiones_jefatura_email", "jefatura_email", "profesores", "email");
        addForeignKeyIfMissing("grupos", "FK_grupos_tutor_email", "tutor_email", "profesores", "email");
        addForeignKeyIfMissing("imparte", "FK_imparte_profesor_email", "profesor_email", "profesores", "email");
        addForeignKeyIfMissing("tareas_expulsion", "FK_tareas_expulsion_profesor_email", "profesor_email", "profesores", "email");
        addForeignKeyIfMissing("alumnos", "FK_alumnos_grupo_id", "grupo_id", "grupos", "id");

        if (!indexExists("grupos", "UK_grupos_tutor_email")) {
            jdbcTemplate.execute("CREATE UNIQUE INDEX UK_grupos_tutor_email ON grupos (tutor_email)");
        }

        if (!indexExists("grupos", "UK_grupos_curso_letra")) {
            jdbcTemplate.execute("CREATE UNIQUE INDEX UK_grupos_curso_letra ON grupos (curso, letra)");
        }
    }

    private void deduplicateForeignKeys() {
        Map<String, String> preferredConstraintByRelation = new HashMap<>();
        preferredConstraintByRelation.put("expulsiones|jefatura_email|profesores|email", "FK_expulsiones_jefatura_email");
        preferredConstraintByRelation.put("grupos|tutor_email|profesores|email", "FK_grupos_tutor_email");
        preferredConstraintByRelation.put("imparte|profesor_email|profesores|email", "FK_imparte_profesor_email");
        preferredConstraintByRelation.put("tareas_expulsion|profesor_email|profesores|email", "FK_tareas_expulsion_profesor_email");
        preferredConstraintByRelation.put("alumnos|grupo_id|grupos|id", "FK_alumnos_grupo_id");

        List<Map<String, Object>> duplicateGroups = jdbcTemplate.queryForList("""
                SELECT k.TABLE_NAME,
                       k.COLUMN_NAME,
                       k.REFERENCED_TABLE_NAME,
                       k.REFERENCED_COLUMN_NAME,
                       GROUP_CONCAT(DISTINCT k.CONSTRAINT_NAME ORDER BY k.CONSTRAINT_NAME SEPARATOR ',') AS CONSTRAINT_NAMES,
                       COUNT(DISTINCT k.CONSTRAINT_NAME) AS FK_COUNT
                FROM information_schema.KEY_COLUMN_USAGE k
                JOIN information_schema.TABLE_CONSTRAINTS tc
                  ON tc.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
                 AND tc.TABLE_NAME = k.TABLE_NAME
                 AND tc.CONSTRAINT_NAME = k.CONSTRAINT_NAME
                WHERE k.TABLE_SCHEMA = DATABASE()
                  AND k.REFERENCED_TABLE_NAME IS NOT NULL
                  AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY'
                GROUP BY k.TABLE_NAME, k.COLUMN_NAME, k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME
                HAVING COUNT(DISTINCT k.CONSTRAINT_NAME) > 1
                """);

        for (Map<String, Object> duplicateGroup : duplicateGroups) {
            String table = (String) duplicateGroup.get("TABLE_NAME");
            String column = (String) duplicateGroup.get("COLUMN_NAME");
            String refTable = (String) duplicateGroup.get("REFERENCED_TABLE_NAME");
            String refColumn = (String) duplicateGroup.get("REFERENCED_COLUMN_NAME");
            String relationKey = relationKey(table, column, refTable, refColumn);

            String constraintNamesRaw = (String) duplicateGroup.get("CONSTRAINT_NAMES");
            List<String> constraintNames = Arrays.stream(constraintNamesRaw.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());

            if (constraintNames.size() <= 1) {
                continue;
            }

            String preferredName = preferredConstraintByRelation.get(relationKey);
            String keepConstraint = (preferredName != null && constraintNames.contains(preferredName))
                    ? preferredName
                    : constraintNames.get(0);

            for (String constraintName : constraintNames) {
                if (!constraintName.equals(keepConstraint)) {
                    dropForeignKey(table, constraintName);
                }
            }
        }
    }

    private String relationKey(String table, String column, String refTable, String refColumn) {
        return table + "|" + column + "|" + refTable + "|" + refColumn;
    }

    private void dropForeignKey(String table, String constraintName) {
        jdbcTemplate.execute(String.format(
                "ALTER TABLE `%s` DROP FOREIGN KEY `%s`",
                sanitizeIdentifier(table),
                sanitizeIdentifier(constraintName)
        ));
    }

    private String sanitizeIdentifier(String identifier) {
        return identifier.replace("`", "");
    }

    private void addForeignKeyIfMissing(String table, String constraintName, String column, String refTable, String refColumn) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
                """, Integer.class, table, constraintName);

        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute(String.format(
                "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s(%s)",
                table,
                constraintName,
                column,
                refTable,
                refColumn
        ));
    }

    private boolean indexExists(String table, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """, Integer.class, table, indexName);
        return count != null && count > 0;
    }

    private void addColumnIfMissing(String table, String columnName, String definitionSql) {
        if (!columnExists(table, columnName)) {
            jdbcTemplate.execute(String.format(
                    "ALTER TABLE %s ADD COLUMN %s %s",
                    table,
                    columnName,
                    definitionSql
            ));
        }
    }

    private void dropProfesoresAsignaturaIfExists() {
        if (tableExists("profesores") && columnExists("profesores", "asignatura")) {
            jdbcTemplate.execute("ALTER TABLE profesores DROP COLUMN asignatura");
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, table, columnName);
        return count != null && count > 0;
    }
}
