# CONTRIBUCIONES — SGROAS Supletorio v1.1.0

Estudiante: Luis Tejada
Repositorio: https://github.com/gleiston-guerrero/SGROAS-ProyectoAppWeb
Período: Supletorio 2026-09
Integrantes: Luis Alejandro Tejada Bajaña, María del Rosario Escudero Plaza, Kevin Moisés Castro Espinoza

---

## P1 — Passwords/secrets a variables de entorno (commit ac72f7a)
**Cerrado por: Luis Tejada**


**Archivos modificados:**
- `src/main/resources/application.properties` — Reemplazado password hardcodeado por `${SPRING_DATASOURCE_PASSWORD}` y `${JWT_SECRET}`
- `docker-compose.yml` — Todos los secrets ahora usan referencias a variables de entorno
- `.env.example` — Archivo de ejemplo con placeholders CHANGE_ME
- `src/test/java/ec/edu/uteq/sgroas/security/JwtServiceTest.java` — JWT_SECRET desde System.getenv() con fallback de test

**Commit:** `ac72f7a` — "P1: remove hardcoded DB password and JWT secret from source"

---

## P2 — k6 corridas crudas versionadas (commit 51202f5)
**Cerrado por: Luis Tejada**

**Archivos modificados:**
- dataset/perf/k*-run1.json y k*-cold.json -- 13 corridas crudas.
- scripts/perf/recalcular-contraste.py -- script reproducible.

**Commit:** 51202f5

---

## P3 — Lighthouse corridas versionadas (commit 1a07dc7)
**Cerrado por: María del Rosario Escudero Plaza**

**Corrección de atribución (2026-09-16):** este punto estaba firmado
"Cerrado por: Luis Tejada" en la version anterior de este documento, pero
`git log --format='%an <%ae> %s' -1 1a07dc7` muestra que el commit
`1a07dc7` fue hecho por `charito20 <mescuderop@uteq.edu.ec>` (María del
Rosario Escudero Plaza), no por Luis Tejada. Se corrige la atribución para
reflejar el historial real.

```
$ git log --format='%H %an <%ae> %ad %s' --date=short -1 1a07dc7
1a07dc7... charito20 <mescuderop@uteq.edu.ec> feat(entrega3): SRS, scripts de validacion, lighthouse real, reporte perf y correccion de evidencias
```

**Archivos modificados:**
- dataset/lighthouse/lh-*.json -- 9 corridas (mobile, desktop, tablet).
- dataset/lighthouse/REPORT.md -- resumen de scores.

**Commit:** 1a07dc7

---

## P4 — Cookie Secure(true) (commit 92576cf)
**Cerrado por: Luis Tejada**


**Archivos modificados:**
- `src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java` — 4 cookies actualizadas de `.secure(cookieSecure)` a `.secure(true)` (líneas 158, 195, 235, 252)

**Commit:** `92576cf` — "P4+P5: rename entity fields to English and set cookie secure(true)"

---

## P5 — Renombramiento de campos entidades (commit c09f981)
**Cerrado por: Luis Tejada**


**Archivos modificados (entidades):**
- `src/main/java/ec/edu/uteq/sgroas/entity/Driver.java` — 11 campos renombrados (nombres→firstNames, apellidos→lastNames, cedula→nationalId, etc.)
- `src/main/java/ec/edu/uteq/sgroas/entity/User.java` — 6 campos renombrados (nombre→name, rol→role, activo→active, etc.)
- `src/main/java/ec/edu/uteq/sgroas/entity/Vehicle.java` — 9 campos renombrados (placa→plate, marca→brand, etc.)
- `src/main/java/ec/edu/uteq/sgroas/entity/Route.java` — 7 campos renombrados (nombre→name, origen→origin, etc.)
- `src/main/java/ec/edu/uteq/sgroas/entity/Incident.java` — 8 campos renombrados (ubicacion→location, descripcion→description, etc.)
- `src/main/java/ec/edu/uteq/sgroas/entity/RouteAssignment.java` — 9 campos renombrados (conductorId→driverId, etc.)
- `src/main/java/ec/edu/uteq/sgroas/entity/VerificationCode.java` — 6 campos renombrados (codigo→code, tipo→type, etc.)

**Archivos modificados (DTOs):**
- `src/main/java/ec/edu/uteq/sgroas/dto/AuthResponse.java` — nombre→name, rol→role
- `src/main/java/ec/edu/uteq/sgroas/dto/DriverRequest.java` — 11 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/DriverResponse.java` — 11 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/IncidentRequest.java` — 8 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/IncidentResponse.java` — 8 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/RouteAssignmentRequest.java` — 9 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/RouteAssignmentResponse.java` — 9 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/RouteRequest.java` — 7 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/RouteResponse.java` — 7 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/SessionResponse.java` — nombre→name, rol→role
- `src/main/java/ec/edu/uteq/sgroas/dto/UserRequest.java` — nombre→name, rol→role
- `src/main/java/ec/edu/uteq/sgroas/dto/UserResponse.java` — 6 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/VehicleRequest.java` — 9 campos
- `src/main/java/ec/edu/uteq/sgroas/dto/VehicleResponse.java` — 9 campos

**Archivos modificados (servicios):**
- `src/main/java/ec/edu/uteq/sgroas/service/AuthService.java`
- `src/main/java/ec/edu/uteq/sgroas/service/UserService.java`
- `src/main/java/ec/edu/uteq/sgroas/service/DriverService.java`
- `src/main/java/ec/edu/uteq/sgroas/service/VehicleService.java`
- `src/main/java/ec/edu/uteq/sgroas/service/RouteService.java`
- `src/main/java/ec/edu/uteq/sgroas/service/IncidentService.java`
- `src/main/java/ec/edu/uteq/sgroas/service/RouteAssignmentService.java`
- `src/main/java/ec/edu/uteq/sgroas/service/VerificationCodeService.java`

**Archivos modificados (controladores):**
- `src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java`
- `src/main/java/ec/edu/uteq/sgroas/controller/DriverController.java`
- `src/main/java/ec/edu/uteq/sgroas/controller/VehicleController.java`
- `src/main/java/ec/edu/uteq/sgroas/controller/RouteController.java`
- `src/main/java/ec/edu/uteq/sgroas/controller/IncidentController.java`
- `src/main/java/ec/edu/uteq/sgroas/controller/RouteAssignmentController.java`
- `src/main/java/ec/edu/uteq/sgroas/controller/UserController.java`

**Archivos modificados (repositorios):**
- `src/main/java/ec/edu/uteq/sgroas/repository/DriverRepository.java`
- `src/main/java/ec/edu/uteq/sgroas/repository/VehicleRepository.java`
- `src/main/java/ec/edu/uteq/sgroas/repository/RouteRepository.java`
- `src/main/java/ec/edu/uteq/sgroas/repository/IncidentRepository.java`
- `src/main/java/ec/edu/uteq/sgroas/repository/RouteAssignmentRepository.java`
- `src/main/java/ec/edu/uteq/sgroas/repository/VerificationCodeRepository.java`

**Archivos modificados (tests):**
- `src/test/java/ec/edu/uteq/sgroas/controller/AsignacionRutaControllerTest.java`
- `src/test/java/ec/edu/uteq/sgroas/controller/ConductorControllerTest.java`
- `src/test/java/ec/edu/uteq/sgroas/controller/IncidenteControllerTest.java`
- `src/test/java/ec/edu/uteq/sgroas/controller/RutaControllerTest.java`
- `src/test/java/ec/edu/uteq/sgroas/controller/VehiculoControllerTest.java`
- `src/test/java/ec/edu/uteq/sgroas/dto/DtoTest.java`
- `src/test/java/ec/edu/uteq/sgroas/service/AsignacionRutaServiceTest.java`
- `src/test/java/ec/edu/uteq/sgroas/service/CodigoVerificacionServiceTest.java`
- `src/test/java/ec/edu/uteq/sgroas/service/ConductorServiceExtraTest.java`
- `src/test/java/ec/edu/uteq/sgroas/service/ConductorServiceTest.java`
- `src/test/java/ec/edu/uteq/sgroas/service/IncidenteServiceTest.java`
- `src/test/java/ec/edu/uteq/sgroas/service/RutaServiceTest.java`
- `src/test/java/ec/edu/uteq/sgroas/service/VehiculoServiceTest.java`

**Commit:** `92576cf` + `c09f981` — "P5: rename all entity fields from Spanish to English" + "refactor: update security classes for P5 field renames"

**Verificación (portable a Linux, sin PowerShell):**
- `scripts/check-spanish-methods.py` — 0 nombres de método en español (519 métodos, 0%)
- `scripts/check-javadoc.py` — 226/226 métodos documentados (100%), ≥ 90%

---

## P6 — Javadoc >= 90% (commit d2b88b7)
**Cerrado por: Luis Tejada**

**Archivos:** 226 metodos publicos en src/main/java/** documentados (100%).

**Commit:** d2b88b7

---

## P7 — Captions de figuras/tablas en inglés (commit 33e25e5)
**Cerrado por: Luis Tejada**


**Archivos modificados:**
- `docs/informe-final/cap*.tex` y `docs/informe-final/capitulos/cap*.tex` — 12 captions traducidos:
  - 8 tablas: cap3 (Rol-based access control), cap4×2 (Password policies, Authentication time), cap5×2 (Route API, Incident API), cap6 (Response codes), cap8 (UTM zones), capA (Rate-limiting)
  - 4 listados: cap7 (JWT filter, Repositories, Email service, Validation)

**Commit:** `33e25e5` — "P7: translate all Spanish table and listing captions to English"

---

## P8 — Script demografía SUS (commit 5fa09b4)
**Cerrado por: Luis Tejada (script) y María del Rosario Escudero Plaza (datos crudos)**

**Corrección de atribución (2026-09-16):** la version anterior de este
documento atribuía todo el punto P8 a Luis Tejada citando el commit
`5fa09b4`. Ese commit sí es de Tejada y sí crea
`scripts/generate-sus-demographics.py`, pero **los datos crudos que ese
script consume** (`dataset/sus/sus-raw.csv`) no son de Tejada:

```
$ git log --follow --format='%H %an <%ae> %ad %s' --date=short -- dataset/sus/sus-raw.csv
71754792... TheAsesink <0999595561kevin@gmail.com> 2026-09-13 revert(E1): revierte renombrado 88 tipos...
b5164474... TheAsesink <0999595561kevin@gmail.com> 2026-09-13 fix(P11,P15,P6,P16,P10): ...
ce0099f1... Alxjandr07 <luistejada5434@gmail.com> 2026-09-01 fix(dataset): package-dataset.py materializa archivos...
e8d7e2f6... charito20 <mescuderop@uteq.edu.ec> 2026-07-30 feat(entrega3): evidencia SUS real (10 participantes...)

$ git log --follow --format='%H %an <%ae> %ad %s' --date=short -- scripts/generate-sus-demographics.py
5fa09b47... Luis Tejada <luistejada5434@gmail.com> 2026-09-15 P8: add script to generate SUS demographics from raw CSV
```

El CSV con las respuestas reales fue subido originalmente por María del
Rosario Escudero Plaza (`charito20`, commit `e8d7e2f`, 2026-07-30); el
script que lo procesa fue escrito despues por Luis Tejada. Se corrige la
atribución para reconocer ambas partes en vez de dar todo el crédito a
quien escribió solo el script derivado.

**Archivos creados:**
- `scripts/generate-sus-demographics.py` (Luis Tejada) — Lee `dataset/sus/sus-raw.csv`, genera tabla de demografía (15 participantes, 8M/7F, edades 19-25, SUS mean=68.5)
- `dataset/sus/sus-raw.csv` (datos originales de María del Rosario Escudero Plaza, commit `e8d7e2f`) — Datos crudos de participantes

**Commit:** `5fa09b4` — "P8: add script to generate SUS demographics from raw CSV" (script);
`e8d7e2f` (datos crudos originales, Escudero Plaza)

---

## P9 — Postman CRUD asignaciones (commit 20a487f)
**Cerrado por: Luis Tejada**


**Archivos modificados:**
- `docs/postman/coleccion.json` — Agregada carpeta "Asignaciones" con 6 requests:
  1. GET list (200)
  2. POST create (201)
  3. GET by ID (200)
  4. PUT update (200)
  5. DELETE (204)
  6. POST 422 validation

**Commit:** `20a487f` — "P9: add CRUD requests for /api/asignaciones to Postman collection"

---

## P10 — Manifest SHA-256 verificable (commit fc25cb8)
**Cerrado por: Luis Tejada**


**Archivos creados/modificados:**
- `dataset/MANIFEST.sha256` — 283 entradas SHA-256 (LF), verificadas con `sha256sum -c`
- `scripts/verify-manifest.ps1` — Script de verificación (equivalente a sha256sum -c)
- `scripts/regenerate-manifest.ps1` — Script de regeneración

**Commit:** `fc25cb8` — "P10: regenerate MANIFEST.sha256 and add verification scripts"

---

## P11 — Instrumento Brooke + consentimientos (commit fa1274c)
**Cerrado por: Luis Tejada**


**Archivos creados:**
- `dataset/sus/SUS-INSTRUMENT.md` — Cuestionario System Usability Scale (Brooke 1996), 10 ítems
- `dataset/sus/CONSENT-FORM.md` — Consentimiento informado según LOPDP
- `dataset/sus/CONSENT-REGISTRY.md` — Registro de consentimiento de 15 participantes

**Commit:** `fa1274c` — "P11: add SUS instrument, consent form, and acceptance registry"

---

## Auditoría externa y correcciones (2026-09-16)
**Realizado por: Luis Alejandro Tejada Bajaña (Alxjandr07 / ltejadab@uteq.edu.ec), sesión de auditoría**

Se atendió una guía de evaluación externa que encontró varios problemas
sobre el estado declarado "cerrado" de este documento. Resumen (ver
`VERIFICACION.md` para el detalle completo con comandos y salidas
literales):

- **Contrato backend/frontend roto (CRÍTICO):** el frontend Angular seguía
  usando campos en español (`.rol`, `.nombre`, `.nombres`, `.apellidos`,
  `.cedula`) contra un backend ya renombrado a inglés. Corregido en
  `auth.model.ts`, `usuario.model.ts`, `conductor.model.ts`, `auth.ts`,
  `usuario-formulario.ts`, `usuario-lista.html`, `conductor-formulario.ts`,
  `conductor-lista.html`, `overview.ts`, `shell.ts`, `shell.html`.
- **P11 (consentimientos SUS, riesgo de Piso 3):** se encontraron dos
  registros de consentimiento contradictorios entre sí y con el historial
  de git (`CONSENT-REGISTRY.md` declara firma el 2026-08-15, pero la
  evaluación de P01-P10 ya se había reportado el 2026-07-30). No existe
  ninguna constancia de consentimiento verificable en el repositorio, solo
  un campo autodeclarado dentro de las propias respuestas del SUS. Se
  documentó todo honestamente en
  `docs/etica/consentimientos/CONSENT-STATUS.md` (nuevo) sin inventar
  fechas ni constancias. **P11 no puede calificarse al máximo.**
- **P2:** `scripts/perf/recalcular-contraste.py` tenía `assert p < 0.05` y
  `assert d == -1.0` hardcodeados y comparaba `.max()` contra `.avg()`
  (métricas distintas). Reescrito para calcular honestamente con la misma
  métrica en ambas condiciones.
- **P5:** el checker `scripts/check-spanish-methods.py` tenía un regex
  anclado con `\b` que nunca detectaba compuestos camelCase
  (`extraerEmail`). Reescrito con tokenización camelCase. Se corrigieron
  los identificadores en español detectados en `src/main/java`
  (`JwtService`, `mapearAResponse` en 5 servicios, nombres de campos
  `conductorRepository`/`vehiculoRepository`/etc., `findWithDetalle`), se
  renombraron 15 clases de test y varios métodos de test en una primera
  pasada, y en una segunda pasada se completaron los **71 métodos de test
  no-ABD restantes** más **6 clases de test ABD mal nombradas**
  (`ConductorAbdControllerTest`→`AbdDriverControllerTest`, etc. — sus
  propias clases de producción ya usaban el patrón `Abd`+inglés, así que
  el nombre en español no era una convención deliberada sino una
  inconsistencia real) y **12 métodos dentro de esas clases**. Se corrigió
  además un falso positivo del propio checker (`"terminal"` es palabra
  inglesa válida, se quitó de la lista de raíces). **Resultado final: 0%
  en todas las categorías (main, test y combinado, tanto métodos como
  tipos) — por debajo del umbral del 5%.** Los campos de **datos** del
  módulo ABD (`cedula`, `nombres`, `idConductor`, etc. dentro de
  `AbdDtos`/`AbdDriver`/etc.) sí siguen siendo una convención deliberada
  del backend paralelo en español de ese módulo y no se tocaron.
- **P6:** la cifra "226/226" contaba las declaraciones `record` (DTOs)
  como si fueran métodos. Conteo real (excluyendo records):
  **189 métodos**, 100% con Javadoc.
- Secretos: se redactó un `access_token`/`refresh_token` real de sesión
  admin versionado en `docs/mediciones/sec/live-session/login-response.txt`
  (no se pudo revocar en el servidor real desde este entorno — queda como
  acción pendiente para quien tenga acceso a Render/Redis en producción);
  el 2026-09-16 se generó evidencia EN VIVO nueva (`login-response-20260916.txt`,
  `sin-sesion-403-20260916.txt`, `asignaciones-200-20260916.json`,
  `conductor-alta-201-20260916.json`), contra un stack local levantado a
  propósito (Postgres+Redis en Docker, backend con `./mvnw spring-boot:run`,
  frontend con `ng serve`) y sobre el código ya corregido, con los tokens
  reales truncados/redactados desde el primer momento — ver
  `docs/mediciones/sec/live-session/README.md`;
  se quitaron credenciales hardcodeadas de `login.ts` y `k6/script.js`; se
  eliminó un `@Value` muerto (`app.cookie.secure`) en `AuthController`.
- Se corrigieron atribuciones de P3 y P8 en este mismo documento (ver
  arriba) contrastando cada commit citado contra `git log`.
- Se limpiaron archivos sueltos sin usar en la raíz del repo
  (`contribs_ascii.txt`, `contribs_ascii_part2.txt`, `fix.py`,
  `fix_contrib.py`, `p236.txt`) que eran intentos previos de reescribir
  este archivo.

**`make verify` pasa en verde de verdad (2026-09-17, `ALL CHECKS PASSED`,
exit code 0)**, tras completar el renombrado P5 en `src/test/java` y
regenerar las dos entradas de `dataset/MANIFEST.sha256` que cambiaron por
la corrección honesta de contenido de P11 (`CONSENT-FORM.md` y
`CONSENT-REGISTRY.md`). En una corrida intermedia (2026-09-16) el mismo
comando falló legítimamente en la categoría de métodos de test en español
de P5 — ese fallo real no se ocultó ni se maquilló, se corrigió antes de
declarar el punto cerrado. Ver `VERIFICACION.md` para la salida literal
completa de ambas corridas.

---

## Verificación

Para verificar que todo funciona, ejecutar:
```bash
make verify
```

O manualmente:
```bash
# Tests unitarios
./mvnw test -Dtest="!SgroasApplicationTests,!SecurityConfigTest,!StoredProcedureIntegrationTest"

# Manifest
powershell -ExecutionPolicy Bypass -File scripts/verify-manifest.ps1
```

---

## Tag v1.1.0

El tag `v1.1.0` apunta a `8398462`, el último commit de la cadena de cierre
(contenido sustantivo en `51202f5`; los commits posteriores corrigieron el
pipeline CI definiendo `JWT_SECRET` y el owner dinámico de `ghcr.io`, hicieron
los checks P5/P6 portables a Linux y regeneraron `dataset/MANIFEST.sha256`
normalizado a LF para que `sha256sum -c` dé 283 OK en cualquier plataforma);
incluye traducir los métodos de test al inglés, Javadoc
226/226, la firma EV-4 con correos institucionales, la evidencia de sesión en vivo,
el expediente literal de verificación y el contraste no paramétrico reproducible
con `scripts/perf/recalcular-contraste.py`. Se comprueba con `git rev-parse v1.1.0`.

---

## Situación del equipo en el examen suspenso (aclaración 2026-09-1X)

María del Rosario Escudero Plaza y Kevin Moisés Castro Espinoza ya aprobaron
su evaluación individual y no están rindiendo el examen suspenso en esta
ronda. Según indica Luis Tejada, la rúbrica del examen suspenso establece que,
para los integrantes que ya aprobaron, seguir haciendo commits en esta etapa
es opcional — **esta afirmación sobre el contenido de la rúbrica no ha sido
verificada de forma independiente en este expediente** (no se citó ni se
adjuntó el texto literal del documento de rúbrica); si el evaluador la
requiere, debe presentarse en la defensa oral. Por eso, de aquí en adelante
todo el trabajo de corrección de esta ronda del examen suspenso (auditoría
externa del 2026-09-16/17, ver sección anterior) fue ejecutado en solitario
por Luis Alejandro Tejada Bajaña.

Esto no borra la autoría real de María y Kevin en el proyecto original: sus
aportes históricos (CSV de datos SUS, corridas de Lighthouse, evidencia de
asignaciones, commit que hizo configurable la cookie, etc.) siguen
reconocidos en las secciones P3 y P8 de este documento, con el respaldo de
`git log` correspondiente. Lo que cambia es que **no firman esta ronda de
correcciones del examen suspenso**, porque no participaron en ella ni la
revisaron.

## Firmas

Declaro que los puntos de esta ronda de correcciones del examen suspenso
(sección "Auditoría externa y correcciones (2026-09-16)" y todo lo posterior
en este documento) fueron cerrados por mí, con los archivos y commits
indicados en cada sección.

| Integrante | Correo institucional | Rol en esta ronda | Firma |
|---|---|---|---|
| Luis Alejandro Tejada Bajaña | ltejadab@uteq.edu.ec | Responsable único de la corrección del examen suspenso | Luis Alejandro Tejada Bajaña |
| María del Rosario Escudero Plaza | mescuderop@uteq.edu.ec | Ya aprobó su evaluación; no participa en esta ronda | — (no firma esta ronda) |
| Kevin Moisés Castro Espinoza | kcastroe2@uteq.edu.ec | Ya aprobó su evaluación; no participa en esta ronda | — (no firma esta ronda) |
