# P11–P15 — no incluidos en el estudio SUS oficial

Estos 5 archivos (`P11.json`–`P15.json`) se conservan como registro histórico,
pero **no forman parte del estudio de usabilidad oficial** (n=10, P01–P10).

## Qué pasó

Los valores de estos 5 archivos, y las filas correspondientes que se agregaron
a `sus-raw.csv`, fueron escritos a mano en `scripts/update-sus.py` en un
commit del 2026-09-06 (`3516019`), casi 6 semanas después de la fecha en que
se declaraba que la prueba de usabilidad había ocurrido (30 de julio de 2026).
No existe ningún archivo fuente anterior a esa fecha (CSV exportado de un
formulario, capturas, registro de sesión) que demuestre que estas 5 personas
efectivamente respondieron el cuestionario SUS con esos valores en esa fecha.

Un evaluador externo detectó esta falta de respaldo documental. Para evitar
presentar datos sin trazabilidad verificable (riesgo de "Piso 3" — datos
inventados), se decidió **retirar a P11–P15 del estudio de usabilidad
oficial** el 2026-09-17 y quedarse solo con los 10 participantes (P01–P10)
que sí tienen respaldo sólido y contemporáneo: CSV subido el 2026-07-30
(`e8d7e2f`), consentimientos firmados el 2026-07-24, sin contradicciones de
fecha.

## Sobre el consentimiento de estas 5 personas

Las 5 personas identificadas como P11–P15 **sí existen y dieron
consentimiento informado real**: hay 5 PDFs firmados, verificados por
SHA-256, fechados 2026-07-26 (ver `dataset/sus/PARTICIPANTES-NO-INCLUIDOS.md`
y `dataset/sus/CONSENT-REGISTRY.md`). Lo que no se pudo verificar es que
hayan respondido el cuestionario SUS con los valores concretos que se les
atribuyeron. Por eso se excluyen sus datos de encuesta del análisis, sin que
esto ponga en duda su consentimiento real.

## Estado de estos archivos

Estos 5 JSON, y las filas equivalentes que existieron en `sus-raw.csv`, ya no
se usan en ningún script del estudio oficial (`generate-sus-demographics.py`,
`gen-sus-demografia-figura.py`, `validate-sus-demografia.sh`, etc. solo leen
`sus-raw.csv`, que contiene únicamente P01–P10). No se borran del repositorio
para no ocultar que este problema existió y cómo se resolvió.
