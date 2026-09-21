#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P7 -- El chequeo original de captions en Makefile/verify.sh es un grep
de 12 palabras (Tabla|Figura|Listado|Resumen|...). Un pie de figura
escrito enteramente en español que no use ninguna de esas 12 palabras
pasa sin que nada lo note -- señalado por una re-evaluacion externa como
mutacion sobreviviente.

Este script extrae el texto real de cada \\caption{...} de
docs/informe-final/**/*.tex y lo contrasta contra un lexico amplio de
palabras funcionales y de dominio en español (scripts/spanish_lexicon.py,
compartido con check-figure-text.py), no una lista fija de 12 palabras.

Uso:
  python3 scripts/check-caption-language.py
"""
from __future__ import annotations

import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spanish_lexicon import SPANISH_LEXICON_STRICT, find_spanish_hits  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_DIR = os.path.join(ROOT, "docs", "informe-final")

CAPTION_RE = re.compile(r"\\caption\{((?:[^{}]|\{[^{}]*\})*)\}", re.DOTALL)


def strip_latex(text):
    """Quita comandos y math mode simples para no confundir el lexico
    con nombres de archivo (\\texttt{...}) o formulas."""
    text = re.sub(r"\\texttt\{[^{}]*\}", " ", text)
    text = re.sub(r"\$[^$]*\$", " ", text)
    text = re.sub(r"\\[a-zA-Z]+", " ", text)
    text = re.sub(r"[{}]", " ", text)
    return text


def find_captions():
    for dirpath, _dirs, files in os.walk(TEX_DIR):
        for name in sorted(files):
            if not name.endswith(".tex"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8", errors="replace") as fh:
                body = fh.read()
            rel = os.path.relpath(path, ROOT).replace(os.sep, "/")
            for m in CAPTION_RE.finditer(body):
                yield rel, m.group(1)


def main():
    problems = []
    checked = 0
    for rel, raw_caption in find_captions():
        checked += 1
        text = strip_latex(raw_caption)
        hits = find_spanish_hits(text, SPANISH_LEXICON_STRICT)
        if hits:
            problems.append((rel, raw_caption.strip()[:80], hits))

    print("  Captions revisados: %d" % checked)
    if problems:
        print("  FAIL: captions con palabras en espanol:")
        for rel, snippet, hits in problems:
            print("    %s -- \"%s...\" (%s)" % (rel, snippet, ", ".join(hits)))
        return 1
    print("  OK - los %d captions no contienen palabras en espanol" % checked)
    return 0


if __name__ == "__main__":
    sys.exit(main())
