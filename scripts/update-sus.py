"""
update-sus.py

** DO NOT RUN. Kept only as evidence of how the P11-P15 data problem
originated; its effect on the repository was reverted on 2026-09-17. **

This script appended 5 participants (P11-P15) to the SUS study by writing
their 10 Likert answers and SUS score literally by hand into `new_data`
below, then generating their P11.json..P15.json and appending 5 rows to
sus-raw.csv. It was committed on 2026-09-06 (commit 3516019) -- almost 6
weeks after the date the repository declared the SUS evaluation had taken
place (2026-07-30). No source file predating that commit (exported CSV,
screenshots, session log) demonstrates that these 5 people actually answered
the SUS questionnaire with these values on that date.

The 5 people behind P11-P15 are real and did give real, verified informed
consent (see dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md for their preserved
consent hashes). What could not be verified is that they answered the SUS
questionnaire with the values hard-coded here. To avoid presenting
unverifiable survey data as part of the official study (a "Piso 3" /
fabricated-data risk), P11-P15 were removed from the official SUS study on
2026-09-17: sus-raw.csv now contains only P01-P10, and the P11-P15 JSON
files were moved to dataset/sus/no-verificados/ and docs/mediciones/sus/
no-verificados/. The official SUS numbers are n=10, mean 63.0,
95% CI [53.07; 72.93] (dataset/sus/REPORT.md).
"""
import json, csv, os, statistics
from scipy import stats as sp_stats

# New participants P11-P15
new_data = [
    {'codigo': 'P11', 'edad': 23, 'sexo': 'Masculino', 'experiencia_web': 'Media', 'dispositivo': 'Computadora', 'consentimiento': 'Si', 'respuestas': [4,1,3,2,5,1,4,2,4,2]},
    {'codigo': 'P12', 'edad': 19, 'sexo': 'Masculino', 'experiencia_web': 'Baja', 'dispositivo': 'Computadora', 'consentimiento': 'Si', 'respuestas': [3,2,4,2,4,2,3,1,4,2]},
    {'codigo': 'P13', 'edad': 21, 'sexo': 'Femenino', 'experiencia_web': 'Media', 'dispositivo': 'Computadora', 'consentimiento': 'Si', 'respuestas': [3,1,4,1,4,1,4,1,4,1]},
    {'codigo': 'P14', 'edad': 21, 'sexo': 'Masculino', 'experiencia_web': 'Baja', 'dispositivo': 'Computadora', 'consentimiento': 'Si', 'respuestas': [3,2,4,1,3,2,5,1,5,1]},
    {'codigo': 'P15', 'edad': 20, 'sexo': 'Femenino', 'experiencia_web': 'Media', 'dispositivo': 'Computadora', 'consentimiento': 'Si', 'respuestas': [3,2,3,1,4,2,4,1,4,1]},
]

def calc_sus(respuestas):
    score = 0
    for i, r in enumerate(respuestas):
        if i % 2 == 0:
            score += r - 1
        else:
            score += 5 - r
    return score * 2.5

# Write individual JSONs
for p in new_data:
    sus = calc_sus(p['respuestas'])
    p['sus_score'] = sus
    with open(f"docs/mediciones/sus/{p['codigo']}.json", 'w', encoding='utf-8') as f:
        json.dump(p, f, indent=2, ensure_ascii=False)
    print(f"{p['codigo']}: {sus}")

# Read existing CSV
existing = []
fieldnames = None
with open('docs/mediciones/sus/sus-raw.csv', 'r', encoding='utf-8-sig') as f:
    reader = csv.DictReader(f)
    fieldnames = reader.fieldnames
    for row in reader:
        existing.append(row)

# Add new rows
for p in new_data:
    row = {
        'codigo': p['codigo'],
        'edad': str(p['edad']),
        'sexo': p['sexo'],
        'experiencia_web': p['experiencia_web'],
        'dispositivo': p['dispositivo'],
        'consentimiento': p['consentimiento'],
        'q1': str(p['respuestas'][0]),
        'q2': str(p['respuestas'][1]),
        'q3': str(p['respuestas'][2]),
        'q4': str(p['respuestas'][3]),
        'q5': str(p['respuestas'][4]),
        'q6': str(p['respuestas'][5]),
        'q7': str(p['respuestas'][6]),
        'q8': str(p['respuestas'][7]),
        'q9': str(p['respuestas'][8]),
        'q10': str(p['respuestas'][9]),
        'sus_score': str(p['sus_score'])
    }
    existing.append(row)

# Write updated CSV
with open('docs/mediciones/sus/sus-raw.csv', 'w', encoding='utf-8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(existing)

# Calculate statistics for all 15
all_scores = [float(r['sus_score']) for r in existing]
n = len(all_scores)
media = statistics.mean(all_scores)
dt = statistics.stdev(all_scores)
se = dt / (n ** 0.5)
t_crit = sp_stats.t.ppf(0.975, df=n-1)
ic_inf = media - t_crit * se
ic_sup = media + t_crit * se

print(f'')
print(f'=== ESTADISTICAS SUS (n={n}) ===')
print(f'Media: {media:.4f}')
print(f'DT: {dt:.4f}')
print(f'Error estandar: {se:.4f}')
print(f't critico (df={n-1}): {t_crit:.4f}')
print(f'IC95%: [{ic_inf:.4f}; {ic_sup:.4f}]')

# Demografia completa
all_participants = []
with open('docs/mediciones/sus/sus-raw.csv', 'r', encoding='utf-8-sig') as f:
    reader = csv.DictReader(f)
    for row in reader:
        all_participants.append(row)

edades = [int(r['edad']) for r in all_participants]
print(f'')
print(f'=== DEMOGRAFIA COMPLETA (n={len(all_participants)}) ===')
print(f'Edades: min={min(edades)}, max={max(edades)}, media={statistics.mean(edades):.1f}')
sexos = {}
for r in all_participants:
    s = r['sexo']
    sexos[s] = sexos.get(s, 0) + 1
print(f'Sexos: {sexos}')
dispositivos = {}
for r in all_participants:
    d = r['dispositivo']
    dispositivos[d] = dispositivos.get(d, 0) + 1
print(f'Dispositivos: {dispositivos}')
experiencias = {}
for r in all_participants:
    e = r['experiencia_web']
    experiencias[e] = experiencias.get(e, 0) + 1
print(f'Experiencia: {experiencias}')

# Save stats
stats_data = {
    'generado': '2026-09-06',
    'n': n,
    'media': round(media, 4),
    'desviacion_tipica': round(dt, 4),
    'error_estandar': round(se, 4),
    't_critico': round(t_crit, 4),
    'ic95_inf': round(ic_inf, 4),
    'ic95_sup': round(ic_sup, 4),
    'adjetiva': 'Bueno' if media >= 70 else 'Regular' if media >= 50 else 'Malo'
}
with open('docs/mediciones/sus/estadisticas-sus.json', 'w', encoding='utf-8') as f:
    json.dump(stats_data, f, indent=2, ensure_ascii=False)
print(f'')
print(f'Stats saved: {stats_data}')
