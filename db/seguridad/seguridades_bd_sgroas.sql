-- =========================================================================
-- PROYECTO: SGROAS (Gestión de Recursos Operativos y Seguridad Vial)
-- IMPLEMENTACIÓN DEL ESQUEMA DE SEGURIDAD NATIVA EN POSTGRESQL (ABD)
-- =========================================================================
-- IMPORTANTE:
--  * Script manual (DBA). Ejecutarlo como superusuario (p. ej. postgres)
--    DESPUÉS de que Flyway haya creado las tablas (V1..V12).
--    NO es migración de Flyway para no afectar el arranque de la aplicación.
--  * Contraseñas parametrizadas via variables psql (sin secretos literales
--    en el script). Ejecutar con:
--      psql -v admin_pw='...' -v coord_pw='...' -v segur_pw='...' \
--           -f db/seguridad/seguridades_bd_sgroas.sql
--    Si no se pasan, psql pedirá el valor de forma interactiva (\prompt) en
--    vez de usar un valor por defecto embebido.
--  * Se usa ENABLE ROW LEVEL SECURITY (no FORCE): el dueño de las tablas
--    (el usuario con el que corre la aplicación web) omite RLS, por lo que
--    la app sigue funcionando; RLS sólo restringe a los roles operativos
--    nativos creados aquí (usr_coordinador, usr_seguridad_vial, ...).
--  * Correcciones según revisión del docente de ABD:
--      - En programacion sólo había políticas SELECT, por lo que los
--        INSERT/UPDATE concedidos al coordinador quedaban bloqueados. Se
--        agregan políticas INSERT/UPDATE para rol_coordinador_ruta.
--      - La política sobre usuario no tenía RLS habilitado. Se habilita.
-- =========================================================================

-- -------------------------------------------------------------------------
-- SECCIÓN 1: LIMPIEZA PREVENTIVA DE ENTORNOS (idempotencia)
-- -------------------------------------------------------------------------
DROP POLICY IF EXISTS ver_viajes_activos      ON programacion;
DROP POLICY IF EXISTS ver_todo_coordinador    ON programacion;
DROP POLICY IF EXISTS programacion_coord_insert ON programacion;
DROP POLICY IF EXISTS programacion_coord_update ON programacion;
DROP POLICY IF EXISTS ver_usuarios_seguridad  ON usuario;

ALTER TABLE programacion DISABLE ROW LEVEL SECURITY;
ALTER TABLE usuario      DISABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public FROM public;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM public;

-- -------------------------------------------------------------------------
-- SECCIÓN 2: CREACIÓN DE USUARIOS OPERATIVOS (LOGINS)
-- -------------------------------------------------------------------------
-- Una cuenta única por actor real para trazabilidad y auditoría.
-- Contraseñas tomadas de variables psql (-v admin_pw=... etc.); si no se
-- pasan por línea de comandos, se piden de forma interactiva.
\if :{?admin_pw}
\else
\prompt 'Contraseña para usr_admin_coop: ' admin_pw
\endif
\if :{?coord_pw}
\else
\prompt 'Contraseña para usr_coordinador: ' coord_pw
\endif
\if :{?segur_pw}
\else
\prompt 'Contraseña para usr_seguridad_vial: ' segur_pw
\endif

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'usr_admin_coop') THEN
        EXECUTE format('CREATE ROLE usr_admin_coop LOGIN PASSWORD %L', :'admin_pw');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'usr_coordinador') THEN
        EXECUTE format('CREATE ROLE usr_coordinador LOGIN PASSWORD %L', :'coord_pw');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'usr_seguridad_vial') THEN
        EXECUTE format('CREATE ROLE usr_seguridad_vial LOGIN PASSWORD %L', :'segur_pw');
    END IF;
END $$;

-- -------------------------------------------------------------------------
-- SECCIÓN 3: CREACIÓN DE ROLES GRUPALES (PERFILES / NOLOGIN)
-- -------------------------------------------------------------------------
-- RBAC: los permisos se consolidan en roles abstractos (plantillas).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_administrador') THEN
        EXECUTE 'CREATE ROLE rol_administrador SUPERUSER CREATEDB CREATEROLE NOLOGIN';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_coordinador_ruta') THEN
        EXECUTE 'CREATE ROLE rol_coordinador_ruta NOLOGIN';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_personal_seguridad') THEN
        EXECUTE 'CREATE ROLE rol_personal_seguridad NOLOGIN';
    END IF;
END $$;

-- -------------------------------------------------------------------------
-- SECCIÓN 4: ASIGNACIÓN DE PRIVILEGIOS SOBRE OBJETOS (TABLAS)
-- -------------------------------------------------------------------------
-- A) ADMINISTRADOR: control total (es SUPERUSER, omitido por RLS).
GRANT ALL PRIVILEGES ON TABLE provincia, ciudad, terminal, usuario, rol,
    auditoria, ruta, conductor, unidad, programacion, incidente, alerta
    TO rol_administrador;

-- B) COORDINADOR DE RUTA: catálogos de consulta + control total de
--    inserciones/ediciones en recursos operativos y despachos.
GRANT SELECT ON TABLE provincia, ciudad, terminal TO rol_coordinador_ruta;
GRANT SELECT, INSERT, UPDATE ON TABLE ruta, conductor, unidad, programacion
    TO rol_coordinador_ruta;
GRANT SELECT ON TABLE incidente, alerta TO rol_coordinador_ruta;

-- C) PERSONAL DE SEGURIDAD VIAL: consulta de planificación y catálogos,
--    pero escritura (INSERT/UPDATE) sólo en incidentes y alertas.
GRANT SELECT ON TABLE provincia, ciudad, terminal, ruta, conductor, unidad,
    programacion TO rol_personal_seguridad;
GRANT SELECT, INSERT, UPDATE ON TABLE incidente, alerta
    TO rol_personal_seguridad;

-- -------------------------------------------------------------------------
-- SECCIÓN 5: PERMISOS SOBRE SECUENCIAS (para INSERT con SERIAL)
-- -------------------------------------------------------------------------
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
    TO rol_administrador, rol_coordinador_ruta, rol_personal_seguridad;

-- -------------------------------------------------------------------------
-- SECCIÓN 6: VINCULACIÓN DE USUARIOS A ROLES
-- -------------------------------------------------------------------------
GRANT rol_administrador      TO usr_admin_coop;
GRANT rol_coordinador_ruta   TO usr_coordinador;
GRANT rol_personal_seguridad TO usr_seguridad_vial;

-- -------------------------------------------------------------------------
-- SECCIÓN 7: SEGURIDAD AVANZADA (RLS y LIMITACIÓN POR COLUMNAS)
-- -------------------------------------------------------------------------
-- A) RLS sobre programacion (sólo afecta a roles NO propietarios).
ALTER TABLE programacion ENABLE ROW LEVEL SECURITY;

-- Seguridad Vial: sólo viajes en estado 'Activo'.
CREATE POLICY ver_viajes_activos ON programacion
    FOR SELECT TO rol_personal_seguridad
    USING (estado = 'Activo');

-- Coordinador: lectura total.
CREATE POLICY ver_todo_coordinador ON programacion
    FOR SELECT TO rol_coordinador_ruta
    USING (true);

-- Coordinador: escritura. Sin estas políticas, RLS habría bloqueado los
-- INSERT/UPDATE concedidos en la Sección 4.
CREATE POLICY programacion_coord_insert ON programacion
    FOR INSERT TO rol_coordinador_ruta
    WITH CHECK (true);

CREATE POLICY programacion_coord_update ON programacion
    FOR UPDATE TO rol_coordinador_ruta
    USING (true) WITH CHECK (true);

-- B) RLS sobre usuario (corrige: la política previa no tenía RLS habilitado).
ALTER TABLE usuario ENABLE ROW LEVEL SECURITY;

-- Seguridad Vial: columnas no sensibles (sin contrasena ni correo).
REVOKE SELECT ON usuario FROM rol_personal_seguridad;
GRANT SELECT (id_usuario, cedula, nombre, estado) ON usuario
    TO rol_personal_seguridad;

CREATE POLICY ver_usuarios_seguridad ON usuario
    FOR SELECT TO rol_personal_seguridad
    USING (true);
