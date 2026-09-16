# VERIFICACION — SGROAS Supletorio v1.1.0

Fecha: 2026-09-15
Commit: 51202f5
Tag: v1.1.0

---

## P1 — Passwords y secrets movidos a variables de entorno (1.2)

**Orden de verificación:**
```bash
# 1. application.properties NO contiene valores literales
grep -n "SPRING_DATASOURCE_PASSWORD\|JWT_SECRET" src/main/resources/application.properties
# 2. docker-compose.yml usa env vars, no secrets hardcodeados
grep -n "SPRING_DATASOURCE_PASSWORD\|JWT_SECRET" docker-compose.yml
# 3. JwtServiceTest compila sin secret hardcodeado
grep -n "JWT_SECRET" src/test/java/ec/edu/uteq/sgroas/security/JwtServiceTest.java
# 4. .env.example tiene placeholders
grep -n "ROTATED" .env.example
# 5. Búsqueda de la contraseña de BD / JWT secret literales (≠ demo de login, ≠ ${...})
grep -rEn "spring.datasource.password=([^$]|$)|POSTGRES_PASSWORD|APP_JWT_SECRET|app.jwt.secret" \
  src docker-compose.yml .env.example k6 .github 2>/dev/null \
  | grep -v '\$\{' | grep -v '<ROTATED' | grep -v 'secrets\.' | grep -v 'APP_COOKIE'
```

**Salida (2026-09-15):**
```
1. spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
   app.jwt.secret=${JWT_SECRET}
2. SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
   APP_JWT_SECRET: ${JWT_SECRET}
3. private static final String JWT_SECRET =
       System.getenv().getOrDefault("JWT_SECRET",
   ReflectionTestUtils.setField(jwtService, "jwtSecret", JWT_SECRET);
4. SPRING_DATASOURCE_PASSWORD=<ROTATED_DB_PASSWORD>
   APP_JWT_SECRET=<ROTATED_JWT_SECRET_MIN_32_CHARS>
5. (búsqueda 5: 0 resultados — ninguna contraseña de BD ni JWT secret literal en el
   árbol; solo `${{...}}`/`${...}`, placeholders `<ROTATED_...>` y credenciales demo
   de login del README; el CI usa `secrets.CI_POSTGRES_PASSWORD` con `trust` local)
```

**Archivos:** `src/main/resources/application.properties`, `docker-compose.yml`,
`.env.example`, `src/test/java/ec/edu/uteq/sgroas/security/JwtServiceTest.java`,
`src/test/resources/application-test.properties`, `.github/workflows/ci.yml`
(contraseña real rotada en el despliegue y declarada en `.env.example`).

---

## P2 — k6 corridas crudas versionadas (1.2)

**Orden de verificación:**
```bash
# Verificar que existen corridas crudas por escenario (k01..k08, incl. frío)
ls dataset/perf/k*.json | wc -l
head -20 dataset/perf/k08-run1.json
# Recalcular el análisis no paramétrico desde las corridas crudas
python scripts/perf/recalcular-contraste.py
```

**Salida (2026-09-15):**
```
13
k01-run1.json k02-run2.json k03-run3.json
k04-cold.json k04-run1.json k05-cold.json k05-run1.json k06-cold.json
k06-run1.json k07-cold.json k07-run1.json k08-cold.json k08-run1.json
(JSON de k08-run1 con métricas agregadas de una corrida v6)

Contraste no parametrico cache frio vs caliente (n = 5 por condicion)
U (Mann-Whitney)       : 0.0
z (aproximacion normal): -2.61
p (bilateral)          : 0.0090
d de Cliff             : -1.00 -> grande
OK: contraste reproducible desde las corridas crudas
```

**Archivos:** `dataset/perf/k*.json` (13 corridas: k01–k03 locales por escenario +
5 calientes + 5 frías), análisis recalculado con `scripts/perf/recalcular-contraste.py`
que carga `scripts/perf/nonparametric.py` (Mann-Whitney + d de Cliff) directamente
desde las corridas crudas; resultados en `docs/mediciones/perf/RENDER-REPORT.md`
(y `dataset/perf/REPORT.md` para la serie local K1).

---

## P3 — Lighthouse corridas versionadas (1.0)

**Orden de verificación:**
```bash
# Verificar que existen al menos 3 corridas por perfil
ls dataset/lighthouse/lh-*.json | wc -l
# Verificar que contienen scores
grep -l '"performance"' dataset/lighthouse/lh-*.json | wc -l
```

**Salida (2026-09-15):**
```
9
9
```

**Archivos:** `dataset/lighthouse/lh-{mobile,desktop,tablet}-{1,2,3}.json` (9 corridas)
contra `https://sgroas-backend.onrender.com`; resumen en `dataset/lighthouse/REPORT.md`.

---

## P4 — Cookie Secure() en todas las respuestas (1.0)

**Orden de verificación:**
```bash
# Verificar que NO existe .secure(cookieSecure) en AuthController
grep -n "\.secure(cookieSecure)" src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java
# Verificar que SÍ existe .secure(true)
grep -n "\.secure(true)" src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java
```

**Salida (2026-09-15):**
```
(búsqueda 1: 0 resultados)
(búsqueda 2: 4 líneas — access_token y refresh_token con .secure(true) en login, refresh y logout)
```

**Archivos:**
- `src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java` — `.secure(true)` + `.httpOnly(true)`
- Cabecera Set-Cookie capturada del despliegue en `docs/mediciones/sec/live-session/login-response.txt`:
  `Set-Cookie: access_token=...; Secure; HttpOnly; SameSite=Strict` (y `refresh_token` ídem, 7 días)

---

## P5 — Métodos en español ≤ 5% (1.4)

**Orden de verificación:**
```bash
python3 scripts/check-spanish-methods.py
```

**Salida (2026-09-15):**
```
Total methods (main + tests): 519
OK: 0 Spanish method names (0%)
```

**Tipos (clases/interfaces/records/enums):** 130, solo 1 señalado (`Terminal`,
cognado inglés, no español) → 0,77% ≤ 5%.

**Archivos modificados:**
- `src/main/java/ec/edu/uteq/sgroas/entity/*.java` — campos renombrados al inglés
  (Driver, User, Vehicle, Route, Incident, RouteAssignment, VerificationCode)
- `src/main/java/ec/edu/uteq/sgroas/service/*.java` y controladores — `desactivar` → `deactivate`
- `src/test/java/**` — 178 nombres de métodos de test traducidos al inglés (32 archivos)
- DTOs, repositorios y tests actualizados a los nuevos nombres

---

## P6 — Javadoc ≥ 90% (0.6)

**Orden de verificación:**
```bash
python3 scripts/check-javadoc.py
./mvnw javadoc:javadoc
```

**Salida (2026-09-15):**
```
Javadoc coverage: 226/226 (100.0%)
OK: Javadoc >= 90%
BUILD SUCCESS (mvn javadoc:javadoc sin errores)
```

**Archivos:** 226 métodos públicos/protected en `src/main/java/**` documentados,
incluidos los 10 records DTO y los servicios.

---

## P7 — Figuras/tablas rotuladas en inglés (0.9)

**Orden de verificación:**
```bash
# Ningún caption en español en el informe
grep -rn "caption{" docs/informe-final/ 2>/dev/null | grep -E "Tabla|Figura|Listado|Resumen|Resultados|Distribución|Síntesis|Desglose|trazados|comparación|puntaje|prioridad"
echo "exit=$?"   # 1 => 0 coincidencias
# Total de captions (todos en inglés)
grep -rn "caption{" docs/informe-final/ 2>/dev/null | wc -l
```

**Salida (2026-09-15):**
```
(búsqueda 1: 0 coincidencias, grep exit 1)
(búsqueda 2: 14 captions en inglés)
```

**Archivos:** captions en `docs/informe-final/capitulos/cap*.tex` traducidos al inglés;
figuras generadas por scripts con rótulos en inglés (ver Makefile target `docs`).
Textos dentro de las figuras (tablas y ejes) producidos por `scripts/gen-figuras.py` en inglés.

---

## P8 — Demografía SUS con trazabilidad CSV (0.7)

**Orden de verificación:**
```bash
python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv
bash scripts/validate-sus-demografia.sh
```

**Salida (2026-09-15):**
```
Total participants: 15
Codes: P01..P15
Gender: 8 male, 7 female
Age range: 19-25 years (mean 21.2)
Web experience: Baja=3, Media=10, Alta=2
SUS score: mean=68.5, min=47.5, max=90.0
OK: demografia cap.5 cruza 1:1 con sus-raw.csv (n=15, 8H/7M, 19-25, B3/M10/A2, media SUS 68.5)
```

**Archivos:**
- `dataset/sus/sus-raw.csv` — datos crudos (15 participantes)
- `scripts/generate-sus-demographics.py` — regenera la tabla desde el CSV
- `scripts/validate-sus-demografia.sh` — cruza la demografía del cap.5 1:1 con el CSV
- Tabla `tab:sus-demografia` en `docs/informe-final/capitulos/cap5-materiales-metodos.tex`
  (sección "Participantes SUS"), regenerada desde el CSV por
  `scripts/generate-sus-demographics.py` y cruzada 1:1 con el CSV por
  `scripts/validate-sus-demografia.sh`

---

## P9 — Endpoint de asignaciones con sesión (0.7)

**Orden de verificación:**
```bash
# Colección Postman con CRUD de asignaciones
grep -c "asignaciones" docs/postman/coleccion.json
grep -o '"method": "[A-Z]*"' docs/postman/coleccion.json | sort -u
# Petición autenticada real contra el despliegue (ver expediente de sesión)
cat docs/mediciones/sec/live-session/asignaciones.json
```

**Salida (2026-09-15):**
```
9
"method": "DELETE"  "method": "GET"  "method": "POST"  "method": "PUT"
(HTTP/1.1 200 — GET /api/asignaciones?page=0&size=10 con cookie: content con 8 elementos)
```

**Reproducción en vivo (2026-09-15):**
```
$ curl -s -o /dev/null -w '%{http_code}\n' -b cookies.txt \
    "https://sgroas-backend.onrender.com/api/asignaciones?page=0&size=10"
200
$ curl -s -o /dev/null -w '%{http_code}\n' -b cookies.txt \
    "https://sgroas-backend.onrender.com/api/auth/me"
200
```
(Login 200 → cookie; asignaciones y `/me` 200 con datos; sin cookie → 403.)

**Archivos:**
- `docs/postman/coleccion.json` — carpeta "Asignaciones" con 6 requests CRUD
- `docs/mediciones/sec/live-session/asignaciones.json` — 200 con datos contra el deploy,
  `auth-me.json` (200 ROLE_ADMIN) y `sin-sesion-403.txt` (403 sin cookie); resumen en
  `docs/mediciones/sec/live-session/REPORT.md`

---

## P10 — Manifest SHA-256 verificable (0.5)

**Orden de verificación:**
```bash
sha256sum -c dataset/MANIFEST.sha256
```

**Salida (2026-09-15, Git Bash):**
```
dataset/DATA-DICTIONARY.md: OK
dataset/DATA-PROVENANCE.md: OK
...
dataset/zenodo.json: OK
sha256sum: dataset/MANIFEST.sha256: 283 lines processed
(283 "OK", 0 FAILED, 0 MISSING; exit code 0)
```

La verificación también se cubre con el script PowerShell (`scripts/verify-manifest.ps1`):
`Results: 283 OK, 0 FAILED, 0 MISSING out of 283 entries`.

**Archivos:**
- `dataset/MANIFEST.sha256` — 283 entradas SHA-256 (LF)
- `scripts/verify-manifest.ps1` — verificación (equivalente a `sha256sum -c`)
- `scripts/regenerate-manifest.ps1` — regeneración

---

## P11 — Instrumento Brooke + consentimientos (0.8)

**Orden de verificación:**
```bash
grep -cE "^[0-9]+\. " dataset/sus/SUS-INSTRUMENT.md
grep -c "| Yes" dataset/sus/CONSENT-REGISTRY.md
ls dataset/sus/CONSENT-FORM.md
```

**Salida (2026-09-15):**
```
10
15
dataset/sus/CONSENT-FORM.md
```

**Archivos:**
- `dataset/sus/SUS-INSTRUMENT.md` — Cuestionario System Usability Scale (Brooke 1996), 10 ítems
- `dataset/sus/CONSENT-FORM.md` — Consentimiento informado según LOPDP
- `dataset/sus/CONSENT-REGISTRY.md` — constancia de aceptación de los 15 participantes (P01..P15)

---

## make verify (EV-2)

**Orden de verificación:**
```bash
make verify
```

**Salida (2026-09-15):**
```
[P1] OK
[P2] Checking raw k6 runs (hot x5 + cold x5) reproducible contrast...
  6 hot runs found
  5 cold runs found
  OK: nonparametric contrast reproducible (nonparametric.py)
[P2] OK
[P4] OK
[P5] OK - no Spanish fields in entities
[P5] Checking Spanish method names in main...
Total methods (main + tests): 519
OK: 0 Spanish method names (0%)
[P6] Checking Javadoc coverage on public methods...
Javadoc coverage: 226/226 (100.0%)
OK: Javadoc >= 90%
[P7] OK - all figure/table captions in English
[P10] Results: 283 OK, 0 FAILED, 0 MISSING out of 283 entries / OK
[P11] OK
[P9] OK
ALL CHECKS PASSED (exit code 0)
```

---

## CONTRIBUCIONES.md (EV-4)

Ver archivo `CONTRIBUCIONES.md` en la raíz del repositorio (firmado por los tres
integrantes con su correo institucional).

---

## Tag v1.1.0 (EV-3)

El tag `v1.1.0` se colocó sobre el ÚLTIMO commit de la cadena de cierre
(contenido sustantivo en `51202f5`; el commit del tag cierra con las correcciones
del pipeline CI: definir la variable de entorno `JWT_SECRET`, publicar la imagen
en `ghcr.io/<owner>/sgroas` con el owner dinámico del workflow, regenerar el
manifiesto `dataset/MANIFEST.sha256` y forzar `text eol=lf` en `dataset/**`
vía `.gitattributes` para que `sha256sum -c` dé 283 OK en cualquier plataforma).
Se verifica:

```bash
git rev-parse v1.1.0
git log --oneline v1.1.0 -1
```

URL pública del sistema en la primera pantalla del README: `https://sgroas-backend.onrender.com`.