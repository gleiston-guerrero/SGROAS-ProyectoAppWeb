# VERIFICACION — SGROAS Supletorio v1.1.0

**ACTUALIZACION CRITICA (2026-09-17):** siguiendo la regla explicita de la
guia del examen ("Lo que no este dentro de la etiqueta no existe. Muevan
v1.1.0 al ultimo commit que quieren que revise"), la etiqueta `v1.1.0` se
MOVIO del commit original `f2fb883` a `c25dc06` (o posterior — ver
`git rev-parse v1.1.0^{commit}`), que incluye TODAS las correcciones
documentadas en este archivo. Los tags `v1.1.1`, `v1.1.2`, `v1.1.3` y
`v1.1.4`, creados durante el proceso de correccion antes de saber que
habia que mover `v1.1.0` en vez de crear tags nuevos, se conservan sin
borrar como historial intermedio, pero **`v1.1.0` es ahora el unico tag
relevante para la evaluacion** — es el que la guia revisa por definicion.

Fecha original: 2026-09-15
Commit citado originalmente: 8398462 (INCORRECTO, ver seccion "Tag v1.1.0"
mas abajo — corregido 2026-09-17: el tag v1.1.0 apuntaba a f2fb883, y
desde el 2026-09-17 apunta al commit final de esta auditoria)

**Re-auditoria: 2026-09-16.** Todos los comandos de este archivo se
re-ejecutaron en este working tree en esa fecha y la salida se pego
literal (sin resumir, sin "..."). Donde el resultado cambio respecto a la
version anterior, se documenta explicitamente por que.

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

**Salida (2026-09-17, re-verificación tras eliminar el último secreto literal
de `JwtServiceTest.java`):**
```
1. spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
   app.jwt.secret=${JWT_SECRET}
2. SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
   APP_JWT_SECRET: ${JWT_SECRET}
3. // ("TEST_ONLY_SECRET_KEY_2026..."). Ahora, si JWT_SECRET no esta definida
   private static final String JWT_SECRET = resolveTestSecret();
       String fromEnv = System.getenv("JWT_SECRET");
   ReflectionTestUtils.setField(jwtService, "jwtSecret", JWT_SECRET);
4. POSTGRES_PASSWORD=<ROTATED_DB_PASSWORD>
   SPRING_DATASOURCE_PASSWORD=<ROTATED_DB_PASSWORD>
   APP_JWT_SECRET=<ROTATED_JWT_SECRET_MIN_32_CHARS>
5. (búsqueda 5: 0 resultados, exit code 1 — ningún secreto literal de BD ni JWT
   en todo el árbol, incluido el de prueba, que ahora se genera con
   SecureRandom en memoria en vez de un valor de texto fijo)
```

**Rotación real declarada por escrito (2026-09-17, ~19:41 UTC):** el
responsable del repositorio (Luis Tejada) roto de verdad, en los paneles
reales de los proveedores:

- **Contraseña de la base de datos** (PostgreSQL gestionado en Supabase,
  no en el Postgres de Render pese a que `render.yaml` originalmente
  asumía un Postgres gestionado por Render): reseteada desde el panel de
  Supabase (Project Settings → Database → Reset database password), y el
  nuevo valor actualizado en las variables de entorno del servicio
  `sgroas-backend` en Render.
- **`APP_JWT_SECRET`**: reemplazado en las variables de entorno de
  `sgroas-backend` en Render por una clave de 48 bytes generada con
  `secrets.token_bytes` (Python `secrets`, no un valor predecible),
  codificada en base64.

**Prueba técnica de que la rotación surtió efecto** (no solo se declaró,
se verificó contra el sistema real tras el redeploy):

```
$ curl -s https://sgroas-backend.onrender.com/actuator/health
{"status":"UP", ... "db":{"status":"UP","details":{"database":"PostgreSQL", ...}}, ...}

$ curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST https://sgroas-backend.onrender.com/api/auth/login \
    -H "Content-Type: application/json" -d '{"email":"admin@sgroas.com","password":"admin123"}'
HTTP 200
```

`db: UP` confirma que el backend logró conectarse a Supabase con la
contraseña NUEVA (si la rotación hubiera fallado o quedado desincronizada,
este health check habría fallado). El login `200` confirma que el
`APP_JWT_SECRET` nuevo firma y valida tokens correctamente de punta a
punta. Ninguno de los dos valores reales (contraseña de BD ni JWT secret)
se expone en este documento ni en ningún archivo del repositorio.

**Archivos:** `src/main/resources/application.properties`, `docker-compose.yml`,
`.env.example`, `src/test/java/ec/edu/uteq/sgroas/security/JwtServiceTest.java`,
`src/test/resources/application-test.properties`, `.github/workflows/ci.yml`.

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

**Salida (2026-09-16, re-auditoria, script `recalcular-contraste.py` reescrito):**
```
13
dataset/perf/k01-run1.json
dataset/perf/k02-run2.json
dataset/perf/k03-run3.json
dataset/perf/k04-cold.json
dataset/perf/k04-run1.json
dataset/perf/k05-cold.json
dataset/perf/k05-run1.json
dataset/perf/k06-cold.json
dataset/perf/k06-run1.json
dataset/perf/k07-cold.json
dataset/perf/k07-run1.json
dataset/perf/k08-cold.json
dataset/perf/k08-run1.json

Metrica fria     (duracion_frio.avg): [156.5167, 179.2081, 207.8895, 519.45, 224.4159]
Metrica caliente (duracion_listado.avg): [6318.661313274337, 4399.611336700337, 3780.3120546268633, 3090.1709987244885, 2247.859510860656]

Contraste no parametrico: 1 VU sin carga vs 50 VUs con carga (n = 5 por condicion)
U (Mann-Whitney)       : 0.0
z (aproximacion normal): -2.61
p (bilateral)          : 0.0090
d de Cliff             : -1.00 -> grande

Este resultado se calcula directamente de los JSON crudos, sin valores
fijados de antemano. No hay garantia de que sea significativo.
```

**Nota de auditoria (2026-09-16):** el script `scripts/perf/recalcular-contraste.py`
anterior tenia `assert p < 0.05` y `assert d == -1.0` HARDCODEADOS antes de
calcular nada, y comparaba `.max()` de la condicion fria contra `.avg()` de
la caliente (metricas distintas, comparacion invalida). Se reescribio desde
cero: sin asserts que fijen el resultado, usando la misma metrica
(`avg`) para ambas condiciones. El numero final coincide con el que ya
estaba documentado, pero ahora se calcula honestamente en vez de estar
garantizado por construccion. En el momento de esas corridas,
`GET /api/conductores` NO tenia `@Cacheable`, por lo que esa comparacion
es "1 VU sin carga" vs "50 VUs con carga", no una cache fria/caliente real.

**Actualización 2026-09-17 — se implementó `@Cacheable` real:**

```
$ grep -n "Cacheable" src/main/java/ec/edu/uteq/sgroas/service/DriverService.java
11:import org.springframework.cache.annotation.Cacheable;
29:    // @Cacheable only intercepts calls that go through the proxy, never a
60:    @Cacheable(value = "conductores", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
```

Se detectó y corrigió, de paso, un defecto real de la propia
implementación: llamar al método cacheado con `this.metodo()` desde
dentro de la misma clase NO pasa por el proxy de Spring AOP y por tanto
NUNCA activa el `@Cacheable` en producción (el mismo patrón —
`listCached()` sin usar — ya existía sin conectar en `IncidentService` y
`RouteAssignmentService`, sin que nadie lo hubiera detectado). Se
corrigió con auto-inyección de un `ObjectProvider<DriverService>` para
invocar el método a través del proxy real. Prueba de que el caché
funciona de verdad, con un `CacheManager` real en memoria (no un mock),
en `DriverServiceCachingTest`:

```
$ ./mvnw test -Dtest=DriverServiceCachingTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

`secondUnfilteredCallIsServedFromCacheNotFromRepository`: dos llamadas
idénticas sin filtro → el repositorio se invoca una sola vez.
`searchedCallIsNeverCached`: con filtro de búsqueda, cada llamada sí pega
al repositorio (comportamiento esperado, no se cachea texto libre).

Las 13 corridas de k6 ya versionadas son anteriores a esta implementación
y no se volvieron a correr contra el endpoint ya cacheado (correrlas de
nuevo requeriría levantar el stack completo con Redis); el contraste
"frío vs caliente" documentado en `ANALISIS-k6.md` para esas corridas
sigue siendo honestamente una comparación de VUs, no de caché real — se
aclara explícitamente en ese documento para no dar a entender que las
cifras ya reflejan el `@Cacheable` nuevo.

**Archivos:** `dataset/perf/k*.json` (13 corridas: k01–k03 locales por escenario +
5 calientes + 5 frías), análisis recalculado con `scripts/perf/recalcular-contraste.py`
(reescrito 2026-09-16) que carga `scripts/perf/nonparametric.py` (Mann-Whitney + d de Cliff)
directamente desde las corridas crudas; resultados en `docs/mediciones/perf/ANALISIS-k6.md`
(y `dataset/perf/REPORT.md` para la serie local K1).
`src/main/java/ec/edu/uteq/sgroas/service/DriverService.java` —
`@Cacheable` real + auto-inyección del proxy.
`src/test/java/ec/edu/uteq/sgroas/service/DriverServiceCachingTest.java` —
prueba de que el caché se activa de verdad.

---

## P3 — Lighthouse corridas versionadas (1.0)

**Nota de auditoria (2026-09-16):** las 9 corridas `lh-{mobile,desktop,tablet}-{1,2,3}.json`
son del 2026-09-15, contra `https://sgroas-backend.onrender.com`, ANTES de la
correccion del contrato backend-frontend y otros defectos corregidos hasta
`v1.1.3`. Ademas el "orden de verificacion" original solo contaba archivos
(`wc -l`) y nunca mostraba la URL auditada ni los scores reales — no probaba
que las corridas fueran validas ni vigentes.

Se generaron 6 corridas FRESCAS (3 movil + 3 escritorio) contra el codigo
actual de `main` (post `v1.1.3`), sirviendo el build de produccion del
frontend Angular con `frontend/serve-gzip.js` (estatico + gzip, replica un
contenedor nginx) en `http://localhost:4200/` — mismo metodo que las 2
corridas locales `lhci-20260730-21{15,17}.json` ya versionadas, para que el
resultado no dependa de la disponibilidad ni del estado de despliegue de
Render. Se intento tambien auditar `https://sgroas-backend.onrender.com`
directamente (para tener evidencia comparable a la anterior) pero el
servicio no respondio: `curl -v https://sgroas-backend.onrender.com/` con
timeout de 60s no recibio bytes (TLS conecta, luego cuelga) — no se pudo
confirmar si el Free tier de Render solo tardaba en despertar o si el
servicio esta caido/no tiene el codigo de `main` desplegado, asi que no se
uso esa URL como evidencia para no arriesgar attribuir a `main` un resultado
que en realidad vino de un despliegue desactualizado o inexistente.

De paso se encontro y corrigio un defecto en `lighthouserc.js`: el preset
movil pasaba `settings.preset = 'mobile'`, pero en Lighthouse 13.4.1 (la
version realmente instalada, confirmada con `npx lighthouse@13.4.1 --help`)
`--preset` solo acepta `perf | experimental | desktop` — `mobile` rompia
`npx lhci autorun` con `Invalid values: Argument: preset, Given: "mobile"`.
Nunca se habia ejecutado el comando documentado de punta a punta con esta
version de Lighthouse. Se quito la clave `preset` del perfil movil (el
default de la CLI ya es mobile con emulacion de pantalla movil) dejando solo
el `throttling` Slow 4G explicito; el preset `desktop` no se toco porque ese
valor si es valido.

**Orden de verificación:**
```bash
# Verificar que existen al menos 3 corridas por perfil (incluye las frescas)
ls dataset/lighthouse/lh-*.json | wc -l
# Mostrar URL auditada y scores reales de las 3 corridas frescas de escritorio
python3 -c "
import json
for i in (1,2,3):
    d = json.load(open(f'dataset/lighthouse/lh-desktop-fresh-20260916-{i}.json', encoding='utf-8'))
    c = d['categories']
    print(i, d['requestedUrl'], {k: round(v['score']*100) for k, v in c.items()})
"
# Mismo, para movil
python3 -c "
import json
for i in (1,2,3):
    d = json.load(open(f'dataset/lighthouse/lh-mobile-fresh-20260916-{i}.json', encoding='utf-8'))
    c = d['categories']
    print(i, d['requestedUrl'], {k: round(v['score']*100) for k, v in c.items()})
"
```

**Salida real (2026-09-16, corridas frescas contra `main`/v1.1.3, local
`http://localhost:4200/` via `serve-gzip.js`):**
```
15
1 http://localhost:4200/ {'performance': 100, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
2 http://localhost:4200/ {'performance': 98, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
3 http://localhost:4200/ {'performance': 100, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
1 http://localhost:4200/ {'performance': 89, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
2 http://localhost:4200/ {'performance': 88, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
3 http://localhost:4200/ {'performance': 85, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
```

Las 6 corridas frescas cumplen los 4 umbrales de `lighthouserc.js`
(`categories:performance >= 0.8`, `accessibility/best-practices/seo >= 0.9`);
confirmado tambien por `npx @lhci/cli assert --config=lighthouserc.js`
(`All results processed!`, sin errores de assertion) para ambos perfiles.

**Reproducir las corridas frescas:**
```bash
cd frontend && npm run build
node serve-gzip.js &                 # sirve dist/ en :4200
cd ..
LHCI_URL=http://localhost:4200/ LHCI_PRESET=mobile   npx @lhci/cli@latest collect --config=lighthouserc.js
npx @lhci/cli@latest assert --config=lighthouserc.js
npx @lhci/cli@latest upload --config=lighthouserc.js   # deja los JSON en docs/mediciones/lighthouse/
rm -rf .lighthouseci
LHCI_URL=http://localhost:4200/ LHCI_PRESET=desktop  npx @lhci/cli@latest collect --config=lighthouserc.js
npx @lhci/cli@latest assert --config=lighthouserc.js
npx @lhci/cli@latest upload --config=lighthouserc.js
```
(el `LHCI_PRESET` NO debe estar seteado al correr `assert` sola en la misma
shell — `lhci assert` tambien lee esa variable como si fuera su propio flag
`--preset`, que solo acepta `lighthouse:all|lighthouse:recommended|lighthouse:no-pwa`,
y falla con "Invalid values". Correr `collect` y `assert` en pasos separados,
o en subshells, evita el choque.)

**Actualización 2026-09-17 — VIGENTE: 6 corridas contra el despliegue
público real** (el criterio de cierre de P3 exige explícitamente "contra
el despliegue público"; las corridas locales de arriba no lo cumplían al
pie de la letra). Tras reconectar el servicio de Render al repositorio
correcto y redesplegar, se corrieron 3 móviles + 3 escritorio directo
contra `https://sgroas-backend.onrender.com/`:

```
$ python3 -c "
import json, glob, os
for f in sorted(glob.glob('dataset/lighthouse/lh-*-render-20260917-*.json')):
    d = json.load(open(f, encoding='utf-8'))
    c = d['categories']
    print(os.path.basename(f), d['requestedUrl'], {k: round(v['score']*100) for k,v in c.items()})
"
lh-desktop-render-20260917-1.json https://sgroas-backend.onrender.com/ {'performance': 95, 'accessibility': 91, 'best-practices': 93, 'seo': 90}
lh-desktop-render-20260917-2.json https://sgroas-backend.onrender.com/ {'performance': 94, 'accessibility': 91, 'best-practices': 93, 'seo': 90}
lh-desktop-render-20260917-3.json https://sgroas-backend.onrender.com/ {'performance': 95, 'accessibility': 91, 'best-practices': 93, 'seo': 90}
lh-mobile-render-20260917-1.json https://sgroas-backend.onrender.com/ {'performance': 88, 'accessibility': 91, 'best-practices': 93, 'seo': 90}
lh-mobile-render-20260917-2.json https://sgroas-backend.onrender.com/ {'performance': 88, 'accessibility': 91, 'best-practices': 93, 'seo': 90}
lh-mobile-render-20260917-3.json https://sgroas-backend.onrender.com/ {'performance': 92, 'accessibility': 91, 'best-practices': 93, 'seo': 90}
```

Las 6 corridas cumplen los 4 umbrales de `lighthouserc.js`
(`performance>=0.8`, `accessibility/best-practices/seo>=0.9`). Nota
técnica: en Windows, `npx @lhci/cli collect` con `numberOfRuns>1` falla
al limpiar su directorio temporal entre corridas (`EPERM` de Node al
borrar `%TEMP%\lighthouse.*`) *después* de generar el resultado — se
corrió con `--numberOfRuns=1` seis veces en pasos separados para evitar
el crash, sin afectar la validez de cada corrida individual (cada una
completa su auditoría antes de fallar en el cleanup posterior).

**Archivos:**
- `dataset/lighthouse/lh-{mobile,desktop}-render-20260917-{1,2,3}.json`
  (6 corridas, VIGENTES, contra el despliegue público real).
- `dataset/lighthouse/lh-{mobile,desktop,tablet}-{1,2,3}.json` (9 corridas,
  2026-09-15, contra Render, PRE-corrección de contrato — se conservan por
  trazabilidad historica).
- `dataset/lighthouse/lh-{mobile,desktop}-fresh-20260916-{1,2,3}.json` (6
  corridas, 2026-09-16, contra `main`/v1.1.3 local — históricas, superadas
  por las corridas contra Render de arriba).
- `dataset/lighthouse/lhci-20260730-2115.json` y `-2117.json` (2 corridas
  locales previas).
- `lighthouserc.js` — fix del preset movil invalido.
- Resumen historico en `dataset/lighthouse/REPORT.md` (no actualizado con
  las corridas frescas; los scores reales quedan documentados arriba).

---

## P4 — Cookie Secure() en todas las respuestas (1.0)

**Orden de verificación:**
```bash
# Verificar que NO existe .secure(cookieSecure) en AuthController
grep -n "\.secure(cookieSecure)" src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java
# Verificar que SÍ existe .secure(true)
grep -c "\.secure(true)" src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java
```

**Salida (2026-09-16, re-auditoria):**
```
(búsqueda 1: 0 resultados, exit code 1)
5
```

Nota (2026-09-16): en la re-auditoria se encontro un campo
`@Value("${app.cookie.secure:false}")` (`cookieSecure`) que ya no se usaba
en ningun `.secure(...)` — codigo muerto. Se elimino en lugar de conectarlo,
porque dejar el flag Secure de una cookie de sesion dependiente de una
propiedad externa es un riesgo si algun entorno quedara con
`app.cookie.secure=false` por error; `.secure(true)` fijo es la opcion mas
segura y no depende de configuracion. Las 5 llamadas `.secure(true)` que se
mantienen cubren access_token y refresh_token en login, refresh y logout.

**Archivos:**
- `src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java` — `.secure(true)` + `.httpOnly(true)`
- Cabecera Set-Cookie capturada EN VIVO el 2026-09-16 contra el backend local
  (post-corrección de contrato) en
  `docs/mediciones/sec/live-session/login-response-20260916.txt`:
  `Set-Cookie: access_token=...; Secure; HttpOnly; SameSite=Strict` (y `refresh_token` ídem, 7 días).
  Reemplaza a `login-response.txt` (2026-09-14, contra Render, con campos aún
  en español y con un JWT/refresh_token reales que quedaron versionados antes
  de ser redactados).

**Actualización 2026-09-17 — evidencia vigente contra el despliegue público
real:** tras reconectar el servicio de Render al repositorio correcto
(`gleiston-guerrero/...`, antes apuntaba al repo viejo) y forzar un
redeploy, se capturó login real directamente contra
`https://sgroas-backend.onrender.com`:

```
$ curl -s -i -X POST https://sgroas-backend.onrender.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@sgroas.com","password":"admin123"}'
HTTP/1.1 200 OK
Set-Cookie: access_token=<REDACTADO>; Path=/; Max-Age=3600; Secure; HttpOnly; SameSite=Strict
Set-Cookie: refresh_token=<REDACTADO>; Path=/api/auth; Max-Age=604800; Secure; HttpOnly; SameSite=Strict
{"name":"Admin SGROAS","email":"admin@sgroas.com","role":"ROLE_ADMIN","expiresIn":3600000}
```

Guardado (tokens redactados) en
`docs/mediciones/sec/live-session/login-response-20260917-render.txt` —
ver `docs/mediciones/sec/live-session/README.md` (Generación 3) para el
detalle completo, incluida una nota honesta sobre el JWT interno (que en
el momento de esta captura aún no había recogido el fix de claims
`nombre`/`rol` → `name`/`role` de un redeploy posterior).

**Cierre del detalle pendiente (2026-09-17, ~19:48 UTC):** tras el
redeploy con el fix de `JwtService.java` (claims `name`/`role`) y la
rotación de secretos, se repitió el login contra Render y se decodificó
el payload del JWT (no cifrado, solo firmado):

```
{"jti":"...","iss":"https://sgroas-backend.onrender.com","sub":"admin@sgroas.com",
 "aud":["sgroas-frontend"],"iat":...,"nbf":...,"exp":...,
 "name":"Admin SGROAS","role":"ROLE_ADMIN"}
```

Confirmado: el JWT interno ya usa `name`/`role` en inglés, igual que el
cuerpo de la respuesta. Guardado (redactado) en
`docs/mediciones/sec/live-session/login-response-20260917-render-final.txt`.
Con esto no queda ningún residuo en español en el contrato de sesión, en
ningún nivel (cuerpo HTTP, cookie, ni JWT).

---

## P5 — Métodos en español ≤ 5% (1.4)

**Orden de verificación:**
```bash
python3 scripts/check-spanish-methods.py
```

**Salida final (2026-09-17, segunda pasada — todas las categorias OK):**
```
[OK] Metodos en src/main: 0/226 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/131 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 0/293 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/test: 0/42 en espanol (0.00%, umbral 5%)
[OK] Metodos combinados (main+test): 0/519 en espanol (0.00%, umbral 5%)
[OK] Tipos combinados (main+test): 0/173 en espanol (0.00%, umbral 5%)

OK: todas las categorias por debajo del umbral del 5%
```

(Salida intermedia previa, 2026-09-16, tras la primera pasada que dejó
`src/main` al 0% pero `src/test` aún en 28.33%/11.90%, se conserva más
abajo en la sección "Lo que se corrigió en la segunda pasada" para dejar
constancia del proceso completo.)

**Nota de auditoria (2026-09-16) — HONESTA, no maquillada:**

El checker anterior (`scripts/check-spanish-methods.py`) usaba un regex
anclado con `\b` al **inicio** del identificador
(`^(extraer|mapear|...)\b`), que nunca puede casar con un compuesto
camelCase como `extraerEmail` o `mapearAResponse`: entre la `r` de
"extraer" y la `E` de "Email" ambos caracteres son de palabra, asi que
`\b` no marca ahi un limite y el regex jamas los detectaba. Por eso el
checker anterior reportaba "0 Spanish method names (0%)" con 519 metodos,
un falso negativo. Se reescribio el script para tokenizar cada
identificador por sus palabras camelCase (`extraerEmail` -> `["extraer",
"Email"]`) y comparar cada palabra contra una lista de raices en espanol,
lo que si detecta los compuestos sin caer en falsos positivos por
subcadena cruda (p.ej. "con" dentro de "Config").

**Lo que se corrigio en esta auditoria (2026-09-16):**
- `src/main/java/ec/edu/uteq/sgroas/security/JwtService.java`:
  `extraerEmail`->`extractEmail`, `extraerJti`->`extractJti`,
  `extraerExpiracion`->`extractExpiration`, `extraerClaim`->`extractClaim`,
  `tokenValido`->`isTokenValid`, `tokenExpirado`->`isTokenExpired`, mas
  variables locales `usuario`->`user`, `ahora`->`now`, `expiracion`->`expiration`.
- `DriverService`, `IncidentService`, `RouteAssignmentService`, `RouteService`,
  `VehicleService`: metodo privado `mapearAResponse`->`mapToResponse`.
- Campos privados `conductorRepository`->`driverRepository`,
  `vehiculoRepository`->`vehicleRepository`, `rutaRepository`->`routeRepository`,
  `incidenteRepository`->`incidentRepository`,
  `asignacionRutaRepository`->`routeAssignmentRepository`,
  `usuarioRepository`->`userRepository`,
  `codigoVerificacionService`->`verificationCodeService`, y los campos
  espejo `conductorService`, `vehiculoService`, `rutaService`,
  `incidenteService`, `asignacionRutaService`, `reporteService`,
  `usuarioService` en los controladores.
- `RouteAssignmentRepository.findWithDetalle`->`findWithDetails` (y su uso
  en `RouteAssignmentService`).
- Clases de test renombradas (con `git mv`): `ConductorControllerTest`->
  `DriverControllerTest`, `VehiculoControllerTest`->`VehicleControllerTest`,
  `ConductorServiceTest`/`ConductorServiceExtraTest`->`DriverServiceTest`/
  `DriverServiceExtraTest`, `VehiculoServiceTest`->`VehicleServiceTest`,
  `RutaControllerTest`->`RouteControllerTest`, `RutaServiceTest`->
  `RouteServiceTest`, `UsuarioControllerTest`->`UserControllerTest`,
  `UsuarioServiceTest`->`UserServiceTest`, `IncidenteControllerTest`->
  `IncidentControllerTest`, `IncidenteServiceTest`->`IncidentServiceTest`,
  `AsignacionRutaControllerTest`->`RouteAssignmentControllerTest`,
  `AsignacionRutaServiceTest`->`RouteAssignmentServiceTest`,
  `ReporteControllerTest`->`ReportControllerTest`,
  `CodigoVerificacionServiceTest`->`VerificationCodeServiceTest`.
- Metodos de test explicitamente senalados en la guia:
  `meSinTokenDebeRetornar401`->`meWithoutTokenShouldReturn401`
  (`AuthControllerTest`), mas 6 metodos de `JwtServiceTest`/
  `JwtAuthenticationFilterTest` (`tokenConEmailDistintoDebeSerInvalido`,
  `tokenExpiradoDebeSerRechazado`, `extraerExpiracionDebeSerFutura`,
  `limpiarContexto`, `limpiarContextoFinal`, `tokenDeCabeceraDebeContinuarCadena`,
  `tokenDeCookieDebeContinuarCadena`, `tokenEnBlacklistDebeResponderNoAutorizado`,
  `tokenConEmailNuloDebeContinuarCadena`, `tokenValidoDebeEstablecerAutenticacion`).

**Lo que se corrigió en la segunda pasada (2026-09-17):**

Tras la primera pasada quedaban 83 métodos de test y 5 tipos de test en
español por encima del umbral. Se investigó cada uno antes de tocarlo:

- **71 métodos no-ABD** se renombraron con un script de reemplazo exacto
  (`\bidentificador_viejo\b` -> nombre nuevo, uno por uno, sin regex
  genérico) en `CacheConfigTest`, `RedisConfigTest`,
  `RenderDataSourceConfigTest`, `SecurityConfigTest`, `AuthControllerTest`
  (17 métodos), `ReportControllerTest`, `DtoTest`,
  `GlobalExceptionHandlerTest`, `CustomUserDetailsServiceTest`,
  `LoginRateLimiterTest`, `AuthServiceExtraTest`, `AuthServiceTest`,
  `DriverServiceExtraTest`, `DriverServiceTest`, `EmailServiceTest`,
  `ReportServiceTest`, `TokenServiceTest`, `UserServiceTest`,
  `VerificationCodeServiceTest`. Ejemplos: `meSinTokenDebeRetornar401`
  (ya renombrado en la primera pasada) y de la segunda pasada
  `loginConCredencialesInvalidasDebeRetornar401` ->
  `loginWithInvalidCredentialsShouldReturn401`,
  `reenviarActivacionDebeEnviarNuevoCodigo` ->
  `resendActivationShouldSendNewCode`,
  `agregarAccessTokenABlacklistConExpiracionFutura` ->
  `addAccessTokenToBlacklistWithFutureExpiration`. El script de reemplazo
  confirmó las 71 coincidencias exactas (0 warnings de "no encontrado"),
  descartando errores de copiar/pegar.

- **Se revisó de nuevo la premisa de "ABD es deliberadamente español"
  para las 5 clases de test señaladas** y resultó ser **incorrecta para
  los NOMBRES DE CLASE de test** (aunque sigue siendo correcta para los
  campos de los DTOs/entidades ABD, que sí son español por diseño de esa
  base de datos): las clases de producción del módulo ABD ya usan nombres
  en inglés con prefijo `Abd` (`AbdDriverController`, `AbdRouteService`,
  `AbdIncidentService`, `AbdScheduleService`, `AbdUnitService`,
  `AbdAlertController` — confirmado leyendo el código:
  `AbdCatalogServiceTest`/`AbdReportServiceTest` ya seguían ese patrón
  correctamente). Las 5 clases señaladas (`ConductorAbdControllerTest`,
  `AlertaAbdControllerTest`, `IncidenteAbdServiceTest`,
  `ProgramacionAbdServiceTest`, `RutaAbdServiceTest`,
  `UnidadAbdServiceTest` — de hecho eran 6, no 5, porque el checker no
  detectaba "programacion" como raíz española) eran una **inconsistencia
  real de nomenclatura**, no una convención deliberada: ponían el
  sustantivo español ANTES de "Abd" en vez de seguir el patrón
  `Abd`+sustantivo-inglés que ya usan sus propias clases bajo prueba. Se
  corrigieron con `git mv` + `sed` a `AbdDriverControllerTest`,
  `AbdAlertControllerTest`, `AbdIncidentServiceTest`,
  `AbdScheduleServiceTest`, `AbdRouteServiceTest`, `AbdUnitServiceTest`.
  Comprobación literal:
  ```
  $ grep -n "class ConductorAbdControllerTest\|AbdDriverController" src/test/java/.../ConductorAbdControllerTest.java
  30:class ConductorAbdControllerTest {
  36:  return MockMvcBuilders.standaloneSetup(new AbdDriverController(conductorAbdRepository))
  ```
  (la clase de test probaba `AbdDriverController`, un nombre en inglés,
  pero se llamaba a sí misma con el sustantivo en español — confirmado
  antes de renombrar, no asumido).

- **12 métodos dentro de esas mismas clases ABD** (`findTerminalDevuelveLaEntidadCuandoExiste`,
  `incidentsByLevelMapeaNivelYTotal`, etc.) también se tradujeron por la
  misma razón: no eran vocabulario de dominio ABD (como sí lo son
  `cedula`, `nombres`, `idConductor` en los DTOs), sino verbos de test
  genéricos (`mapea`->`Maps`, `devuelve`->`Returns`, `lanza`->`Throws`)
  que debían seguir la convención del resto del proyecto.

- Se corrigió además un falso positivo real en el propio checker: la
  palabra `"terminal"` estaba en la lista de raíces españolas, pero
  `Terminal` es una palabra inglesa válida e idéntica en ambos idiomas
  (una terminal de buses); causaba que `createOkAndSameTerminalFails` y
  `updateOkSameTerminalAndNotFoundFail` (ya en inglés) se marcaran como
  español. Se quitó `"terminal"` de `SPANISH_TEST_EXTRA` en
  `scripts/check-spanish-methods.py`, dejando esos dos métodos sin tocar
  (estaban bien).

**Confirmación explícita sobre ABD (pedida por el usuario):** los **campos
de datos** del módulo ABD (`cedula`, `nombres`, `apellidos`, `idConductor`,
`idRuta`, `nombreProvincia`, etc., dentro de `AbdDtos`, `AbdDriver`,
`AbdRoute`, etc., y su espejo en `frontend/src/app/core/models/abd.model.ts`)
**sí son una convención deliberada y consistente de ese módulo** (un
backend paralelo en español, confirmado leyendo `AbdDtos.java` línea por
línea) y no se tocaron. Lo que NO era deliberado, y por eso se corrigió,
eran los **nombres de las clases y métodos de test** que mezclaban
sustantivos españoles con las clases de producción ya renombradas a
inglés — eso era una inconsistencia, no un diseño.

**Resultado final de percentiles P5:**

| Categoría | Antes (16-sep) | Después primera pasada | Después segunda pasada (final) |
|---|---|---|---|
| Métodos en `src/main` | (falso 0%, checker roto) | 0/226 (0.00%) | 0/226 (0.00%) |
| Tipos en `src/main` | (falso 0%, checker roto) | 0/131 (0.00%) | 0/131 (0.00%) |
| Métodos en `src/test` | (falso 0%, checker roto) | 83/293 (28.33%) | 0/293 (0.00%) |
| Tipos en `src/test` | (falso 0%, checker roto) | 5/42 (11.90%) | 0/42 (0.00%) |
| Métodos combinados | (falso 0%, checker roto) | 83/519 (15.99%) | 0/519 (0.00%) |
| Tipos combinados | (falso 0%, checker roto) | 5/173 (2.89%) | 0/173 (0.00%) |

Todas las categorías, en `src/main`, `src/test` y combinadas, están ahora
al **0.00%**, muy por debajo del umbral del 5%.

---

## P6 — Javadoc ≥ 90% (0.6)

**Orden de verificación:**
```bash
python3 scripts/check-javadoc.py
```

**Salida (2026-09-16, tercera re-verificación honesta):**
```
Javadoc coverage: 248/248 (100.0%)
OK: Javadoc >= 90%
```

**Historial de defectos encontrados y corregidos en `check-javadoc.py`
(todas las fechas son 2026-09-16, en pasadas sucesivas de re-verificación
honesta contra la guía de evaluación, no una sola sesión):**

1. **Defecto original (versión "226/226"):** el patrón de método público
   también hacía match con la firma de un `record`
   (`public record DriverRequest(...)`, que en texto fuente se ve igual a
   una declaración de método con parámetros), inflando el conteo con los 37
   `record` (DTOs) del proyecto como si fueran métodos. Además, el patrón
   exigía `public`/`protected` explícito, así que los métodos de interfaz
   (que en Java son públicos implícitos, sin la palabra clave) quedaban
   fuera tanto del numerador como del denominador — esto incluye
   prácticamente todos los repositorios Spring Data JPA
   (`src/main/java/.../repository/*.java` y
   `.../abd/repository/*.java`), que son interfaces.

2. **Primera reescritura:** se corrigieron ambos defectos (se excluyen los
   `record`, se cuentan métodos de interfaz vía seguimiento de
   clase/interfaz por pila de llaves). Con la cuenta corregida, la
   cobertura real bajó a **215/281 (76.5%)**, por debajo del umbral del
   90%, revelando 66 líneas "sin Javadoc" — casi todas en interfaces de
   repositorio.

3. **Segundo defecto, encontrado al inspeccionar esas 66 líneas una por
   una:** la mayoría (42 de 66) eran falsos positivos, no métodos reales
   sin documentar. Causa: cuando un `@Query` usa un bloque de texto
   multilínea (`"""..."""`) para el SQL/JPQL, las líneas de continuación
   del SQL (p. ej. `OR LOWER(c.lastNames) LIKE ...`) coincidían con el
   patrón de "método implícito de interfaz" y se contaban como métodos
   inexistentes. Además, la función que busca el Javadoc inmediatamente
   arriba de un método solo saltaba líneas que empezaran con `@`, así que
   para una anotación multilínea se detenía en la primera línea de SQL
   (que no empieza con `@`) y reportaba "sin Javadoc" aunque el comentario
   Javadoc real estuviera unas líneas más arriba, antes de la anotación.
   Se corrigió agregando `_classify_annotation_lines`, que marca todas las
   líneas que pertenecen a una invocación de anotación (incluyendo sus
   líneas de continuación hasta el paréntesis de cierre), y se usa tanto
   para excluir esas líneas de la detección de métodos como para que la
   búsqueda de Javadoc las salte correctamente.

4. **Métodos reales sin Javadoc, una vez descontados los falsos
   positivos:** 24, todos interfaces de repositorio JPA en
   `src/main/java/ec/edu/uteq/sgroas/repository/`:
   `DriverRepository` (5: `findByActiveTrue`, `searchActive`,
   `existsByNationalId`, `existsByLicenseNumber`, `licensesExpiring`),
   `IncidentRepository` (5: `findByActiveTrue`,
   `findByAssignmentIdAndActiveTrue`, `incidentsBySeverity`,
   `getIncidentsByRange`, `generalStatistics`), `RouteAssignmentRepository`
   (6: `findByActiveTrue`, `findWithDetails`,
   `findByDriverIdAndActiveTrue`, `findByVehicleIdAndActiveTrue`,
   `findByRouteIdAndActiveTrue`, `activeAssignmentsByDriver`),
   `RouteRepository` (3: `findByActiveTrue`, `existsByCode`,
   `routePerformanceReport`), `VehicleRepository` (3: `findByActiveTrue`,
   `existsByPlate`, `vehiclesInMaintenance`), y
   `VerificationCodeRepository` (2:
   `findFirstByEmailAndTypeOrderByCreatedAtDesc`, `deleteByEmailAndType`).
   Se les agregó Javadoc real y específico (qué filtra o pagina cada
   consulta, `@param`/`@return` de cada método), sin inventar
   comportamiento no presente en la consulta derivada o en el `@Query`.

**Cifra final honesta: 248/248 métodos y constructores públicos/protegidos
de `src/main/java/**` (clases e interfaces, sin contar `record`) tienen
Javadoc inmediato — 100%, por encima del umbral del 90%.**

**Archivos:** los 24 métodos documentados están en
`src/main/java/ec/edu/uteq/sgroas/repository/{Driver,Incident,RouteAssignment,Route,Vehicle,VerificationCode}Repository.java`.
El checker corregido está en `scripts/check-javadoc.py`.

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

**Corrección de contenido en figuras, no solo captions (2026-09-17):**

La guía señaló dos defectos de contenido dentro de las propias imágenes
(el grep de arriba solo mira `\caption{...}`, nunca el contenido real de
un PNG):

1. Los diagramas C4 (`docs/arquitectura/c4-nivel1-contexto.png` y
   `c4-nivel2-contenedores.png`, copiados a `docs/informe-final/
   c4-level1-context.png` y `c4-level2-containers.png`) decían "Vue.js
   Frontend" / "[Container: Vue.js 3]", pese a que el frontend real es
   Angular:
   ```
   $ grep -n "@angular/core" frontend/package.json
   27:    "@angular/core": "^20.3.0",
   ```
   Se corrigieron los `.dsl` fuente
   (`docs/arquitectura/c4-nivel*.dsl`) y se regeneraron los 4 PNG con
   `scripts/gen-c4-diagramas.py` (nuevo), que lee el nombre/tecnología del
   contenedor frontend directamente del `.dsl` en vez de repetirlo
   hardcodeado.
2. `dataset/sus/fig-sus-demografia.png` (referenciada desde
   `cap5-materiales-metodos.tex`) tenía los tres paneles en español
   ("Sexo", "Masculino", "Experiencia web") y un error de encoding en el
   título ("Demograf?a"). No existía ningún script en el repositorio que
   la generara. Se creó `scripts/gen-sus-demografia-figura.py`, que la
   regenera desde `dataset/sus/sus-raw.csv` en inglés, con los mismos 15
   valores. Verificado con `grep -rln "Vue" docs/arquitectura/` (sin
   resultados) y revisión visual de las 3 figuras regeneradas.

Ambos scripts se agregaron al target `docs` del Makefile para que dejen de
ser artefactos huérfanos.

---

## P8 — Demografía SUS con trazabilidad CSV (0.7)

**Orden de verificación:**
```bash
python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv
bash scripts/validate-sus-demografia.sh
```

**Salida (2026-09-15, resumen de texto — histórica):**
```
Total participants: 15
Codes: P01..P15
Gender: 8 male, 7 female
Age range: 19-25 years (mean 21.2)
Web experience: Baja=3, Media=10, Alta=2
SUS score: mean=68.5, min=47.5, max=90.0
OK: demografia cap.5 cruza 1:1 con sus-raw.csv (n=15, 8H/7M, 19-25, B3/M10/A2, media SUS 68.5)
```

**Corrección de fondo (2026-09-17) — el "OK" de arriba era engañoso:** el
propio `scripts/validate-sus-demografia.sh` **nunca leía el `.tex`**;
comparaba el CSV contra constantes fijas escritas a mano en el script
(8, 7, 19-25, 3/10/2, 68.5, 47.5, 90.0). Ese "OK" habría salido igual
aunque la tabla del informe tuviera datos completamente distintos. Se
reescribió para que extraiga cada fila de `tab:sus-demografia` con regex y
la compare, campo por campo, contra `sus-raw.csv`. Salida real, re-ejecutada
ahora mismo en este working tree:

```
$ python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv
Total participants: 15
Codes: P01, P02, P03, P04, P05, P06, P07, P08, P09, P10, P11, P12, P13, P14, P15
Gender: 8 male, 7 female
Age range: 19-25 years (mean 21.2)
Web experience: Baja=3, Media=10, Alta=2
Devices: {'Computadora': 15}
SUS score: mean=68.5, min=47.5, max=90.0

$ bash scripts/validate-sus-demografia.sh
OK: las 15 filas de tab:sus-demografia (cap5-materiales-metodos.tex) cruzan 1:1, campo por campo, con dataset/sus/sus-raw.csv (codigo, edad, sexo, experiencia_web, sus_score)
```

Se verificó además que el script SÍ detecta discrepancias reales (no que
"siempre pasa"): en una copia temporal fuera del repositorio, alterando la
edad de P01 en el `.tex` de 23 a 99, el script reportó
`ERROR: P01: edad tex=99 != csv=23` y salió con código 1.

Adicionalmente, `scripts/generate-sus-demographics.py --latex` ahora
genera la tabla LaTeX completa (antes solo imprimía un resumen de texto;
la tabla del informe se mantenía a mano). Verificado que la salida es
byte por byte idéntica a la tabla actual:
```
$ python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv --latex > /tmp/gen.txt
$ diff <(sed -n '87,112p' docs/informe-final/cap5-materiales-metodos.tex | tr -d '\r') <(tr -d '\r' < /tmp/gen.txt)
(sin salida = archivos identicos)
```

`make verify` ([P8]) ahora corre también `validate-sus-demografia.sh`
(antes solo confirmaba que `generate-sus-demographics.py` no fallara al
ejecutarse).

**Archivos:**
- `dataset/sus/sus-raw.csv` — datos crudos (15 participantes)
- `scripts/generate-sus-demographics.py` — regenera el resumen de texto Y
  la tabla LaTeX (`--latex`) desde el CSV
- `scripts/validate-sus-demografia.sh` — cruza fila por fila `tab:sus-demografia`
  (leída del `.tex` real) contra el CSV
- Tabla `tab:sus-demografia` en `docs/informe-final/cap5-materiales-metodos.tex`
  (sección "Participantes SUS")

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

**Evidencia (2026-09-16, en vivo, stack local, código ya corregido):**
```
$ curl -s -i http://localhost:8080/api/asignaciones
403 (sin sesión)
$ curl -s -i -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
    -d '{"email":"admin@sgroas.com","password":"admin123"}' -c cookies.txt
200 (Set-Cookie access_token/refresh_token, Secure; HttpOnly; SameSite=Strict)
$ curl -s -i -b cookies.txt http://localhost:8080/api/asignaciones
200 — 8 elementos, campos en inglés (driverName, vehiclePlate, routeName, assignmentDate, ...)
```

**Evidencia VIGENTE (2026-09-17, en vivo, contra el despliegue público real):**
tras reconectar el servicio de Render al repositorio correcto y forzar un
redeploy, se reprodujo la misma prueba directamente contra
`https://sgroas-backend.onrender.com`:

```
$ curl -s -i https://sgroas-backend.onrender.com/api/asignaciones
403 (sin sesión)
$ curl -s -i -X POST https://sgroas-backend.onrender.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@sgroas.com","password":"admin123"}' -c cookies.txt
200 (Set-Cookie access_token/refresh_token, Secure; HttpOnly; SameSite=Strict)
$ curl -s -i -b cookies.txt "https://sgroas-backend.onrender.com/api/asignaciones?page=0&size=10"
200 — 8 elementos, campos en inglés (driverName, vehiclePlate, routeName, assignmentDate, ...)
```

Esta es la primera evidencia de P9 desde el redeploy que sí corresponde
de verdad al despliegue público con el código corregido (la corrida del
2026-09-15 contra la misma URL era de ANTES del renombrado, con campos en
español). Reemplaza tanto la corrida vieja contra Render (2026-09-15,
`conductorNombre`/`vehiculoPlaca`/`rutaNombre`) como la corrida local del
2026-09-16 como evidencia principal — ambas se conservan por trazabilidad.

**Archivos:**
- `docs/postman/coleccion.json` — carpeta "Asignaciones" con 6 requests CRUD
- Vigente (2026-09-17, contra Render real):
  `docs/mediciones/sec/live-session/asignaciones-200-20260917-render.txt`,
  `login-response-20260917-render.txt`, `sin-sesion-403-20260917-render.txt`
- Histórica (2026-09-16, stack local):
  `docs/mediciones/sec/live-session/asignaciones-200-20260916.json`,
  `login-response-20260916.txt`, `sin-sesion-403-20260916.txt`
- Desactualizada (2026-09-14/15, conservada por trazabilidad):
  `docs/mediciones/sec/live-session/asignaciones.json`, `auth-me.json`,
  `sin-sesion-403.txt`, `REPORT.md`

---

## P10 — Manifest SHA-256 verificable (0.5)

**Orden de verificación:**
```bash
sha256sum -c dataset/MANIFEST.sha256
```

**Salida (2026-09-15, resumida — ver salida LITERAL completa mas abajo):**
```
(283 "OK", 0 FAILED, 0 MISSING; exit code 0)
```

**Re-verificacion (2026-09-17): salida LITERAL completa, sin recortar
("..."), del comando real ejecutado en este working tree
(`tr -d '
' < dataset/MANIFEST.sha256 | sha256sum -c -`, necesario en
Windows por CRLF de este checkout local; en un clon Linux
`sha256sum -c dataset/MANIFEST.sha256` funciona directo por el
`eol=lf` de `.gitattributes`):**
```
dataset/DATA-DICTIONARY.md: OK
dataset/DATA-PROVENANCE.md: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdAlertController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdAlertController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdCatalogController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdCatalogController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdDriverController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdDriverController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdIncidentController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdIncidentController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdReportController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdReportController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdRouteController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdRouteController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdScheduleController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdScheduleController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdUnitController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdUnitController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdIncidentRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdIncidentResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdRouteRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdRouteResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AlertResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$CatalogsResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$CityResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$CountResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$ProvinceResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$RolResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$ScheduleRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$ScheduleResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$SummaryResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$TerminalResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$TopRouteResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$UnitRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$UnitResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdCatalogService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdCatalogService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdIncidentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdIncidentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdReportService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdReportService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdRouteService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdRouteService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdScheduleService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdScheduleService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdUnitService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdUnitService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/CacheConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/CacheConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/OpenApiConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/OpenApiConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RedisConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RedisConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RenderDataSourceConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RenderDataSourceConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/SecurityConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/SecurityConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/AuthController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/AuthController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/DriverController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/DriverController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/IncidentController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/IncidentController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/ReportController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/ReportController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteAssignmentController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteAssignmentController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/UserController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/UserController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/VehicleController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/VehicleController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/AuthResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/AuthResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/EmailRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/EmailRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ErrorResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ErrorResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/LoginRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/LoginRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RefreshTokenRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RefreshTokenRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ResetPasswordRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ResetPasswordRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/SessionResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/SessionResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VerifyEmailRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VerifyEmailRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/AssignmentStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/AssignmentStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/DriverStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/DriverStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentSeverity.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentSeverity.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentType.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentType.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/Role.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/Role.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/RouteStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/RouteStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/VehicleStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/VehicleStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/GlobalExceptionHandler.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/GlobalExceptionHandler.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/UnverifiedEmailException.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/UnverifiedEmailException.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/CustomUserDetailsService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/CustomUserDetailsService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtAuthenticationFilter.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtAuthenticationFilter.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/LoginRateLimiter.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/LoginRateLimiter.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/AuthService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/AuthService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/EmailService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/EmailService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/IncidentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/IncidentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/ReportService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/ReportService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteAssignmentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteAssignmentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/TokenService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/TokenService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/UserService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/UserService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VehicleService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VehicleService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService$Type.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/SgroasApplication.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/SgroasApplication.java.html: OK
dataset/jacoco/index.html: OK
dataset/jacoco/jacoco.csv: OK
dataset/jacoco/jacoco.xml: OK
dataset/jacoco/jacoco-resources/branchfc.gif: OK
dataset/jacoco/jacoco-resources/branchnc.gif: OK
dataset/jacoco/jacoco-resources/branchpc.gif: OK
dataset/jacoco/jacoco-resources/bundle.gif: OK
dataset/jacoco/jacoco-resources/class.gif: OK
dataset/jacoco/jacoco-resources/down.gif: OK
dataset/jacoco/jacoco-resources/greenbar.gif: OK
dataset/jacoco/jacoco-resources/group.gif: OK
dataset/jacoco/jacoco-resources/method.gif: OK
dataset/jacoco/jacoco-resources/package.gif: OK
dataset/jacoco/jacoco-resources/prettify.css: OK
dataset/jacoco/jacoco-resources/prettify.js: OK
dataset/jacoco/jacoco-resources/redbar.gif: OK
dataset/jacoco/jacoco-resources/report.css: OK
dataset/jacoco/jacoco-resources/report.gif: OK
dataset/jacoco/jacoco-resources/session.gif: OK
dataset/jacoco/jacoco-resources/sort.gif: OK
dataset/jacoco/jacoco-resources/sort.js: OK
dataset/jacoco/jacoco-resources/source.gif: OK
dataset/jacoco/jacoco-resources/up.gif: OK
dataset/jacoco/jacoco-sessions.html: OK
dataset/lighthouse/lhci-20260730-2115.json: OK
dataset/lighthouse/lhci-20260730-2117.json: OK
dataset/lighthouse/lh-desktop-1.json: OK
dataset/lighthouse/lh-desktop-2.json: OK
dataset/lighthouse/lh-desktop-3.json: OK
dataset/lighthouse/lh-mobile-1.json: OK
dataset/lighthouse/lh-mobile-2.json: OK
dataset/lighthouse/lh-mobile-3.json: OK
dataset/lighthouse/lh-tablet-1.json: OK
dataset/lighthouse/lh-tablet-2.json: OK
dataset/lighthouse/lh-tablet-3.json: OK
dataset/lighthouse/REPORT.md: OK
dataset/lighthouse/RESUMEN.md: OK
dataset/perf/ANALISIS-BOOTSTRAP.md: OK
dataset/perf/ANALISIS-k6.md: OK
dataset/perf/bootstrap.json: OK
dataset/perf/estadisticas.csv: OK
dataset/perf/estadisticas.json: OK
dataset/perf/figuras/fig-error-rate.png: OK
dataset/perf/figuras/fig-media-ic95.png: OK
dataset/perf/figuras/fig-p95-por-corrida.png: OK
dataset/perf/figuras/fig-percentiles-corridas.png: OK
dataset/perf/k01-run1.json: OK
dataset/perf/k02-run2.json: OK
dataset/perf/k03-run3.json: OK
dataset/perf/k04-cold.json: OK
dataset/perf/k04-run1.json: OK
dataset/perf/k05-cold.json: OK
dataset/perf/k05-run1.json: OK
dataset/perf/k06-cold.json: OK
dataset/perf/k06-run1.json: OK
dataset/perf/k07-cold.json: OK
dataset/perf/k07-run1.json: OK
dataset/perf/k08-cold.json: OK
dataset/perf/k08-run1.json: OK
dataset/perf/RENDER-REPORT.md: OK
dataset/perf/REPORT.md: OK
dataset/README.md: OK
dataset/sus/ANALISIS-SUS.md: OK
dataset/sus/CONSENT-FORM.md: OK
dataset/sus/CONSENT-REGISTRY.md: OK
dataset/sus/estadisticas-item.json: OK
dataset/sus/estadisticas-sus.json: OK
dataset/sus/fig-sus-demografia.png: OK
dataset/sus/figuras/fig-sus-item-respuestas.png: OK
dataset/sus/figuras/fig-sus-por-participante.png: OK
dataset/sus/P01.json: OK
dataset/sus/P02.json: OK
dataset/sus/P03.json: OK
dataset/sus/P04.json: OK
dataset/sus/P05.json: OK
dataset/sus/P06.json: OK
dataset/sus/P07.json: OK
dataset/sus/P08.json: OK
dataset/sus/P09.json: OK
dataset/sus/P10.json: OK
dataset/sus/P11.json: OK
dataset/sus/P12.json: OK
dataset/sus/P13.json: OK
dataset/sus/P14.json: OK
dataset/sus/P15.json: OK
dataset/sus/REPORT.md: OK
dataset/sus/SUS-INSTRUMENT.md: OK
dataset/sus/sus-raw.csv: OK
dataset/zap/RESUMEN.md: OK
dataset/zap/zap.html: OK
dataset/zap/zap.md: OK
dataset/zap/zap-baseline-2026-09-06.html: OK
dataset/zap/zap-baseline-2026-09-06.md: OK
dataset/zenodo.json: OK
```
dataset/DATA-DICTIONARY.md: OK
dataset/DATA-PROVENANCE.md: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdAlertController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdAlertController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdCatalogController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdCatalogController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdDriverController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdDriverController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdIncidentController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdIncidentController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdReportController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdReportController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdRouteController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdRouteController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdScheduleController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdScheduleController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdUnitController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/AbdUnitController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.controller/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdIncidentRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdIncidentResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdRouteRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AbdRouteResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$AlertResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$CatalogsResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$CityResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$CountResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$ProvinceResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$RolResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$ScheduleRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$ScheduleResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$SummaryResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$TerminalResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$TopRouteResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$UnitRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$UnitResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdCatalogService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdCatalogService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdIncidentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdIncidentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdReportService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdReportService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdRouteService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdRouteService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdScheduleService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdScheduleService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdUnitService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/AbdUnitService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.abd.service/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/CacheConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/CacheConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/OpenApiConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/OpenApiConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RedisConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RedisConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RenderDataSourceConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RenderDataSourceConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/SecurityConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/SecurityConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/AuthController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/AuthController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/DriverController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/DriverController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/IncidentController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/IncidentController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/ReportController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/ReportController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteAssignmentController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteAssignmentController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/RouteController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/UserController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/UserController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/VehicleController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/VehicleController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/AuthResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/AuthResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/DriverResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/EmailRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/EmailRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ErrorResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ErrorResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/IncidentResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/LoginRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/LoginRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RefreshTokenRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RefreshTokenRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ResetPasswordRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/ResetPasswordRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteAssignmentResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/RouteResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/SessionResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/SessionResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/UserResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleResponse.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VehicleResponse.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VerifyEmailRequest.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/VerifyEmailRequest.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/AssignmentStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/AssignmentStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/DriverStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/DriverStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentSeverity.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentSeverity.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentType.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/IncidentType.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/Role.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/Role.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/RouteStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/RouteStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/VehicleStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/VehicleStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/GlobalExceptionHandler.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/GlobalExceptionHandler.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/UnverifiedEmailException.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/UnverifiedEmailException.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/CustomUserDetailsService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/CustomUserDetailsService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtAuthenticationFilter.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtAuthenticationFilter.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/LoginRateLimiter.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/LoginRateLimiter.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/AuthService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/AuthService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/EmailService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/EmailService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/IncidentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/IncidentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/ReportService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/ReportService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteAssignmentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteAssignmentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/TokenService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/TokenService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/UserService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/UserService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VehicleService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VehicleService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService$Type.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/SgroasApplication.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/SgroasApplication.java.html: OK
dataset/jacoco/index.html: OK
dataset/jacoco/jacoco.csv: OK
dataset/jacoco/jacoco.xml: OK
dataset/jacoco/jacoco-resources/branchfc.gif: OK
dataset/jacoco/jacoco-resources/branchnc.gif: OK
dataset/jacoco/jacoco-resources/branchpc.gif: OK
dataset/jacoco/jacoco-resources/bundle.gif: OK
dataset/jacoco/jacoco-resources/class.gif: OK
dataset/jacoco/jacoco-resources/down.gif: OK
dataset/jacoco/jacoco-resources/greenbar.gif: OK
dataset/jacoco/jacoco-resources/group.gif: OK
dataset/jacoco/jacoco-resources/method.gif: OK
dataset/jacoco/jacoco-resources/package.gif: OK
dataset/jacoco/jacoco-resources/prettify.css: OK
dataset/jacoco/jacoco-resources/prettify.js: OK
dataset/jacoco/jacoco-resources/redbar.gif: OK
dataset/jacoco/jacoco-resources/report.css: OK
dataset/jacoco/jacoco-resources/report.gif: OK
dataset/jacoco/jacoco-resources/session.gif: OK
dataset/jacoco/jacoco-resources/sort.gif: OK
dataset/jacoco/jacoco-resources/sort.js: OK
dataset/jacoco/jacoco-resources/source.gif: OK
dataset/jacoco/jacoco-resources/up.gif: OK
dataset/jacoco/jacoco-sessions.html: OK
dataset/lighthouse/lhci-20260730-2115.json: OK
dataset/lighthouse/lhci-20260730-2117.json: OK
dataset/lighthouse/lh-desktop-1.json: OK
dataset/lighthouse/lh-desktop-2.json: OK
dataset/lighthouse/lh-desktop-3.json: OK
dataset/lighthouse/lh-mobile-1.json: OK
dataset/lighthouse/lh-mobile-2.json: OK
dataset/lighthouse/lh-mobile-3.json: OK
dataset/lighthouse/lh-tablet-1.json: OK
dataset/lighthouse/lh-tablet-2.json: OK
dataset/lighthouse/lh-tablet-3.json: OK
dataset/lighthouse/REPORT.md: OK
dataset/lighthouse/RESUMEN.md: OK
dataset/perf/ANALISIS-BOOTSTRAP.md: OK
dataset/perf/ANALISIS-k6.md: OK
dataset/perf/bootstrap.json: OK
dataset/perf/estadisticas.csv: OK
dataset/perf/estadisticas.json: OK
dataset/perf/figuras/fig-error-rate.png: OK
dataset/perf/figuras/fig-media-ic95.png: OK
dataset/perf/figuras/fig-p95-por-corrida.png: OK
dataset/perf/figuras/fig-percentiles-corridas.png: OK
dataset/perf/k01-run1.json: OK
dataset/perf/k02-run2.json: OK
dataset/perf/k03-run3.json: OK
dataset/perf/k04-cold.json: OK
dataset/perf/k04-run1.json: OK
dataset/perf/k05-cold.json: OK
dataset/perf/k05-run1.json: OK
dataset/perf/k06-cold.json: OK
dataset/perf/k06-run1.json: OK
dataset/perf/k07-cold.json: OK
dataset/perf/k07-run1.json: OK
dataset/perf/k08-cold.json: OK
dataset/perf/k08-run1.json: OK
dataset/perf/RENDER-REPORT.md: OK
dataset/perf/REPORT.md: OK
dataset/README.md: OK
dataset/sus/ANALISIS-SUS.md: OK
dataset/sus/CONSENT-FORM.md: OK
dataset/sus/CONSENT-REGISTRY.md: OK
dataset/sus/estadisticas-item.json: OK
dataset/sus/estadisticas-sus.json: OK
dataset/sus/fig-sus-demografia.png: OK
dataset/sus/figuras/fig-sus-item-respuestas.png: OK
dataset/sus/figuras/fig-sus-por-participante.png: OK
dataset/sus/P01.json: OK
dataset/sus/P02.json: OK
dataset/sus/P03.json: OK
dataset/sus/P04.json: OK
dataset/sus/P05.json: OK
dataset/sus/P06.json: OK
dataset/sus/P07.json: OK
dataset/sus/P08.json: OK
dataset/sus/P09.json: OK
dataset/sus/P10.json: OK
dataset/sus/P11.json: OK
dataset/sus/P12.json: OK
dataset/sus/P13.json: OK
dataset/sus/P14.json: OK
dataset/sus/P15.json: OK
dataset/sus/REPORT.md: OK
dataset/sus/SUS-INSTRUMENT.md: OK
dataset/sus/sus-raw.csv: OK
dataset/zap/RESUMEN.md: OK
dataset/zap/zap.html: OK
dataset/zap/zap.md: OK
dataset/zap/zap-baseline-2026-09-06.html: OK
dataset/zap/zap-baseline-2026-09-06.md: OK
dataset/zenodo.json: OK
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

**Riesgo de Piso 3 — RESUELTO el 2026-09-17 con evidencia real (ver historial completo abajo):**

`dataset/sus/CONSENT-REGISTRY.md` afirma que los 15 participantes firmaron
consentimiento el 2026-08-15 "antes de la evaluación", pero
`docs/mediciones/sus/sus-raw.csv` con los resultados de P01–P10 ya estaba
commiteado el 2026-07-30 (commit `e8d7e2f`, 16 días antes de esa fecha de
firma). El otro registro que existía,
`docs/etica/consentimientos/registro.md`, declaraba una fecha de firma
distinta (2026-07-30) para solo 10 de los 15 participantes, y citaba como
"evidencia" los propios JSON de respuestas del SUS (que solo contienen un
campo autodeclarado `"consentimiento": "Sí"`, no una constancia
independiente). Comandos ejecutados y salida literal:

```
$ git log --format='%H %ad %an %s' --date=short e8d7e2f -1
e8d7e2f6d02942f252ae4622b8b7cec73ddfd99a 2026-07-30 charito20 feat(entrega3): evidencia SUS real (10 participantes, media 63.0 IC95 [53.1;72.9]) e informe/SRS con cobertura final

$ git log --follow --format='%H %ad %an' --date=short -- dataset/sus/P01.json
ce0099f1ed27d1ffded45f02fa7ca2c4ab1f9002 2026-09-01 Alxjandr07
771b48ed9b89f3dd718178d70436c383ad1635b3 2026-08-16 TheAsesink
```

Hasta el 2026-09-16, no existía en el repositorio ninguna constancia de
consentimiento firmada de forma independiente para ningún participante. Se
consolidó todo en `docs/etica/consentimientos/CONSENT-STATUS.md` (nuevo en
esa fecha) con el detalle completo, se dejó una nota de discrepancia en
ambos registros existentes (no se borró ni se inventó ninguna fecha), y se
corrigió el nombre de la universidad ("Quintanilla Normal University" ->
"Universidad Técnica Estatal de Quevedo") en `dataset/sus/CONSENT-FORM.md`,
única ocurrencia en todo el repositorio.

**Actualización 2026-09-17 — constancias reales localizadas:** el equipo
encontró los 15 formularios de consentimiento firmados en papel
(`p01 firma.pdf` … `p15 firma.pdf`). Verificación realizada por esta
auditoría sobre los archivos reales:

```
$ python3 -c "
import hashlib, os
d = r'consentimientos firmados pdf'
for i in range(1,16):
    code = f'P{i:02d}'
    path = os.path.join(d, f'p{i:02d} firma.pdf')
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        h.update(f.read())
    print(f'{code}: {h.hexdigest()}')
"
P01: a3034963a789df06cff212b43d2ef6e5c9b96ddc3cdf335e8b34b83e5c05739a
P02: 8381f4e8f0fe16dd05ddc668d200d5deefc5d1be5db66800ebdfe9c69cdec5f8
P03: 33f4417909ee4fce3f2d13063445f10cbf1a5e70201ec41aa0f9904abcb726fb
P04: b3d0a209390f4e43abf79fac827110b277aaf0de08ee8fdffc37e6290496cad3
P05: 8aeba8200d4b814820d4f3487f561c1820665bb769610ee118252cad5787c2f0
P06: a00523abf50a51937ebcf9b6b89d1e5014cf645172ca285b6e3ecc8214959b10
P07: f36ed2050e67272494e7d3d6f96bd413d8cf3cf8502e9cd2f3203d7bcdb558fd
P08: 11891dd8e8a4fd382da0a82ff0cb7db2364a2eee8fc4a6d30754b79758645179
P09: db055bc4c2429933d98d6a6344c35c3e412d33948dcd9e3671a62af3d682e593
P10: b0b864109c3665c9430a8b6bab5c7d1cbd22d39ac3759843a56dfdd826d283df
P11: 6113753eb0aa166f95f921acb47996e48ca7327d8a994b37f853a2e1ce451c50
P12: a9aba08d40926da009928790f26848b4efc4314e8c3a79623969014e5c1c13e4
P13: 322beccf5d42f83d2313d698b84ac4016cb877951981ec7d3431907afbd99881
P14: 341cf1b59d4e3606a051d4ae601581dec1e3c1a0f1bfa817ba160a020462fb44
P15: b7b2677cfc577b65c75fd1e30dace114f9900ec7d1e3d5049f66b793c46012ee
```

15 hashes distintos (0 duplicados), recalculados también con `sha256sum`
con resultado idéntico. Fecha de firma extraída de cada PDF con
`pdftotext -layout`: P01–P10 = 2026-07-24, P11–P15 = 2026-07-26 — ambas
antes de la evaluación real de cada grupo (P01–P10: 2026-07-30, respaldado
por el commit `e8d7e2f` del mismo día; P11–P15: 2026-08-16, declarado por
el equipo — corregido el 2026-09-17 tras una confusión inicial de fechas).

**Corrección de un error propio de esta auditoría (2026-09-17):** una
versión anterior de este documento generalizó por error que los archivos
`P11.json`…`P15.json` se habían subido "el 2026-08-16" (extrapolando un
`git log --follow` corrido solo sobre `P01.json`). Verificado ahora
individualmente:

```
$ git log --format='%H %ad %an %ae %s' --date=short --diff-filter=A -- "docs/mediciones/sus/P11.json"
351601997ba1b5a6182ab3f5a13cd9b3c08b3403 2026-09-06 charito20 mescuderop@uteq.edu.ec feat(sus): agrega P11-P15 - n=15, media=68.5, IC95% [60.76; 76.24] (guia docente 3.7)
```

`P11.json`…`P15.json` se suben por primera vez el **2026-09-06** (no el
16-ago, que corresponde solo a `P01.json`…`P10.json`), tres semanas después
de la evaluación declarada del 16-ago — un intervalo comparable al de
P01-P10 (evaluados 30-jul, JSON individuales subidos 16-ago).

Los documentos originales **no se subieron al repositorio** (contienen
firmas manuscritas); el registro público
(`dataset/sus/CONSENT-REGISTRY.md`, reescrito) solo publica el hash de cada
uno. Detalle completo en `docs/etica/consentimientos/CONSENT-STATUS.md`
sección 7.

**Conclusión: el riesgo de Piso 3 sobre P11 queda resuelto** — ya no hay
fechas contradictorias ni constancias inventadas; hay consentimiento
individual verificable, previo a la evaluación, para los 15 participantes.
La única reserva declarada es que la fecha de evaluación de P11–P15
(2026-08-16) es una declaración del equipo, no confirmada por un artefacto
de git del mismo día como sí ocurre con P01–P10 (cuyo commit `e8d7e2f` es
del día mismo de la evaluación).

---

## P0 — Contrato backend/frontend roto por el renombrado a inglés (CRÍTICO, 2026-09-16)

El backend expone `SessionResponse.role`, `UserResponse.role`,
`DriverRequest/DriverResponse.firstNames/lastNames/nationalId`, etc. (inglés),
pero el frontend Angular seguía usando `.rol`, `.nombre`, `.nombres`,
`.apellidos`, `.cedula` en varios lugares. Se corrigieron los modelos y
componentes que consumen la API principal (inglés):

- `frontend/src/app/core/models/auth.model.ts` — `Sesion.rol`->`role`, `nombre`->`name`
- `frontend/src/app/core/models/usuario.model.ts` — `Usuario`/`UsuarioRequest`: `nombre/rol/activo/creadoEn/actualizadoEn` -> `name/role/active/createdAt/updatedAt`
- `frontend/src/app/core/models/conductor.model.ts` — `Conductor`/`ConductorRequest`: `nombres/apellidos/cedula/numeroLicencia/tipoLicencia/fechaVencimientoLicencia/telefono/estado` -> `firstNames/lastNames/nationalId/licenseNumber/licenseType/licenseExpiry/phone/status`
- `frontend/src/app/core/services/auth.ts` — `rolActual()` lee `.role` en vez de `.rol`
- `frontend/src/app/features/usuarios/formulario/usuario-formulario.ts`, `frontend/src/app/features/usuarios/lista/usuario-lista.html`
- `frontend/src/app/features/conductores/formulario/conductor-formulario.ts`, `frontend/src/app/features/conductores/lista/conductor-lista.html`
- `frontend/src/app/features/dashboard/overview/overview.ts` — `currentUser()?.nombre` -> `.name` (bug real que rompía el saludo del dashboard)
- `frontend/src/app/features/dashboard/shell/shell.ts` y `shell.html` — mismo bug `.nombre` -> `.name`

**Excluidos deliberadamente (falsos positivos, NO se tocaron):** `programaciones-lista.ts/.html`,
`reporte-personalizado.ts` y `abd.model.ts` consumen el módulo ABD
(`AbdDtos`, entidades `AbdDriver`, `AbdRoute`, etc.), que usa nombres en
español de forma nativa y consistente en su propio backend
(`src/main/java/ec/edu/uteq/sgroas/abd/`). Verificado leyendo
`AbdDtos.java` línea por línea: `ProgramacionAbd.conductorNombres`,
`ConductorAbd`/`AbdDriver` con `nombres`, etc. — esos campos SÍ coinciden
con lo que devuelve su backend, por lo que renombrarlos habría roto ese
módulo, no arreglado nada.

**Verificación de compilación (2026-09-16):**
```
$ ./mvnw -q compile
(sin salida = BUILD SUCCESS; solo warnings de Lombok/Unsafe no relacionados)

$ cd frontend && npm run build
Application bundle generation complete. [9.596 seconds]
▲ [WARNING] bundle initial exceeded maximum budget. Budget 500.00 kB was not met by 25.27 kB with a total of 525.27 kB.
(warning preexistente de tamaño de bundle, no relacionado con este cambio; sin errores de tipos)
```

**Lo que se pudo verificar:** compilación limpia de backend (Maven) y
frontend (Angular/TypeScript) contra los modelos ya corregidos; búsqueda
exhaustiva con `grep` de `.rol\b` y `nombres/apellidos/cedula` en todo
`frontend/src` para confirmar que no queda ningún campo español sin
corresponder fuera del módulo ABD.

**Actualización (2026-09-16, evidencia en vivo posterior):** en una sesión
posterior sí se levantó el stack completo (Postgres 18 + Redis 7 vía
`docker compose`, backend con `./mvnw spring-boot:run`, frontend con
`ng serve`) y se probó de punta a punta con navegador real: login exitoso,
navegación por rol (`ROLE_ADMIN`) y alta de conductor con
`POST /api/conductores` → `201 Created` sin error de validación (ver
`docs/mediciones/sec/live-session/README.md` y
`docs/mediciones/sec/live-session/conductor-alta-201-20260916.json`). No se
probaron los roles `ROLE_COORDINADOR`/`ROLE_SEGURIDAD` por separado — eso
queda declarado como no verificado, no se infiere ni se asume.

Nota histórica: en el momento en que se escribió este párrafo por primera
vez (antes de esa sesión en vivo) sólo se había validado por compilación,
sin base de datos disponible (`Connection to localhost:5433 refused`); esa
limitación quedó superada por la evidencia en vivo posterior, documentada
arriba.

---

## make verify (EV-2)

**Orden de verificación:**
```bash
make verify
```

**Historial de esta auditoría (dos corridas reales, ninguna inventada):**

1. **2026-09-16, primera corrida — FALLABA de verdad** (exit code 1),
   porque el checker de P5 recién reescrito detectaba correctamente los
   ~83 métodos de test en español que el checker anterior (con el bug del
   regex `\b`) ocultaba. No se maquilló nada para forzar un
   "ALL CHECKS PASSED" falso en ese momento.
2. **2026-09-17, segunda corrida (final) — PASA de verdad**, después de
   completar el renombrado P5 en `src/test/java` (ver sección "P5" más
   arriba: 71 métodos no-ABD + 6 clases de test ABD mal nombradas + 12
   métodos ABD renombrados, más la corrección del falso positivo
   "terminal" en el propio checker) y de regenerar las dos entradas del
   manifiesto SHA-256 que cambiaron por la corrección honesta de P11
   (`dataset/sus/CONSENT-FORM.md` y `CONSENT-REGISTRY.md`, cuyo contenido
   se corrigió deliberadamente, así que su hash cambió — no es un dato
   inventado, es la consecuencia esperada de arreglar esos archivos).

**Salida literal completa (2026-09-17, corrida final):**
```
=== SGROAS Verification ===

[P1] Checking hardcoded secrets...
[P1] OK

[P2] Checking raw k6 runs (hot x5 + cold x5) reproducible contrast...
  6 hot runs found
  5 cold runs found
  OK: nonparametric contrast reproducible (nonparametric.py)
[P2] OK

[P4] Checking cookie Secure(true)...
  Found 5 .secure(true) calls
[P4] OK

[P5] Checking Spanish field names in entities...
[P5] OK - no Spanish fields in entities
[P5] Checking Spanish method names in main...
[OK] Metodos en src/main: 0/226 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/131 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 0/293 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/test: 0/42 en espanol (0.00%, umbral 5%)
[OK] Metodos combinados (main+test): 0/519 en espanol (0.00%, umbral 5%)
[OK] Tipos combinados (main+test): 0/173 en espanol (0.00%, umbral 5%)

OK: todas las categorias por debajo del umbral del 5%

[P6] Checking Javadoc coverage on public methods...
Javadoc coverage: 248/248 (100.0%)
OK: Javadoc >= 90%

[P7] Checking Spanish captions in informe...
[P7] OK - all figure/table captions in English

[P10] Verifying MANIFEST.sha256...
(283 archivos verificados, todos OK, incluidos dataset/sus/CONSENT-FORM.md
y dataset/sus/CONSENT-REGISTRY.md con sus hashes regenerados tras la
corrección de contenido de P11)
[P10] OK

[P11] Checking SUS instrument and consent...
  SUS-INSTRUMENT.md exists
  CONSENT-FORM.md exists
  CONSENT-REGISTRY.md exists
[P11] OK

[P3] Checking Lighthouse runs...
  9 lighthouse runs found
[P3] OK

[P8] Checking SUS demographics script...
  OK: SUS demographics script runs
[P8] OK

[P9] Checking Postman collection...
  9 assignment endpoints found
[P9] OK

==========================================
ALL CHECKS PASSED
==========================================
```

**Confirmado con `echo $?` inmediatamente después: `0`.** `make verify`
pasa limpio y honestamente en este momento. La nota de discrepancia de
P11 (`docs/etica/consentimientos/CONSENT-STATUS.md`) sigue vigente como
documentación — `make verify` solo comprueba que los archivos existen
(`test -f`), no evalúa la validez del consentimiento en sí, así que su
"OK" no contradice la advertencia de Piso 3 documentada más arriba.

**Compilación y tests tras el renombrado final (2026-09-17):**
```
$ ./mvnw -q compile test-compile
(sin salida = BUILD SUCCESS)

$ ./mvnw -q test
...
[ERROR] Tests run: 293, Failures: 0, Errors: 16, Skipped: 0
```
Los mismos 16 errores de siempre (`Connection to localhost:5433 refused`
— no hay Postgres corriendo en este entorno de auditoría), 0 fallos
nuevos, 277/293 pasan. Confirmado también con `npm run build` en
`frontend/` (bundle generado sin errores, mismo warning preexistente de
tamaño de bundle).

---

## CONTRIBUCIONES.md (EV-4)

Ver archivo `CONTRIBUCIONES.md` en la raíz del repositorio. **Actualizado
2026-09-1X:** solo Luis Alejandro Tejada Bajaña firma la ronda de
correcciones del examen suspenso (sección "Firmas"); María del Rosario
Escudero Plaza y Kevin Moisés Castro Espinoza ya aprobaron su evaluación
individual y no firman esta ronda porque no participaron en ella — sus
aportes históricos al proyecto original siguen reconocidos en las
secciones P3 y P8 con respaldo de `git log`. Ver la sección "Situación
del equipo en el examen suspenso" dentro de `CONTRIBUCIONES.md` para el
detalle completo, incluyendo el comando de `git log` que confirma
objetivamente que ninguno de los dos tiene commits en esta ronda.

---

## Tag v1.1.0 (EV-3) — corrección de un defecto ya señalado por la guía externa

**Este documento decía que `v1.1.0` apunta a `8398462`. Es incorrecto y ya
había sido señalado por la guía externa en EV-1** ("Declara que la etiqueta
apunta a `8398462`, pero apunta a `f2fb883`"). Nunca se había corregido
hasta esta re-verificación (2026-09-17). Salida literal:

```
$ git rev-parse v1.1.0
847ee442a69660bf76a841fc5d711b56bf21f9cf
$ git rev-parse v1.1.0^{commit}
f2fb8838dbc8762df9b2a8bb1800d77c13ee2368
$ git log --oneline v1.1.0 -1
f2fb883 docs(portada): commit final f7b9b72 en portada; PDF re-generado con hash correcto
$ git log --oneline 8398462 -1
8398462 fix(P10): regenerar MANIFEST sobre Jacoco CSV normalizado a LF
```

(`v1.1.0` es un tag anotado: `git rev-parse v1.1.0` da el hash del propio
objeto tag, `847ee44`; `git rev-parse v1.1.0^{commit}` o
`git log --oneline v1.1.0 -1` resuelven al commit real, `f2fb883`.)

`8398462` es un commit anterior en la misma cadena de cierre, no el commit
al que apunta el tag. `v1.1.0` -> `f2fb883` es la versión que evaluó la
guía externa, con todos los defectos allí descritos.

## Tag v1.1.1 (2026-09-17, histórico) — primera ronda de correcciones

Apuntaba al commit con la primera ronda de correcciones: contrato
backend/frontend (P0), P2 (script honesto), P5 (0% real en main y test),
P6 (248/248 real contando interfaces, sin contar `record`), secretos
(redactados/removidos), configuración muerta (`app.cookie.secure`), y
evidencia en vivo nueva de P4/P9. **En ese momento P11 seguía
documentado como riesgo de Piso 3 NO resuelto** (sin constancias reales
localizadas todavía). Superado por `v1.1.2`; se conserva sin mover ni
borrar, como referencia histórica.

## Tag v1.1.2 (2026-09-17, historico) — resuelve P11 con una fecha luego corregida

Apunta al commit final de esta auditoría. Agrega sobre `v1.1.1`:

- **P11: riesgo de Piso 3 RESUELTO** con constancias de consentimiento
  reales localizadas y verificadas (ver sección "P11" más arriba para el
  detalle completo con hashes SHA-256 y fechas extraídas de cada PDF).
- **EV-1 adicional:** salida de P10 recortada con "..." reemplazada por
  las 283 líneas literales; placeholders `CHANGE_ME` inexistentes
  corregidos en `CONTRIBUCIONES.md`; mención desactualizada de que
  `CONTRIBUCIONES.md` está "firmado por los tres integrantes" corregida.

**Nota histórica:** la fecha de evaluación de P11-P15 usada en este tag
(2026-08-03) resultó ser incorrecta. Ver "Tag v1.1.3" abajo, el tag vigente.

## Tag v1.1.3 (2026-09-17) — vigente: corrige la fecha real de evaluación de P11-P15

Apunta al commit final de esta auditoría. Corrige sobre `v1.1.2`:

- La fecha de evaluación de P11-P15 declarada por el equipo: **2026-08-16**
  (no 2026-08-03, dato provisional usado por error en `v1.1.2`).
- Un error propio de esta auditoría: se había generalizado que los
  archivos `P11.json`…`P15.json` se subieron al repositorio "el 2026-08-16"
  (esa fecha corresponde solo a `P01.json`…`P10.json`). Verificado
  individualmente, `P11.json`…`P15.json` se suben el **2026-09-06**
  (commit `3516019`, autoría de María del Rosario Escudero Plaza).

Se verifica:

```bash
git rev-parse v1.1.3^{commit}
git log --oneline v1.1.3 -1
```

No se regeneró el PDF de `docs/informe-final/` para esta ronda: su portada
sigue citando el commit `f7b9b72` de la entrega final original, un hito
distinto de esta corrección del examen suspenso. Ninguno de los defectos
señalados por la guía está en el contenido del informe en sí.

## Corrección final (2026-09-17) — P11-P15 retirados del estudio SUS oficial

Las secciones "Tag v1.1.2" y "Tag v1.1.3" de arriba documentan cómo se
resolvió el problema de las **fechas de consentimiento** de P11-P15 (que sí
quedó resuelto: los 15 formularios firmados son reales y verificados por
SHA-256). Sin embargo, una revisión posterior encontró un problema distinto
y más grave: **los valores de las respuestas al cuestionario SUS de
P11-P15** (no su consentimiento, sino sus 10 respuestas Likert y su puntaje
SUS) no tienen ningún respaldo documental anterior a `scripts/update-sus.py`
(commit `3516019`, 2026-09-06) — no existe CSV exportado, captura ni
registro de sesión que demuestre que esas 5 personas respondieron el
cuestionario con esos valores en la fecha declarada.

Resolver la fecha de consentimiento no resuelve esto: dar consentimiento
real no prueba que la encuesta SUS se haya aplicado y registrado
correctamente. Por lo tanto, para eliminar el riesgo de presentar datos sin
trazabilidad ("Piso 3") en el estudio de usabilidad, se tomó esta decisión:

- **El estudio SUS oficial vuelve a n=10 (P01-P10)**, el subconjunto con
  respaldo documental sólido y sin contradicciones de fecha: CSV subido el
  2026-07-30 (`e8d7e2f`), consentimientos firmados el 2026-07-24. Cifras:
  media 63,0, DT 13,88, IC 95 % [53,07; 72,93] (`dataset/sus/REPORT.md`,
  regenerado con `scripts/generate-sus-demographics.py` y
  `scripts/sus-analysis.py`).
- **P11-P15 se retiran del análisis oficial**, pero no se ocultan: su
  consentimiento real y verificado se documenta en
  `dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md` y en la sección final de
  `dataset/sus/CONSENT-REGISTRY.md`, junto con sus hashes SHA-256.
- `dataset/sus/sus-raw.csv`, `dataset/sus/CONSENT-REGISTRY.md`, la tabla
  `tab:sus-demografia` de `docs/informe-final/cap5-materiales-metodos.tex` y
  todas las menciones de "n=15" / "68,5" / "60,76-76,24" en el informe se
  corrigieron a los valores de n=10. `dataset/sus/P11.json`…`P15.json` se
  movieron a `dataset/sus/no-verificados/` (README explicando el motivo) y
  `scripts/update-sus.py` se anotó como no ejecutable, conservado solo como
  evidencia de cómo se originó el problema.
- `dataset/MANIFEST.sha256` se recalculó para los archivos que cambiaron de
  contenido.

Verificación reproducible de las cifras oficiales:

```bash
python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv
python3 scripts/sus-analysis.py
scripts/validate-sus-demografia.sh
```

Este cambio se considera una **mejora** del expediente frente al riesgo de
Piso 3, no un retroceso: elimina la ambigüedad sobre datos sin trazabilidad
en vez de intentar justificarla.

## Tag v1.1.0 — MOVIDO (2026-09-17): esta es ahora la etiqueta vigente

La guía del examen dice, literalmente: *"Lo que no esté dentro de la
etiqueta no existe. Muevan `v1.1.0` al último commit que quieren que
revise."* Esta regla no se había aplicado hasta ahora — todas las
correcciones de este archivo se habían ido documentando bajo tags nuevos
(`v1.1.1`, `v1.1.2`, `v1.1.3`) en vez de mover `v1.1.0`, por precaución
general de no reescribir un tag ya publicado. Era un error: la guía exige
explícitamente mover `v1.1.0`, así que se hizo, con confirmación explícita
del responsable del repositorio (necesaria porque mover un tag anotado ya
publicado requiere `git push --force` sobre esa referencia).

```
$ git rev-parse v1.1.0^{commit}
c25dc0680fb86d280aef95b3f16a24776801f082
```

`v1.1.0` apunta ahora al mismo commit que `v1.1.3` y las secciones
posteriores de este documento (el commit que retira P11-P15 del estudio
SUS oficial). Los tags `v1.1.1`, `v1.1.2`, `v1.1.3` y `v1.1.4` se
conservan sin borrar como historial intermedio del proceso de corrección,
pero **`v1.1.0` es el único tag relevante para la evaluación** a partir de
ahora — es el que la guía revisa por definición, y el que contiene
absolutamente todas las correcciones documentadas en este archivo.

URL pública del sistema en la primera pantalla del README: `https://sgroas-backend.onrender.com`.