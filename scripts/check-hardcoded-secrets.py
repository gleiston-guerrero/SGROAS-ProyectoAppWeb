#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P1 -- Comprueba que no haya secretos literales (contrasena de BD, JWT
secret) en ningun archivo de configuracion versionado.

Antes esta logica vivia duplicada, como lineas de grep sueltas, en el
Makefile y en scripts/verify.sh -- exactamente el patron que ya hizo
que los dos scripts se desincronizaran varias veces en rondas
anteriores (el umbral de P4, los globs de P2, los bloques P2/P3/P6/P8
que le faltaban a verify.sh). Este script es la unica fuente de verdad;
ambos wrappers solo lo invocan.

Cubre, ademas de application.properties/docker-compose.yml (el alcance
original de P1, senalado como insuficiente en varias rondas):
  - src/test/resources/application-test.properties
  - render.yaml (valores literales en las claves sensibles, en vez de
    fromDatabase/generateValue)
  - todos los .sql versionados (db/, database/migrations/,
    src/main/resources/db/migration/)

Uso:
  python3 scripts/check-hardcoded-secrets.py
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

LITERAL_WORDS = re.compile(r"CHANGE_ME|password123|secret_key", re.IGNORECASE)

PROPERTIES_FILES = [
    "src/main/resources/application.properties",
    "src/test/resources/application-test.properties",
]

RENDER_SENSITIVE_KEYS = [
    "PGPASSWORD", "APP_JWT_SECRET", "SPRING_DATASOURCE_PASSWORD",
    "JWT_SECRET", "POSTGRES_PASSWORD",
]

SQL_DIRS = ["db", "database/migrations", "src/main/resources/db/migration"]


def read(rel):
    path = os.path.join(ROOT, rel.replace("/", os.sep))
    if not os.path.isfile(path):
        return None
    with open(path, encoding="utf-8", errors="ignore") as fh:
        return fh.read()


def find_sql_files():
    found = []
    for base in SQL_DIRS:
        base_path = os.path.join(ROOT, base.replace("/", os.sep))
        if not os.path.isdir(base_path):
            continue
        for dirpath, _dirs, files in os.walk(base_path):
            for name in sorted(files):
                if name.lower().endswith(".sql"):
                    found.append(os.path.relpath(
                        os.path.join(dirpath, name), ROOT
                    ).replace(os.sep, "/"))
    return found


def check_literal_words(rel, content, problems):
    for m in LITERAL_WORDS.finditer(content):
        if m.group(0).upper() == "CHANGE_ME":
            continue  # placeholder documentado, no un secreto real
        line_no = content.count("\n", 0, m.start()) + 1
        problems.append("%s:%d -- palabra de la lista negra (%s)" % (
            rel, line_no, m.group(0)))


def check_datasource_password(rel, content, problems):
    for i, line in enumerate(content.splitlines(), 1):
        m = re.search(r"spring\.datasource\.password=(.*)", line)
        if m and not m.group(1).startswith("$"):
            problems.append("%s:%d -- spring.datasource.password literal" % (rel, i))
        m = re.search(r"SPRING_DATASOURCE_PASSWORD=(.*)", line)
        if m and not m.group(1).startswith("$"):
            problems.append("%s:%d -- SPRING_DATASOURCE_PASSWORD literal" % (rel, i))
        m = re.search(r"(?:app\.)?jwt\.secret=(.*)", line, re.IGNORECASE)
        if m and not m.group(1).startswith("$"):
            problems.append("%s:%d -- jwt.secret literal" % (rel, i))


def check_render_yaml(problems):
    content = read("render.yaml")
    if content is None:
        return
    lines = content.splitlines()
    for i, line in enumerate(lines):
        m = re.search(r"key:\s*(\w+)", line)
        if not m or m.group(1) not in RENDER_SENSITIVE_KEYS:
            continue
        # La clave sensible debe resolverse con fromDatabase/fromService/
        # generateValue/sync:false -- nunca con un "value:" literal.
        for j in range(i + 1, min(i + 4, len(lines))):
            nxt = lines[j].strip()
            if nxt.startswith("- key:"):
                break
            if nxt.startswith("value:"):
                problems.append(
                    "render.yaml:%d -- %s tiene un 'value:' literal en vez "
                    "de fromDatabase/fromService/generateValue"
                    % (j + 1, m.group(1)))
                break
    check_literal_words("render.yaml", content, problems)


def main():
    problems = []

    for rel in PROPERTIES_FILES:
        content = read(rel)
        if content is None:
            continue
        check_literal_words(rel, content, problems)
        check_datasource_password(rel, content, problems)

    docker_compose = read("docker-compose.yml")
    if docker_compose is not None:
        check_literal_words("docker-compose.yml", docker_compose, problems)
        check_datasource_password("docker-compose.yml", docker_compose, problems)

    check_render_yaml(problems)

    sql_files = find_sql_files()
    sql_checked = 0
    for rel in sql_files:
        content = read(rel)
        if content is None:
            continue
        sql_checked += 1
        check_literal_words(rel, content, problems)
        check_datasource_password(rel, content, problems)

    print("  Archivos .properties/render.yaml/docker-compose.yml revisados: %d"
          % (len(PROPERTIES_FILES) + 2))
    print("  Archivos .sql revisados: %d" % sql_checked)

    if problems:
        print("  FAIL:")
        for p in problems:
            print("    %s" % p)
        return 1
    print("  OK - sin secretos literales en configuracion ni SQL")
    return 0


if __name__ == "__main__":
    sys.exit(main())
