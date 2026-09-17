# Registro de consentimientos informados — SUS

> **Nota de auditoría (2026-09-16):** este archivo reemplaza una versión previa
> que entraba en contradicción con `dataset/sus/CONSENT-REGISTRY.md`. Ver
> `docs/etica/consentimientos/CONSENT-STATUS.md` para el detalle completo de
> la discrepancia encontrada, la evidencia de `git log` usada para resolverla,
> y la conclusión honesta sobre el estado real del consentimiento informado
> (no calificable al máximo en P11 por falta de constancias verificables).

## Qué existe realmente en el repositorio

No existe ninguna constancia de consentimiento firmada de forma independiente
(PDF, imagen escaneada, firma digital) para ningún participante P01–P15. Lo
único que existe es:

1. Una plantilla de consentimiento (`plantilla.md`, este directorio) y un
   formulario en inglés (`dataset/sus/CONSENT-FORM.md`).
2. Un campo autodeclarado `"consentimiento": "Sí"` dentro de cada archivo de
   respuestas crudas del SUS (`dataset/sus/P01.json` … `P15.json`).
3. Dos registros tabulares que **afirman** que hubo consentimiento firmado,
   con fechas que se contradicen entre sí y con el propio historial de git
   (ver detalle en `CONSENT-STATUS.md`).

## Reglas de custodia declaradas (sin poder verificarlas)

Según la versión anterior de este archivo, los consentimientos firmados en
papel o digitales se custodiarían fuera del repositorio, en una carpeta
privada (`docs/etica/consentimientos/firmados/`, no versionada). Esa carpeta
no es accesible desde este entorno de auditoría, por lo que **no se puede
confirmar ni descartar** que existan constancias físicas fuera del repo. Lo
que sí se puede afirmar es que, dentro del repositorio, no hay evidencia
verificable más allá del campo autodeclarado en el JSON de cada participante.

Plantilla utilizada: [plantilla.md](plantilla.md)
