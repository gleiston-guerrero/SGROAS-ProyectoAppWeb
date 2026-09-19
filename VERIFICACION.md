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
  | grep -vF '${' | grep -vF '<ROTATED' | grep -vF 'secrets.' | grep -vF 'APP_COOKIE'
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

**Corrección real (2026-09-18) — el comando del paso 5 estaba roto,
señalado por una re-evaluación externa:** la versión anterior usaba
`grep -v '\$\{'` (regex extendida escapando `\{`), que en este `grep`
(GNU grep, modo básico para el patrón de `-v`) interpreta `\{` como el
inicio de una expresión de intervalo (`\{n,m\}`) y falla con
`grep: Unmatched \{` — el comando nunca llegaba a ejecutar la búsqueda
real, por eso "no encontraba nada": no es que no hubiera secretos, es
que el comando no corría. Corregido usando `grep -F` (cadena literal,
sin interpretar como regex) para las cuatro exclusiones. Re-ejecutado
ahora mismo:
```
$ grep -rEn "spring.datasource.password=([^$]|$)|POSTGRES_PASSWORD|APP_JWT_SECRET|app.jwt.secret" \
    src docker-compose.yml .env.example k6 .github 2>/dev/null \
    | grep -vF '${' | grep -vF '<ROTATED' | grep -vF 'secrets.' | grep -vF 'APP_COOKIE'
(sin salida, exit code 1 -- 0 lineas, ningun secreto literal encontrado)
```
Corre sin error y el resultado es el mismo que se declaraba (0
resultados), pero ahora es porque el comando de verdad se ejecutó y no
encontró nada, no porque fallara silenciosamente.

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
{"status":"UP","groups":["liveness","readiness"],"components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"diskSpace":{"status":"UP","details":{"total":414921494528,"free":68775636992,"threshold":10485760,"path":"/app/.","exists":true}},"livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"},"redis":{"status":"UP","details":{"version":"7.2.4"}},"ssl":{"status":"UP","details":{"validChains":[],"invalidChains":[]}}}}

$ curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST https://sgroas-backend.onrender.com/api/auth/login \
    -H "Content-Type: application/json" -d '{"email":"admin@sgroas.com","password":"admin123"}'
HTTP 200
```
(Salida re-verificada 2026-09-17, tarde: la versión anterior de este bloque
tenía la respuesta de `/actuator/health` resumida con "..." en vez de
literal — un defecto real señalado en una auditoría rigurosa propia. Se
corrige aquí con la salida completa y actual.)

`db: UP` confirma que el backend logró conectarse a Supabase con la
contraseña NUEVA (si la rotación hubiera fallado o quedado desincronizada,
este health check habría fallado). El login `200` confirma que el
`APP_JWT_SECRET` nuevo firma y valida tokens correctamente de punta a
punta. Ninguno de los dos valores reales (contraseña de BD ni JWT secret)
se expone en este documento ni en ningún archivo del repositorio.

**Hallazgo real y corrección adicional (2026-09-18):** la prueba anterior
rotaba la contraseña de la BD y el JWT secret (infraestructura), pero
usaba de forma deliberada `admin123` como credencial de la CUENTA de
aplicación `admin@sgroas.com` para el smoke test — la misma contraseña
sembrada por Flyway en `V2__seed.sql` para desarrollo local. Eso dejaba la
cuenta admin real de producción con una contraseña débil y conocida
públicamente en este mismo repositorio, un hallazgo correcto y grave
señalado en una auditoría externa. Se corrigió de inmediato: el
responsable del repositorio cambió la contraseña de `admin@sgroas.com` en
producción vía el flujo `POST /api/auth/forgot-password` +
`POST /api/auth/reset-password` (código enviado al correo real del admin,
sin que la nueva contraseña pase nunca por este asistente ni quede
registrada en ningún archivo). Verificado en vivo tras el cambio:

```
$ curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST https://sgroas-backend.onrender.com/api/auth/login \
    -H "Content-Type: application/json" -d '{"email":"admin@sgroas.com","password":"admin123"}'
HTTP 401
```

`admin123` ya NO es válida contra `admin@sgroas.com` en producción. Todos
los bloques anteriores de este documento que muestran un login `200` con
`admin123` contra Render (este bloque, y los de las secciones P1/P4/P9 más
abajo) quedan como evidencia histórica de cuándo se detectó y corrigió el
problema, no como estado actual del sistema. Las credenciales
`admin123`/`coord123`/`segur123` de `README.md` y `V2__seed.sql` siguen
siendo válidas únicamente para el seed de un entorno LOCAL de desarrollo
(`docker compose up` con una BD propia), nunca para el despliegue público.

**Limitación declarada (2026-09-17):** una re-evaluación externa señaló,
correctamente, que esta prueba demuestra que la contraseña NUEVA funciona,
pero no que la VIEJA haya dejado de ser válida — son afirmaciones
distintas. No es posible cerrar esa brecha desde este expediente sin
violar una regla de seguridad que se sigue en todo este proceso: quien
asiste en este repositorio nunca debe ver, recibir ni manipular una
credencial real de producción (solo el responsable del repositorio la
conoce, y solo él la ingresa directamente en los paneles de Supabase/
Render). Probar que la contraseña vieja ya no autentica requeriría que
alguien la escriba en algún punto de este flujo — exactamente lo que esa
regla prohíbe. La única evidencia disponible sin romper esa regla es la
de arriba (contraseña nueva funcionando de punta a punta) más la
declaración del responsable de haber rotado la contraseña desde el panel
de Supabase (Project Settings → Database → Reset database password, que
por diseño de Supabase invalida el valor anterior en el mismo acto de
generar el nuevo). Si se requiere prueba directa de que el valor viejo
falla, solo el propio responsable podría generarla, ejecutando la conexión
con el valor viejo desde su propio cliente fuera de este expediente y
pegando aquí únicamente el mensaje de error (nunca la contraseña) — no se
hizo porque el valor viejo ya no se recuerda tras la rotación, lo cual es,
en sí mismo, una señal más de que la rotación fue real y no cosmética.

**Archivos:** `src/main/resources/application.properties`, `docker-compose.yml`,
`.env.example`, `src/test/java/ec/edu/uteq/sgroas/security/JwtServiceTest.java`,
`src/test/resources/application-test.properties`, `.github/workflows/ci.yml`.

**Hallazgo pendiente, declarado sin ocultarlo:** las corridas crudas de k6
en `dataset/perf/*.json` capturan el `Authorization: Bearer <token>` real
que k6 usó en cada request, porque el propio k6 lo registra en el JSON de
salida — no es un secreto insertado a mano, es un efecto colateral de cómo
k6 documenta sus corridas. Quedan 13 tokens de admin versionados. Prueba de
que ya no son explotables:

```
$ TOKEN=$(grep -oE "eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}" dataset/perf/k01-run1.json | head -1)
$ echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null
{"jti":"f063abe3-125b-483b-9e86-c1a09b462358","iss":"https://sgroas.uteq.edu.ec","sub":"admin@sgroas.com","aud":["sgroas-frontend"],"iat":1785366530,"nbf":1785366530,"exp":1785370130,"nombre":"Admin SGROAS","rol":"ROLE_ADMIN"}
# exp=1785370130 -> 2026-07-29 (ya vencido: hoy es 2026-09-17)

$ curl -s -o /dev/null -w "HTTP %{http_code}\n" -H "Authorization: Bearer $TOKEN" \
    https://sgroas-backend.onrender.com/api/conductores
HTTP 403
```

El token es rechazado por dos razones independientes: (1) su `exp` venció
hace más de un mes, y (2) el `APP_JWT_SECRET` que lo firmó ya fue rotado
(sección anterior), así que ni siquiera un token no-expirado firmado con el
secreto viejo pasaría la verificación de firma. No se retiraron los 13
archivos del historial de git (reescribir historia es una operación
destructiva que no se hizo sin acuerdo explícito), pero los tokens que
contienen están probadamente inertes.

**Corrección real (2026-09-18) — el check de P1 en `Makefile` nunca podía
fallar, señalado por una re-evaluación externa:** las dos líneas del
check de secretos terminaban en `|| true`, así que el resultado del
`grep` se descartaba por completo — `make verify` reportaba `[P1] OK` sin
importar lo que hubiera en el árbol. Corregido eliminando `|| true`.
Prueba de mutación (revertida, no se deja en el repositorio): se agregó
temporalmente la línea `FAKE_SECRET: password123` a `docker-compose.yml`
y se corrió `make verify`:
```
[P1] Checking hardcoded secrets...
docker-compose.yml:60:    driver: bridgeFAKE_SECRET: password123
make: *** [Makefile:94: verify] Error 1
```
Ahora sí falla con un secreto real presente. Con el árbol restaurado
(`git status` limpio), `make verify` vuelve a dar `[P1] OK`.

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

**Actualización 2026-09-17 — hallazgo real confirmado y corregido: `@Cacheable`
sobre `Page` con serializador de Redis rompía en producción.** Un hallazgo de
una re-evaluación externa señaló que anotar con `@Cacheable` un método que
devuelve `Page<DriverResponse>`, con `Jackson2JsonRedisSerializer` configurado
en `CacheConfig`, rompería en cuanto la caché se activara de verdad contra
Redis. Se verificó sin necesidad de levantar Redis (el serializador de
Jackson es exactamente lo que convierte a/desde bytes; Redis solo los
almacena) en `CacheRedisSerializationTest`:

```
$ ./mvnw test -Dtest=CacheRedisSerializationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

El hallazgo era real y más grave de lo que parecía a primera vista:

1. `sinFix_cachedDriverPageVuelveComoMapGenerico_noComoElTipoReal`: con el
   `ObjectMapper` que `CacheConfig` tenía ANTES de este fix (sin
   `activateDefaultTyping`), `Jackson2JsonRedisSerializer.deserialize()`
   **nunca** reconstruye el tipo real cacheado, ni `Page` ni un DTO simple —
   devuelve un `LinkedHashMap` genérico, sin lanzar ninguna excepción. Es
   peor que un crash: falla en silencio, y cualquier código que intente
   llamar a un método propio del tipo real (`.getContent()`, `.content()`)
   sobre ese valor revienta con `ClassCastException`/`NoSuchMethodError` en
   el segundo hit de caché (el primero, sin caché, nunca lo expone).
2. `conFix_pageImplSigueSinPoderReconstruirse`: incluso arreglando el
   `ObjectMapper` para que sí incruste el tipo real (`activateDefaultTyping`
   restringido a `ec.edu.uteq.sgroas.*`, `java.util.*`, `java.time.*` y
   `java.lang.*`, para no habilitar deserialización polimórfica insegura
   sobre clases arbitrarias), `PageImpl` sigue sin poder reconstruirse:
   Jackson no tiene ningún constructor que pueda usar para esa clase
   (`Cannot construct instance of PageImpl ... no Creators`). `Page` no es
   cacheable directamente con Jackson vía Redis, sin importar cómo se
   configure el mapper.
3. `conFix_cachedDriverPageRoundTripsComoElTipoReal`: con el mapper
   corregido, un DTO simple (`record`) sí sobrevive el ciclo completo.

**Corrección aplicada:**
`src/main/java/ec/edu/uteq/sgroas/config/CacheConfig.java` — se activó
`activateDefaultTyping` sobre el `ObjectMapper` usado por el serializador de
Redis. `src/main/java/ec/edu/uteq/sgroas/service/DriverService.java` —
`listActiveCached` ya NO devuelve `Page<DriverResponse>`: devuelve un nuevo
record `CachedDriverPage(List<DriverResponse> content, long totalElements)`,
serializable/deserializable de verdad, y `list()` reconstruye el `Page` real
(`new PageImpl<>(...)`) fuera de la ruta cacheada, después de leer del caché.

**Actualización (2026-09-17 tarde) — auditoría rigurosa: el mismo bug (y
uno peor) estaba presente en otros 4 servicios, sin documentar hasta
ahora.** Al revisar sistemáticamente qué más podía depender del fix de
`DriverService`, se encontró que `IncidentService`, `RouteService` y
`VehicleService` llamaban a su propio `listCached(pageable)` de forma
directa (`this.` implícito, no vía proxy) — el mismo bug de
auto-invocación, nunca detectado porque no había ningún test que
verificara que el repositorio se invoca una sola vez en llamadas
repetidas (como sí existe para `DriverService` desde el fix anterior).
Además, un defecto de corrección real, más grave que el de caché:

```java
// ANTES, en los 3 servicios:
public Page<XResponse> list(Pageable pageable) {
    List<XResponse> contenido = listCached(pageable);
    return new PageImpl<>(contenido, pageable, contenido.size());
}
```
`contenido.size()` es el tamaño de la página actual (p.ej. 10), no el
total real de registros — `Page.getTotalElements()`/`getTotalPages()`
quedaban mal calculados en producción para Incidentes, Rutas y Vehículos,
independientemente del bug de caché.

`RouteAssignmentService` tenía un tercer defecto, distinto: su
`listCached()` nunca se llamaba desde ningún lado — `list()` consultaba el
repositorio directamente. Confirmado sin llamadores en todo el árbol:
```
$ grep -rn "\.listCached(" src/main/java src/test/java
(0 resultados antes del fix)
```
Este es exactamente el método que responde `GET /api/asignaciones`, el
endpoint verificado en vivo en la sección "P9" más abajo.

**Corrección aplicada, igual en los cuatro servicios** (mismo patrón que
`DriverService`): auto-inyección de `ObjectProvider<XService>` para que la
llamada pase por el proxy de Spring, y un record `CachedXPage(List<XResponse>
content, long totalElements)` en vez de `Page<XResponse>` cacheado
directamente (mismo motivo que `DriverService`: `PageImpl` no es
serializable por Jackson). Verificado:

```
$ ./mvnw test -Dtest=CacheRedisSerializationTest,IncidentServiceTest,RouteServiceTest,VehicleServiceTest,RouteAssignmentServiceTest
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
```

`CacheRedisSerializationTest` se extendió con
`withFix_cachedIncidentPageRoundTripsAsRealType`, probando que el mismo
patrón (`List<DTO>` + `long`) sobrevive el ciclo real de
serialización/deserialización de Redis para el nuevo record — los otros
tres (`CachedRoutePage`, `CachedVehiclePage`, `CachedAssignmentPage`) usan
exactamente la misma forma, ya probada.

Suite completa contra PostgreSQL real (Docker), tras el fix:
```
$ ./mvnw test
Tests run: 299, Failures: 0, Errors: 0, Skipped: 0
```

**Archivos modificados:**
- `src/main/java/ec/edu/uteq/sgroas/service/IncidentService.java`,
  `RouteService.java`, `VehicleService.java`, `RouteAssignmentService.java`
  — auto-inyección de proxy + `CachedXPage` en vez de `Page` cacheado.
- `src/test/java/ec/edu/uteq/sgroas/service/IncidentServiceTest.java`,
  `RouteServiceTest.java`, `VehicleServiceTest.java`,
  `RouteAssignmentServiceTest.java` — mock de `ObjectProvider<XService>`.
- `src/test/java/ec/edu/uteq/sgroas/config/CacheRedisSerializationTest.java`
  — nueva prueba para `CachedIncidentPage`.

**Actualización 2026-09-18 — contraste frío/caliente real, con
metodología corregida:** una re-evaluación externa señaló, correctamente,
tres problemas reales en el contraste anterior: (1) las 13 corridas
versionadas (k01–k08) son del 6-sep, ANTES del fix de auto-invocación de
`@Cacheable` de más arriba, así que ninguna midió caché real (ya se
reconocía esto explícitamente en este documento); (2) el contraste
comparaba `k6/cold.js` (1 usuario, 1 iteración) contra `k6/script.js` (50
usuarios, 30s) — perfiles de carga distintos, no comparables; (3) el
informe (`cap8-evaluacion.tex`, tabla de síntesis) presentaba 173.13ms
como si fuera "p95 caché caliente" sin aclarar que es una corrida LOCAL,
cuando el p95 real contra Render (con carga) es de 4.3–11.4s.

Se corrigieron los tres problemas de raíz, no solo el texto:

1. **Nuevo script `k6/cache-contrast.js`**: mismo usuario (1 VU), misma
   corrida secuencial, para frío Y caliente — sin mezclar perfiles de
   carga. La primera petición usa una clave de caché (`page=0&size=7`)
   garantizada nunca solicitada antes por ninguna otra corrida → miss
   real. Las 10 siguientes, contra la misma clave, ya con el caché
   activo → hits reales.
2. **Corrida real contra producción** (2026-09-18, después del fix de
   `@Cacheable`), ejecutada por el responsable del repositorio con la
   contraseña real de producción (nunca vista por este asistente):
   ```
   $ k6 run --summary-export=... -e K6_LOGIN_PASSWORD=*** \
       -e BASE_URL=https://sgroas-backend.onrender.com k6/cache-contrast.js
   duracion_fria      : avg=514.75ms med=514.75ms min=514.75ms max=514.75ms p(95)=514.75ms  (n=1, miss real)
   duracion_caliente  : avg=249.89ms med=191.59ms min=162.08ms max=721.00ms p(95)=504.77ms  (n=10, hits reales)
   checks.........................: 100.00% 11 out of 11
   http_req_failed.................: 0.00%   0 out of 12
   ```
   Guardado en `docs/mediciones/perf/k09-cache-contrast.json` y
   `dataset/perf/k09-cache-contrast.json` (el JSON original traía un JWT
   de sesión real en `setup_data.token`, vigente por 1 hora desde su
   emisión; se eliminó ese campo antes de versionar el archivo — no queda
   ningún secreto en el repositorio).
3. **Tabla de síntesis del informe corregida** (`cap8-evaluacion.tex`):
   ahora tiene dos filas separadas — "p95 < 200ms (local, sin
   throttling)" = 173.13ms = Sí, y "p95 < 200ms (Render, caché caliente,
   mismo usuario)" = 504.77ms = **No** — en vez de una sola fila que
   mezclaba ambas cosas bajo una sola etiqueta "caché caliente".

**Interpretación honesta:** la caché sí reduce la latencia típica
(mediana 514.75ms → 191.59ms, -63%), un efecto real medido con el mismo
usuario y el mismo perfil de carga en ambas condiciones — esto es
evidencia genuina de que `@Cacheable` funciona en producción, algo que
nunca se había medido hasta ahora. Pero el p95 en caliente (504.77ms)
sigue sin cumplir el umbral de 200ms: en el plan gratuito de Render la
variabilidad de red/CPU domina sobre el ahorro de consultar Redis en vez
de PostgreSQL. La caché ayuda, pero no es suficiente por sí sola para
cumplir el umbral de rendimiento en ese entorno — eso se declara así,
sin maquillar el resultado.

Verificado que el informe sigue compilando limpio tras el cambio:
```
$ cd docs/informe-final && pdflatex main && biber main && pdflatex main && pdflatex main
Output written on main.pdf (98 pages, 1259723 bytes).
```
(0 errores, 0 referencias sin resolver; sube de 97 a 98 páginas por la
fila nueva de la tabla y el párrafo de contraste K9.)

---

## P3 — Lighthouse corridas versionadas (1.0)

**CORRECCIÓN IMPORTANTE (2026-09-17) — la nota de abajo (2026-09-16) estaba
equivocada y llevó a trabajo redundante:** esa nota afirmaba que las 9
corridas `lh-{mobile,desktop,tablet}-{1,2,3}.json` eran "PRE-corrección de
contrato" y no contra el despliegue público, sin haber revisado el mensaje
de commit ni los metadatos del propio JSON. Verificado ahora:

```
$ git log --follow --format="%H %ad %an %s" --date=short -1 -- dataset/lighthouse/lh-desktop-1.json
b516447 2026-09-13 TheAsesink fix(P11,P15,P6,P16,P10): valida trazabilidad CRLF+exit1, dataset 275 arch sha256 OK, cookie HttpOnly sin token JSON, evidencia asignaciones 200
$ git log --format="%H %ad %an %s" --date=short -- dataset/lighthouse/lh-desktop-1.json | tail -1
dd6b81d 2026-09-06 charito20 perf(lighthouse): desktop corrida 1 - P=0.95 A=0.91 BP=0.92 SEO=0.9 (Render URL)
$ python3 -c "import json; d=json.load(open('dataset/lighthouse/lh-desktop-1.json')); print(d['requestedUrl'], d['lighthouseVersion'])"
https://sgroas-backend.onrender.com/ 13.4.1
```

`lh-desktop-{1,2,3}.json` / `lh-mobile-{1,2,3}.json` (María Escudero,
2026-09-06) **ya eran evidencia real contra el despliegue público**, con
Lighthouse 13.4.1 — el propio mensaje de commit lo dice literalmente
("Render URL"). Promediando sus 3 corridas por perfil: escritorio
P=95/A=91/BP=92/SEO=90, móvil P≈77 ((79+76+75)/3) /A=91/BP=92/SEO=90 —
estos son EXACTAMENTE los números que ya cita `resumen.tex` y
`capitulos/cap8-evaluacion.tex`. El informe nunca estuvo desactualizado
con datos falsos: ya citaba corridas reales y vigentes.

Lo que sí fue un error real, cometido en dos rondas de esta misma
auditoría (16 y 17 de septiembre): al no verificar el origen real de
`lh-desktop-1,2,3.json`, se generaron DOS tandas adicionales de corridas
completamente redundantes (`fresh-20260916`, contra un build local, y
`render-20260917`, contra el despliegue público otra vez pero con
Lighthouse 12.6.1 — una versión MÁS VIEJA que la 13.4.1 ya usada en
2026-09-06). Esa es la causa exacta del hallazgo de la re-evaluación
externa ("las puntuaciones no se propagaron al informe" — porque no había
nada mejor que propagar, las del informe ya eran las correctas — "la
versión retrocedió de 13.4.1 a 12.6.1" — por generar una tanda nueva sin
comprobar qué versión tenía la evidencia existente). Se corrige aquí la
narrativa: NO se generó nueva evidencia hoy porque ya no hacía falta; se
deja constancia del error de proceso para que no se repita.

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

**Orden de verificación (evidencia VIGENTE, la que cita el informe):**
```bash
# Mostrar URL auditada, version de Lighthouse y scores reales de las 3
# corridas de escritorio y 3 de movil que resumen.tex/cap8 realmente citan
python3 -c "
import json
for perfil in ('desktop', 'mobile'):
    for i in (1,2,3):
        d = json.load(open(f'dataset/lighthouse/lh-{perfil}-{i}.json', encoding='utf-8'))
        c = d['categories']
        print(perfil, i, d['requestedUrl'], d['lighthouseVersion'],
              {k: round(v['score']*100) for k, v in c.items()})
"
```

**Salida real:**
```
desktop 1 https://sgroas-backend.onrender.com/ 13.4.1 {'performance': 95, 'accessibility': 91, 'best-practices': 92, 'seo': 90, 'agentic-browsing': 100}
desktop 2 https://sgroas-backend.onrender.com/ 13.4.1 {'performance': 95, 'accessibility': 91, 'best-practices': 92, 'seo': 90, 'agentic-browsing': 100}
desktop 3 https://sgroas-backend.onrender.com/ 13.4.1 {'performance': 95, 'accessibility': 91, 'best-practices': 92, 'seo': 90, 'agentic-browsing': 100}
mobile 1 https://sgroas-backend.onrender.com/ 13.4.1 {'performance': 79, 'accessibility': 91, 'best-practices': 92, 'seo': 90, 'agentic-browsing': 100}
mobile 2 https://sgroas-backend.onrender.com/ 13.4.1 {'performance': 76, 'accessibility': 91, 'best-practices': 92, 'seo': 90, 'agentic-browsing': 100}
mobile 3 https://sgroas-backend.onrender.com/ 13.4.1 {'performance': 75, 'accessibility': 91, 'best-practices': 92, 'seo': 90, 'agentic-browsing': 100}
```
(`agentic-browsing` es una categoría nueva de Lighthouse 13.x sin umbral
propio en `lighthouserc.js`; no forma parte de los 4 umbrales evaluados por
la guía, se incluye aquí solo porque la salida debe ser literal.)

Promedios: escritorio P=95/A=91/BP=92/SEO=90; móvil P=76,7≈77/A=91/BP=92/SEO=90
— coincide exactamente con `resumen.tex` y `capitulos/cap8-evaluacion.tex`.
Los 4 umbrales de `lighthouserc.js` (`performance>=0.8` escritorio,
`accessibility/best-practices/seo>=0.9` ambos perfiles) se cumplen en
escritorio; en móvil, Performance (77) queda bajo el umbral de 80,
documentado como hallazgo esperado en el propio informe (`cap8`, sección
"Calidad web").

**Corridas adicionales redundantes (no aportan evidencia distinta, se
conservan solo por trazabilidad del proceso — ver corrección arriba):**

*2026-09-16, contra `main`/v1.1.3 local (`http://localhost:4200/` vía
`serve-gzip.js`), Lighthouse 13.4.1:*
```
1 http://localhost:4200/ {'performance': 100, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
2 http://localhost:4200/ {'performance': 98, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
3 http://localhost:4200/ {'performance': 100, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
1 http://localhost:4200/ {'performance': 89, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
2 http://localhost:4200/ {'performance': 88, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
3 http://localhost:4200/ {'performance': 85, 'accessibility': 91, 'best-practices': 100, 'seo': 90}
```

**Reproducir estas corridas locales (redundantes, no necesarias):**
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

**2026-09-17 — otras 6 corridas contra el despliegue público, TAMBIÉN
redundantes** (se generaron creyendo que hacía falta evidencia contra el
despliegue público porque no se había verificado que ya existía desde
2026-09-06; ver corrección al inicio de esta sección). Tras reconectar el
servicio de Render al repositorio correcto y redesplegar, se corrieron 3
móviles + 3 escritorio directo contra `https://sgroas-backend.onrender.com/`,
pero con Lighthouse 12.6.1 (más vieja que la 13.4.1 ya usada en las
corridas vigentes):

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
- `dataset/lighthouse/lh-{mobile,desktop,tablet}-{1,2,3}.json` (9 corridas,
  2026-09-06, María Escudero, contra el despliegue público real, Lighthouse
  13.4.1 — **son las VIGENTES, las que cita el informe**, ver corrección
  arriba).
- `dataset/lighthouse/lh-{mobile,desktop}-fresh-20260916-{1,2,3}.json` (6
  corridas, 2026-09-16, contra `main`/v1.1.3 local — redundantes, generadas
  por no haber verificado el origen de las de arriba; se conservan por
  trazabilidad del proceso, no aportan evidencia adicional necesaria).
- `dataset/lighthouse/lh-{mobile,desktop}-render-20260917-{1,2,3}.json` (6
  corridas, 2026-09-17, contra el despliegue público, Lighthouse 12.6.1 —
  también redundantes y con una versión de la herramienta más vieja que
  las de 2026-09-06; se conservan por trazabilidad del proceso).
- `dataset/lighthouse/lhci-20260730-2115.json` y `-2117.json` (2 corridas
  locales previas).
- `lighthouserc.js` — fix del preset movil invalido.
- Resumen historico en `dataset/lighthouse/REPORT.md`.

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

**Corrección real (2026-09-18) — el check de P4 en `Makefile` tampoco
podía fallar, señalado por una re-evaluación externa:** la línea pasaba
el resultado de `grep -c` por `xargs -I{} echo`, así que solo imprimía el
conteo como texto informativo — `echo` siempre sale con éxito, sin
importar el número (hasta un conteo de 0 hubiera dado `[P4] OK`).
Corregido con un `test $$(...) -ge 2` que sí falla si el conteo cae por
debajo de 2. Prueba de mutación (revertida): se redujeron temporalmente
los `.secure(true)` de `AuthController.java` de 5 a 1 y se corrió
`make verify`:
```
[P4] Checking cookie Secure(true)...
make: *** [Makefile:106: verify] Error 1
```
Ahora sí falla. Con el archivo restaurado (`git status` limpio), vuelve
a dar `[P4] OK` con las 5 llamadas reales.

**Archivos:**
- `src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java` — `.secure(true)` + `.httpOnly(true)`
- Cabecera Set-Cookie capturada EN VIVO el 2026-09-16 contra el backend local
  (post-corrección de contrato) en
  `docs/mediciones/sec/live-session/login-response-20260916.txt`:
  `Set-Cookie: access_token=...; Secure; HttpOnly; SameSite=Strict` (y `refresh_token` ídem, 7 días).
  Reemplaza a `login-response.txt` (2026-09-14, contra Render, con campos aún
  en español y con un JWT/refresh_token reales que quedaron versionados antes
  de ser redactados).

**Hallazgo adicional (2026-09-17 tarde, auditoría rigurosa propia, no es
uno de los 11 puntos de la guía pero se documenta igual): CSP bloqueaba el
propio `onload` inline que Angular genera para el CSS.** Verificado en la
consola del navegador contra el despliegue real:

```
Executing inline event handler violates the following Content Security
Policy directive 'script-src 'self''. Either the 'unsafe-inline' keyword,
a hash ('sha256-...'), or a nonce ('nonce-...') is required to enable
inline execution. ... The action has been blocked.
```

Causa real, confirmada inspeccionando el DOM (`document.querySelectorAll('*')`
filtrando atributos `on*`):
```
<link rel="stylesheet" href="styles-KUYJMIDB.css" media="print" onload="this.media='all'">
```
Angular CLI (build de producción, optimizador de estilos) inyecta este
patrón — "inline critical CSS" — para diferir la carga de CSS no crítico:
el `<link>` se carga con `media="print"` (no bloquea el render) y su
`onload` inline cambia `media` a `all` cuando termina de cargar. Nuestra
CSP (`script-src 'self'`, sin `unsafe-inline` ni nonce) bloquea ese
`onload`, así que la hoja de estilos nunca cambia de `print` a `all`. No
rompe visualmente la página en la práctica (los estilos igual se aplican
por cómo Chrome trata `media="print"` en pantalla en este caso concreto),
pero es un error de consola real y reproducible, no documentado hasta
ahora.

**Corrección:** se desactivó el "inline critical CSS" de Angular
(`frontend/angular.json`, configuración `production`:
`optimization.styles.inlineCritical: false`), lo que hace que Angular
emita un `<link rel="stylesheet">` normal, sin `onload` inline. Verificado:

```
$ grep -o '<link[^>]*stylesheet[^>]*>' frontend/dist/sgroas-frontend/browser/index.html
<link rel="stylesheet" href="styles-KUYJMIDB.css">
```

Sin errores de CSP en consola tras el cambio (probado localmente con
`serve-gzip.js` + navegador real). Impacto en rendimiento verificado con
Lighthouse local antes/después (mismo perfil móvil, mismo umbral,
`http://localhost:4200/`):

| | Antes (con inlineCritical) | Después (sin inlineCritical) |
|---|---|---|
| Performance | 88-89 (corridas `fresh-20260916`) | 87 |
| Best Practices | 100 | 100 |
| Accessibility | 91 | 91 |
| SEO | 90 | 90 |

Diferencia de rendimiento dentro del ruido normal entre corridas (no una
regresión real).

**Confirmado en producción (2026-09-18):** el auto-deploy de Render
(webhook GitHub→Render) se había quedado atascado en un commit 11 commits
atrás (`bb5a931`), sin desplegar nada de esta auditoría hasta ese punto —
un hallazgo de infraestructura real, no relacionado con el código. Se
disparó un "Manual Deploy" desde el dashboard de Render y, tras
completarse, se verificó en vivo:
```
$ [en la consola del navegador contra https://sgroas-backend.onrender.com]
document.querySelectorAll('*') filtrando atributos on* -> []
```
Sin `onload` inline, sin violación de CSP en consola. `dataset/README.md`
/`README.md` no requieren cambios (la URL pública es la misma).

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
{"jti":"b7b8434a-dfe0-4aa2-96d2-05bf1188e9f5","iss":"https://sgroas-backend.onrender.com","sub":"admin@sgroas.com","aud":["sgroas-frontend"],"iat":1789698509,"nbf":1789698509,"exp":1789702109,"name":"Admin SGROAS","role":"ROLE_ADMIN"}
```
(Re-verificado 2026-09-17 tarde con un login fresco contra el despliegue
real: la versión anterior de este bloque tenía varios campos con "..." en
vez del valor literal, defecto de la misma clase señalado en una
auditoría rigurosa propia.)

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

**Salida final (2026-09-18, cuarta pasada — checker corregido de raíz +
identificadores reales renombrados, ver notas de abajo):**
```
[OK] Metodos en src/main: 0/442 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/136 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 0/299 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/test: 0/45 en espanol (0.00%, umbral 5%)
[OK] Metodos combinados (main+test): 0/741 en espanol (0.00%, umbral 5%)
[OK] Tipos combinados (main+test): 0/181 en espanol (0.00%, umbral 5%)

OK: todas las categorias por debajo del umbral del 5%
```
Este 0.00% SÍ es reproducible: a diferencia del "0/484" de la pasada
anterior (denominador inflado, léxico incompleto), aquí el denominador
es el real (442, sin los 42 records duplicados) y el léxico ya cubre las
raíces del módulo `abd/` confirmadas manualmente — y además los 8
identificadores que ese léxico corregido encontró se renombraron de
verdad, no solo se dejaron documentados como pendientes.

**Nota de auditoria (2026-09-18) — tercer bug real del checker, esta vez
en dos direcciones a la vez:** una re-evaluación externa señaló,
correctamente, que el "0/484 (0.00%)" declarado no se podía reproducir de
forma creíble. Investigado a fondo, había dos defectos reales
combinados:

1. **Denominador inflado:** `METHOD_PATTERN` (con `public|protected`
   opcional desde el fix anterior) también casaba con declaraciones de
   `record` — `public record Foo(Integer x)` quedaba contado dos veces,
   una vez como tipo (correcto) y otra vez como si `Foo` fuera el nombre
   de un método (incorrecto, con la lista de componentes del record como
   si fueran sus argumentos). Con 42 records en `src/main`, esto inflaba
   el denominador de métodos de 442 reales a 484. Corregido: si una línea
   ya casó con `TYPE_PATTERN`, se cuenta solo como tipo y no se evalúa
   también contra `METHOD_PATTERN`.
2. **Léxico incompleto:** `SPANISH_ROOTS` se escribió antes de que
   existiera el módulo `abd/` (Bases de Datos Avanzadas, con su propio
   esquema nativo en español), y nunca se actualizó. Búsqueda manual
   confirmó identificadores reales en español en nombres de método/tipo
   que el checker no podía detectar por no estar en su lista: `mapa`,
   `nivel`, `provincia`, `ciudad`, `rol`, `sesion`, `sugerido`, `disco`,
   `numero`. Se agregaron esas 9 raíces a `SPANISH_ROOTS`.

Con ambos defectos corregidos aparecieron, en una primera pasada, 7
métodos y 1 tipo reales en español en `src/main`
(`findByNivelSugeridoIgnoreCase`, `getNivel`,
`findByProvinciaIdProvinciaOrderByIdCiudadAsc`,
`findByCiudadIdCiudadOrderByIdTerminalAsc`,
`existsByNumeroDiscoIgnoreCase`, `aSesion`, `mapa`, `RolResponse`).

**Nota de auditoria (2026-09-18) — cuarto bug real, esta vez en
`src/test`:** el mismo escaneo con un léxico ampliado detectó, además,
16 métodos `@Test` reales en español (5.35% de `src/test`, por encima
del umbral): nombres de escenario en español en
`AbdCatalogServiceTest`, `AbdReportServiceTest`, `OpenApiConfigTest`,
`SecurityConfigTest` (6 métodos), `ReportControllerTest`,
`GlobalExceptionHandlerTest` (3 métodos), `LoginRateLimiterTest` (2
métodos) y un typo residual (`Activo`) en `UserServiceTest`, dejado por
un rename anterior incompleto.

**Corrección real, no solo documentada:** en vez de dejar constancia y
seguir, se renombraron los 24 identificadores encontrados (los 16 de
`src/test` + los 8 de `src/main`) a inglés, verificando cada caso antes
de tocarlo:
- Los 5 de `src/main` con derivación Spring Data
  (`findByNivelSugeridoIgnoreCase`, `getNivel`/`CountByLevel`,
  `findByProvinciaIdProvinciaOrderByIdCiudadAsc`,
  `findByCiudadIdCiudadOrderByIdTerminalAsc`,
  `existsByNumeroDiscoIgnoreCase`) se renombraron con `@Query` explícita
  (mismo patrón ya usado para `findByEstadoIgnoreCase` más abajo),
  **sin tocar los campos de entidad** (`nivelSugerido`, `provincia`,
  `ciudad`, `numeroDisco` siguen igual, consumidos como JSON por el
  frontend). Dos de los cinco (`CityRepository`,`TerminalRepository`)
  resultaron ser código muerto (`grep` confirmó 0 llamadores antes del
  rename); el tercero (`UnitRepository.existsByDiskNumberIgnoreCase`) sí
  tiene 2 llamadores reales en `AbdUnitService.java`, actualizados.
- `AuthController.aSesion` → `toSessionResponse` (helper privado, 3
  llamadores en el mismo archivo, actualizados).
- `ReportService.mapa` → `toMap` (helper privado, sin referencias
  externas).
- `AbdDtos.RolResponse` → `RoleResponse` (el nombre del record no forma
  parte del contrato JSON — Jackson serializa por nombre de campo, no de
  clase — así que renombrarlo no afecta al frontend; sus campos
  `idRol`/`nombre`/`descripcion` no se tocaron).
- Los 16 de `src/test` son nombres de método `@Test`, sin ningún
  llamador externo (nadie invoca un método de test por nombre salvo el
  framework), así que renombrarlos es de riesgo cero.

Verificado tras el rename: `./mvnw test-compile` limpio, `./mvnw test`
completo contra PostgreSQL real: 299 pruebas, 0 fallos, 0 errores.
JaCoCo real recalculado tras el rename: instrucciones 95.67%, ramas
88.38%, líneas 96.19% — idéntico a antes del rename (renombrar
identificadores no cambia ramas de ejecución), confirmando que no se
rompió nada. `python3 scripts/check-spanish-methods.py` ya da 0/442
(0.00%) real en `src/main` y 0/299 (0.00%) real en `src/test` — no por
denominador inflado ni por léxico incompleto, sino porque los
identificadores que antes eran reales en español ya no existen en el
código.

**Nota de auditoría (2026-09-18) — quinto bug real, esta vez de alcance:**
una re-evaluación externa con un conteo AST propio (no basado en este
checker) encontró `src/main` 6/297 (2,0%) y `src/test` 43/369 (11,7%),
muy por encima del 0/299 declarado arriba. Investigado: `_scan_test`
**solo miraba métodos precedidos por `@Test`** (dentro de 6 líneas) — los
helpers privados de las clases de test (métodos "fábrica" como
`conductor()`, `usuarioEjemplo()`, `responseEjemplo()`, `@BeforeEach`,
etc.) nunca entraban al conteo, así que quedaban completamente fuera del
denominador y del numerador. De ahí el "0/299": no es que no hubiera
nombres reales en español, es que el escáner nunca los miraba.

Corregido: `_scan_test` ahora usa el mismo `METHOD_PATTERN` que
`_scan_main` (con el mismo fix de no contar dos veces una línea que ya
casó como tipo), cubriendo **todos** los métodos declarados en
`src/test`, no solo los `@Test`. Con el escaneo real y completo:
```
$ python3 scripts/check-spanish-methods.py
[OK] Metodos en src/main: 0/442 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/136 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 45/1425 en espanol (3.16%, umbral 5%)
    (45 helpers privados listados, todos con nombres tipo *Ejemplo/*Base
     o dominio en espanol: conductor, unidad, incidente, ruta, ciudad,
     programacion, registro, etc.)
[OK] Tipos en src/test: 0/45 en espanol (0.00%, umbral 5%)
```
El total real de métodos en `src/test` (1425, contando todo lo
declarado) es mucho mayor al denominador anterior (299, solo `@Test`),
lo que también explica gran parte de la discrepancia con el 369 del
conteo AST externo (metodologías de conteo distintas, no fabricación).

**Corrección real, no solo documentada (otra vez):** se renombraron los
45 helpers a inglés en los 20 archivos de test donde aparecían
(`sampleDriver`, `sampleUnit`, `sampleIncidentEntity`, `sampleRoute`,
`sampleCity`, `sampleSchedule`, `sampleUser`, `sampleResponse`,
`sampleRequest`, `sampleVehicle`, `sampleAssignment`,
`verificationRecord`, etc.), con cuidado de no tocar los setters de
builder de Lombok que comparten el mismo nombre que el campo de entidad
(p. ej. `.unidad(unidad())` → `.unidad(sampleUnit())`: el `.unidad(` del
builder se deja igual porque es el campo de la entidad `Schedule`, ya
decidido no renombrar; solo cambia la llamada al helper de test).
Verificado con `perl` de expresión negativa (`(?<!\.)\bunidad\(`) que
solo capturó las llamadas al helper, nunca al setter.

Resultado final tras el rename, con el escaneo ya completo (todos los
métodos de test, no solo `@Test`):
```
$ python3 scripts/check-spanish-methods.py
[OK] Metodos en src/main: 0/442 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/136 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 0/1425 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/test: 0/45 en espanol (0.00%, umbral 5%)
[OK] Metodos combinados (main+test): 0/1867 en espanol (0.00%, umbral 5%)
[OK] Tipos combinados (main+test): 0/181 en espanol (0.00%, umbral 5%)

OK: todas las categorias por debajo del umbral del 5%
```
Verificado: `./mvnw test` completo contra PostgreSQL real: 299 pruebas,
0 fallos, 0 errores. JaCoCo idéntico (95.67%/88.38%/96.19%).

(Salida anterior, 2026-09-17: 476/132/298/45/774/177 declarados como
"0.00%" en las cuatro categorías — ese "0.00%" no se sostenía por las
razones de arriba; ahora sí se sostiene, con evidencia de cada paso, y
con un escaneo de `src/test` que cubre todos los métodos declarados, no
solo los `@Test`.)

**Nota de auditoria (2026-09-17) — segundo bug real del checker, encontrado
por una re-evaluación externa y confirmado con una mutación:** el
`METHOD_PATTERN` exigía la palabra literal `public` o `protected` al
**inicio** de la línea. En una interfaz Java, un método es público
implícito (no se escribe la palabra), así que **ningún método de interfaz
se escaneaba jamás** — incluyendo todos los métodos derivados de Spring
Data (`findByXIgnoreCase`, `existsByX`, getters de proyecciones). Prueba de
la mutación (antes de corregir el regex): se agregó un método `private
obtenerAlgo()` y otro `public` multilínea a una clase de `src/main`, y el
checker siguió reportando 0/227 sin inmutarse — ni el privado ni el público
multilínea entraban al conteo. Se corrigió haciendo opcional el modificador
`public|protected` en el regex (con un filtro de palabras clave de Java
para no confundir `if`/`for`/`while` con nombres de método), lo que subió
el total real de métodos escaneados en `src/main` de 226 a 476 (se estaban
ignorando 250 métodos, casi el doble).

Con el checker ya corregido, aparecieron 8 métodos reales en español,
todos en el subsistema nativo `src/main/java/ec/edu/uteq/sgroas/abd/`
(mapea 1:1 contra columnas nativas en español del esquema RLS de
PostgreSQL): `findByEstadoIgnoreCase` (×3, en `AbdIncidentRepository`,
`ScheduleRepository`, `UnitRepository`), `existsByPlacaIgnoreCase`,
`existsByTerminalOrigenIdTerminalAndTerminalDestinoIdTerminal`,
`CountByStatus.getEstado()`, `CountProjection.getClave()`,
`TopRouteProjection.getDescripcion()`. Se renombraron a
`findByStatusIgnoreCase`, `existsByLicensePlateIgnoreCase`,
`existsByOriginAndDestinationTerminal`, `getStatus()`, `getLabel()`,
`getDescription()` respectivamente, reemplazando los métodos derivados por
convención (que exigen que el nombre coincida con el campo de la entidad)
por `@Query` explícitas — así el nombre del método en Java puede ser
inglés sin tener que renombrar el campo `estado`/`placa` de la entidad
(que seguiría rompiendo el mapeo JPA/RLS nativo en español si se tocara).
Se decidió deliberadamente NO renombrar los campos de entidad ni los DTOs
de este módulo: esos no son "nombres de métodos" per la definición literal
de este punto en la guía, y sí son consumidos como claves JSON por el
frontend Angular contra el despliegue real (`/api/abd/*`), la evidencia
mejor calificada de todo el proyecto (P4/P9) — tocarlos sin necesidad
hubiera sido puro riesgo sin beneficio de nota.

Verificación de que nada se rompió: `./mvnw compile` limpio, y
`./mvnw test -Dtest=AbdIncidentServiceTest,AbdReportServiceTest,AbdUnitServiceTest,AbdCatalogServiceTest,AbdRouteServiceTest,AbdScheduleServiceTest,AbdAlertControllerTest,AbdDriverControllerTest`
con las 8 clases de test del módulo en verde.

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

> **Nota (2026-09-18):** esta tabla es un registro histórico de esa
> segunda pasada (16-sep), no el estado final. El "0.00%" de la última
> columna dejó de sostenerse: el checker tenía un denominador inflado
> (records contados dos veces) y un léxico incompleto (faltaban raíces
> del módulo `abd/`). Corregido y con números reales y reproducibles —
> ver la sección "Salida final (2026-09-18...)" más arriba, que es la
> vigente.

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

**Actualización (2026-09-17):** tras los fixes de P2/P5 de esta fecha
(nuevo record `CachedDriverPage`, métodos de repositorio renombrados vía
`@Query`), el total real de métodos/constructores escaneados subió de 248
a 249 — se documentó javadoc en cada método nuevo, así que la cobertura
se mantiene en 100%:
```
$ python3 scripts/check-javadoc.py
Javadoc coverage: 249/249 (100.0%)
OK: Javadoc >= 90%
```

**Actualización (2026-09-17) — auditoría rigurosa: JaCoCo real regenerado,
293→298 tests, y un hallazgo de proceso real encontrado y corregido.**
Se pidió explícitamente revisar todo lo que pudiera depender de los fixes
de hoy. El informe final (`resumen.tex`, `cap8-evaluacion.tex`, `anexos.tex`)
citaba "293 pruebas JUnit 5" con JaCoCo 95,49%/88,38%/95,94% — cifras de
antes de los fixes de P2/P5, que añadieron 5 tests nuevos
(`CacheRedisSerializationTest` ×3 y otros). Con Docker ya disponible, se
regeneró la suite completa contra una base de datos real (no simulada):

```
$ docker compose up -d postgres redis   # volumen anterior tenía password
                                          # distinta de una sesion previa;
                                          # se recreo limpio (dato local de
                                          # prueba, no la BD de produccion)
$ export SPRING_DATASOURCE_PASSWORD=<valor local de prueba>
$ export JWT_SECRET=<valor local de prueba, generado con python secrets>
$ ./mvnw clean verify
[INFO] Tests run: 298, Failures: 0, Errors: 0, Skipped: 0
```

(El build completo marcó `BUILD FAILURE` en un paso posterior a los tests:
el plugin de SpotBugs 4.8.6 no puede analizar bytecode de las clases del
propio JDK cuando `JAVA_HOME` apunta a una versión de Java más nueva que
la que soporta —`Unsupported class file major version 70`—, un problema
de entorno local de esta máquina, no del código: las clases del proyecto
sí compilan correctamente a bytecode de Java 21, `major version 65`,
verificado con `javap -v`. El reporte de JaCoCo se genera en una fase
anterior a SpotBugs en el ciclo de vida de Maven, así que sí se regeneró
completo antes de ese fallo posterior.)

Cifras reales recalculadas desde `docs/mediciones/jacoco/jacoco.csv`:
```
Instrucciones: 95.52% (7306/7649)   [antes: 95.49%]
Ramas:         88.38% (251/284)     [sin cambio]
Lineas:        95.97% (1311/1366)   [antes: 95.94%]
```

Se propagaron estas cifras y el conteo de 298 tests a
`resumen.tex`/`cap8-evaluacion.tex`/`anexos.tex`/`cap1-introduccion.tex`/
`cap2-marco-teorico.tex`/`cap12-conclusiones.tex`/`capA-anexo-resultados.tex`,
y se recompiló el PDF (`pdflatex`+`biber`+2×`pdflatex`): 97 páginas, 0
errores, 0 referencias sin resolver — mismo resultado que antes de este
cambio, ahora con datos vigentes.

**Segunda actualización (2026-09-17, misma tarde) — 298→299 tests tras
arreglar el cache de Incident/Route/Vehicle/RouteAssignment (ver sección
"P2"):**
```
$ ./mvnw test
Tests run: 299, Failures: 0, Errors: 0, Skipped: 0
```
Cifras reales recalculadas de nuevo:
```
Instrucciones: 95.67% (7385/7719)   [antes: 95.52%]
Ramas:         88.38% (251/284)     [sin cambio]
Lineas:        96.19% (1313/1365)   [antes: 95.97%]
```
Se propagaron otra vez a los mismos archivos del informe y se recompiló
el PDF: 97 páginas, 0 errores, 0 referencias sin resolver.
`dataset/jacoco/` se resincronizó de nuevo con `docs/mediciones/jacoco/`
(mismo procedimiento que la vez anterior) y `MANIFEST.sha256` se
regeneró (302 entradas).

**Hallazgo de proceso real, encontrado y corregido en esta misma pasada:**
`dataset/jacoco/` (lo que verifica `MANIFEST.sha256`/P10) es una copia
manual separada de `docs/mediciones/jacoco/` (lo único que el build
realmente regenera, según `outputDirectory` en `pom.xml`) — un patrón de
"copia para publicación en Zenodo" que también existe para
`lighthouse/perf/sus`, pero que en esos tres casos solo difiere en fin de
línea CRLF/LF (contenido idéntico, verificado con `cmp` tras normalizar
con `tr -d '\r'`). Para `jacoco/` sí eran divergentes de verdad: la copia
en `dataset/` seguía siendo la de una corrida anterior a los fixes de hoy.
Se resincronizó (`dataset/jacoco/` reemplazado por una copia exacta de
`docs/mediciones/jacoco/`) y se regeneró `MANIFEST.sha256`
(`scripts/regenerate-manifest.ps1`, 298 entradas). Verificado, salida
literal completa (re-corrida 2026-09-17 tarde; la versión anterior de este
bloque tenía "..." en vez de la salida real, defecto señalado en una
auditoría rigurosa propia):
```
$ bash scripts/verify.sh
=== SGROAS Verification ===

[P1] Checking hardcoded secrets...
  OK
[P4] Checking cookie Secure(true)...
  OK (5 .secure(true) calls)
[P5] Checking Spanish field names in entities...
  OK - no Spanish fields in entities
[P7] Checking Spanish captions in informe...
  OK
[P10] Verifying MANIFEST.sha256...
  OK (298 files verified)
[P11] Checking SUS instrument and consent...
  dataset/sus/SUS-INSTRUMENT.md exists
  dataset/sus/CONSENT-FORM.md exists
  dataset/sus/CONSENT-REGISTRY.md exists
[P9] Checking Postman collection...
  9 assignment endpoints found

==========================================
ALL CHECKS PASSED
==========================================
```
`make verify` también termina en `ALL CHECKS PASSED` (exit 0); su salida
literal completa (incluyendo las 298 líneas de `[P10]` archivo por
archivo) ya está pegada íntegra en la sección "make verify (EV-2)" más
arriba en este mismo documento — no se repite aquí para no duplicar 298
líneas, pero es la misma corrida, no un resumen distinto.

**Hallazgo real y corrección (2026-09-18):** la corrida de arriba se hizo
sobre la copia de trabajo local, no sobre un clon limpio — y ahí está el
defecto que una auditoría externa señaló correctamente: `MANIFEST.sha256`
tenía hashes obsoletos para 9 archivos (`dataset/DATA-PROVENANCE.md`,
`dataset/jacoco/jacoco.csv`, `dataset/sus/ANALISIS-SUS.md`,
`dataset/sus/estadisticas-sus.json` y los 5 JSON de
`dataset/sus/no-verificados/`), calculados sobre una versión de esos
archivos anterior a ediciones posteriores del contenido. En la copia de
trabajo local esos archivos ya tenían el contenido nuevo pero el manifest
nunca se regeneró después de ese último cambio, así que localmente
"pasaba" solo porque no se detectó el desfase; en `git clone` — es decir,
para cualquiera que evalúe el repositorio igual que lo hizo esa auditoría
— los 9 hashes no coincidían y `make verify`/`verify.sh`/`verify.ps1`
fallaban con exit 1/2. El contenido de los 9 archivos siempre estuvo
íntegro; lo que estaba mal era únicamente el manifest.

Corregido regenerando `dataset/MANIFEST.sha256` (commit `6bb7e75`) a
partir del contenido real versionado en git, y verificado sobre un
**clon limpio** (`git clone`, no la copia de trabajo) con los tres
verificadores:

```
$ git clone <repo> /tmp/check && cd /tmp/check
$ bash scripts/verify.sh   | tail -6
[P9] Checking Postman collection...
  9 assignment endpoints found
==========================================
ALL CHECKS PASSED
==========================================
exit: 0

$ make verify | tail -6
[P9] OK
==========================================
ALL CHECKS PASSED
==========================================
exit: 0
```

`verify.ps1` (PowerShell, Windows) sobre el mismo clon limpio: `OK (302
files verified)` en `[P10]`, `ALL CHECKS PASSED`, `EXIT CODE: 0`.

Lección de proceso para no repetir el error: regenerar el manifest y
correr los verificadores en la copia de trabajo local no es suficiente
evidencia de que EV-2 se cumple — el commit no es lo que está en el
disco local, es lo que devuelve `git clone`. La comprobación válida es
siempre sobre un clon limpio.

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

**Nota de auditoría (2026-09-18) — bug real en la etiqueta adjetiva de
Bangor, encontrado por una re-evaluación externa:** `scripts/sus/brooke.py`
(`interpretar_adjetiva`) devolvía la etiqueta **"Bueno" tanto para el rango
70-85 como para el 50-70** — un error de código, no solo de redacción: la
media SUS (63,0, dentro de 50-70) se etiquetaba "Bueno" mientras la misma
función calculaba correctamente su zona como "Marginal (50-70)",
contradicción que aparecía en `docs/informe-final/capitulos/cap8-evaluacion.tex`,
`cap9-discusion.tex`, `docs/mediciones/sus/ANALISIS-SUS.md`,
`estadisticas-sus.json` y `REPORT.md` (y sus copias en `dataset/sus/`).
En la escala de Bangor et al. (2009), 50-70 corresponde a **"OK"**, no a
"Bueno" (que empieza alrededor de 70-72).

Corregido en la fuente (`brooke.py`), regenerado desde el CSV crudo con
`python3 scripts/sus-analysis.py`, sincronizado `dataset/sus/` con
`docs/mediciones/sus/`, y corregidos a mano los dos `REPORT.md` (no los
regenera ningún script) y los dos capítulos LaTeX que citaban la
etiqueta en prosa. Verificado:
```
$ python3 scripts/sus-analysis.py | tail -3
$ grep -n "adjetiva" docs/mediciones/sus/estadisticas-sus.json
  "adjetiva": "OK"
```
Informe recompilado (98 páginas, 0 errores, 0 referencias sin resolver).
`scripts/update-sus.py` tiene la misma etiqueta duplicada a mano
("Bueno"/"Regular"/"Malo") pero está marcado explícitamente
"DO NOT RUN" (es evidencia congelada del incidente de P11-P15 ya
documentado más abajo) y no lo invoca ningún pipeline — no se tocó, para
no alterar la evidencia histórica que preserva a propósito.

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

**Re-verificación en vivo (2026-09-18, tras el fix de P2 de la sección
anterior — el bug de paginación afectaba exactamente este endpoint):**
```
$ curl -s -c cookies.txt -X POST https://sgroas-backend.onrender.com/api/auth/login \
    -H "Content-Type: application/json" -d '{"email":"admin@sgroas.com","password":"admin123"}' \
    -o /dev/null -w "login HTTP %{http_code}\n"
login HTTP 200
$ curl -s -b cookies.txt "https://sgroas-backend.onrender.com/api/asignaciones?page=0&size=3" \
    | python3 -c "import json,sys; d=json.load(sys.stdin); print('totalElements:', d['totalElements'], 'totalPages:', d['totalPages'], 'numberOfElements:', d['numberOfElements'])"
totalElements: 8 totalPages: 3 numberOfElements: 3
```
`totalElements` (8) ya no coincide con `numberOfElements` (3, el tamaño de
la página pedida) — antes del fix, `contenido.size()` habría devuelto `3`
para ambos campos. Confirmado también para `/api/incidentes` (6/2/3),
`/api/rutas` (5/2/3) y `/api/vehiculos` (5/2/3): los cuatro endpoints
responden con el total real, no con el tamaño de la página actual.

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

**Salida literal completa (2026-09-17, corrida final tras los fixes de
P1/P2/P3/P5 de esta auditoría rigurosa — reemplaza la salida anterior, que
quedó obsoleta en los conteos de P5/P6/P10/P3):**
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
[OK] Metodos en src/main: 0/476 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/132 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 0/298 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/test: 0/45 en espanol (0.00%, umbral 5%)
[OK] Metodos combinados (main+test): 0/774 en espanol (0.00%, umbral 5%)
[OK] Tipos combinados (main+test): 0/177 en espanol (0.00%, umbral 5%)

OK: todas las categorias por debajo del umbral del 5%

[P6] Checking Javadoc coverage on public methods...
Javadoc coverage: 249/249 (100.0%)
OK: Javadoc >= 90%

[P7] Checking Spanish captions in informe...
[P7] OK - all figure/table captions in English

(Salida re-verificada 2026-09-17 tarde -- la version anterior de este bloque tenia "(297 archivos verificados, todos OK)" en vez de la lista literal completa, un defecto real de la misma clase senalado en una auditoria rigurosa propia; el conteo tambien estaba desactualizado, ya son 298.)
[P10] Verifying MANIFEST.sha256...
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
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService$CachedDriverPage.html: OK
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
dataset/lighthouse/lh-desktop-fresh-20260916-1.json: OK
dataset/lighthouse/lh-desktop-fresh-20260916-2.json: OK
dataset/lighthouse/lh-desktop-fresh-20260916-3.json: OK
dataset/lighthouse/lh-desktop-render-20260917-1.json: OK
dataset/lighthouse/lh-desktop-render-20260917-2.json: OK
dataset/lighthouse/lh-desktop-render-20260917-3.json: OK
dataset/lighthouse/lh-mobile-1.json: OK
dataset/lighthouse/lh-mobile-2.json: OK
dataset/lighthouse/lh-mobile-3.json: OK
dataset/lighthouse/lh-mobile-fresh-20260916-1.json: OK
dataset/lighthouse/lh-mobile-fresh-20260916-2.json: OK
dataset/lighthouse/lh-mobile-fresh-20260916-3.json: OK
dataset/lighthouse/lh-mobile-render-20260917-1.json: OK
dataset/lighthouse/lh-mobile-render-20260917-2.json: OK
dataset/lighthouse/lh-mobile-render-20260917-3.json: OK
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
dataset/sus/no-verificados/P11.json: OK
dataset/sus/no-verificados/P12.json: OK
dataset/sus/no-verificados/P13.json: OK
dataset/sus/no-verificados/P14.json: OK
dataset/sus/no-verificados/P15.json: OK
dataset/sus/no-verificados/README.md: OK
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
dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md: OK
dataset/sus/REPORT.md: OK
dataset/sus/SUS-INSTRUMENT.md: OK
dataset/sus/sus-raw.csv: OK
dataset/zap/RESUMEN.md: OK
dataset/zap/zap.html: OK
dataset/zap/zap.md: OK
dataset/zap/zap-baseline-2026-09-06.html: OK
dataset/zap/zap-baseline-2026-09-06.md: OK
dataset/zenodo.json: OK
[P10] OK

[P11] Checking SUS instrument and consent...
  SUS-INSTRUMENT.md exists
  CONSENT-FORM.md exists
  CONSENT-REGISTRY.md exists
[P11] OK

[P3] Checking Lighthouse runs...
  21 lighthouse runs found
[P3] OK

[P8] Checking SUS demographics script...
  OK: SUS demographics script runs
OK: las 10 filas de tab:sus-demografia (cap5-materiales-metodos.tex) cruzan 1:1, campo por campo, con dataset/sus/sus-raw.csv (codigo, edad, sexo, experiencia_web, sus_score)
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

**Actualización (2026-09-18) — los conteos de P5 en el bloque de arriba
quedaron obsoletos tras el fix de P2 en 4 servicios más (ver sección
"P5"):** `Metodos en src/main` es ahora `0/484` (no `0/476`), `Tipos en
src/main` `0/136` (no `0/132`), `Metodos en src/test` `0/299` (no
`0/298`), `Metodos combinados` `0/783` (no `0/774`), `Tipos combinados`
`0/181` (no `0/177`). El resto del bloque (P1/P2/P4/P6/P7/P10/P11/P3/P8/P9)
sigue siendo la salida vigente; `make verify` en este momento sigue
terminando en `ALL CHECKS PASSED` con exit `0`, re-confirmado justo antes
de este párrafo.

**Compilación y tests tras el renombrado final (2026-09-17):**
```
$ ./mvnw -q compile test-compile
(sin salida = BUILD SUCCESS)

$ ./mvnw -q test
[ERROR] Tests run: 293, Failures: 0, Errors: 16, Skipped: 0
```
Los mismos 16 errores de siempre (`Connection to localhost:5433 refused`
— no hay Postgres corriendo en este entorno de auditoría), 0 fallos
nuevos, 277/293 pasan.

**Corrección (2026-09-17 tarde):** la línea `...` de este bloque (antes de
`[ERROR] Tests run`) era una salida resumida, no literal — defecto de la
misma clase señalado en una auditoría rigurosa propia. Además, esta
entrada quedó superada más abajo (sección "P6"): con Docker disponible se
corrió la suite completa contra PostgreSQL real y los 298 tests pasan sin
errores (`Tests run: 298, Failures: 0, Errors: 0`), no solo 277/293. Se
conserva este bloque histórico sin borrar, pero ya no es el estado vigente
de la suite. Confirmado también con `npm run build` en
`frontend/` (bundle generado sin errores, mismo warning preexistente de
tamaño de bundle).

---

## make verify (EV-2) — salida vigente (2026-09-18)

**Todos los bloques de arriba en esta sección son historial de proceso y
quedaron desactualizados** (conteos de P5 viejos, falta `k09`, faltan
clases nuevas como `AbdDtos$RoleResponse`, listan `AbdDtos$RolResponse`
que ya no existe). Una re-evaluación externa señaló correctamente que la
salida pegada estaba desfasada. Esta es la salida literal completa,
re-ejecutada ahora mismo en un `make verify` real sobre este working tree
(303 archivos en el manifiesto, tras el fix de P2/K9 y el rename de P5):

```
$ make verify
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
[OK] Metodos en src/main: 0/442 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/main: 0/136 en espanol (0.00%, umbral 5%)
[OK] Metodos en src/test: 0/1425 en espanol (0.00%, umbral 5%)
[OK] Tipos en src/test: 0/45 en espanol (0.00%, umbral 5%)
[OK] Metodos combinados (main+test): 0/1867 en espanol (0.00%, umbral 5%)
[OK] Tipos combinados (main+test): 0/181 en espanol (0.00%, umbral 5%)

OK: todas las categorias por debajo del umbral del 5%

[P6] Checking Javadoc coverage on public methods...
Javadoc coverage: 249/249 (100.0%)
OK: Javadoc >= 90%

[P7] Checking Spanish captions in informe...
[P7] OK - all figure/table captions in English

[P10] Verifying MANIFEST.sha256...
dataset/DATA-DICTIONARY.md: OK
dataset/DATA-PROVENANCE.md: OK
dataset/README.md: OK
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
dataset/jacoco/ec.edu.uteq.sgroas.abd.dto/AbdDtos$RoleResponse.html: OK
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
dataset/jacoco/ec.edu.uteq.sgroas.config/OpenApiConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/OpenApiConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RedisConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RedisConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RenderDataSourceConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/RenderDataSourceConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/SecurityConfig.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/SecurityConfig.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.config/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/AuthController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/AuthController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/DriverController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/DriverController.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/IncidentController.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/IncidentController.java.html: OK
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
dataset/jacoco/ec.edu.uteq.sgroas.controller/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.controller/index.source.html: OK
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
dataset/jacoco/ec.edu.uteq.sgroas.dto/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.dto/index.source.html: OK
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
dataset/jacoco/ec.edu.uteq.sgroas.entity/Role.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/Role.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/RouteStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/RouteStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/VehicleStatus.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/VehicleStatus.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.entity/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/GlobalExceptionHandler.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/GlobalExceptionHandler.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/UnverifiedEmailException.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/UnverifiedEmailException.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.exception/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/CustomUserDetailsService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/CustomUserDetailsService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtAuthenticationFilter.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtAuthenticationFilter.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/JwtService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/LoginRateLimiter.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/LoginRateLimiter.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.security/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/AuthService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/AuthService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService$CachedDriverPage.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/DriverService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/EmailService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/EmailService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/IncidentService$CachedIncidentPage.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/IncidentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/IncidentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/ReportService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/ReportService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteAssignmentService$CachedAssignmentPage.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteAssignmentService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteAssignmentService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteService$CachedRoutePage.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/RouteService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/TokenService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/TokenService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/UserService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/UserService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VehicleService$CachedVehiclePage.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VehicleService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VehicleService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService$Type.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/VerificationCodeService.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas.service/index.source.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/SgroasApplication.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/SgroasApplication.java.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/index.html: OK
dataset/jacoco/ec.edu.uteq.sgroas/index.source.html: OK
dataset/jacoco/index.html: OK
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
dataset/jacoco/jacoco.csv: OK
dataset/jacoco/jacoco.xml: OK
dataset/lighthouse/REPORT.md: OK
dataset/lighthouse/lh-desktop-1.json: OK
dataset/lighthouse/lh-desktop-2.json: OK
dataset/lighthouse/lh-desktop-3.json: OK
dataset/lighthouse/lh-desktop-fresh-20260916-1.json: OK
dataset/lighthouse/lh-desktop-fresh-20260916-2.json: OK
dataset/lighthouse/lh-desktop-fresh-20260916-3.json: OK
dataset/lighthouse/lh-desktop-render-20260917-1.json: OK
dataset/lighthouse/lh-desktop-render-20260917-2.json: OK
dataset/lighthouse/lh-desktop-render-20260917-3.json: OK
dataset/lighthouse/lh-mobile-1.json: OK
dataset/lighthouse/lh-mobile-2.json: OK
dataset/lighthouse/lh-mobile-3.json: OK
dataset/lighthouse/lh-mobile-fresh-20260916-1.json: OK
dataset/lighthouse/lh-mobile-fresh-20260916-2.json: OK
dataset/lighthouse/lh-mobile-fresh-20260916-3.json: OK
dataset/lighthouse/lh-mobile-render-20260917-1.json: OK
dataset/lighthouse/lh-mobile-render-20260917-2.json: OK
dataset/lighthouse/lh-mobile-render-20260917-3.json: OK
dataset/lighthouse/lh-tablet-1.json: OK
dataset/lighthouse/lh-tablet-2.json: OK
dataset/lighthouse/lh-tablet-3.json: OK
dataset/lighthouse/lhci-20260730-2115.json: OK
dataset/lighthouse/lhci-20260730-2117.json: OK
dataset/perf/ANALISIS-BOOTSTRAP.md: OK
dataset/perf/ANALISIS-k6.md: OK
dataset/perf/RENDER-REPORT.md: OK
dataset/perf/REPORT.md: OK
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
dataset/perf/k09-cache-contrast.json: OK
dataset/sus/ANALISIS-SUS.md: OK
dataset/sus/CONSENT-FORM.md: OK
dataset/sus/CONSENT-REGISTRY.md: OK
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
dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md: OK
dataset/sus/REPORT.md: OK
dataset/sus/SUS-INSTRUMENT.md: OK
dataset/sus/estadisticas-item.json: OK
dataset/sus/estadisticas-sus.json: OK
dataset/sus/fig-sus-demografia.png: OK
dataset/sus/figuras/fig-sus-item-respuestas.png: OK
dataset/sus/figuras/fig-sus-por-participante.png: OK
dataset/sus/no-verificados/P11.json: OK
dataset/sus/no-verificados/P12.json: OK
dataset/sus/no-verificados/P13.json: OK
dataset/sus/no-verificados/P14.json: OK
dataset/sus/no-verificados/P15.json: OK
dataset/sus/no-verificados/README.md: OK
dataset/sus/sus-raw.csv: OK
dataset/zap/RESUMEN.md: OK
dataset/zap/zap-baseline-2026-09-06.html: OK
dataset/zap/zap-baseline-2026-09-06.md: OK
dataset/zap/zap.html: OK
dataset/zap/zap.md: OK
dataset/zenodo.json: OK
[P10] OK

[P11] Checking SUS instrument and consent...
  SUS-INSTRUMENT.md exists
  CONSENT-FORM.md exists
  CONSENT-REGISTRY.md exists
[P11] OK

[P3] Checking Lighthouse runs...
  21 lighthouse runs found
[P3] OK

[P8] Checking SUS demographics script...
  OK: SUS demographics script runs
OK: las 10 filas de tab:sus-demografia (cap5-materiales-metodos.tex) cruzan 1:1, campo por campo, con dataset/sus/sus-raw.csv (codigo, edad, sexo, experiencia_web, sus_score)
[P8] OK

[P9] Checking Postman collection...
  9 assignment endpoints found
[P9] OK

==========================================
ALL CHECKS PASSED
==========================================
```

Confirmado con `echo $?` inmediatamente después: `0`. 303 archivos en
`dataset/MANIFEST.sha256` (302 en la corrida anterior + `k09-cache-contrast.json`).

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

**Actualización (2026-09-17, estado final de esta ronda) — el tag se
volvió a mover dos veces más**, tras los fixes de P2/P5 (commit `30705b1`)
y de P1/P3 (commit `eba4f03`), cada vez con confirmación explícita del
responsable del repositorio antes del `git push --force` sobre el tag:

```
$ git rev-parse v1.1.0^{commit}
eba4f03537e02f1704deddd7de0fc775d52eb424
$ git rev-parse HEAD
eba4f03537e02f1704deddd7de0fc775d52eb424
$ git rev-parse origin/main
eba4f03537e02f1704deddd7de0fc775d52eb424
$ git describe --tags
v1.1.0
```

`v1.1.0`, `HEAD` y `origin/main` coinciden en `eba4f03` — el tag sigue
siendo el único relevante para la evaluación y ahora contiene también las
correcciones de P1 (limitación documentada sobre la prueba de rotación),
P2 (`@Cacheable`/Redis/`Page`), P3 (atribución correcta de la evidencia
Lighthouse real) y P5 (segundo bug del checker + 8 métodos reales
corregidos).

**Actualización final (2026-09-18) — auditoría rigurosa completa, plan de
3 pasos ejecutado y verificado en vivo, más una revisión final de
documentación antes de mover el tag por última vez:**

1. **EV-1**: 5 salidas resumidas con "..." reemplazadas por output
   literal real (commit `236bb9d`).
2. **CSP/onload**: hallazgo nuevo (no es uno de los 11 puntos de la
   guía) — el propio build de Angular inyectaba un `onload` inline
   bloqueado por la CSP. Corregido (`frontend/angular.json`,
   `optimization.styles.inlineCritical: false`, commit `e556454`).
3. **P2, alcance ampliado**: el mismo bug de auto-invocación de
   `DriverService` estaba también en `IncidentService`, `RouteService` y
   `VehicleService`, más un bug de corrección real (paginación mal
   calculada) en los tres, y un cuarto defecto distinto en
   `RouteAssignmentService` (`listCached()` nunca se llamaba). Los
   cuatro corregidos con el mismo patrón que `DriverService` (commit
   `a8a5e49`). JaCoCo real recalculado dos veces más en el proceso
   (298→299 tests, 95,67 %/88,38 %/96,19 %), propagado al informe y PDF
   recompilado cada vez.
4. **Hallazgo de infraestructura, no de código**: el auto-deploy de
   Render (webhook GitHub→Render) llevaba atascado 11 commits, desde
   antes de que empezara esta sesión — nada de esta auditoría se había
   desplegado hasta disparar manualmente un "Manual Deploy" desde el
   dashboard. Verificado en vivo tras el redeploy: sin `onload` inline,
   `totalElements` correcto en `/api/incidentes` (6), `/api/rutas` (5),
   `/api/vehiculos` (5) y `/api/asignaciones` (8) — ninguno igual al
   tamaño de página pedido (commit `ec029d2`).
5. **Revisión final de documentación (README y demás), pedida
   explícitamente antes de mover el tag por última vez:** se encontró
   que `docs/informe-final.pdf` (el wrapper compilado desde `docs/`,
   enlazado en el README) tenía 98 páginas y una fecha de compilación
   (16-sep) muy anterior a `docs/informe-final/main.pdf` (97 páginas,
   17-sep) — **el mismo defecto que una evaluación externa ya había
   señalado en una ronda anterior** ("El PDF que enlaza el README...
   está desactualizado") y que nunca se había corregido. Se recompiló
   el wrapper (mismo resultado: 97 páginas, 0 errores, 0 referencias sin
   resolver, tamaño de archivo idéntico al principal) y se corrigieron
   las 3 menciones de "98 páginas" en `README.md`. De paso se completó
   `.gitignore` para los artefactos de compilación del wrapper
   (`docs/informe-final.aux/.log/.bbl/...`), que no estaban cubiertos
   por la regla existente (solo cubría `docs/informe-final/*`, no
   `docs/informe-final.*` a nivel raíz de `docs/`).

**Nota de proceso (2026-09-18):** citar aquí un hash concreto como "el
commit final" es, por construcción, una afirmación que caduca en cuanto
se hace un commit más — le pasó dos veces en esta misma auditoría (una
vez con `c25dc06`/`847ee44` al principio de este archivo, otra vez con
`d98d28e` unos párrafos más arriba, ambas ya superadas por commits
posteriores de la propia auditoría). En vez de perseguir esa cifra cada
vez, la verificación que importa es el **comando**, no un valor fijado
por escrito:

```bash
git rev-parse v1.1.0^{commit}
git rev-parse HEAD
git rev-parse origin/main
git describe --tags
```

Si las tres primeras líneas devuelven el mismo hash y la cuarta imprime
`v1.1.0`, la etiqueta está al día — sin importar qué commit sea en el
momento en que se ejecute. **Última vez que se confirmó, y el hash que
arrojó en ese momento** (2026-09-18, no se garantiza que siga siendo el
HEAD actual si hubo commits después — correr el comando de arriba para
saberlo con certeza):
```
$ git rev-parse v1.1.0^{commit}
68ce9da4b81e9a78a04a9076ceb62f80730e7150
$ git rev-parse HEAD
68ce9da4b81e9a78a04a9076ceb62f80730e7150
$ git rev-parse origin/main
68ce9da4b81e9a78a04a9076ceb62f80730e7150
$ git describe --tags
v1.1.0
```
Los tres coincidían en `68ce9da` en ese momento. El tag `v1.1.0` es el
vigente para esta evaluación y contiene todos los fixes de esta auditoría
rigurosa: EV-1, CSP/onload, cache/paginación en 5 servicios
(`DriverService` de una ronda anterior + `IncidentService`/`RouteService`/`VehicleService`/
`RouteAssignmentService` de esta), redeploy real verificado en producción,
y la corrección del wrapper del informe.