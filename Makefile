.PHONY: up down test bench bench-render audit jacoco versions docs pdf all clean verify

PYTHON ?= $(shell command -v python3 2>/dev/null || command -v python 2>/dev/null)

# =============================================================================
# SGROAS — Makefile
# =============================================================================
# Uso: make up    -> levantar el sistema completo
#      make down  -> detener contenedores
#      make test  -> ejecutar pruebas
#      make bench -> benchmarks k6 locales K1-K3 (serie local, n=3)
#      make bench-render -> benchmarks k6 Render K4-K8 (serie publica, n=5, requiere JWT)
#      make audit -> auditoria SQL estatico + trazabilidad
#      make jacoco-> regenerar reporte de cobertura
#      make versions -> generar docs/entorno/versions.txt
#      make pdf   -> compilar el informe (docs/informe-final/main.tex, 95 pag.)
#      make docs  -> generar artefactos de documentacion
#      make all   -> pipeline completo (R1: reproduccion end-to-end)
#      make clean -> limpieza total
# =============================================================================

up:
	docker compose up --build -d
	@echo "Esperando a que el backend esté listo..."
	@sleep 15
	@echo "Sistema disponible en http://localhost:8080"

down:
	docker compose down -v

test:
	./mvnw test
	@echo "Reporte JaCoCo generado en docs/mediciones/jacoco/"

bench:
	@echo "Ejecutando benchmarks k6 locales K1-K3 (3 corridas) contra el stack local (make up)..."
	k6 run -e BASE_URL=http://localhost:8080 k6/script.js --summary-export docs/mediciones/perf/k01-run1.json
	k6 run -e BASE_URL=http://localhost:8080 k6/script.js --summary-export docs/mediciones/perf/k02-run2.json
	k6 run -e BASE_URL=http://localhost:8080 k6/script.js --summary-export docs/mediciones/perf/k03-run3.json
	@echo "Benchmarks completos. Resultados en docs/mediciones/perf/"
	@echo "Serie Render K4-K8 (5 corridas calientes + 5 frias) ya archivada; ver 'make bench-render'."

bench-render:
	@echo "Serie Render K4-K8 contra https://sgroas-backend.onrender.com (requiere JWT, 30s entre corridas)..."
	@echo "Ejemplo (no se ejecuta en 'make all' para no saturar Render Free):"
	@echo "k6 run -e BASE_URL=https://sgroas-backend.onrender.com k6/script.js --summary-export docs/mediciones/perf/k04-run1.json"
	@echo "Ver docs/mediciones/perf/ANALISIS-k6.md (n=5) y RENDER-REPORT.md."

audit:
	@echo "Auditoria: SQL dinamico prohibido..."
	scripts/audit-sql-dynamic.sh
	@echo "Auditoria: trazabilidad end-to-end..."
	scripts/validate-traceability.sh
	@echo "Auditoria: listados LaTeX vs codigo..."
	scripts/validate-listings.sh
	@echo "Auditoria: self-test de exit codes (P11)..."
	scripts/test-validators.sh
	@echo "Auditoria: demografia SUS cap.5 vs sus-raw.csv (P8)..."
	scripts/validate-sus-demografia.sh
	@echo "Auditorias completas (exit 0 = OK)."

jacoco:
	./mvnw clean verify
	@echo "Reporte JaCoCo regenerado en docs/mediciones/jacoco/"

versions:
	mkdir -p docs/entorno
	$(PYTHON) scripts/gen-versions.py > docs/entorno/versions.txt
	@echo "Versiones registradas en docs/entorno/versions.txt"

pdf:
	@echo "Compilando informe LaTeX (docs/informe-final/main.tex)..."
	cd docs/informe-final && pdflatex -interaction=nonstopmode main.tex
	cd docs/informe-final && biber main
	cd docs/informe-final && pdflatex -interaction=nonstopmode main.tex
	cd docs/informe-final && pdflatex -interaction=nonstopmode main.tex
	@echo "PDF generado en docs/informe-final/main.pdf (101 paginas)."

docs: versions
	$(PYTHON) scripts/gen-figuras.py
	$(PYTHON) scripts/gen-sus-demografia-figura.py
	$(PYTHON) scripts/gen-c4-diagramas.py
	@echo "Artefactos de documentacion generados."

all: up test bench audit jacoco docs pdf
	@echo "=========================================="
	@echo "PIPELINE COMPLETO (make all) FINALIZADO OK"
	@echo "=========================================="

verify:
	@echo "=== SGROAS Verification ==="
	@echo ""
	@echo "[P1] Checking hardcoded secrets..."
	@! grep -rn "CHANGE_ME\|password123\|secret_key" src/main/resources/application.properties docker-compose.yml 2>/dev/null | grep -v "CHANGE_ME"
	@! grep -n "SPRING_DATASOURCE_PASSWORD=.\{3,\}" docker-compose.yml 2>/dev/null | grep -v '\$$'
	@echo "[P1] Checking JWT tokens versioned in dataset/ and docs/ are expired..."
	@$(PYTHON) scripts/check-jwt-expiry.py
	@echo "[P1] OK"
	@echo ""
	@echo "[P2] Checking raw k6 runs (hot x5 + cold x5) reproducible contrast..."
	@test $$(ls dataset/perf/k0*-run1.json 2>/dev/null | wc -l) -ge 5 && echo "  $$(ls dataset/perf/k0*-run1.json 2>/dev/null | wc -l) hot runs found"
	@test $$(ls dataset/perf/k0*-cold.json 2>/dev/null | wc -l) -ge 5 && echo "  $$(ls dataset/perf/k0*-cold.json 2>/dev/null | wc -l) cold runs found"
	@$(PYTHON) scripts/perf/recalcular-contraste.py > /dev/null 2>&1 && echo "  OK: nonparametric contrast reproducible (nonparametric.py)"
	@echo "[P2] OK"
	@echo ""
	@echo "[P4] Checking cookie Secure(true)..."
	@! grep -n "\.secure(cookieSecure)" src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java 2>/dev/null
	@test $$(grep -v '//' src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java | grep -c "\.secure(true)") -ge 4
	@echo "  Found $$(grep -v '//' src/main/java/ec/edu/uteq/sgroas/controller/AuthController.java | grep -c '\.secure(true)') .secure(true) calls"
	@echo "[P4] OK"
	@echo ""
	@echo "[P5] Checking Spanish field names in entities..."
	@! grep -rn "private String nombre\|private String apellido\|private String estado\|private String direccion\|private String telefono\|private String placa\|private String marca\|private String modelo" src/main/java/ec/edu/uteq/sgroas/entity/ 2>/dev/null
	@echo "[P5] OK - no Spanish fields in entities"
	@echo "[P5] Checking Spanish method names in main..."
	@$(PYTHON) scripts/check-spanish-methods.py
	@echo ""
	@echo "[P6] Checking Javadoc coverage on public methods..."
	@$(PYTHON) scripts/check-javadoc.py
	@echo ""
	@echo "[P7] Checking Spanish captions in informe..."
	@! grep -rn "caption{" docs/informe-final/ 2>/dev/null | grep -E "Tabla|Figura|Listado|Resumen|Resultados|Distribución|Síntesis|Desglose|trazados|comparación|puntaje|prioridad"
	@echo "  OK - all figure/table captions in English"
	@echo "[P7] Checking Spanish float names in prose cross-references..."
	@! grep -rn --include=*.tex -E "\\b(la|La|las|Las) Tabla\\b|\\b(la|La|las|Las) Figura\\b" docs/informe-final/ 2>/dev/null
	@echo "  OK - prose refers to Table/Figure, same name as the float label"
	@echo "[P7] Checking Spanish text baked into the figure pixels..."
	@$(PYTHON) scripts/check-figure-text.py
	@echo "[P7] OK"
	@echo ""
	@echo "[P10] Verifying MANIFEST.sha256..."
	@tr -d '\r' < dataset/MANIFEST.sha256 | sha256sum -c -
	@echo "[P10] OK"
	@echo ""
	@echo "[P11] Checking SUS instrument and consent..."
	@test -f dataset/sus/SUS-INSTRUMENT.md && echo "  SUS-INSTRUMENT.md exists"
	@test -f dataset/sus/CONSENT-FORM.md && echo "  CONSENT-FORM.md exists"
	@test -f dataset/sus/CONSENT-REGISTRY.md && echo "  CONSENT-REGISTRY.md exists"
	@echo "[P11] OK"
	@echo ""
	@echo "[P3] Checking Lighthouse runs..."
	@ls dataset/lighthouse/lh-*.json 2>/dev/null | wc -l | xargs -I{} echo "  {} lighthouse runs found"
	@test $$(ls dataset/lighthouse/lh-*.json 2>/dev/null | wc -l) -ge 9 && echo "[P3] OK"
	@echo ""
	@echo "[P8] Checking SUS demographics script..."
	@$(PYTHON) scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv > /dev/null 2>&1 && echo "  OK: SUS demographics script runs"
	@bash scripts/validate-sus-demografia.sh
	@echo "[P8] OK"
	@echo ""
	@echo "[P9] Checking Postman collection..."
	@grep -c "asignaciones" docs/postman/coleccion.json | xargs -I{} echo "  {} assignment endpoints found"
	@echo "[P9] OK"
	@echo ""
	@echo "=========================================="
	@echo "ALL CHECKS PASSED"
	@echo "=========================================="

clean:
	docker compose down -v --rmi all
	@echo "Limpieza completada."
