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
$ git log --format='%ad' --date=short --diff-filter=A -- "docs/mediciones/sus/P01.json"
2026-08-16
$ git log --format='%H %ad %an %ae %s' --date=short --diff-filter=A -- "docs/mediciones/sus/P11.json"
351601997ba1b5a6182ab3f5a13cd9b3c08b3403 2026-09-06 charito20 mescuderop@uteq.edu.ec feat(sus): agrega P11-P15 - n=15, media=68.5, IC95% [60.76; 76.24] (guia docente 3.7)
```

**Corrección (2026-09-17):** una versión anterior de este documento decía
que los archivos `P11.json`…`P15.json` se subían "por primera vez el
2026-08-16", igual que P01-P10. Ese dato salió de un comando
`git log --follow` corrido solo sobre `P01.json`, y se generalizó por error
a P11-P15 sin verificar cada archivo por separado. Verificado ahora
individualmente: `P01.json`…`P10.json` sí se subieron el **2026-08-16**
(commits `771b48e` y `2a118b8`), pero `P11.json`…`P15.json` se subieron
por primera vez el **2026-09-06** (commit `3516019`, autoría de María del
Rosario Escudero Plaza / `charito20`).

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

---

## 7. Resolución (2026-09-17): constancias reales localizadas y verificadas

Todo lo anterior (secciones 1-6) documenta el estado del repositorio **hasta
el 2026-09-16**: sin ninguna constancia verificable. El 2026-09-17 el equipo
localizó los 15 formularios de consentimiento firmados en papel, escaneados a
PDF, con nombres de archivo `p01 firma.pdf` … `p15 firma.pdf`. Se verificaron
de la siguiente forma:

```
$ python3 -c "
import hashlib, os
d = r'consentimientos firmados pdf'
for i in range(1,16):
    code = f'P{i:02d}'
    path = os.path.join(d, f'p{i:02d} firma.pdf')
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        h.update(f.read())
    print(f'{code}: {h.hexdigest()}')
"
P01: a3034963a789df06cff212b43d2ef6e5c9b96ddc3cdf335e8b34b83e5c05739a
P02: 8381f4e8f0fe16dd05ddc668d200d5deefc5d1be5db66800ebdfe9c69cdec5f8
P03: 33f4417909ee4fce3f2d13063445f10cbf1a5e70201ec41aa0f9904abcb726fb
P04: b3d0a209390f4e43abf79fac827110b277aaf0de08ee8fdffc37e6290496cad3
P05: 8aeba8200d4b814820d4f3487f561c1820665bb769610ee118252cad5787c2f0
P06: a00523abf50a51937ebcf9b6b89d1e5014cf645172ca285b6e3ecc8214959b10
P07: f36ed2050e67272494e7d3d6f96bd413d8cf3cf8502e9cd2f3203d7bcdb558fd
P08: 11891dd8e8a4fd382da0a82ff0cb7db2364a2eee8fc4a6d30754b79758645179
P09: db055bc4c2429933d98d6a6344c35c3e412d33948dcd9e3671a62af3d682e593
P10: b0b864109c3665c9430a8b6bab5c7d1cbd22d39ac3759843a56dfdd826d283df
P11: 6113753eb0aa166f95f921acb47996e48ca7327d8a994b37f853a2e1ce451c50
P12: a9aba08d40926da009928790f26848b4efc4314e8c3a79623969014e5c1c13e4
P13: 322beccf5d42f83d2313d698b84ac4016cb877951981ec7d3431907afbd99881
P14: 341cf1b59d4e3606a051d4ae601581dec1e3c1a0f1bfa817ba160a020462fb44
P15: b7b2677cfc577b65c75fd1e30dace114f9900ec7d1e3d5049f66b793c46012ee
```

Verificaciones adicionales realizadas:
- Los 15 hashes son distintos entre sí (0 duplicados).
- Se recalcularon con `sha256sum` (Git Bash) y con `hashlib.sha256` (Python)
  de forma independiente, con resultados idénticos en ambas herramientas.
- Se extrajo con `pdftotext -layout` la fecha de firma legible de cada uno
  de los 15 PDF, confirmando: **P01–P10 firmados el 2026-07-24**, **P11–P15
  firmados el 2026-07-26**.
- Ninguno de los 15 documentos menciona "Quintanilla Normal University" ni
  ningún otro nombre de institución (el formulario firmado no incluye ese
  campo).

### Coherencia cronológica final

| Grupo | Firma de consentimiento | Evaluación real | Consentimiento previo a la evaluación |
|---|---|---|---|
| P01–P10 | 2026-07-24 | 2026-07-30 (`e8d7e2f`, ver sección 2) | Sí, 6 días antes |
| P11–P15 | 2026-07-26 | 2026-08-16 (declarada por el equipo, corregida el 2026-09-17 tras una confusión inicial de fechas; los archivos `P11.json`–`P15.json` se subieron al repo después, el 2026-09-06 — commit `3516019`) | Sí, 21 días antes |

Con esto, los **15 participantes tienen consentimiento informado individual,
firmado en papel antes de su respectiva evaluación**, con evidencia
verificable mediante hash sin necesidad de exponer las firmas o identidades
reales en el repositorio público. Los documentos originales **no se suben a
este repositorio** — se conservan fuera de control de versiones, en poder
del equipo, siguiendo el mismo principio de protección de datos personales
descrito en la sección 5.

### Por qué no se borran las secciones 1-6

Las secciones anteriores documentan un hallazgo real de esta auditoría (dos
registros contradictorios, sin respaldo, que existieron en el repositorio
durante semanas) y el proceso honesto seguido para resolverlo. Borrarlas
ahora que se encontró la evidencia real daría la impresión de que la
contradicción nunca existió. Se mantienen como registro histórico de la
auditoría, con esta sección 7 documentando la resolución final.

### Conclusión actualizada

**El riesgo de Piso 3 sobre P11 queda resuelto**: ya no hay una fecha
inventada ni una contradicción sin explicar — hay evidencia verificable
(hash + fecha de firma legible en cada documento) de consentimiento
individual, previo a la evaluación, para los 15 participantes. La única
reserva que queda, y que se declara explícitamente, es que la fecha de
evaluación de P11–P15 (2026-08-16) es una declaración del equipo (corregida
una vez el 2026-09-17 tras una confusión inicial con otra fecha), no algo
verificado de forma independiente contra un artefacto externo con esa fecha
exacta — el primer artefacto de git relacionado con esos datos es el commit
`3516019` del 2026-09-06, tres semanas después de la evaluación declarada
(intervalo comparable al de P01-P10, cuyos JSON individuales también se
subieron semanas después de la evaluación: 30-jul evaluación, 16-ago subida
de JSON individuales). A diferencia de P01–P10, cuya fecha de evaluación
(30-jul) sí está respaldada por un commit de git del mismo día
(`e8d7e2f`), la fecha exacta de evaluación de P11-P15 no tiene ese mismo
respaldo directo — sí es consistente con toda la cadena de fechas
(consentimiento 26-jul, subida a git 6-sep), simplemente no está anclada a
un artefacto externo del día mismo.
