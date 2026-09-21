#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P2 -- Codifica, como comprobacion automatica, las identidades
forenses que una evaluacion externa viene recalculando a mano en cada
ronda para probar que los JSON de k6 en dataset/perf/ son
exportaciones reales de "--summary-export" y no reconstrucciones o
valores falseados:

  1. http_req_duration.avg == avg(waiting) + avg(sending) + avg(receiving)
     (identidad de punto flotante: k6 descompone la duracion total en
     esas tres fases: una reconstruccion a mano casi nunca reproduce
     esto al bit).
  2. Cada metrica de latencia es monotona: min <= p50 <= p90 <= p95 <=
     p99 <= max. Falsear un solo percentil (por ejemplo para invertir
     una conclusion) rompe esta cadena con altisima probabilidad.
  3. root_group.id es el MD5 de la cadena vacia (asi arranca siempre el
     agrupador raiz de k6); cualquier otro valor es un JSON que no
     salio de un runtime real de k6.

Esto no prueba que la CARGA reportada (VUs, duracion) sea la que se dice
que fue -- eso requeriria re-ejecutar la prueba contra el sistema real,
fuera del alcance de un chequeo estatico -- pero sí hace mucho mas dificil
falsear un numero individual sin que las demas cifras del mismo archivo
dejen de cuadrar entre si.

Uso:
  python3 scripts/check-k6-authenticity.py
"""
from __future__ import annotations

import glob
import hashlib
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PERF_DIR = os.path.join(ROOT, "dataset", "perf")

DURATION_METRIC = "http_req_duration"
PHASE_METRICS = ["http_req_waiting", "http_req_sending", "http_req_receiving"]
PERCENTILE_KEYS = ["min", "p(50)", "p(90)", "p(95)", "p(99)", "max"]

TOLERANCE = 0.01  # ms de margen por acumulacion de redondeo en JSON


def find_k6_files():
    return sorted(
        f for f in glob.glob(os.path.join(PERF_DIR, "*.json"))
        if os.path.basename(f).startswith("k")
    )


def check_avg_decomposition(rel, metrics, problems):
    dur = metrics.get(DURATION_METRIC)
    if not dur or "avg" not in dur:
        return
    phase_avgs = []
    for name in PHASE_METRICS:
        phase = metrics.get(name)
        if not phase or "avg" not in phase:
            return  # estructura inesperada -- no aplica esta identidad
        phase_avgs.append(phase["avg"])
    total = sum(phase_avgs)
    if abs(total - dur["avg"]) > TOLERANCE:
        problems.append(
            "%s: http_req_duration.avg (%.6f) != waiting+sending+receiving.avg "
            "(%.6f), diff=%.6f" % (rel, dur["avg"], total, abs(total - dur["avg"])))


def check_monotonic(rel, metrics, problems):
    for metric_name in [DURATION_METRIC] + PHASE_METRICS:
        m = metrics.get(metric_name)
        if not m:
            continue
        values = []
        for key in PERCENTILE_KEYS:
            if key not in m:
                break
            values.append((key, m[key]))
        else:
            for (k1, v1), (k2, v2) in zip(values, values[1:]):
                if v1 - v2 > TOLERANCE:
                    problems.append(
                        "%s: %s.%s (%.6f) > %s.%s (%.6f), no es monotono"
                        % (rel, metric_name, k1, v1, metric_name, k2, v2))


def check_root_group(rel, data, problems):
    root_group = data.get("root_group")
    if not root_group:
        return
    expected = hashlib.md5(b"").hexdigest()
    actual = root_group.get("id")
    if actual != expected:
        problems.append(
            "%s: root_group.id = %s, se esperaba MD5('') = %s"
            % (rel, actual, expected))


def main():
    files = find_k6_files()
    if not files:
        print("  FAIL: no se encontro ningun JSON de k6 en dataset/perf/")
        return 1

    problems = []
    for path in files:
        rel = os.path.relpath(path, ROOT).replace(os.sep, "/")
        try:
            with open(path, encoding="utf-8") as fh:
                data = json.load(fh)
        except (OSError, ValueError) as exc:
            problems.append("%s: no se pudo leer/parsear (%s)" % (rel, exc))
            continue
        metrics = data.get("metrics", {})
        check_avg_decomposition(rel, metrics, problems)
        check_monotonic(rel, metrics, problems)
        check_root_group(rel, data, problems)

    print("  Archivos k6 verificados: %d" % len(files))
    if problems:
        print("  FAIL: identidades internas rotas (posible dato falseado):")
        for p in problems:
            print("    %s" % p)
        return 1
    print("  OK - todas las identidades internas de los %d archivos k6 son consistentes"
          % len(files))
    return 0


if __name__ == "__main__":
    sys.exit(main())
