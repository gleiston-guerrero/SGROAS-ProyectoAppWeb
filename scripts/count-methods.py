#!/usr/bin/env python3
"""Cuenta metodos y constructores publicos reales en src/main/java (P6).

La cifra "226/226" usada anteriormente en el informe contaba las
declaraciones de `record` como si fueran metodos (los `record` no declaran
metodos explicitos: sus accesores son generados por el compilador, no texto
fuente) y no distinguia interfaces de clases. Este script:

  - Cuenta metodos publicos/protegidos declarados explicitamente en el
    codigo fuente (clases E interfaces), sin inventar accesores de record.
  - Cuenta constructores publicos declarados explicitamente.
  - Excluye por completo las declaraciones `record` de la cuenta de
    metodos (sus componentes no son metodos en el sentido de este conteo).
  - Reporta con y sin Javadoc inmediatamente encima.

Uso: python scripts/count-methods.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java")

TYPE_DECL = re.compile(
    r"^\s*(public|protected)?\s*(static\s+)?(final\s+)?(abstract\s+)?"
    r"(class|interface|enum|record)\s+(\w+)"
)
METHOD_PATTERN = re.compile(
    r"^\s*(public|protected)\s+((static|final|synchronized|abstract|default)\s+)*"
    r"[\w<>, \.\?\[\]]+\s+(\w+)\s*\(",
    re.IGNORECASE,
)
CONSTRUCTOR_PATTERN_TMPL = r"^\s*(public|protected)\s+{name}\s*\("


def _has_javadoc_above(lines, i):
    j = i - 1
    while j >= 0 and lines[j].strip().startswith("@"):
        j -= 1
    return j >= 0 and lines[j].strip().endswith("*/")


def main():
    methods_total = methods_doc = 0
    ctors_total = ctors_doc = 0
    record_components_skipped = 0

    for dirpath, _dirs, files in os.walk(SRC):
        for fname in files:
            if not fname.endswith(".java"):
                continue
            path = os.path.join(dirpath, fname)
            with open(path, "r", encoding="latin-1") as fh:
                lines = fh.read().splitlines()

            # Rastrea el tipo declarado mas reciente (para saber si una
            # linea de "metodo" cae dentro de un `record` o no, y para
            # excluir el constructor implicito canonico de los records).
            record_names = set()
            for i, line in enumerate(lines):
                t = TYPE_DECL.match(line)
                if t and t.group(5) == "record":
                    record_names.add(t.group(6))

            for i, line in enumerate(lines):
                # Si la linea es la propia declaracion de un record (con sus
                # componentes en la firma), no se cuenta como metodo.
                if TYPE_DECL.match(line) and TYPE_DECL.match(line).group(5) == "record":
                    record_components_skipped += 1
                    continue

                m = METHOD_PATTERN.match(line)
                if m:
                    methods_total += 1
                    if _has_javadoc_above(lines, i):
                        methods_doc += 1
                    continue

                for name in record_names:
                    if re.match(CONSTRUCTOR_PATTERN_TMPL.format(name=re.escape(name)), line):
                        # Constructor compacto/canonico de un record explicito en fuente.
                        ctors_total += 1
                        if _has_javadoc_above(lines, i):
                            ctors_doc += 1

    print("Conteo real de metodos y constructores publicos/protegidos (P6)")
    print("=" * 70)
    print(f"Metodos publicos/protegidos declarados en fuente : {methods_total}")
    print(f"  ... con Javadoc inmediato                       : {methods_doc}")
    print(f"  ... sin Javadoc                                 : {methods_total - methods_doc}")
    print(f"Constructores publicos/protegidos declarados      : {ctors_total}")
    print(f"  ... con Javadoc inmediato                       : {ctors_doc}")
    print(f"  ... sin Javadoc                                 : {ctors_total - ctors_doc}")
    print(f"Total (metodos + constructores)                   : {methods_total + ctors_total}")
    print(f"Declaraciones 'record' excluidas de la cuenta      : {record_components_skipped}")
    print()
    print("Nota: los componentes de un `record` (accesores, equals, hashCode,")
    print("toString generados por el compilador) NO se cuentan aqui como")
    print("metodos, porque no existen como texto fuente explicito.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
