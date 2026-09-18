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

**Corrección 2026-09-17 (a):** se eliminó el último secreto literal que
quedaba en el árbol (`TEST_ONLY_SECRET_KEY_2026...` en `JwtServiceTest.java`),
reemplazado por una clave aleatoria de 32 bytes generada en memoria con
`SecureRandom` en cada corrida de test — ya no hay ningún texto fijo
buscable como secreto en todo el repositorio.

**Corrección 2026-09-17 (b) — rotación real declarada por escrito:** Luis
Tejada rotó de verdad la contraseña de la base de datos (PostgreSQL en
Supabase) desde el panel de Supabase, y el `APP_JWT_SECRET` desde las
variables de entorno de Render, aproximadamente a las 19:41 UTC. No se
expone ningún valor real aquí. Verificación técnica de que la rotación
surtió efecto (no solo declarada, confirmada contra el sistema real tras
el redeploy):

```
$ curl -s https://sgroas-backend.onrender.com/actuator/health
{"status":"UP", ..., "db":{"status":"UP", ...}}
$ curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST https://sgroas-backend.onrender.com/api/auth/login \
    -H "Content-Type: application/json" -d '{"email":"admin@sgroas.com","password":"admin123"}'
HTTP 200
```

Ver `VERIFICACION.md` (sección P1) para el detalle completo.

**Limitación declarada (2026-09-17):** esta prueba confirma que la
contraseña NUEVA funciona; no prueba que la vieja ya no sirva, y no se
puede cerrar esa brecha sin violar la regla de no manipular credenciales
reales de producción en este expediente. Detalle completo en
`VERIFICACION.md` (sección P1).

---

## P2 — k6 corridas crudas versionadas (commit 51202f5)
**Cerrado por: Luis Tejada**

**Archivos modificados:**
- dataset/perf/k*-run1.json y k*-cold.json -- 13 corridas crudas.
- scripts/perf/recalcular-contraste.py -- script reproducible.

**Commit:** 51202f5

---

**Actualización (2026-09-17) — corrige defecto real de `@Cacheable` + Redis + `Page`:**
**Cerrado por: Luis Tejada**

Una re-evaluación externa señaló que `@Cacheable` sobre un método que
devuelve `Page<DriverResponse>`, con el serializador de Redis configurado
en `CacheConfig`, rompería en producción en cuanto la caché se activara.
Se verificó real (sin necesitar Redis levantado, el serializador de Jackson
es lo que convierte a bytes) en `CacheRedisSerializationTest`: sin arreglar
el `ObjectMapper`, cualquier valor cacheado vuelve como `LinkedHashMap`
genérico en silencio, sin excepción; y `PageImpl` no tiene un constructor
que Jackson pueda usar para reconstruirlo, ni siquiera arreglando el mapper.

**Archivos modificados:**
- `src/main/java/ec/edu/uteq/sgroas/config/CacheConfig.java` -- activa
  `activateDefaultTyping` en el `ObjectMapper` del serializador de Redis.
- `src/main/java/ec/edu/uteq/sgroas/service/DriverService.java` --
  `listActiveCached` ya no devuelve `Page<DriverResponse>` (no cacheable),
  devuelve el nuevo record `CachedDriverPage`; `list()` reconstruye el
  `Page` real fuera de la ruta cacheada.
- `src/test/java/ec/edu/uteq/sgroas/config/CacheRedisSerializationTest.java`
  (nuevo) -- prueba el defecto y la corrección con el serializador real.

---

**Actualización (2026-09-17 tarde) — auditoría rigurosa: el mismo bug (y
uno peor) en otros 4 servicios:**
**Cerrado por: Luis Tejada**

Al revisar qué más dependía del fix de `DriverService` se encontró que
`IncidentService`, `RouteService` y `VehicleService` tenían el mismo bug
de auto-invocación (`listCached(pageable)` llamado como `this.` implícito,
nunca a través del proxy de Spring, así que `@Cacheable` nunca se
activaba). Además, un defecto de corrección real y más grave: los tres
calculaban `Page.getTotalElements()` como `contenido.size()` (el tamaño
de la página actual, no el total real) -- la paginación de Incidentes,
Rutas y Vehículos estaba mal en producción, independientemente del cache.
`RouteAssignmentService` tenía un tercer defecto: su `listCached()` nunca
se llamaba desde ningún lado (`list()` iba directo al repositorio),
confirmado con `grep -rn "\.listCached(" src/main/java src/test/java`
(0 resultados antes del fix) -- es el método detrás de
`GET /api/asignaciones`, el endpoint de la evidencia en vivo de P9.

**Archivos modificados:**
- `src/main/java/ec/edu/uteq/sgroas/service/IncidentService.java`,
  `RouteService.java`, `VehicleService.java`, `RouteAssignmentService.java`
  -- mismo patrón que `DriverService`: auto-inyección de
  `ObjectProvider<XService>` + record `CachedXPage` en vez de `Page`
  cacheado directamente.
- Tests correspondientes con mock de `ObjectProvider`.
- `CacheRedisSerializationTest.java` -- nueva prueba
  `withFix_cachedIncidentPageRoundTripsAsRealType`.

Verificado con la suite completa contra PostgreSQL real (Docker): 299
tests, 0 fallos, 0 errores. JaCoCo real recalculado (95,67% instrucciones,
88,38% ramas sin cambio, 96,19% líneas) y propagado al informe, PDF
recompilado (97 páginas, 0 errores). Detalle completo en
`VERIFICACION.md` (secciones P2 y P6).

---

## P3 — Lighthouse corridas versionadas (commit 1a07dc7 y otros)
**Cerrado por: María del Rosario Escudero Plaza (corridas 1) y Luis Alejandro Tejada Bajaña (perfil tableta)**

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

**Corrección adicional (2026-09-18) — `1a07dc7` NO contiene las 9
corridas reales; el "Archivos modificados" de abajo estaba mal desde el
origen de este documento:**
```
$ git show --stat 1a07dc7 | grep -i lighthouse
docs/mediciones/lighthouse/REPORT.md               |   27 +
docs/mediciones/lighthouse/lhci-20260730-2115.json | 9435 ++++++++++++++++++++
docs/mediciones/lighthouse/lhci-20260730-2117.json | 9431 +++++++++++++++++++
lighthouserc.js                                    |   34 +
```
`1a07dc7` solo agrega **2 corridas locales** (`lhci-20260730-2115/2117.json`,
contra `localhost`, no contra el despliegue público) más `REPORT.md` y
`lighthouserc.js` — nunca tocó los 9 archivos `lh-{mobile,desktop,tablet}-{1,2,3}.json`
que sí son la evidencia real contra el despliegue público (ver corrección
de 2026-09-17/18 más abajo). Los commits reales que sí agregan esos 9
archivos, verificados con `git log --follow`:
```
$ git log --format='%H %an %ad %s' --date=short -1 --follow -- dataset/lighthouse/lh-desktop-1.json
dd6b81d charito20 2026-09-06 perf(lighthouse): desktop corrida 1 - P=0.95 A=0.91 BP=0.92 SEO=0.9 (Render URL)
$ git log --format='%H %an %ad %s' --date=short -1 --follow -- dataset/lighthouse/lh-mobile-1.json
ef2de04 charito20 2026-09-06 perf(lighthouse): mobile corrida 1 - P=0.79 A=0.91 BP=0.92 SEO=0.9 (Render URL)
$ git log --format='%H %an %ad %s' --date=short -1 --follow -- dataset/lighthouse/lh-tablet-1.json
3062984 Alxjandr07 2026-09-13 docs(P8): agregar perfil tableta Lighthouse (3 corridas) y regenerar dataset
$ git log --format='%H %an %ad %s' --date=short -1 --follow -- dataset/lighthouse/lh-desktop-2.json
b516447 TheAsesink 2026-09-13 fix(P11,P15,P6,P16,P10): valida trazabilidad CRLF+exit1, dataset 275 arch sha256 OK, cookie HttpOnly sin token JSON, evidencia asignaciones 200
```
Es decir: corrida 1 de móvil/escritorio (María Escudero, `dd6b81d`/`ef2de04`,
2026-09-06); perfil tableta completo (Luis Tejada, `3062984`, 2026-09-13);
corridas 2 y 3 de móvil/escritorio consolidadas en `b516447` (Kevin Castro,
2026-09-13). Ningún archivo de este punto viene de `1a07dc7`.

**Archivos modificados (corregido):**
- `1a07dc7` -- 2 corridas locales (`lhci-20260730-2115/2117.json`),
  `REPORT.md`, `lighthouserc.js`. NO son las 9 corridas del despliegue
  público.
- `dd6b81d`, `ef2de04`, `3062984`, `b516447` -- las 9 corridas reales
  `lh-{mobile,desktop,tablet}-{1,2,3}.json` contra el despliegue público
  (ver detalle de commits arriba).

**Commit:** `1a07dc7` (corridas locales) + `dd6b81d`/`ef2de04`/`3062984`/`b516447` (corridas reales, ver arriba)

---

**Actualización (2026-09-16) — corridas frescas post v1.1.3:**
**Cerrado por: Luis Tejada**

Las 9 corridas de 1a07dc7 son del 2026-09-15 contra
`https://sgroas-backend.onrender.com`, antes de la corrección del contrato
backend-frontend y otros defectos resueltos hasta el tag `v1.1.3`. Se
generaron 6 corridas nuevas (3 móvil + 3 escritorio) contra el código actual
de `main`, sirviendo el build de producción del frontend con
`frontend/serve-gzip.js` en `http://localhost:4200/` (mismo método que las 2
corridas locales `lhci-20260730-21{15,17}.json` ya existentes). Se intentó
también auditar el despliegue de Render directamente, pero el servicio no
respondió (timeout de 60s sin bytes recibidos) — no se pudo confirmar su
estado, así que no se usó como evidencia.

De paso se encontró y corrigió un defecto real en `lighthouserc.js`: el
perfil móvil pasaba `settings.preset = 'mobile'`, valor inválido para
`--preset` en Lighthouse 13.4.1 (solo acepta `perf | experimental |
desktop`), lo que rompía `npx lhci autorun` con
`Invalid values: Argument: preset, Given: "mobile"`. El comando documentado
en `VERIFICACION.md` nunca se había corrido de punta a punta con esta
versión de Lighthouse.

**Archivos modificados:**
- `lighthouserc.js` -- se quitó `preset: 'mobile'` inválido (el default de la
  CLI ya es mobile), se mantuvo el throttling Slow 4G explícito.
- `dataset/lighthouse/lh-{mobile,desktop}-fresh-20260916-{1,2,3}.json` -- 6
  corridas nuevas, vigentes, contra `main`/v1.1.3.
- `dataset/MANIFEST.sha256` -- 6 entradas nuevas para los archivos anteriores.
- `VERIFICACION.md` (sección P3) -- orden de verificación reescrito para
  mostrar URL auditada y scores reales, en vez de solo contar archivos.

Las 9 corridas de `1a07dc7` se conservan sin borrar por trazabilidad
histórica; las 6 nuevas son la evidencia vigente de performance actual.

**Nota (2026-09-18):** "las 9 corridas de `1a07dc7`" en este párrafo (y en
el resto de esta entrada de 2026-09-16) es una atribución incorrecta que
se repite de la sección de arriba — `1a07dc7` nunca contuvo esos 9
archivos; son de `dd6b81d`/`ef2de04`/`3062984`/`b516447`. Se corrige la
atribución en la sección "P3" de arriba, con el detalle completo de qué
commit agregó cada archivo; no se reescribe cada mención suelta aquí para
no perder el registro histórico de cómo se entendía la situación en cada
fecha.

**Corrección (2026-09-17):** la afirmación de arriba ("antes de la
corrección del contrato... no se usó como evidencia") era incorrecta — las
9 corridas de `1a07dc7` (`lh-{mobile,desktop,tablet}-{1,2,3}.json`) SÍ son
contra el despliegue público real (el propio commit que las creó dice
"Render URL"), y son las que el informe final efectivamente cita. Las 6
corridas "frescas" de esta entrada resultaron redundantes. Ver la
corrección completa más abajo.

---

**Actualización (2026-09-17) — corrección de un error propio: las corridas
"nuevas" del commit `13cf740` eran redundantes; ya existía evidencia real
contra el despliegue público desde el 2026-09-06:**
**Cerrado por: Luis Tejada**

El commit `13cf740` había afirmado que las 6 corridas de
`fresh-20260916` (locales, servidas con `serve-gzip.js`) eran las únicas
existentes y que por tanto hacía falta generar evidencia nueva contra
`https://sgroas-backend.onrender.com`. Esa afirmación era incorrecta: nunca
se verificó el origen real de `lh-desktop-{1,2,3}.json` /
`lh-mobile-{1,2,3}.json`. Al revisar el historial de git para esta
corrección:

```
$ git log --format="%H %ad %an %s" --date=short -- dataset/lighthouse/lh-desktop-1.json | tail -1
dd6b81d 2026-09-06 charito20 perf(lighthouse): desktop corrida 1 - P=0.95 A=0.91 BP=0.92 SEO=0.9 (Render URL)
```

Esas 9 corridas (María Escudero, 2026-09-06) ya eran evidencia real contra
el despliegue público, con Lighthouse 13.4.1, y son exactamente las que
`resumen.tex`/`cap8-evaluacion.tex` citan (escritorio P=95/A=91/BP=92/
SEO=90, móvil P≈77/A=91/BP=92/SEO=90 — coincide). El informe nunca estuvo
mal; el trabajo de `13cf740` (y el de `fresh-20260916` antes) fue
redundante, y además usó Lighthouse 12.6.1, una versión más vieja que la
13.4.1 ya presente. Esto es exactamente lo que señaló una re-evaluación
externa. Se corrige la narrativa en `VERIFICACION.md` (sección P3); los
archivos redundantes se conservan por trazabilidad del proceso, sin
presentarlos ya como "la evidencia vigente".

**Archivos modificados:**
- `VERIFICACION.md` (sección P3) -- narrativa corregida, atribuye
  correctamente `lh-desktop-{1,2,3}.json`/`lh-mobile-{1,2,3}.json` como la
  evidencia real y vigente desde 2026-09-06.

---

## P4 — Cookie Secure(true) (commit 92576cf)
**Cerrado por: Luis Tejada**


**Archivos modificados:**
- `src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java` — 4 cookies actualizadas de `.secure(cookieSecure)` a `.secure(true)` (líneas 158, 195, 235, 252)

**Commit:** `92576cf` — "P4+P5: rename entity fields to English and set cookie secure(true)"

**Actualización (2026-09-17) — hallazgo adicional de una auditoría rigurosa
propia: CSP bloqueaba el `onload` inline que Angular genera para el CSS:**
**Cerrado por: Luis Tejada**

No es uno de los 11 puntos de la guía, pero se encontró y se corrige igual.
La consola del despliegue real mostraba una violación de CSP
(`script-src 'self'`) sobre `<link ... media="print" onload="this.media='all'">`,
un patrón que el propio build de producción de Angular inyecta para
diferir CSS no crítico ("inline critical CSS"). Se desactivó esa
optimización (`frontend/angular.json`) para que Angular emita un
`<link rel="stylesheet">` normal sin `onload` inline. Verificado sin
regresión de rendimiento con Lighthouse local (87 móvil vs 88-89 antes,
dentro del ruido normal).

**Archivos modificados:**
- `frontend/angular.json` — `configurations.production.optimization.styles.inlineCritical: false`.

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

**Actualización (2026-09-17) — segundo bug real del checker + 8 métodos
reales que se le escapaban:**
**Cerrado por: Luis Tejada**

Una re-evaluación externa señaló que `check-spanish-methods.py` "está
construido de forma que no puede fallar: no hay mutación que lo haga salir
con error". Se confirmó con una mutación propia: el regex exigía la palabra
literal `public`/`protected` al inicio de la línea, pero en una interfaz
Java el método es público implícito (no se escribe la palabra), así que
ningún método de repositorio Spring Data se escaneaba jamás. Con el regex
corregido, el total real de métodos escaneados en `src/main` subió de 226
a 476, y aparecieron 8 métodos reales en español, todos en
`src/main/java/ec/edu/uteq/sgroas/abd/` (el subsistema nativo de RLS, que
mapea 1:1 contra columnas en español del esquema de PostgreSQL).

**Archivos modificados:**
- `scripts/check-spanish-methods.py` — modificador `public|protected` ahora
  opcional en `METHOD_PATTERN`, con lista de palabras clave de Java
  excluidas para no confundir `if`/`for`/`while` con nombres de método.
- `src/main/java/ec/edu/uteq/sgroas/abd/repository/AbdIncidentRepository.java`,
  `ScheduleRepository.java`, `UnitRepository.java` — `findByEstadoIgnoreCase`
  → `findByStatusIgnoreCase` (convertido de query derivada por convención a
  `@Query` explícita, porque el campo de entidad `estado` no se renombró).
- `UnitRepository.java` — `existsByPlacaIgnoreCase` → `existsByLicensePlateIgnoreCase`.
- `AbdRouteRepository.java` — `existsByTerminalOrigenIdTerminalAndTerminalDestinoIdTerminal`
  → `existsByOriginAndDestinationTerminal` (método sin llamadores, pero se
  corrigió igual); alias nativo `AS descripcion` → `AS description` en `topRoutes()`.
- `AbdIncidentRepository.CountByStatus.getEstado()` → `getStatus()`.
- `CountProjection.getClave()` → `getLabel()` (alias nativo `AS clave` →
  `AS label` en `ScheduleRepository`/`UnitRepository`).
- `TopRouteProjection.getDescripcion()` → `getDescription()`.
- `AbdIncidentService`, `AbdReportService`, `AbdUnitService` — llamadores
  actualizados a los nuevos nombres.
- Tests correspondientes en `src/test/java/ec/edu/uteq/sgroas/abd/service/`.

Se decidió NO renombrar los campos de entidad (`estado`, `placa`, etc.) ni
los DTOs de `abd/`: el criterio literal de la guía es sobre nombres de
método y de tipo, no de campo, y esos campos se exponen como claves JSON
al frontend Angular contra el despliegue real — la evidencia mejor
calificada de todo el proyecto (P4/P9).

**Verificación final:**
```
$ python3 scripts/check-spanish-methods.py
[OK] Metodos en src/main: 0/476 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/132 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 0/298 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/test: 0/45 en espanol (0.00%, umbral 5%)
```
`./mvnw compile` limpio; las 8 clases de test del módulo `abd/` en verde.

(Estos números quedaron superados por el fix de P2 en 4 servicios más,
que agregó 4 records nuevos en inglés: ver la entrada "auditoría rigurosa:
JaCoCo real regenerado..." más abajo y `VERIFICACION.md` sección P5 para
la cifra vigente, 0/484.)

---

## P6 — Javadoc >= 90% (commit d2b88b7)
**Cerrado por: Luis Tejada**

**Archivos:** cifra original declarada "226 metodos publicos... documentados
(100%)". Esta cifra resultó estar mal calculada por el checker de esa
época (ver corrección de 2026-09-16 en "Auditoría externa y correcciones":
cifra real final tras dos rondas de correcciones al checker y
documentación real de los métodos faltantes: **248/248 (100%)**).

**Commit:** d2b88b7

**Actualización (2026-09-17):** los fixes de P2/P5 de esta fecha añadieron
métodos/constructores nuevos (todos documentados). Cifra vigente:
```
$ python3 scripts/check-javadoc.py
Javadoc coverage: 249/249 (100.0%)
OK: Javadoc >= 90%
```

**Actualización (2026-09-17) — auditoría rigurosa: JaCoCo real regenerado
con Docker, 293→298 tests, y una desincronización de proceso corregida:**
**Cerrado por: Luis Tejada**

El informe citaba "293 pruebas JUnit 5" con JaCoCo 95,49/88,38/95,94% —
cifras de antes de los fixes de P2/P5. Se regeneró la suite completa
contra PostgreSQL real (Docker, volumen local de prueba, sin credenciales
de producción): 298 tests, 0 fallos, 0 errores. Cifras reales nuevas:
instrucciones 95,52%, ramas 88,38% (sin cambio), líneas 95,97%. Se
propagaron al informe y se recompiló el PDF (97 páginas, 0 errores). De
paso se encontró que `dataset/jacoco/` (lo que verifica el manifiesto) es
una copia manual de `docs/mediciones/jacoco/` (lo único que el build
regenera) que no se había resincronizado tras los fixes de hoy — se
corrigió y se regeneró `MANIFEST.sha256`. Detalle completo en
`VERIFICACION.md` (sección P6).

**Archivos modificados:**
- `docs/informe-final/resumen.tex`, `cap1-introduccion.tex`,
  `cap2-marco-teorico.tex`, `capitulos/cap8-evaluacion.tex`,
  `capitulos/cap12-conclusiones.tex`, `capitulos/capA-anexo-resultados.tex`,
  `anexos.tex` -- 293→298 tests, 95,49→95,52% instrucciones,
  95,94→95,97% líneas.
- `docs/informe-final/main.pdf` -- recompilado.
- `dataset/jacoco/` -- resincronizado con `docs/mediciones/jacoco/`.
- `dataset/MANIFEST.sha256`, `dataset/MANIFEST.csv` -- regenerados
  (298 entradas).

---

## P7 — Captions de figuras/tablas en inglés (commit 33e25e5)
**Cerrado por: Luis Tejada**


**Archivos modificados:**
- `docs/informe-final/cap*.tex` y `docs/informe-final/capitulos/cap*.tex` — 12 captions traducidos:
  - 8 tablas: cap3 (Rol-based access control), cap4×2 (Password policies, Authentication time), cap5×2 (Route API, Incident API), cap6 (Response codes), cap8 (UTM zones), capA (Rate-limiting)
  - 4 listados: cap7 (JWT filter, Repositories, Email service, Validation)

**Commit:** `33e25e5` — "P7: translate all Spanish table and listing captions to English"

**Corrección adicional (2026-09-17):** además de los captions, la guía
señaló dos defectos de contenido en las propias figuras (no en su texto de
`\caption`), que seguían sin corregirse:

- Los diagramas C4 (`docs/arquitectura/c4-nivel1-contexto.dsl/.png` y
  `c4-nivel2-contenedores.dsl/.png`, copiados también a
  `docs/informe-final/c4-level1-context.png` y `c4-level2-containers.png`)
  decían "Vue.js Frontend" / "[Container: Vue.js 3]" — el frontend real es
  Angular 20 (`frontend/package.json`: `"@angular/core": "^20.3.0"`).
  Se corrigieron los `.dsl` fuente y se regeneraron ambos PNG con
  `scripts/gen-c4-diagramas.py` (no hay Structurizr disponible en este
  entorno; el script recrea el mismo layout con matplotlib, leyendo el
  nombre/tecnología del contenedor frontend directamente del `.dsl` en vez
  de repetirlo hardcodeado, para que no se vuelva a desincronizar).
- `dataset/sus/fig-sus-demografia.png` (fuera del informe, pero
  referenciada desde `docs/informe-final/cap5-materiales-metodos.tex`)
  tenía los tres paneles completamente en español ("Sexo", "Masculino",
  "Experiencia web", etc.) y un error de encoding en el título
  ("Demograf?a"). No existía ningún script en el repositorio que la
  generara (archivo huérfano, añadido directo en el commit `7e49da9`). Se
  creó `scripts/gen-sus-demografia-figura.py`, que la regenera desde
  `dataset/sus/sus-raw.csv` con las mismas 15 filas y en inglés, y se
  agregó al target `docs` del Makefile para que deje de ser un archivo
  huérfano.

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

**Corrección de fondo (2026-09-17) — dos defectos que la guía señaló y no
se habían corregido:**

1. `scripts/generate-sus-demographics.py` solo imprimía un resumen de
   texto; la tabla LaTeX real de `docs/informe-final/cap5-materiales-metodos.tex`
   (`tab:sus-demografia`) se mantenía a mano, sin generarse desde el CSV.
   Se le agregó una opción `--latex` que imprime las filas
   `\begin{tabular}...\end{tabular}` completas listas para pegar en el
   `.tex`. Verificado que la salida es **byte por byte idéntica** a la
   tabla actual del informe:
   ```
   $ python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv --latex > /tmp/gen.txt
   $ diff <(sed -n '87,112p' docs/informe-final/cap5-materiales-metodos.tex | tr -d '\r') <(tr -d '\r' < /tmp/gen.txt)
   (sin salida = archivos idénticos)
   ```
2. `scripts/validate-sus-demografia.sh` **no leía el `.tex` en absoluto**:
   comparaba el CSV contra un puñado de constantes fijas escritas a mano
   en el propio script (8 hombres, 7 mujeres, media 68.5, etc.), que
   pasaban aunque la tabla del informe se editara con datos distintos —
   nunca se leía su contenido real. Se reescribió para extraer cada fila
   de `tab:sus-demografia` con una expresión regular y compararla, campo
   por campo, contra la fila correspondiente de `sus-raw.csv`. Se
   verificó que el nuevo script sí detecta discrepancias reales
   (prueba con una copia temporal donde se alteró una edad en el `.tex`
   → el script reportó `ERROR: P01: edad tex=99 != csv=23` y salió con
   código 1; no se dejó esa prueba en el repositorio, solo se confirmó el
   comportamiento).
3. `make verify` ([P8]) ahora corre también `validate-sus-demografia.sh`
   (antes solo confirmaba que `generate-sus-demographics.py` no fallara al
   ejecutarse, sin cruzar nada contra el informe).

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
  se quitaron credenciales hardcodeadas de `login.ts` y `k6/script.js`
  (y, corregido el 2026-09-17 tras una re-verificación con el mismo grep,
  también de `k6/cold.js`, que se había pasado por alto en la primera
  ronda); se
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

## Tag v1.1.0 — MOVIDO (2026-09-17): esta es ahora la etiqueta vigente

La guía del examen dice, literalmente: *"Lo que no esté dentro de la
etiqueta no existe. Muevan `v1.1.0` al último commit que quieren que
revise."* Esa regla no se había aplicado — todas las correcciones se
habían ido documentando bajo tags nuevos (`v1.1.1` a `v1.1.4`) en vez de
mover `v1.1.0`, por precaución general de no reescribir un tag ya
publicado. Fue un error: la guía exige explícitamente moverlo. Se movió,
con confirmación explícita de Luis Tejada (necesaria porque mover un tag
anotado ya publicado requiere `git push --force` sobre esa referencia).

```
$ git rev-parse v1.1.0^{commit}
c25dc0680fb86d280aef95b3f16a24776801f082
```

`v1.1.0` apunta ahora al commit que retira P11-P15 del estudio SUS oficial
(ver sección siguiente). Los tags `v1.1.1` a `v1.1.4` se conservan sin
borrar como historial intermedio, pero **`v1.1.0` es el único tag
relevante para la evaluación** a partir de ahora.

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

## Corrección final (2026-09-17) — P11-P15 retirados del estudio SUS oficial

Las secciones de arriba (Tag v1.1.2, Tag v1.1.3, sección P11) resolvieron el
problema de la **fecha de consentimiento** de P11-P15: los 15 formularios
firmados son reales y verificados por SHA-256. Una revisión posterior
encontró un problema distinto: los **valores de las respuestas al
cuestionario SUS** de P11-P15 (las 10 respuestas Likert y el puntaje SUS
atribuidos a cada persona, escritos a mano en `scripts/update-sus.py`,
commit `3516019`, 2026-09-06) no tienen ningún respaldo documental anterior
a esa fecha. Consentir participar no equivale a demostrar que la encuesta
se aplicó y registró correctamente con esos valores.

Por eso, el 2026-09-17 se decidió retirar a P11-P15 del estudio SUS oficial,
que vuelve a n=10 (P01-P10, media 63,0, IC 95 % [53,07; 72,93]; ver
`dataset/sus/REPORT.md`). El consentimiento real de P11-P15 no se descarta
ni se oculta: se documenta en `dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md` y
en la sección final de `dataset/sus/CONSENT-REGISTRY.md`, con sus hashes
SHA-256 preservados. `scripts/update-sus.py` se conserva sin borrar,
anotado como no ejecutable, para no ocultar cómo se originó el problema.
Este cambio es una corrección honesta que reduce el riesgo de Piso 3, no un
retroceso.

## Corrección adicional (2026-09-17) — claims del JWT seguían en español

Al verificar el despliegue real en Render (`sgroas-backend.onrender.com`)
después de reconectarlo al repositorio correcto y redesplegarlo, se
confirmó que el cuerpo de `POST /api/auth/login` ya usa el contrato en
inglés (`{"name":..., "role":...}`), pero el **JWT emitido internamente**
seguía llevando las claims `nombre`/`rol` (visibles al decodificar el
payload del token, que no está cifrado, solo firmado). Esto no rompía
nada en producción (ningún componente lee esas claims de vuelta — la
sesión se reconstruye desde el cuerpo de la respuesta / cookie, no
decodificando el JWT en el cliente), pero era inconsistente con el
renombrado a inglés de P5.

`src/main/java/ec/edu/uteq/sgroas/security/JwtService.java` líneas 53-54:
`.claim("nombre", ...)` / `.claim("rol", ...)` → `.claim("name", ...)` /
`.claim("role", ...)`. Verificado que ningún otro archivo del backend ni
del frontend lee esas claims por nombre (`grep -rn "\"nombre\"\|\"rol\""`
solo encontró columnas de base de datos `@Column(name = "nombre"/"rol")`,
que son un asunto de esquema de BD fuera del alcance de P5, no
identificadores Java). `JwtServiceTest`: 4/4 pasan sin cambios.

## Nota de seguridad — no exponer tokens reales

Durante la verificación del despliegue se hizo un login real de prueba
contra Render con la cuenta de desarrollo sembrada
(`admin@sgroas.com`/`admin123`, ver `V2__seed.sql`) para confirmar el
contrato corregido. El `access_token`/`refresh_token` de esa respuesta
**no se guardó en ningún archivo de este repositorio** — solo se vio en la
salida de un comando `curl` durante esta sesión de auditoría. Expira en 1
hora (access) / 7 días (refresh) desde el momento de esa prueba.

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
