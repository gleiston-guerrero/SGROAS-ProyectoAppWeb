#!/usr/bin/env python3
"""Recalcula el contraste no parametrico cache frio vs caliente (Bloque C).

Lee las corridas crudas versionadas en dataset/perf/kNN-cold.json y
kNN-run1.json y reproduce la U de Mann-Whitney y el d de Cliff usando
scripts/perf/nonparametric.py.

Ambas condiciones se leen de la MISMA metrica k6 (nombre distinto por
condicion porque los checks de k6 las etiquetan por separado, pero
representan lo mismo: la duracion de la peticion GET /api/conductores) y
con la MISMA funcion estadistica (avg), para que la comparacion sea
homogenea:
  - Frio  ("duracion_frio"):   1 VU, primer GET tras ~90 s de pausa.
  - Caliente ("duracion_listado"): 50 VUs, 30 s de carga sostenida.

Esta comparacion NO es "cache fria vs cache caliente" en el sentido de un
Redis/HTTP cache: el codigo actual de ConductorService no tiene
@Cacheable (ver docs/mediciones/perf/ANALISIS-k6.md). Es una comparacion
entre 1 VU sin carga y 50 VUs con carga sostenida contra el mismo endpoint
sin cache aplicativa.

El script no fija de antemano si el resultado sera significativo: calcula
todo a partir de los datos y solo imprime lo que sale.

Uso: python scripts/perf/recalcular-contraste.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


from perf.nonparametric import cliffs_delta, interpretar_cliffs_delta, mann_whitney_u

RAIZ = Path(__file__).resolve().parents[2]
PERF = RAIZ / "dataset" / "perf"

FRIAS = ["k04-cold.json", "k05-cold.json", "k06-cold.json", "k07-cold.json", "k08-cold.json"]
CALIENTES = ["k04-run1.json", "k05-run1.json", "k06-run1.json", "k07-run1.json", "k08-run1.json"]

METRICA_FRIA = "duracion_frio"
METRICA_CALIENTE = "duracion_listado"
ESTADISTICO = "avg"  # misma funcion estadistica para ambas condiciones


def _leer_metrica(archivo: Path, nombre_metrica: str, estadistico: str) -> float:
    with open(archivo, encoding="utf-8") as fh:
        datos = json.load(fh)
    metricas = datos["metrics"]
    if nombre_metrica not in metricas:
        raise KeyError(
            f"{archivo.name}: la metrica '{nombre_metrica}' no existe en este JSON. "
            f"Metricas disponibles: {sorted(metricas.keys())}"
        )
    return float(metricas[nombre_metrica][estadistico])


def main() -> int:
    frias = [_leer_metrica(PERF / f, METRICA_FRIA, ESTADISTICO) for f in FRIAS]
    calientes = [_leer_metrica(PERF / c, METRICA_CALIENTE, ESTADISTICO) for c in CALIENTES]

    print(f"Metrica fria     ({METRICA_FRIA}.{ESTADISTICO}): {frias}")
    print(f"Metrica caliente ({METRICA_CALIENTE}.{ESTADISTICO}): {calientes}")
    print()

    u, z, p = mann_whitney_u(frias, calientes)
    d = cliffs_delta(frias, calientes)

    print("Contraste no parametrico: 1 VU sin carga vs 50 VUs con carga (n = 5 por condicion)")
    print(f"U (Mann-Whitney)       : {u}")
    print(f"z (aproximacion normal): {z:.2f}")
    print(f"p (bilateral)          : {p:.4f}")
    print(f"d de Cliff             : {d:.2f} -> {interpretar_cliffs_delta(d)}")
    print()
    print("Este resultado se calcula directamente de los JSON crudos, sin valores")
    print("fijados de antemano. No hay garantia de que sea significativo.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
