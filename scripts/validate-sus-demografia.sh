#!/usr/bin/env bash
# =============================================================================
# validate-sus-demografia.sh
# Verifica que la tabla de demografia del capitulo 5 del informe
# (docs/informe-final/cap5-materiales-metodos.tex, tabla tab:sus-demografia)
# cruza 1:1, FILA POR FILA, con los datos crudos SUS (dataset/sus/sus-raw.csv).
#
# Corregido 2026-09-17: la version anterior de este script NO leia el .tex en
# absoluto -- comparaba el CSV contra un puñado de constantes fijas
# (8 hombres, 7 mujeres, media 68.5, etc.) escritas a mano en el propio
# script. Esa comparacion pasaba aunque la tabla del informe se editara con
# datos distintos, porque nunca se leia su contenido real. Esta version
# extrae cada fila de la tabla LaTeX con una expresion regular y la compara,
# participante por participante, contra la fila correspondiente del CSV.
#
# Uso: scripts/validate-sus-demografia.sh   (requiere python3)
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CSV="$ROOT/dataset/sus/sus-raw.csv"
TEX="$ROOT/docs/informe-final/cap5-materiales-metodos.tex"

if [[ ! -f "$CSV" ]]; then
    echo "ERROR: no existe $CSV" >&2
    exit 1
fi
if [[ ! -f "$TEX" ]]; then
    echo "ERROR: no existe $TEX" >&2
    exit 1
fi

python3 - "$CSV" "$TEX" <<'PY'
import csv, re, sys

csv_path, tex_path = sys.argv[1], sys.argv[2]

# --- 1. Leer la tabla real del .tex (fuente de verdad declarada en el informe) ---
tex = open(tex_path, encoding="utf-8").read()

m = re.search(r"\\label\{tab:sus-demografia\}.*?\\begin\{tabular\}.*?\n(.*?)\\bottomrule",
              tex, re.DOTALL)
if not m:
    print("ERROR: no se encontro la tabla tab:sus-demografia en el .tex", file=sys.stderr)
    sys.exit(1)

tex_rows = {}
row_re = re.compile(
    r"^\s*(P\d{2})\s*&\s*(\d+)\s*&\s*(Masculino|Femenino)\s*&\s*(Baja|Media|Alta)\s*&\s*([\d.]+)\s*\\\\",
    re.MULTILINE,
)
for match in row_re.finditer(m.group(1)):
    codigo, edad, sexo, exp, sus = match.groups()
    tex_rows[codigo] = {
        "edad": int(edad),
        "sexo": sexo,
        "experiencia_web": exp,
        "sus_score": float(sus),
    }

if not tex_rows:
    print("ERROR: la tabla tab:sus-demografia existe pero no se pudo parsear ninguna fila "
          "(revisar el formato de las filas en el .tex)", file=sys.stderr)
    sys.exit(1)

# --- 2. Leer el CSV real ---
csv_rows = {r["codigo"]: r for r in csv.DictReader(open(csv_path, encoding="utf-8-sig"))}

errs = []

if set(tex_rows) != set(csv_rows):
    solo_tex = sorted(set(tex_rows) - set(csv_rows))
    solo_csv = sorted(set(csv_rows) - set(tex_rows))
    if solo_tex:
        errs.append(f"codigos en el .tex pero no en el CSV: {solo_tex}")
    if solo_csv:
        errs.append(f"codigos en el CSV pero no en el .tex: {solo_csv}")

for codigo in sorted(set(tex_rows) & set(csv_rows)):
    t = tex_rows[codigo]
    c = csv_rows[codigo]
    if t["edad"] != int(c["edad"]):
        errs.append(f"{codigo}: edad tex={t['edad']} != csv={c['edad']}")
    if t["sexo"] != c["sexo"]:
        errs.append(f"{codigo}: sexo tex={t['sexo']} != csv={c['sexo']}")
    if t["experiencia_web"] != c["experiencia_web"]:
        errs.append(f"{codigo}: experiencia_web tex={t['experiencia_web']} != csv={c['experiencia_web']}")
    if abs(t["sus_score"] - float(c["sus_score"])) > 0.01:
        errs.append(f"{codigo}: sus_score tex={t['sus_score']} != csv={c['sus_score']}")

if errs:
    for e in errs:
        print(f"ERROR: {e}", file=sys.stderr)
    print(f"CRUCE DEMOGRAFICO FALLIDO: {len(errs)} error(es)", file=sys.stderr)
    sys.exit(1)

n = len(tex_rows)
print(f"OK: las {n} filas de tab:sus-demografia ({tex_path.split('/')[-1]}) "
      f"cruzan 1:1, campo por campo, con dataset/sus/sus-raw.csv "
      f"(codigo, edad, sexo, experiencia_web, sus_score)")
PY
