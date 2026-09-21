# -*- coding: utf-8 -*-
"""Lexico compartido de palabras que solo existen en espanol, usado por
check-figure-text.py (OCR de pixeles) y check-caption-language.py (texto
de \\caption{...} en LaTeX). Antes cada check tenia su propia lista
pequeña y ad hoc, lo que dejaba pasar frases en español que ninguna de
las dos cubria (una re-evaluacion senalo, con razon, que un pie de
figura entero en español sin ninguna de las ~12 palabras vigiladas
originales pasaria sin aviso). Una sola lista amplia y compartida cierra
ese hueco para ambos checks a la vez.
"""
from __future__ import annotations

import re
import unicodedata

# Palabras funcionales/comunes que solo existen en espanol (deaccented,
# minusculas). Para texto LaTeX limpio (sin ruido de OCR) se puede usar
# una lista mucho mas agresiva que la de check-figure-text.py, que evita
# a proposito palabras cortas por falsos positivos de OCR.
SPANISH_STOPWORDS = [
    "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del",
    "al", "en", "con", "por", "para", "sobre", "entre", "hasta",
    "desde", "hacia", "segun", "ante", "bajo", "tras", "durante",
    "mediante", "contra", "y", "o", "u", "que", "como", "pero", "aunque",
    "porque", "pues", "se", "su", "fue",
    "fueron", "era", "eran", "ha", "han", "hay", "estan", "esto",
    "esta", "estos", "estas", "ese", "esa", "esos", "esas", "aquel",
    "aquella", "muy", "tambien", "solo", "sola",
    "solamente", "cada", "todo", "toda", "todos", "todas", "otro", "otra",
    "otros", "otras", "mismo", "misma", "mismos", "mismas",
    # Excluidos a proposito por colisionar con acronimos/palabras reales
    # del propio informe (falsos amigos, mismo criterio que la lista de
    # check-figure-text.py): "sus" (SUS, System Usability Scale), "no"
    # (palabra inglesa comun), "son"/"sin"/"mas"/"es" (ambiguas o
    # demasiado cortas para el contexto de un caption tecnico en ingles).
]

# Raices tecnicas/de dominio, en español, que aparecen en el codigo, el
# informe o los mensajes de commit de este proyecto (no son stopwords
# genericos, pero son igual de inequivocamente español).
SPANISH_DOMAIN_WORDS = [
    "corrige", "corregir", "corregido", "corrida", "corridas",
    "recaptura", "recapturar", "espanol", "horneado", "horneados",
    "revierte", "revertir", "cobertura", "parcial", "correr", "incluyo",
    "permite", "despliegue", "despliegues", "anterior", "conecta",
    "paginacion", "servicios", "auditoria", "encontro", "fallar",
    "archivo", "archivos", "captura", "capturas", "informe", "tabla",
    "tablas", "figura", "figuras", "listado", "listados", "cambios",
    "mensaje", "mensajes", "prueba", "pruebas", "datos", "verificacion",
    "agrega", "agregar", "ajusta", "ajustar", "actualiza", "actualizar",
    "elimina", "eliminar", "correccion", "afirmacion", "reales",
    "nuevas", "nuevos", "nueva", "nuevo", "usuario", "usuarios",
    "conductor", "conductores", "vehiculo", "vehiculos", "ruta", "rutas",
    "incidente", "incidentes", "asignacion", "asignaciones", "resumen",
    "resultados", "distribucion", "sintesis", "desglose", "trazados",
    "comparacion", "puntaje", "prioridad", "corridas", "cuadro",
    "cuadros",
]

SPANISH_LEXICON_OCR = SPANISH_DOMAIN_WORDS

SPANISH_LEXICON_STRICT = sorted(set(SPANISH_STOPWORDS + SPANISH_DOMAIN_WORDS))


def deaccent(text: str) -> str:
    return "".join(
        c for c in unicodedata.normalize("NFD", text)
        if unicodedata.category(c) != "Mn"
    )


def find_spanish_hits(text: str, lexicon) -> list[str]:
    flat = deaccent(text).lower()
    hits = []
    for word in lexicon:
        if re.search(r"(?<![a-z])" + re.escape(word) + r"(?![a-z])", flat):
            hits.append(word)
    return hits
