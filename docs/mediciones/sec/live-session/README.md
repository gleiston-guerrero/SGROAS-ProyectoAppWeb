# Evidencia en vivo — sesiones registradas

Esta carpeta contiene TRES generaciones de evidencia en vivo. Se conservan
las generaciones anteriores (no se borran) porque documentan el proceso real,
pero **solo la Generacion 3 es evidencia vigente de P4/P9** (es la unica
capturada contra el despliegue PUBLICO real, con el codigo ya corregido
desplegado).

## Generacion 3 (VIGENTE) — 2026-09-17, contra el despliegue publico real

Capturada con `curl` directamente contra `https://sgroas-backend.onrender.com`,
DESPUES de reconectar el servicio de Render al repositorio correcto
(`gleiston-guerrero/SGROAS-ProyectoAppWeb`, antes apuntaba por error al
repositorio viejo `Alxjandr07/...`, ver `CONTRIBUCIONES.md`) y forzar un
redeploy del commit `69ed1e7` (o posterior).

| Archivo | Contenido |
|---|---|
| `login-response-20260917-render.txt` | `POST /api/auth/login` real contra Render, 200 OK, cookies `access_token`/`refresh_token` con `Secure; HttpOnly; SameSite=Strict`, tokens REDACTADOS; cuerpo con `name`/`role` en ingles |
| `sin-sesion-403-20260917-render.txt` | `GET /api/asignaciones` sin cookie contra Render → 403 real |
| `asignaciones-200-20260917-render.txt` | `GET /api/asignaciones` con cookie de sesion real contra Render → 200 OK, 8 asignaciones, campos en **ingles** (`driverName`, `vehiclePlate`, `routeName`, `assignmentDate`, ...) |

**Nota honesta:** en la primera corrida de esta generacion, el JWT interno
(decodificable, no cifrado) todavia mostraba las claims viejas
`nombre`/`rol` — el redeploy de Render no habia recogido todavia el commit
`69ed1e7` (que corrige exactamente eso en `JwtService.java`) en el momento
de la captura. Esto no afecta la validez de P4 (que exige `Secure`+`HttpOnly`
en la cookie, presentes) ni P9 (que exige `role`/campos en ingles en el
CUERPO de la respuesta, tambien correcto), pero se declara explicitamente
en vez de ocultarlo. Si se repite la captura despues de un redeploy con ese
commit o posterior, el JWT tambien deberia mostrar `name`/`role`.

## Generacion 2 (historica) — 2026-09-16

Capturada en una sesion local, contra el codigo YA CORREGIDO (post-renombrado
de campos backend/frontend a ingles, post-cierre P4/P9/P5/P2/P6/P11), con el
stack completo levantado de verdad:

- Postgres 18 y Redis 7 via `docker compose up -d postgres redis`.
- Backend Spring Boot corriendo localmente con `./mvnw spring-boot:run`
  (mismas migraciones Flyway V1–V13 que en produccion).
- Frontend Angular con `npm start` (`ng serve`) en `http://localhost:4200`,
  apuntando a `http://localhost:8080/api`.
- Usuario de prueba: `admin@sgroas.com` / `admin123`, credencial de
  DESARROLLO sembrada por `V2__seed.sql` (no es una credencial real ni de
  produccion).

Archivos:

| Archivo | Contenido |
|---|---|
| `login-response-20260916.txt` | `POST /api/auth/login` real, 200 OK, cookies `access_token`/`refresh_token` con `Secure; HttpOnly; SameSite=Strict`, JWT y refresh token REDACTADOS |
| `sin-sesion-403-20260916.txt` | `GET /api/asignaciones` sin cookie → 403 real |
| `asignaciones-200-20260916.json` | `GET /api/asignaciones` con cookie de sesion real → 200 OK, campos en **ingles** (`driverName`, `vehiclePlate`, `routeName`, `assignmentDate`, ...) |
| `conductor-alta-201-20260916.json` | Alta de conductor real desde el formulario Angular (navegador automatizado), `POST /api/conductores` → 201 Created, sin error de validacion, con `firstNames`/`lastNames`/`nationalId` en ambos lados |

Adicionalmente se confirmo en vivo (sin archivo binario, descrito aqui por
texto):

- **Login exitoso** en el frontend (http://localhost:4200) con
  `admin@sgroas.com` / `admin123`.
- **Navegacion por rol**: tras iniciar sesion como administrador, el menu
  mostro "Inicio", "Usuarios y Roles", "Flota Vehicular", "Unidades", etc., y
  el panel mostro "Panel de Administracion — Bienvenido, Admin SGROAS" con
  metricas del sistema (rol `ROLE_ADMIN` aplicado correctamente al layout).
- **Alta de conductor end-to-end**: se lleno el formulario "Nuevo conductor"
  con datos de prueba (`firstNames="Prueba Evidencia"`, `lastNames="Live E2E"`,
  `nationalId="1799999991"`, `licenseNumber="LIC-E2E-0001"`) y el submit
  devolvio `201 Created` sin errores de validacion; el listado de conductores
  paso de "Pagina 1 de 6001" a "Pagina 1 de 6002" inmediatamente despues,
  confirmando la insercion real en la base de datos.

Lo que NO se pudo probar en esta sesion (declarado explicitamente, sin
inventar salida):

- No se probo el flujo con un usuario `ROLE_COORDINADOR` o `ROLE_SEGURIDAD`
  distinto del admin (las credenciales de esos usuarios de demostracion
  estan documentadas en `V2__seed.sql`/pantalla de login, pero no se hizo el
  login con ellos en esta sesion por limite de tiempo). El admin si confirma
  que el layout cambia segun el rol devuelto por `/api/auth/me`
  (`ROLE_ADMIN` -> menu completo), pero no se capturo evidencia separada de
  los otros dos roles.
- No se genero una captura de pantalla binaria (PNG) versionada; la
  navegacion se describe en texto a partir de lo observado en el navegador
  automatizado durante la sesion.

## Generacion 1 (desactualizada) — 2026-09-14

`login-response.txt`, `sin-sesion-403.txt`, `asignaciones.json`,
`auth-me.json` y `REPORT.md`: evidencia contra el deploy publico de Render,
capturada ANTES del renombrado de campos a ingles (usa `nombre`/`rol` en el
body de login y `conductorNombre`/`vehiculoPlaca`/`rutaNombre` en
asignaciones) y con una nota de auditoria admitiendo que un
`access_token`/`refresh_token` reales habian quedado versionados en texto
plano (posteriormente redactados, ver nota dentro de `login-response.txt`).
Se conserva por trazabilidad historica del hallazgo, pero la evidencia
vigente de los criterios P4/P9 es la de la Generacion 3 (2026-09-17,
contra el despliegue publico real).
