#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P1 -- El grep de secretos del Makefile/verify.sh solo mira
application.properties y docker-compose.yml; nunca mira dataset/ ni
docs/, donde las exportaciones crudas de k6 y la evidencia de pruebas de
seguridad (A02, A05) versionan JWT reales de admin@sgroas.com (usados
como Authorization: Bearer para las peticiones autenticadas de la carga
y de la auditoria OWASP).

Esos tokens no son un secreto "hardcodeado" en el sentido de P1 (no son
una contrasena ni una clave de firma; son la salida de una sesion de
prueba real, necesaria para que las exportaciones de k6 sean autenticas
y reproducibles), pero SI serian un riesgo real si no hubieran expirado.
Este script no los redacta (harerlo rompería la cadena de verificación
de autenticidad de P2, que compara estos JSON byte a byte contra la
estructura real de --summary-export) -- en vez de eso, decodifica el
claim "exp" de cada JWT versionado y falla si alguno sigue vigente.

Uso:
  python3 scripts/check-jwt-expiry.py
"""
from __future__ import annotations

import base64
import json
import os
import re
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Directorios con evidencia versionada que puede contener JWT reales.
SCAN_DIRS = ["dataset", "docs"]
SKIP_EXT = {".png", ".jpg", ".jpeg", ".pdf", ".gif"}
JWT_RE = re.compile(
    r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"
)


def b64url_decode(segment):
    padded = segment + "=" * (-len(segment) % 4)
    return base64.urlsafe_b64decode(padded.encode("ascii"))


def decode_exp(token):
    parts = token.split(".")
    if len(parts) != 3:
        return None
    try:
        payload = json.loads(b64url_decode(parts[1]))
    except (ValueError, UnicodeDecodeError):
        return None
    return payload.get("exp")


def find_files():
    for base in SCAN_DIRS:
        base_path = os.path.join(ROOT, base)
        if not os.path.isdir(base_path):
            continue
        for dirpath, _dirs, files in os.walk(base_path):
            for name in sorted(files):
                if os.path.splitext(name)[1].lower() in SKIP_EXT:
                    continue
                yield os.path.join(dirpath, name)


def main():
    now = int(time.time())
    live = []
    checked_tokens = 0
    checked_files = 0

    for path in find_files():
        try:
            with open(path, encoding="utf-8", errors="ignore") as fh:
                content = fh.read()
        except (OSError, UnicodeDecodeError):
            continue
        tokens = JWT_RE.findall(content)
        if not tokens:
            continue
        checked_files += 1
        rel = os.path.relpath(path, ROOT).replace(os.sep, "/")
        for token in set(tokens):
            exp = decode_exp(token)
            checked_tokens += 1
            if exp is None:
                live.append((rel, "token sin claim 'exp' decodificable"))
                continue
            if exp > now:
                live.append((rel, "exp=%d (vigente, vence %s)" % (
                    exp, time.strftime("%Y-%m-%d", time.gmtime(exp)))))

    print("  Archivos con JWT versionados: %d" % checked_files)
    print("  Tokens verificados: %d" % checked_tokens)

    if live:
        print("  FAIL: hay JWT vigentes (no expirados) versionados en el repositorio:")
        for rel, reason in live:
            print("    %s -- %s" % (rel, reason))
        return 1

    print("  OK - todos los JWT versionados en dataset/ y docs/ estan expirados")
    return 0


if __name__ == "__main__":
    sys.exit(main())
