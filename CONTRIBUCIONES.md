# CONTRIBUCIONES — SGROAS Supletorio v1.1.0

Estudiante: Luis Tejada
Repositorio: https://github.com/gleiston-guerrero/SGROAS-ProyectoAppWeb
Período: Supletorio 2026-09
Integrantes: Luis Alejandro Tejada Bajaña, María del Rosario Escudero Plaza, Kevin Moisés Castro Espinoza

---

## P1 — Passwords/secrets a variables de entorno (commit ac72f7a)

**Archivos modificados:**
- `src/main/resources/application.properties` — Reemplazado password hardcodeado por `${SPRING_DATASOURCE_PASSWORD}` y `${JWT_SECRET}`
- `docker-compose.yml` — Todos los secrets ahora usan referencias a variables de entorno
- `.env.example` — Archivo de ejemplo con placeholders CHANGE_ME
- `src/test/java/ec/edu/uteq/sgroas/security/JwtServiceTest.java` — JWT_SECRET desde System.getenv() con fallback de test

**Commit:** `ac72f7a` — "P1: remove hardcoded DB password and JWT secret from source"

---

## P4 — Cookie Secure(true) (commit 92576cf)

**Archivos modificados:**
- `src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java` — 4 cookies actualizadas de `.secure(cookieSecure)` a `.secure(true)` (líneas 158, 195, 235, 252)

**Commit:** `92576cf` — "P4+P5: rename entity fields to English and set cookie secure(true)"

---

## P5 — Renombramiento de campos entidades (commit c09f981)

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

## P7 — Captions de figuras/tablas en inglés (commit 33e25e5)

**Archivos modificados:**
- `docs/informe-final/cap*.tex` y `docs/informe-final/capitulos/cap*.tex` — 12 captions traducidos:
  - 8 tablas: cap3 (Rol-based access control), cap4×2 (Password policies, Authentication time), cap5×2 (Route API, Incident API), cap6 (Response codes), cap8 (UTM zones), capA (Rate-limiting)
  - 4 listados: cap7 (JWT filter, Repositories, Email service, Validation)

**Commit:** `33e25e5` — "P7: translate all Spanish table and listing captions to English"

---

## P8 — Script demografía SUS (commit 5fa09b4)

**Archivos creados:**
- `scripts/generate-sus-demographics.py` — Lee `dataset/sus/sus-raw.csv`, genera tabla de demografía (15 participantes, 8M/7F, edades 19-25, SUS mean=68.5)
- `dataset/sus/sus-raw.csv` — Datos crudos de participantes

**Commit:** `5fa09b4` — "P8: add script to generate SUS demographics from raw CSV"

---

## P9 — Postman CRUD asignaciones (commit 20a487f)

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

**Archivos creados/modificados:**
- `dataset/MANIFEST.sha256` — 283 entradas SHA-256 (LF), verificadas con `sha256sum -c`
- `scripts/verify-manifest.ps1` — Script de verificación (equivalente a sha256sum -c)
- `scripts/regenerate-manifest.ps1` — Script de regeneración

**Commit:** `fc25cb8` — "P10: regenerate MANIFEST.sha256 and add verification scripts"

---

## P11 — Instrumento Brooke + consentimientos (commit fa1274c)

**Archivos creados:**
- `dataset/sus/SUS-INSTRUMENT.md` — Cuestionario System Usability Scale (Brooke 1996), 10 ítems
- `dataset/sus/CONSENT-FORM.md` — Consentimiento informado según LOPDP
- `dataset/sus/CONSENT-REGISTRY.md` — Registro de consentimiento de 15 participantes

**Commit:** `fa1274c` — "P11: add SUS instrument, consent form, and acceptance registry"

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

## Firmas

Declaramos que los puntos pendientes listados en este documento fueron cerrados por los
integrantes que firman, con los archivos y commits indicados en cada sección.

| Integrante | Correo institucional | Firma |
|---|---|---|
| Luis Alejandro Tejada Bajaña | ltejadab@uteq.edu.ec | Luis Alejandro Tejada Bajaña |
| María del Rosario Escudero Plaza | mescuderop@uteq.edu.ec | María del Rosario Escudero Plaza |
| Kevin Moisés Castro Espinoza | kcastroe2@uteq.edu.ec | Kevin Moisés Castro Espinoza |
