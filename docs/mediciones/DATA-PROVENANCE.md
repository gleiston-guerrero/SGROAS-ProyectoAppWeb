# Procedencia de datos (DATA PROVENANCE) — SGROAS

**Objetivo:** para cada tabla y figura del informe académico, este documento indica el
archivo de datos crudos, el script que la genera y el commit que la introdujo.
Todo número del informe es re-derivable desde estas fuentes (reglas de oro 6 y 9 del plan).

**Verificación:** 16 de agosto de 2026 — Kevin (Castro Espinoza)

## 1. Rendimiento (k6) — Bloque C.1

| Artefacto (tabla/figura) en informe | Datos crudos | Script | Commit |
|---|---|---|---|
| Tabla "Configuracion de k6" | `k6/opts.js` | — | `62bf8fa` |
| Tabla K1 local (corridas k01-k03) | `docs/mediciones/perf/k01-run1.json`, `k02-run2.json`, `k03-run3.json` | `scripts/perf-analysis.py` | `62bf8fa` (datos), `72b919c` (script) |
| Tabla Render caliente (K4-K8) | `docs/mediciones/perf/k04-run1.json` ... `k08-run1.json` | `scripts/perf-analysis.py` | `9ded2a69` |
| Muestras frias Render (K4-K8) | `docs/mediciones/perf/k04-cold.json` ... `k08-cold.json` | `scripts/perf/nonparametric.py` | `9ded2a69` |
| Tabla de estadistica agregada (media/DT/IC95) | ídem | `scripts/perf-analysis.py` | `761e5e1` |
| Fig. "Perfil de percentiles" | ídem | `scripts/gen-figuras.py` → `fig-percentiles-corridas.png` | `cba0e96` |
| Fig. "p95 por corrida" | ídem | `scripts/gen-figuras.py` → `fig-p95-por-corrida.png` | `cba0e96` |
| Fig. "Media e IC95" | ídem | `scripts/gen-figuras.py` → `fig-media-ic95.png` | `cba0e96` |
| Contraste frio vs caliente (Mann-Whitney) | k0X-cold.json + k0X-run1.json | `scripts/perf/nonparametric.py` | `9ded2a69` |
| Informe de analisis k6 | `docs/mediciones/perf/ANALISIS-k6.md` | `scripts/perf-analysis.py` | `9ded2a69` |
| IC bootstrap (validacion) | `docs/mediciones/perf/ANALISIS-BOOTSTRAP.md` | `scripts/perf-bootstrap.py` → `bootstrap.json` | `683cea3` |
| RENDER-REPORT.md | `docs/mediciones/perf/k04-k08` | `scripts/perf/nonparametric.py` | `9ded2a69` |

## 2. Usabilidad (SUS) — Bloque C.3

| Artefacto (tabla/figura) en informe | Datos crudos | Script | Commit |
|---|---|---|---|
| Tabla de puntuación por participante | `docs/mediciones/sus/sus-raw.csv` | `scripts/sus-analysis.py` | `e8d7e2f` (datos), `042bffa` (script) |
| Datos por participante (anexo) | `docs/mediciones/sus/P01.json` … `P10.json` | `scripts/sus-analysis.py` | `771b48e`, `2a118b8` |
| Estadísticos descriptivos e IC95 | `sus-raw.csv` | `scripts/sus-analysis.py` | `fe41053` |
| Fig. "SUS por participante" | `sus-raw.csv` | `scripts/sus-analysis.py` → `fig-sus-por-participante.png` | `458f322` |
| Puntuación (regla Brooke) | — | `scripts/sus/brooke.py` | `1a5c42c` |
| Informe de análisis SUS | `docs/mediciones/sus/ANALISIS-SUS.md` | `scripts/sus-analysis.py` | `fe41053` |
| Estadísticos por ítem (q1–q10) | `sus-raw.csv` | `scripts/sus-analysis.py` → `estadisticas-item.json` | `af9962d` |
| Fig. "Media de respuesta por ítem" | `sus-raw.csv` | `scripts/sus-analysis.py` → `fig-sus-item-respuestas.png` | `af9962d` |

## 3. Cobertura (JaCoCo) — Bloque C.4

| Artefacto (tabla/figura) en informe | Datos crudos | Script | Commit |
|---|---|---|---|
| Tabla de cobertura por clase | `docs/mediciones/jacoco/jacoco.csv` | `./mvnw verify` (reporte JaCoCo) | `dea7940` |
| Reporte HTML de cobertura | `docs/mediciones/jacoco/index.html` | JaCoCo Maven plugin | `dea7940` |

## 4. Calidad web (Lighthouse) — Bloque C.5

| Artefacto (tabla/figura) en informe | Datos crudos | Script | Commit |
|---|---|---|---|
| Tabla de categorías por corrida | `docs/mediciones/lighthouse/lhci-20260730-2115.json`, `lhci-20260730-2117.json` | `npx lighthouse` (ver `lighthouserc.js`) | `1a07dc7` |
| RESUMEN de lighthouse | `docs/mediciones/lighthouse/RESUMEN.md` | informe manual sobre JSON | por definir |

## 5. Seguridad (OWASP + ZAP) — Bloque C.2

| Artefacto (tabla/figura) en informe | Datos crudos | Script | Commit |
|---|---|---|---|
| Evidencias OWASP A01–A09 | `docs/mediciones/sec/*` | scripts de evidencia (`A01-*.sh`, etc.) | `34f3a6a`, `56002f3`, `9f67bfc` |
| ZAP baseline | `docs/mediciones/sec/zap/zap-baseline-2026-09-06.html`, `.md` | `scripts/zap/run-zap.sh` | `ca7f700` |
| Sesión en vivo + cookie segura (P6/P16) | `docs/mediciones/sec/live-session/{login-response.txt,auth-me.json,asignaciones.json,sin-sesion-403.txt}` | `curl` contra `https://sgroas-backend.onrender.com` | `7e49da9` |

## 5b. Usabilidad (SUS) — ampliación demográfica (P13)

| Artefacto (tabla/figura) en informe | Datos crudos | Script | Commit |
|---|---|---|---|
| Tabla "Demografía y puntaje SUS" (`tab:sus-demografia`) | `dataset/sus/sus-raw.csv` (10 filas `P01`…`P10`, con `edad/sexo/experiencia_web/dispositivo`) | `scripts/validate-sus-demografia.sh` | `7e49da9` |
| Fig. "Demografía vs SUS" | ídem | `scripts/gen-sus-demografia-figura.py` → `fig-sus-demografia.png` | `7e49da9` |
| Reporte SUS alineado (n=10) | ídem | — | `7e49da9` |

> **Nota 2026-09-17:** esta sección documentaba originalmente 15 filas
> (P01–P15). 5 de esas filas (P11–P15) no tenían respaldo documental
> verificable de la fecha de recolección y fueron retiradas del estudio
> oficial; ver `dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md`. El estudio
> oficial queda en n=10.

## 6. Dataset público (Zenodo) — Bloque G / K6

| Artefacto | Datos crudos | Script | Commit |
|---|---|---|---|
| ZIP del dataset (249 archivos) | `docs/mediciones/{perf,sus,lighthouse,zap,jacoco}` | `scripts/zenodo/package-dataset.py` | `d30e980` |
| Checksums SHA-256 | — | idem → `dataset/MANIFEST.csv` + `dataset/MANIFEST.sha256` | `d30e980` |
| DOI dataset | — | depósito Zenodo 10.5281/zenodo.21973297 | `b18444b` |

## 7. Convención de nombres

- Datos crudos: formato abierto (JSON / CSV), nunca editados a mano después de su captura.
- Scripts: en `scripts/`; su salida se versiona para que el informe siempre sea reproducible.
- Cada figura generada incluye el commit del script que la creó.