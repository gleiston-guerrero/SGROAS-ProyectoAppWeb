#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P7 — Comprueba que NO quede texto en espanol DENTRO de los pixeles de las
figuras del informe final.

El grep de \caption{...} del Makefile solo ve el texto LaTeX; no puede ver
los mensajes de commit, rotulos o titulos horneados en un PNG/JPEG. Este
script cierra ese hueco:

  1. Descubre las figuras realmente incluidas en el informe leyendo los
     \includegraphics de docs/informe-final/**/*.tex (no una lista fija).
  2. Para cada figura exige una instantanea OCR versionada en
     docs/informe-final/figuras/ocr/<nombre>.txt cuya cabecera declara el
     sha256 de la imagen. Si la imagen cambia y la instantanea no, el hash
     no coincide y el check FALLA.
  3. Busca un lexico de palabras que solo existen en espanol sobre ese OCR.
     Si aparece una, el check FALLA.
  4. Si tesseract esta instalado, rehace el OCR en el momento y compara: la
     instantanea no se puede "limpiar" a mano sin que se note. Si no esta
     instalado (clon limpio minimo), usa la instantanea versionada y lo dice
     en la salida.

Uso:
  python3 scripts/check-figure-text.py            # verificar (exit 1 si falla)
  python3 scripts/check-figure-text.py --update   # regenerar instantaneas
"""
from __future__ import annotations

import hashlib
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_DIR = os.path.join(ROOT, "docs", "informe-final")
OCR_DIR = os.path.join(TEX_DIR, "figuras", "ocr")

# Palabras que no existen en ingles. Se buscan como palabra completa, sin
# distinguir mayusculas ni tildes. Se excluyen a proposito los falsos amigos
# frecuentes en la UI de GitHub/Render (real, version, manual, total, error,
# final, son, era, sin, mas) para que el check no falle por ruido de OCR.
SPANISH_LEXICON = [
    "con", "para", "una", "unos", "unas", "los", "las", "del", "por", "que",
    "como", "pero", "este", "esta", "estos", "estas", "donde", "cuando",
    "porque", "sobre", "entre", "hasta", "desde", "muy", "tambien", "solo",
    "sola", "solamente", "fue", "tiene", "tienen", "hacer", "hace",
    "corrige", "corregir", "corrida", "corridas", "recaptura", "recapturar",
    "espanol", "horneado", "horneados", "revierte", "revertir", "cobertura",
    "parcial", "correr", "incluyo", "permite", "despliegue", "despliegues",
    "anterior", "conecta", "paginacion", "servicios", "auditoria",
    "encontro", "fallar", "archivo", "archivos", "captura", "capturas",
    "informe", "tabla", "tablas", "figura", "figuras", "listado", "listados",
    "cambios", "mensaje", "mensajes", "prueba", "pruebas", "datos",
    "verificacion", "agrega", "agregar", "ajusta", "ajustar", "actualiza",
    "actualizar", "elimina", "eliminar", "correccion", "afirmacion",
    "reales", "nuevas", "nuevos", "nueva", "nuevo", "mismo", "misma",
]


def deaccent(text):
    return "".join(
        c for c in unicodedata.normalize("NFD", text)
        if unicodedata.category(c) != "Mn"
    )


def sha256_of(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def discover_figures():
    """Rutas (relativas a ROOT) de las imagenes incluidas en el informe."""
    pattern = re.compile(r"\\includegraphics(?:\[[^\]]*\])?\{([^}]+)\}")
    found = []
    for dirpath, _dirs, files in os.walk(TEX_DIR):
        if os.sep + "ocr" in dirpath:
            continue
        for name in sorted(files):
            if not name.endswith(".tex"):
                continue
            tex_path = os.path.join(dirpath, name)
            with open(tex_path, encoding="utf-8", errors="replace") as fh:
                body = fh.read()
            for ref in pattern.findall(body):
                candidate = os.path.normpath(os.path.join(dirpath, ref))
                if not os.path.isfile(candidate):
                    candidate = os.path.normpath(os.path.join(TEX_DIR, ref))
                if os.path.isfile(candidate):
                    rel = os.path.relpath(candidate, ROOT).replace(os.sep, "/")
                    if rel not in found:
                        found.append(rel)
                else:
                    print("  FAIL: includegraphics apunta a un archivo "
                          "inexistente: %s (%s)" % (ref, tex_path))
                    found.append("__MISSING__" + ref)
    return found


def _tesseract_once(image_path):
    out = subprocess.run(
        ["tesseract", image_path, "stdout", "-l", "eng"],
        stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, timeout=180,
    )
    if out.returncode != 0:
        return ""
    return out.stdout.decode("utf-8", "replace")


def run_tesseract(image_path):
    """OCR con preprocesado.

    A tamano original tesseract pierde las lineas pequenas: los mensajes de
    commit de la UI de GitHub/Render se quedan fuera del texto reconocido y
    el check pasaria en vacio. Se escala x2 en escala de grises y se hace
    OCR de la version normal y de la invertida (las capturas de Render son
    de tema oscuro); se concatenan ambas lecturas.
    """
    if not shutil.which("tesseract"):
        return None
    try:
        from PIL import Image, ImageOps
    except ImportError:
        try:
            return _tesseract_once(image_path)
        except (OSError, subprocess.TimeoutExpired):
            return None
    try:
        with Image.open(image_path) as src:
            gray = src.convert("L")
            big = gray.resize((gray.width * 2, gray.height * 2), Image.LANCZOS)
        chunks = []
        for tag, img in (("plain", big), ("inverted", ImageOps.invert(big))):
            fd, tmp = tempfile.mkstemp(suffix="-%s.png" % tag)
            os.close(fd)
            try:
                img.save(tmp)
                chunks.append("### %s\n%s" % (tag, _tesseract_once(tmp)))
            finally:
                try:
                    os.unlink(tmp)
                except OSError:
                    pass
        return "\n".join(chunks)
    except (OSError, ValueError, subprocess.TimeoutExpired):
        return None


def spanish_hits(text):
    flat = deaccent(text).lower()
    hits = []
    for word in SPANISH_LEXICON:
        if re.search(r"(?<![a-z])" + re.escape(word) + r"(?![a-z])", flat):
            hits.append(word)
    return hits


def snapshot_path(rel_image):
    return os.path.join(OCR_DIR, os.path.basename(rel_image) + ".txt")


def write_snapshot(rel_image, digest, text):
    if not os.path.isdir(OCR_DIR):
        os.makedirs(OCR_DIR)
    header = (
        "# OCR snapshot (tesseract -l eng, grayscale x2, plain + inverted)\n"
        "# P7 figure text check - scripts/check-figure-text.py\n"
        "# image: %s\n"
        "# sha256: %s\n"
        "# regenerate: python3 scripts/check-figure-text.py --update\n"
        "\n" % (rel_image, digest)
    )
    fh = open(snapshot_path(rel_image), "w", encoding="utf-8", newline="\n")
    try:
        fh.write(header + text.strip() + "\n")
    finally:
        fh.close()


def read_snapshot(rel_image):
    path = snapshot_path(rel_image)
    if not os.path.isfile(path):
        return None, None
    with open(path, encoding="utf-8", errors="replace") as fh:
        raw = fh.read()
    m = re.search(r"^# sha256: ([0-9a-f]{64})$", raw, re.M)
    # La cabecera lleva la ruta de la imagen (".../informe-final/figuras/...")
    # y comentarios en espanol: se descarta antes de buscar el lexico, o el
    # check se acusaria a si mismo.
    body = "\n".join(l for l in raw.splitlines() if not l.startswith("#"))
    return (m.group(1) if m else None), body


def main():
    update = "--update" in sys.argv
    figures = discover_figures()
    if not figures:
        print("  FAIL: no se encontro ninguna figura incluida en el informe")
        return 1

    has_tesseract = shutil.which("tesseract") is not None
    mode = "OCR en vivo (tesseract)" if has_tesseract else "instantanea versionada"
    print("  Figuras incluidas en el informe: %d" % len(figures))
    print("  Modo: %s" % mode)

    # El OCR es lo caro (~13 s por figura); se lanzan en paralelo para que
    # "make verify" no tarde minutos. El orden del informe se conserva.
    jobs = [r for r in figures if not r.startswith("__MISSING__")]
    live_by_rel = {}
    if has_tesseract:
        try:
            from concurrent.futures import ThreadPoolExecutor
            workers = min(len(jobs), max(1, (os.cpu_count() or 2)))
            with ThreadPoolExecutor(max_workers=workers) as pool:
                paths = [os.path.join(ROOT, r.replace("/", os.sep)) for r in jobs]
                for rel, text in zip(jobs, pool.map(run_tesseract, paths)):
                    live_by_rel[rel] = text
        except ImportError:
            pass

    failed = 0
    for rel in figures:
        if rel.startswith("__MISSING__"):
            failed += 1
            continue
        abs_path = os.path.join(ROOT, rel.replace("/", os.sep))
        digest = sha256_of(abs_path)
        live = live_by_rel.get(rel) if rel in live_by_rel else run_tesseract(abs_path)

        if update:
            if live is None:
                print("  FAIL: --update requiere tesseract instalado")
                return 1
            write_snapshot(rel, digest, live)
            hits = spanish_hits(live)
            print("  %-42s actualizado (%s)" % (
                os.path.basename(rel),
                "ESPANOL: " + ", ".join(hits) if hits else "sin espanol"))
            if hits:
                failed += 1
            continue

        snap_digest, snap_text = read_snapshot(rel)
        if snap_text is None:
            print("  FAIL: falta la instantanea OCR de %s" % rel)
            failed += 1
            continue
        if snap_digest != digest:
            print("  FAIL: %s cambio y su instantanea OCR no "
                  "(sha256 imagen %s != instantanea %s)"
                  % (rel, digest[:12], (snap_digest or "-")[:12]))
            failed += 1
            continue

        hits = spanish_hits(snap_text)
        if hits:
            print("  FAIL: texto en espanol dentro de %s: %s"
                  % (rel, ", ".join(hits)))
            failed += 1
            continue

        if live is not None:
            live_hits = spanish_hits(live)
            if live_hits:
                print("  FAIL: el OCR en vivo de %s encuentra espanol que la "
                      "instantanea no declara: %s" % (rel, ", ".join(live_hits)))
                failed += 1
                continue
        print("  OK   %-42s sha256 %s" % (os.path.basename(rel), digest[:12]))

    if failed:
        print("  [P7] FAIL: %d figura(s) con problemas" % failed)
        return 1
    print("  0 palabras en espanol en los pixeles de las figuras del informe")
    return 0


if __name__ == "__main__":
    sys.exit(main())
