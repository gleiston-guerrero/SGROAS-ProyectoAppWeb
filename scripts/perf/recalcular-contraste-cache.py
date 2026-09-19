#!/usr/bin/env python3
"""Recalcula el contraste no parametrico de cache real (frio vs caliente).

A diferencia de recalcular-contraste.py (que compara 1 VU sin carga vs 50
VUs con carga, sin @Cacheable activo -- ver ese script), este lee las 5
corridas de k6/cache-contrast.js (mismo usuario, mismo perfil de carga,
@Cacheable ya activo en produccion): la primera peticion de cada corrida
es un miss real (clave de cache nunca antes solicitada, un `page size`
distinto por corrida), las 10 siguientes son hits reales contra la misma
clave.

Cada corrida se identifica por su k6/cache-contrast.js
CACHE_CONTRAST_PAGE_SIZE (11 a 15, para no compartir clave de cache con
la corrida piloto k09, que uso 7).

Uso: python scripts/perf/recalcular-contraste-cache.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from perf.nonparametric import cliffs_delta, interpretar_cliffs_delta, mann_whitney_u

RAIZ = Path(__file__).resolve().parents[2]
PERF = RAIZ / "docs" / "mediciones" / "perf"

CORRIDAS = ["k10-cache-contrast.json", "k11-cache-contrast.json",
            "k12-cache-contrast.json", "k13-cache-contrast.json",
            "k14-cache-contrast.json"]

METRICA_FRIA = "duracion_fria"
METRICA_CALIENTE = "duracion_caliente"
ESTADISTICO = "avg"


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
    frias = [_leer_metrica(PERF / f, METRICA_FRIA, ESTADISTICO) for f in CORRIDAS]
    calientes = [_leer_metrica(PERF / f, METRICA_CALIENTE, ESTADISTICO) for f in CORRIDAS]

    print(f"Metrica fria     ({METRICA_FRIA}.{ESTADISTICO}, n=5, miss real): {frias}")
    print(f"Metrica caliente ({METRICA_CALIENTE}.{ESTADISTICO}, n=5, hits reales): {calientes}")
    print()

    u, z, p = mann_whitney_u(frias, calientes)
    d = cliffs_delta(frias, calientes)

    print("Contraste no parametrico: cache miss real vs cache hit real (n = 5 por condicion,")
    print("mismo usuario y mismo perfil de carga en ambas condiciones)")
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
