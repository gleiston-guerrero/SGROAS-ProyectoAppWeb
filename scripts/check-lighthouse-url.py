#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3 -- Nada comprobaba que las 9 corridas de Lighthouse citadas en el
informe (dataset/lighthouse/RESUMEN.md) se hayan ejecutado de verdad
contra el despliegue publico. Una re-evaluacion senalo que una corrida
apuntando a localhost pasaria sin que nada lo notara.

Lee el campo "requestedUrl" de cada JSON de Lighthouse citado y falla si
alguno no apunta al dominio publico, o si apunta a localhost/127.0.0.1.

Uso:
  python3 scripts/check-lighthouse-url.py
"""
from __future__ import annotations

import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LH_DIR = os.path.join(ROOT, "dataset", "lighthouse")
PUBLIC_HOST = "sgroas-backend.onrender.com"

# Las 9 corridas que dataset/lighthouse/RESUMEN.md declara como la
# evidencia oficial citada en el informe (3 moviles + 3 escritorio + 3
# tableta). Otros archivos lh-*.json en la carpeta son corridas
# adicionales/historicas, no las citadas para el umbral de P3.
CITED_RUNS = [
    "lh-mobile-1.json", "lh-mobile-2.json", "lh-mobile-3.json",
    "lh-desktop-1.json", "lh-desktop-2.json", "lh-desktop-3.json",
    "lh-tablet-1.json", "lh-tablet-2.json", "lh-tablet-3.json",
]


def main():
    bad = []
    checked = 0
    for name in CITED_RUNS:
        path = os.path.join(LH_DIR, name)
        if not os.path.isfile(path):
            bad.append((name, "archivo no encontrado"))
            continue
        try:
            with open(path, encoding="utf-8") as fh:
                data = json.load(fh)
        except (OSError, ValueError) as exc:
            bad.append((name, "no se pudo leer/parsear: %s" % exc))
            continue
        checked += 1
        url = data.get("requestedUrl") or data.get("finalUrl") or ""
        if "localhost" in url or "127.0.0.1" in url:
            bad.append((name, "apunta a localhost: %s" % url))
        elif PUBLIC_HOST not in url:
            bad.append((name, "no apunta al dominio publico: %s" % (url or "(vacio)")))

    print("  Corridas Lighthouse citadas verificadas: %d/%d" % (checked, len(CITED_RUNS)))
    if bad:
        print("  FAIL:")
        for name, reason in bad:
            print("    %s -- %s" % (name, reason))
        return 1
    print("  OK - las 9 corridas citadas apuntan a %s" % PUBLIC_HOST)
    return 0


if __name__ == "__main__":
    sys.exit(main())
