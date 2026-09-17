# Estado real del consentimiento informado — SUS (P11)

Fecha de esta auditoría: 2026-09-16.

## 1. Los dos registros contradictorios encontrados

### `dataset/sus/CONSENT-REGISTRY.md`
Afirma que los 15 participantes (P01–P15) firmaron consentimiento el
**2026-08-15**, "before the evaluation session" (antes de la sesión de
evaluación).

### `docs/etica/consentimientos/registro.md` (versión previa a esta auditoría)
Afirmaba que P01–P10 firmaron el **2026-07-30**, no mencionaba a P11–P15, y
citaba como "evidencia" los archivos `P01.json` … `P10.json` — que en
realidad son las respuestas crudas del cuestionario SUS, no una constancia de
consentimiento independiente. Dentro de esos JSON solo hay un campo
autodeclarado `"consentimiento": "Sí"`.

## 2. Qué dice el historial real de git

Comandos ejecutados en este working tree, salida literal:

```
$ git log --format='%H %ad %an %s' --date=short e8d7e2f -1
e8d7e2f6d02942f252ae4622b8b7cec73ddfd99a 2026-07-30 charito20 feat(entrega3): evidencia SUS real (10 participantes, media 63.0 IC95 [53.1;72.9]) e informe/SRS con cobertura final
```

Ese commit (`e8d7e2f`, 2026-07-30 23:45:08 -0500) ya sube
`docs/mediciones/sus/sus-raw.csv` con las 10 respuestas agregadas de
P01–P10, es decir, **la evaluación con esos 10 participantes ya había
ocurrido el 2026-07-30**.

```
$ git log --follow --format='%H %ad %an' --date=short -- dataset/sus/P01.json
ce0099f1ed27d1ffded45f02fa7ca2c4ab1f9002 2026-09-01 Alxjandr07
771b48ed9b89f3dd718178d70436c383ad1635b3 2026-08-16 TheAsesink
```

Los archivos JSON individuales por participante (`P01.json`…`P15.json`,
incluidos P11–P15) se suben por primera vez el **2026-08-16**.

```
$ git log --format='%H %ad %an %ae' --date=short -- dataset/sus/CONSENT-REGISTRY.md
fa1274cc4d0a53e95c4aaca2261bb26aa76991f1 2026-09-14 Luis Tejada luistejada5434@gmail.com

$ git log --format='%H %ad %an %ae' --date=short -- docs/etica/consentimientos/registro.md
66e5e87dbbbc10021f4d0a63f12581bce3419fb5 2026-08-16 TheAsesink 0999595561kevin@gmail.com
```

## 3. La contradicción, explicada

- `CONSENT-REGISTRY.md` dice que el consentimiento de los 15 participantes se
  firmó el 2026-08-15 "antes de la evaluación". Pero la evaluación de al
  menos 10 de esos participantes (P01–P10) ya había producido resultados
  agregados el **2026-07-30** — 16 días antes de la fecha de firma
  declarada. Un consentimiento "previo a la evaluación" fechado **después**
  de que la evaluación ya ocurrió y fue reportada es cronológicamente
  imposible tal como está redactado.
- `registro.md` (versión previa) decía que P01–P10 firmaron el 2026-07-30,
  la misma fecha en que se subió el CSV agregado de resultados — es decir,
  en el mejor de los casos la firma habría sido el mismo día de la
  evaluación, no en un momento verificablemente anterior. Además ese
  registro no incluía a P11–P15 en absoluto, y la "evidencia" que citaba
  (los JSON P01–P10) no se sube al repo hasta el 2026-08-16, casi tres
  semanas después de la fecha de firma que el propio registro declara.

## 4. Cuál de los dos registros es más creíble

Ninguno de los dos es una constancia de consentimiento verificable. Sin
embargo, `registro.md` (versión previa) es **relativamente más creíble** que
`CONSENT-REGISTRY.md`, porque:

- Su fecha de firma declarada (2026-07-30) al menos coincide con el día en
  que la evaluación de P01–P10 fue reportada, en lugar de postularse **16
  días después** como hace `CONSENT-REGISTRY.md`.
- Es honesto en admitir que las constancias firmadas, si existen, se
  custodian fuera del repositorio y no son verificables desde aquí.

Pero ninguno de los dos resuelve el problema de fondo: **no hay ninguna
constancia de consentimiento verificable en el repositorio**, solo un campo
autodeclarado `"consentimiento": "Sí"` dentro de las propias respuestas del
cuestionario que se pretende validar con ese consentimiento.

## 5. Conclusión honesta (sin datos inventados)

- No se encontraron constancias de consentimiento firmadas de forma
  independiente (PDF, imagen, firma digital) en ningún directorio del
  repositorio (`dataset/sus/`, `docs/etica/`).
- La carpeta declarada como custodia externa
  (`docs/etica/consentimientos/firmados/`) no existe en este working tree y
  no puede confirmarse ni descartarse su contenido desde esta auditoría.
- Las dos fechas de firma declaradas en los registros existentes son
  incompatibles entre sí y, en el caso de `CONSENT-REGISTRY.md`, incompatibles
  con el propio historial de git del repositorio.
- Por lo tanto, **el punto P11 (consentimiento informado) no puede
  calificarse al máximo**: lo único verificable es un campo autodeclarado
  dentro de los datos que se busca validar, no una constancia de
  consentimiento independiente y con fecha coherente.
- Este documento no crea ni fecha ninguna constancia nueva. Se limita a
  señalar la contradicción, documentar la evidencia real de `git log`, y
  dejar constancia explícita de la limitación.

## 6. Corrección de nombre de institución

`dataset/sus/CONSENT-FORM.md` identificaba a la universidad como
"Quintanilla Normal University (UTEQ)". Se corrigió a "Universidad Técnica
Estatal de Quevedo (UTEQ)", que es el nombre correcto usado de forma
consistente en el resto del repositorio (ver `docs/etica/consentimientos/plantilla.md`).
Se verificó con `grep -rln "Quintanilla Normal University"` que esta era la
única ocurrencia en todo el repositorio.
