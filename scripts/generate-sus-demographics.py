#!/usr/bin/env python3
"""
generate-sus-demographics.py
Reads sus-raw.csv and generates the SUS demographics table for cap.5.
Output: tab-separated table suitable for LaTeX inclusion.
Usage: python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv
       python3 scripts/generate-sus-demographics.py dataset/sus/sus-raw.csv --latex

Corregido 2026-09-17 (P8): antes este script solo imprimia un resumen de
texto; la tabla LaTeX real que aparece en
docs/informe-final/cap5-materiales-metodos.tex (tab:sus-demografia) se
mantenia a mano, sin generarse desde el CSV. Con --latex, imprime las filas
`\\begin{tabular}...\\end{tabular}` completas, listas para pegar en el .tex,
generadas directamente desde el CSV (misma fuente que valida
scripts/validate-sus-demografia.sh).
"""
import csv
import sys
from collections import Counter

sys.stdout.reconfigure(encoding="utf-8")

if len(sys.argv) not in (2, 3):
    print(f"Usage: {sys.argv[0]} <sus-raw.csv> [--latex]", file=sys.stderr)
    sys.exit(1)

LATEX_MODE = len(sys.argv) == 3 and sys.argv[2] == "--latex"
csv_path = sys.argv[1]
try:
    with open(csv_path, encoding="utf-8-sig") as f:
        rows = list(csv.DictReader(f))
except FileNotFoundError:
    print(f"ERROR: file not found: {csv_path}", file=sys.stderr)
    sys.exit(1)

if LATEX_MODE:
    rows.sort(key=lambda r: int(r["codigo"][1:]))
    print(r"\begin{table}[htbp]")
    print(r"\caption{SUS demographics and score by participant (P01--P15). Source: \texttt{docs/mediciones/sus/sus-raw.csv}.}")
    print(r"\label{tab:sus-demografia}")
    print(r"\small")
    print(r"\begin{tabular}{@{}llllr@{}}")
    print(r"\toprule")
    print(r"\textbf{Código} & \textbf{Edad} & \textbf{Sexo} & \textbf{Exp. web} & \textbf{SUS} \\")
    print(r"\midrule")
    for r in rows:
        sus = r["sus_score"]
        # Match the existing table's style: trailing .0 kept only where the
        # source already carries it (e.g. "80.0"), not invented here.
        print(f"{r['codigo']} & {r['edad']} & {r['sexo']} & {r['experiencia_web']} & {sus} \\\\")
    print(r"\bottomrule")
    print(r"\end{tabular}")
    print(r"\end{table}")
    sys.exit(0)

n = len(rows)
codes = sorted(r["codigo"] for r in rows)
genders = Counter(r["sexo"] for r in rows)
ages = [int(r["edad"]) for r in rows]
experience = Counter(r["experiencia_web"] for r in rows)
devices = Counter(r["dispositivo"] for r in rows)
scores = [float(r["sus_score"]) for r in rows]
mean_score = sum(scores) / n

print(f"Total participants: {n}")
print(f"Codes: {', '.join(codes)}")
print(f"Gender: {genders.get('Masculino', 0)} male, {genders.get('Femenino', 0)} female")
print(f"Age range: {min(ages)}-{max(ages)} years (mean {sum(ages)/n:.1f})")
print(f"Web experience: Baja={experience.get('Baja', 0)}, Media={experience.get('Media', 0)}, Alta={experience.get('Alta', 0)}")
print(f"Devices: {dict(devices)}")
print(f"SUS score: mean={mean_score:.1f}, min={min(scores)}, max={max(scores)}")
print()
print("Per-participant data:")
print(f"{'Code':<6} {'Age':<5} {'Gender':<11} {'Experience':<11} {'Device':<14} {'SUS':<6}")
print("-" * 55)
for r in rows:
    print(f"{r['codigo']:<6} {r['edad']:<5} {r['sexo']:<11} {r['experiencia_web']:<11} {r['dispositivo']:<14} {r['sus_score']:<6}")
