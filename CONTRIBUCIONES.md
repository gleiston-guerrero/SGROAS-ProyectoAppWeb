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
- `.env.example` — Archivo de ejemplo con placeholders `<ROTATED_...>` (corregido
  2026-09-17: este documento decía "placeholders CHANGE_ME", que nunca
  existieron en el archivo real — otro defecto que la guía externa ya había
  señalado y no se había corregido)
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
- `scripts/check-javadoc.py` — 248/248 métodos documentados (100%), ≥ 90%
  (cifra corregida el 2026-09-16 en una segunda re-verificación; ver
  "Auditoría externa y correcciones" más abajo)

---

## P6 — Javadoc >= 90% (commit d2b88b7)
**Cerrado por: Luis Tejada**

**Archivos:** cifra original declarada "226 metodos publicos... documentados
(100%)". Esta cifra resultó estar mal calculada por el checker de esa
época (ver corrección de 2026-09-16 en "Auditoría externa y correcciones":
cifra real final tras dos rondas de correcciones al checker y
documentación real de los métodos faltantes: **248/248 (100%)**).

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
Rosario Escudero Plaza (`charito20`, commit `e8d7e2f`, 2026-07-30) con los
primeros 10 participantes; el script que lo procesa fue escrito despues por
Luis Tejada. Se corrige la atribución para reconocer ambas partes en vez de
dar todo el crédito a quien escribió solo el script derivado.

**Adición (2026-09-17):** los datos de P11–P15 (agregados al mismo CSV y a
los archivos `P11.json`…`P15.json`) también son autoría de María del
Rosario Escudero Plaza, no de Luis Tejada ni de Kevin Castro:

```
$ git log --format='%H %ad %an %ae %s' --date=short --diff-filter=A -- "docs/mediciones/sus/P11.json"
351601997ba1b5a6182ab3f5a13cd9b3c08b3403 2026-09-06 charito20 mescuderop@uteq.edu.ec feat(sus): agrega P11-P15 - n=15, media=68.5, IC95% [60.76; 76.24] (guia docente 3.7)
```

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
  como si fueran métodos, y además excluía los métodos de interfaz (los
  repositorios Spring Data JPA son interfaces, sus métodos no llevan
  `public`/`protected` explícito, así que el regex original ni los veía).
  Al reescribir el checker para contar clases e interfaces sin contar
  records, la cobertura real cayó a **215/281 (76.5%)**, por debajo del
  90% — 66 líneas reportadas como "sin Javadoc". Al revisar esas 66 líneas
  una por una se encontró un **segundo defecto en la reescritura**: 42 de
  esas 66 eran falsos positivos causados por líneas de continuación de
  SQL/JPQL dentro de bloques `@Query("""...""")` multilínea, que el regex
  de "método implícito de interfaz" confundía con firmas de método, y que
  además rompían la búsqueda del Javadoc inmediato superior (se detenía en
  la línea de SQL en vez de seguir subiendo hasta el comentario real). Se
  corrigió `check-javadoc.py` una vez más (función
  `_classify_annotation_lines`, que marca las líneas de una anotación
  multilínea completa) y **se documentaron con Javadoc real y específico
  los 24 métodos que sí carecían de él de verdad** — todos interfaces de
  repositorio (`DriverRepository`, `IncidentRepository`,
  `RouteAssignmentRepository`, `RouteRepository`, `VehicleRepository`,
  `VerificationCodeRepository`), describiendo qué filtra o pagina cada
  consulta derivada o `@Query`/`@Procedure`, sin inventar comportamiento.
  **Cifra final honesta, verificada con `python3 scripts/check-javadoc.py`:
  248/248 (100%).** No se ajustó el checker para "hacerlo pasar": los dos
  defectos corregidos (records/interfaces primero, anotaciones
  multilínea después) eran errores reales de parsing que se detectaron
  inspeccionando manualmente el código fuente reportado como problemático,
  y los 24 métodos genuinamente sin documentar se documentaron uno por uno
  con Javadoc honesto, no genérico. Detalle completo con el listado de los
  24 métodos y el razonamiento línea por línea en `VERIFICACION.md`.
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

## Tag v1.1.0 (histórico, corregido 2026-09-17)

**Corrección (2026-09-17):** este documento decía que el tag `v1.1.0`
apuntaba a `8398462` — la guía de evaluación externa ya había señalado esto
como un defecto de EV-1 ("declara que la etiqueta apunta a `8398462`, pero
apunta a `f2fb883`"), y nunca se había corregido hasta ahora. Verificado:

```
$ git rev-parse v1.1.0
847ee442a69660bf76a841fc5d711b56bf21f9cf
$ git rev-parse v1.1.0^{commit}
f2fb8838dbc8762df9b2a8bb1800d77c13ee2368
$ git log -1 --format='%H %s' 8398462
8398462d04c675b6ddb3bee8bf8393bbbef5af95 fix(P10): regenerar MANIFEST sobre Jacoco CSV normalizado a LF
```

El tag `v1.1.0` apunta realmente a `f2fb883` ("docs(portada): commit final
f7b9b72 en portada; PDF re-generado con hash correcto"), no a `8398462`
(un commit anterior en la misma cadena). Esta es la versión que evaluó la
guía externa (`Evaluacion_SGROAS.md`), con los defectos allí descritos:
contrato backend/frontend roto, P5/P6 mal contados, P11 con riesgo de
Piso 3, secretos versionados, etc.

## Tag v1.1.1 (2026-09-17, histórico) — primera ronda de correcciones sobre v1.1.0

Apuntaba al commit con la primera ronda de correcciones: contrato
backend/frontend reparado, P2 recalculado sin resultados fijados de
antemano, P5 al 0% real (main y test), P6 al 100% real contando interfaces
(248/248), secretos redactados/removidos, evidencia en vivo nueva de P4/P9.
**En ese momento P11 seguía documentado como riesgo de Piso 3 NO resuelto**
(sin constancias reales localizadas todavía). Superado por `v1.1.2` (ver
abajo); se conserva como referencia histórica y no se mueve ni se borra.

## Tag v1.1.2 (2026-09-17, historico) — resuelve P11 con una fecha luego corregida

Apunta al commit final de esta auditoría, que agrega sobre `v1.1.1`:

- **P11: riesgo de Piso 3 RESUELTO.** Se localizaron y verificaron los 15
  formularios de consentimiento firmados en papel (P01–P15): SHA-256 de
  cada uno (verificado con `sha256sum` y `hashlib.sha256`, resultados
  idénticos), fecha de firma legible extraída con `pdftotext`: P01–P10
  firmados 2026-07-24 (evaluados 2026-07-30), P11–P15 firmados 2026-07-26
  (evaluados 2026-08-16, según declaración del equipo). Ambos grupos con
  consentimiento previo a su evaluación. Los documentos originales **no**
  se versionaron (contienen firmas manuscritas); el registro público
  (`dataset/sus/CONSENT-REGISTRY.md`) solo publica el hash de cada uno.
- **EV-1 adicional:** salida de P10 recortada con "..." (reemplazada por
  las 283 líneas literales), placeholders `CHANGE_ME` citados que nunca
  existieron, y una mención desactualizada de que este documento está
  "firmado por los tres integrantes".

Se comprueba con:

```
git rev-parse v1.1.2^{commit}
git log --oneline v1.1.2 -1
```

**No se regeneró el PDF de `docs/informe-final/` ni se cambió su portada**
(`docs/informe-final/portada.tex` sigue citando el commit `f7b9b72` de la
entrega final original): esa portada documenta la entrega final del curso,
un hito distinto de esta ronda de correcciones del examen suspenso, y
regenerar un PDF de 98 páginas no era necesario para corregir los defectos
señalados por la guía (que son de código, scripts y expedientes, no de
contenido del informe). Si el evaluador requiere que la portada del informe
también cite `v1.1.2`, es un paso pendiente adicional, no incluido en esta
ronda.

**Nota de autorreferencia honesta (histórica):** cuando se creó `v1.1.2`,
este documento todavía citaba `v1.1.1` en sus propias secciones de "Tag"
(el mismo problema de autorreferencia que ya se explicaba aquí). Esa
inconsistencia de referencias cruzadas, por sí sola, no ameritaba un tag
nuevo. Lo que sí lo ameritó fue encontrar, poco después, que la fecha de
evaluación de P11-P15 usada en `v1.1.2` (2026-08-03) era incorrecta —ver
"Tag v1.1.3" abajo—, así que el tag nuevo se creó por esa corrección
sustantiva, no por las referencias cruzadas.

## Tag v1.1.3 (2026-09-17) — vigente: corrige la fecha real de evaluación de P11-P15

Apunta al commit final de esta auditoría. Corrige sobre `v1.1.2`:

- La fecha de evaluación de P11-P15 declarada por el equipo: **2026-08-16**
  (no 2026-08-03, dato provisional usado por error en `v1.1.2`).
- Un error propio de esta auditoría: se había generalizado que los
  archivos `P11.json`…`P15.json` se subieron al repositorio "el 2026-08-16"
  (esa fecha corresponde solo a `P01.json`…`P10.json`). Verificado
  individualmente, `P11.json`…`P15.json` se suben el **2026-09-06**
  (commit `3516019`, autoría de María del Rosario Escudero Plaza —
  atribución agregada en la sección P8 de este documento).

Se comprueba con:

```
git rev-parse v1.1.3^{commit}
git log --oneline v1.1.3 -1
```

---

## Situación del equipo en el examen suspenso (aclaración 2026-09-1X)

María del Rosario Escudero Plaza y Kevin Moisés Castro Espinoza ya aprobaron
su evaluación individual y no están rindiendo el examen suspenso en esta
ronda. Esto se puede verificar de forma objetiva en el propio historial de
git: ninguno de los dos tiene commits posteriores al commit `110d5a1`
(13-sep-2026, fecha en que se revisó la guía de evaluación) ni durante esta
ronda de correcciones (2026-09-16/17):

```
$ git log --format='%an <%ae> %ad' --date=short 110d5a1..HEAD | grep -iE "escudero|castro|mescuderop|kcastroe|0999595561kevin" 
(sin resultados)
```

Luis Tejada afirma que, según la rúbrica del examen suspenso, para los
integrantes que ya aprobaron individualmente seguir haciendo commits en
esta etapa es opcional. **Esta evaluación es automatizada y no incluye una
defensa oral**, por lo que esta afirmación sobre el contenido de la rúbrica
no puede sustentarse verbalmente en ningún momento posterior: queda
registrada aquí como una declaración de Luis Tejada, sin el respaldo de una
cita textual del documento de rúbrica (que no se adjuntó a este
repositorio). Lo que sí es objetivamente verificable con el comando de
arriba es que ninguno de los dos hizo trabajo en esta ronda; lo que no es
verificable desde este repositorio es si la rúbrica realmente exime de esa
obligación a quienes ya aprobaron. Por eso, de aquí en adelante todo el
trabajo de corrección de esta ronda del examen suspenso (auditoría externa
del 2026-09-16/17, ver sección anterior) fue ejecutado en solitario por
Luis Alejandro Tejada Bajaña.

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
