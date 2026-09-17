#!/usr/bin/env python3
"""Checks Javadoc coverage on public/protected methods and constructors in
src/main/java (P6), counting classes AND interfaces, and excluding `record`
declarations from the method count (a record's accessors/equals/hashCode/
toString are compiler-generated, not text explicitly written as a method).

Rationale for the rewrite: the previous version used a single regex that (a)
matched `public record Foo(...)` itself as if it were a method named `Foo`
(inflating the count with declarations, not methods), and (b) required an
explicit `public`/`protected` keyword, which interface members never carry
(they are implicitly public) -- so interface methods were silently excluded
from both the numerator and the denominator. Both defects were flagged by an
external evaluation of this repository. This version tracks the enclosing
type (class/interface/enum/record) via a brace-depth stack so it can:
  - skip a `record` type-declaration line itself (never counted as a method),
  - still count explicit methods written inside a record body,
  - count interface members (abstract, `default`, `static`) as public even
    though they carry no explicit modifier.

A second re-verification pass found a further defect in this same rewrite:
a multi-line annotation body (e.g. an @Query using a triple-quoted text
block for its SQL/JPQL) spanning several
lines) was walked line by line by the method-detection regexes, so a SQL
fragment inside the text block (e.g. `OR LOWER(c.lastNames) LIKE ...`) could
spuriously match the "type name(" shape and be counted as a fake method: it
inflated both the total and the "missing" count without ever being real.
The same multi-line annotation also broke the Javadoc-lookup, which stopped
at the first line not starting with "@" -- for a multi-line annotation that
is a SQL continuation line, not the real Javadoc comment several lines above
the annotation's start. `_classify_annotation_lines` now marks every line
that belongs to an annotation invocation (including its continuation lines
up to its closing parenthesis) so both the method scan and the Javadoc
lookup skip over them correctly.

Exit 0 = OK (>=90%), 1 = below 90%.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java")

TYPE_DECL = re.compile(
    r"^\s*(public|protected|private)?\s*(static\s+)?(final\s+)?(abstract\s+)?"
    r"(class|interface|enum|record)\s+(\w+)"
)
EXPLICIT_METHOD = re.compile(
    r"^\s*(public|protected)\s+((static|final|synchronized|abstract|default)\s+)*"
    r"[\w<>, \.\?\[\]]+\s+(\w+)\s*\(",
    re.IGNORECASE,
)
# Interface members carry no explicit public/protected (it's implied).
IMPLICIT_IFACE_METHOD = re.compile(
    r"^\s*(default\s+|static\s+)?[\w<>, \.\?\[\]]+\s+(\w+)\s*\(",
    re.IGNORECASE,
)


def _classify_annotation_lines(lines):
    """Returns a set of line indices that are part of an annotation, including
    every continuation line of a multi-line annotation invocation such as
    an @Query with a triple-quoted text block, or an @Query(value = ...,
    nativeQuery = true) spanning several lines.

    Without this, a text-block body passed to @Query is walked line-by-line
    by the method-detection regexes (a SQL fragment like
    `OR LOWER(c.lastNames) LIKE ...` can spuriously match "type name(" and be
    counted as a fake method), and the Javadoc-lookup below stops at the
    first non-"@" line it meets, which for a multi-line annotation is a SQL
    continuation line, not the Javadoc block above the annotation.
    """
    annotation_lines = set()
    ann_depth = 0
    for i, line in enumerate(lines):
        stripped = line.strip()
        if ann_depth > 0:
            annotation_lines.add(i)
            ann_depth += line.count("(") - line.count(")")
            continue
        if stripped.startswith("@"):
            annotation_lines.add(i)
            opens = line.count("(")
            closes = line.count(")")
            if opens > closes:
                ann_depth = opens - closes
    return annotation_lines


def _has_javadoc_above(lines, i, annotation_lines):
    j = i - 1
    while j >= 0 and j in annotation_lines:
        j -= 1
    return j >= 0 and lines[j].strip().endswith("*/")


def _brace_delta(line):
    # Best-effort: ignores braces inside string/char literals. Acceptable for
    # this codebase's style (no stray braces in string literals in headers).
    return line.count("{") - line.count("}")


def main():
    total = 0
    doc = 0
    missing = []

    for dirpath, _dirs, files in os.walk(SRC):
        for fname in files:
            if not fname.endswith(".java"):
                continue
            path = os.path.join(dirpath, fname)
            with open(path, "r", encoding="latin-1") as fh:
                lines = fh.read().splitlines()

            annotation_lines = _classify_annotation_lines(lines)

            depth = 0
            # Stack of (kind, depth_at_which_body_starts)
            stack = []

            for i, line in enumerate(lines):
                stripped = line.strip()
                type_m = TYPE_DECL.match(line)

                if type_m:
                    kind = type_m.group(5)
                    if kind == "record":
                        # The record's own declaration line is never a method.
                        depth += _brace_delta(line)
                        stack.append((kind, depth))
                        continue
                    depth += _brace_delta(line)
                    stack.append((kind, depth))
                    continue

                current_kind = stack[-1][0] if stack else None

                counted = False
                if (
                    i not in annotation_lines
                    and not stripped.startswith("@")
                    and not stripped.startswith("//")
                    and not stripped.startswith("*")
                ):
                    if EXPLICIT_METHOD.match(line):
                        counted = True
                    elif current_kind == "interface" and IMPLICIT_IFACE_METHOD.match(line):
                        counted = True

                if counted:
                    total += 1
                    if _has_javadoc_above(lines, i, annotation_lines):
                        doc += 1
                    else:
                        rel = os.path.relpath(path, ROOT)
                        missing.append(f"{rel}:{i + 1}")

                depth += _brace_delta(line)
                while stack and depth < stack[-1][1]:
                    stack.pop()

    pct = 100.0 * doc / total if total > 0 else 0.0
    print(f"Javadoc coverage: {doc}/{total} ({pct:.1f}%)")
    if pct < 90:
        print(f"Missing Javadoc ({len(missing)}):")
        for item in missing:
            print(f"  {item}")
        print("FAIL: Javadoc below 90%")
        return 1
    print("OK: Javadoc >= 90%")
    return 0


if __name__ == "__main__":
    sys.exit(main())
