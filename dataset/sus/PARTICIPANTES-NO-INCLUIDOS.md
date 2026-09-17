# Participantes no incluidos en el estudio SUS oficial (P11-P15)

**Fecha de esta decisión:** 2026-09-17

## Resumen

P11-P15 son 5 personas reales que dieron **consentimiento informado real y
verificado** para participar en la evaluación de usabilidad SGROAS (5 PDFs
firmados, verificados por SHA-256, fechados 2026-07-26). Sin embargo, **sus
respuestas al cuestionario SUS no tienen ningún respaldo documental
verificable** de que hayan sido recolectadas en la fecha en que se declaró.

Por eso se excluyen del estudio de usabilidad oficial (que queda en n=10,
P01-P10), pero se documentan aquí para no ocultar que estas personas
existieron y consintieron participar.

## Qué se encontró

- Los 15 valores de respuesta al cuestionario SUS de P11-P15 (10 respuestas
  Likert + puntaje SUS por persona, 5 personas) están escritos literalmente
  a mano dentro de `scripts/update-sus.py`, en el commit `3516019`
  (2026-09-06).
- Esa fecha es casi 6 semanas posterior a la fecha en que se declaraba que la
  evaluación había ocurrido.
- No existe ningún archivo fuente anterior a esa fecha (CSV exportado de un
  formulario, capturas de pantalla, registro de sesión, correo) que
  demuestre que P11-P15 efectivamente respondieron el cuestionario con esos
  valores específicos.
- En contraste, P01-P10 sí tienen respaldo contemporáneo sólido: el CSV con
  sus 10 respuestas se subió el 2026-07-30 (commit `e8d7e2f`), coherente con
  la fecha de consentimiento firmado (2026-07-24) y sin contradicciones.

## Qué sí está verificado: el consentimiento

Las 5 personas identificadas como P11-P15 existen y dieron consentimiento
informado real. Los hashes SHA-256 de sus 5 formularios firmados (PDFs, con
firma manuscrita, no subidos al repositorio por contener información
identificable) son:

| Código | Edad | Sexo | Firmado el | SHA-256 del PDF firmado |
|--------|------|------|------------|--------------------------|
| P11 | 23 | Masculino | 2026-07-26 | `6113753eb0aa166f95f921acb47996e48ca7327d8a994b37f853a2e1ce451c50` |
| P12 | 19 | Masculino | 2026-07-26 | `a9aba08d40926da009928790f26848b4efc4314e8c3a79623969014e5c1c13e4` |
| P13 | 21 | Femenino  | 2026-07-26 | `322beccf5d42f83d2313d698b84ac4016cb877951981ec7d3431907afbd99881` |
| P14 | 21 | Masculino | 2026-07-26 | `341cf1b59d4e3606a051d4ae601581dec1e3c1a0f1bfa817ba160a020462fb44` |
| P15 | 20 | Femenino  | 2026-07-26 | `b7b2677cfc577b65c75fd1e30dace114f9900ec7d1e3d5049f66b793c46012ee` |

Estos hashes se conservan como evidencia de que el consentimiento fue real,
independientemente de que sus datos de encuesta no se usen.

## Qué no está verificado: las respuestas al cuestionario

Los 10 valores Likert (q1-q10) y el puntaje SUS atribuidos a cada uno de
P11-P15 no tienen ninguna fuente documental previa a `scripts/update-sus.py`
(2026-09-06). No se puede afirmar con evidencia que esas personas hayan
respondido el cuestionario SUS, ni que lo hayan hecho con esos valores, ni
en la fecha declarada.

## Decisión y su justificación

Dado que no hay forma de verificar los datos de encuesta de P11-P15, se
decidió **no usarlos** en el análisis oficial de usabilidad, en vez de
intentar reconstruir o re-justificar una fecha de recolección. Esta decisión:

- Elimina el riesgo de presentar datos inventados o sin trazabilidad
  ("Piso 3") en el estudio SUS.
- No oculta ni niega que P11-P15 existan o hayan dado consentimiento real.
- Deja el estudio oficial en n=10 (P01-P10), con respaldo documental sólido
  y sin contradicciones de fecha: media 63,0, IC95% [53,07; 72,93] (ver
  `dataset/sus/REPORT.md` y `dataset/sus/ANALISIS-SUS.md`).

## Dónde quedan estos archivos

- `dataset/sus/no-verificados/P11.json` .. `P15.json`: los 5 archivos
  agregados por `scripts/update-sus.py`, movidos aquí y marcados como no
  verificados. No se usan en ningún script del estudio oficial.
- `scripts/update-sus.py`: se conserva sin borrar, con un comentario que
  explica qué se descubrió y que su efecto fue revertido el 2026-09-17.
- `dataset/sus/sus-raw.csv`: contiene únicamente P01-P10.
