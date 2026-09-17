# Registro de consentimientos informados — SUS

> **Actualización 2026-09-17: constancias reales localizadas y verificadas.**
> Este archivo documentaba, hasta el 2026-09-16, que no existía ninguna
> constancia de consentimiento verificable. El 2026-09-17 el equipo localizó
> los 15 formularios de consentimiento firmados en papel (P01–P15), y se
> verificaron uno por uno. Ver
> `docs/etica/consentimientos/CONSENT-STATUS.md` para el historial completo
> de la discrepancia original, cómo se resolvió, y la evidencia de
> verificación.

## Qué existe ahora, verificado

15 formularios de consentimiento firmados en papel, uno por participante
(P01–P15), fotografiados/escaneados a PDF. Verificación realizada el
2026-09-17:

- Se calculó el SHA-256 de cada uno de los 15 archivos con dos herramientas
  distintas (`sha256sum` y `hashlib.sha256` de Python), con resultados
  idénticos en ambas.
- Los 15 hashes son distintos entre sí (sin duplicados).
- Se extrajo la fecha de firma legible de cada PDF con `pdftotext -layout`,
  confirmando: P01–P10 firmados el 2026-07-24, P11–P15 firmados el
  2026-07-26 — ambas fechas anteriores a la evaluación real de cada grupo
  (P01–P10 evaluados el 2026-07-30, P11–P15 evaluados el 2026-08-03).
- El registro consolidado con código, edad, sexo, fecha de firma, fecha de
  evaluación, medio y hash está en `dataset/sus/CONSENT-REGISTRY.md`.

## Dónde están los documentos originales

**No se suben al repositorio** (contienen firmas manuscritas, es decir,
información personal identificable). Se conservan fuera de control de
versiones, en poder del equipo. El registro público (`CONSENT-REGISTRY.md`)
solo publica el hash de cada documento, que permite a cualquiera con acceso
al original verificarlo sin exponer la firma ni el nombre del participante.

## Historial de la discrepancia original (para trazabilidad)

Antes de esta actualización, existían dos registros contradictorios (uno
declaraba firma el 2026-08-15 para los 15, después de que la evaluación de
P01–P10 ya había ocurrido; otro declaraba el 2026-07-30 solo para P01–P10,
citando como "evidencia" los propios JSON de respuestas). Ninguno de los dos
tenía respaldo verificable. El detalle completo de esa contradicción, los
comandos de `git log` usados para detectarla, y por qué no se resolvió
inventando una fecha, sigue documentado en `CONSENT-STATUS.md` — no se borra
esa sección porque documenta un hallazgo real de la auditoría, aunque ya esté
superado por las constancias encontradas después.

Plantilla utilizada: [plantilla.md](plantilla.md)
