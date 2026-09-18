"""Puntuacion SUS (System Usability Scale) segun Brooke (1996).

Regla estandar:
  * items impares (q1, q3, q5, q7, q9):  contribucion = respuesta - 1
  * items pares  (q2, q4, q6, q8, q10): contribucion = 5 - respuesta
  * puntuacion final = suma de contribuciones * 2.5   (rango 0-100)
"""

from __future__ import annotations

from typing import List, Sequence, Tuple


def contribuciones(respuestas: Sequence[int]) -> List[int]:
    """Convierte las 10 respuestas Likert en las 10 contribuciones SUS."""
    if len(respuestas) != 10:
        raise ValueError("El SUS exige exactamente 10 items")
    contrib: List[int] = []
    for i, r in enumerate(respuestas, start=1):
        if not (1 <= r <= 5):
            raise ValueError(f"Item q{i} fuera de escala 1-5: {r}")
        contrib.append(r - 1 if i % 2 == 1 else 5 - r)
    return contrib


def puntuacion_sus(respuestas: Sequence[int]) -> float:
    """Puntuacion SUS final (0-100) de un participante."""
    return sum(contribuciones(respuestas)) * 2.5


def interpretar_adjetiva(media: float) -> Tuple[str, str]:
    """Calificacion adjetiva y zona de aceptabilidad (Bangor et al., 2009).

    Devuelve (adjetivo, zona_aceptabilidad).

    Bug real corregido (2026-09-18, senalado por una re-evaluacion externa):
    esta funcion devolvia "Bueno" tanto para el rango 70-85 como para el
    50-70, contradiciendo la propia zona de aceptabilidad que la misma
    funcion calculaba ("Marginal (50-70)" con la etiqueta "Bueno" al lado,
    algo que no tiene sentido). En la escala de Bangor et al. (2009), 63.0
    (dentro de 50-70) corresponde a la calificacion "OK", no a "Bueno"
    (que empieza alrededor de 70-72).
    """
    if media >= 85:
        return "Excelente", "Aceptable (>= 70)"
    if media >= 70:
        return "Bueno", "Aceptable (>= 70)"
    if media >= 50:
        return "OK", "Marginal (50-70)"
    if media >= 35:
        return "Regular", "No aceptable"
    return "Pobre", "No aceptable"
