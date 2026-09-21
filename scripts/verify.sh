#!/usr/bin/env bash
# SGROAS Verification Script (EV-2)
# Equivalente POSIX de verify.ps1
# Exit code 0 = all checks passed, non-zero = failure

set -u
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
failed=0

echo "=== SGROAS Verification ==="
echo

echo "[P1] Checking hardcoded secrets..."
p1_bad=0
grep -E "SPRING_DATASOURCE_PASSWORD=[^\$][^[:space:]]*" src/main/resources/application.properties >/dev/null 2>&1 && p1_bad=1
grep -E "JWT_SECRET=[^\$][^[:space:]]*" src/main/resources/application.properties >/dev/null 2>&1 && p1_bad=1
grep -E "spring\.datasource\.password=[^\$][^[:space:]]*" src/main/resources/application.properties src/test/resources/application-test.properties >/dev/null 2>&1 && p1_bad=1
grep -rn "CHANGE_ME\|password123\|secret_key" src/test/resources/application-test.properties render.yaml 2>/dev/null | grep -v "CHANGE_ME" >/dev/null 2>&1 && p1_bad=1
if [ "$p1_bad" -eq 1 ]; then
  echo "  FAIL: hardcoded secrets found in application*.properties / render.yaml"
  failed=$((failed+1))
else
  echo "  OK"
fi

echo "[P1] Checking JWT tokens versioned in dataset/ and docs/ are expired..."
if "${PYTHON:-python3}" scripts/check-jwt-expiry.py; then
  :
else
  echo "  FAIL: a live (non-expired) JWT is versioned in the repository"
  failed=$((failed+1))
fi

echo "[P2] Checking raw k6 runs (hot x5 + cold x5) reproducible contrast..."
hot=$(ls dataset/perf/k0[4-8]-run1.json 2>/dev/null | wc -l)
cold=$(ls dataset/perf/k0[4-8]-cold.json 2>/dev/null | wc -l)
contrast=$(ls dataset/perf/k1[0-4]-cache-contrast.json 2>/dev/null | wc -l)
if [ "$hot" -lt 5 ] || [ "$cold" -lt 5 ] || [ "$contrast" -lt 5 ]; then
  echo "  FAIL: hot=$hot cold=$cold cache-contrast=$contrast (expected >= 5 each)"
  failed=$((failed+1))
else
  echo "  $hot hot runs, $cold cold runs, $contrast cache-contrast runs (K10-K14) found"
  if "${PYTHON:-python3}" scripts/perf/recalcular-contraste.py >/dev/null 2>&1; then
    echo "  OK: nonparametric contrast reproducible (nonparametric.py)"
  else
    echo "  FAIL: nonparametric contrast script did not run cleanly"
    failed=$((failed+1))
  fi
fi

echo "[P1] Checking ZAP scan really targeted the public deployment..."
if grep -q "sgroas-backend.onrender.com" dataset/zap/zap.html dataset/zap/zap-baseline-2026-09-06.html 2>/dev/null \
   && ! grep -qi "localhost\|127.0.0.1" dataset/zap/zap.html dataset/zap/zap-baseline-2026-09-06.html 2>/dev/null; then
  echo "  OK - ZAP reports target the public URL, not localhost"
else
  echo "  FAIL: ZAP report does not clearly target the public URL"
  failed=$((failed+1))
fi

echo "[P4] Checking cookie Secure(true)..."
if grep -E "\.secure\(cookieSecure\)" src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java >/dev/null 2>&1; then
  echo "  FAIL: .secure(cookieSecure) found"
  failed=$((failed+1))
else
  count=$(grep -v '//' src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java | grep -c '\.secure(true)')
  if [ "$count" -lt 4 ]; then
    echo "  FAIL: only $count .secure(true) calls found (expected >= 4)"
    failed=$((failed+1))
  else
    echo "  OK ($count .secure(true) calls)"
  fi
fi

echo "[P5] Checking Spanish field names in entities..."
if grep -rE "private String (nombre|apellido|estado|direccion|telefono|placa|marca|modelo)\b" src/main/java/ec/edu/uteq/sgroas/entity/*.java >/dev/null 2>&1; then
  echo "  FAIL: Spanish fields found"
  failed=$((failed+1))
else
  echo "  OK - no Spanish fields in entities"
fi

echo "[P6] Checking Javadoc coverage on public methods..."
if "${PYTHON:-python3}" scripts/check-javadoc.py; then
  :
else
  echo "  FAIL: Javadoc coverage below threshold"
  failed=$((failed+1))
fi

echo "[P7] Checking Spanish captions in informe..."
# Antes este check apuntaba a docs/informe-final/*.md, que no existe: el glob
# no expandia, grep no encontraba nada y el check no podia fallar nunca.
# Ahora mira las fuentes reales (.tex) y ademas el texto horneado en las
# imagenes, que es donde estaba el defecto que senalo la evaluacion.
if grep -rn --include=*.tex "caption{" docs/informe-final/ 2>/dev/null \
     | grep -E "Tabla|Figura|Listado|Resumen|Resultados|Distribución|Síntesis|Desglose|trazados|comparación|puntaje|prioridad" >/dev/null 2>&1; then
  echo "  FAIL: Spanish captions found"
  failed=$((failed+1))
else
  echo "  OK - all figure/table captions in English"
fi

echo "[P7] Checking Spanish float names in prose cross-references..."
if grep -rn --include=*.tex -E "\b(la|La|las|Las) (Tabla|Figura)\b" docs/informe-final/ >/dev/null 2>&1; then
  echo "  FAIL: prose still says 'la Tabla/Figura N' while the float is labelled Table/Figure N"
  failed=$((failed+1))
else
  echo "  OK - prose refers to Table/Figure"
fi

echo "[P7] Checking Spanish text baked into the figure pixels..."
if "${PYTHON:-python3}" scripts/check-figure-text.py; then
  echo "  OK"
else
  echo "  FAIL: Spanish text inside a figure of the report"
  failed=$((failed+1))
fi

echo "[P10] Verifying MANIFEST.sha256..."
all_ok=1
manifest_count=0
while IFS= read -r line; do
  [ -z "$line" ] && continue
  manifest_count=$((manifest_count+1))
  hash=$(printf '%s' "$line" | awk -F'  ' '{print $1}')
  file=$(printf '%s' "$line" | awk -F'  ' '{print $2}')
  if [ ! -f "$file" ]; then
    echo "  MISSING: $file"
    all_ok=0
    failed=$((failed+1))
    continue
  fi
  actual=$(sha256sum "$file" | awk '{print $1}')
  if [ "$hash" != "$actual" ]; then
    echo "  MISMATCH: $file"
    all_ok=0
    failed=$((failed+1))
  fi
done < dataset/MANIFEST.sha256
[ "$all_ok" -eq 1 ] && echo "  OK ($manifest_count files verified)"

echo "[P3] Checking Lighthouse runs..."
lh=$(ls dataset/lighthouse/lh-*.json 2>/dev/null | wc -l)
if [ "$lh" -lt 9 ]; then
  echo "  FAIL: only $lh lighthouse runs found (expected >= 9)"
  failed=$((failed+1))
else
  echo "  $lh lighthouse runs found"
fi

echo "[P8] Checking SUS demographics script..."
if "${PYTHON:-python3}" scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv >/dev/null 2>&1; then
  echo "  OK: SUS demographics script runs"
else
  echo "  FAIL: SUS demographics script did not run cleanly"
  failed=$((failed+1))
fi
if bash scripts/validate-sus-demografia.sh; then
  :
else
  echo "  FAIL: SUS demographics table does not match dataset/sus/sus-raw.csv"
  failed=$((failed+1))
fi

echo "[P11] Checking SUS instrument and consent..."
for f in dataset/sus/SUS-INSTRUMENT.md dataset/sus/CONSENT-FORM.md dataset/sus/CONSENT-REGISTRY.md; do
  if [ -f "$f" ]; then
    echo "  $f exists"
  else
    echo "  MISSING: $f"
    failed=$((failed+1))
  fi
done

echo "[P9] Checking Postman collection..."
p9=$(grep -c "asignaciones" docs/postman/coleccion.json 2>/dev/null || echo 0)
echo "  $p9 assignment endpoints found"

echo
if [ "$failed" -eq 0 ]; then
  echo "=========================================="
  echo "ALL CHECKS PASSED"
  echo "=========================================="
  exit 0
else
  echo "=========================================="
  echo "FAILED: $failed check(s) failed"
  echo "=========================================="
  exit 1
fi
