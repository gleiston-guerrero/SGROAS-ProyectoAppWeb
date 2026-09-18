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
if grep -E "SPRING_DATASOURCE_PASSWORD=[^\$][^[:space:]]*" src/main/resources/application.properties >/dev/null 2>&1 \
   || grep -E "JWT_SECRET=[^\$][^[:space:]]*" src/main/resources/application.properties >/dev/null 2>&1; then
  echo "  FAIL: hardcoded secrets found in application.properties"
  failed=$((failed+1))
else
  echo "  OK"
fi

echo "[P4] Checking cookie Secure(true)..."
if grep -E "\.secure\(cookieSecure\)" src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java >/dev/null 2>&1; then
  echo "  FAIL: .secure(cookieSecure) found"
  failed=$((failed+1))
else
  count=$(grep -c '\.secure(true)' src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java)
  echo "  OK ($count .secure(true) calls)"
fi

echo "[P5] Checking Spanish field names in entities..."
if grep -rE "private String (nombre|apellido|estado|direccion|telefono|placa|marca|modelo)\b" src/main/java/ec/edu/uteq/sgroas/entity/*.java >/dev/null 2>&1; then
  echo "  FAIL: Spanish fields found"
  failed=$((failed+1))
else
  echo "  OK - no Spanish fields in entities"
fi

echo "[P7] Checking Spanish captions in informe..."
if grep -rE "\b(Tabla|Figura|Listado|cuadro|figura|listado)\b" docs/informe-final/*.md >/dev/null 2>&1; then
  echo "  FAIL: Spanish captions found"
  failed=$((failed+1))
else
  echo "  OK"
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
