# Reporte de usabilidad (SUS) — Bloque C.3

## Resumen

| Métrica | Valor |
|---|---|
| Participantes | 10 (P01–P10) |
| Instrumento | System Usability Scale (Brooke, 1996), 10 ítems Likert 1–5 |
| Tareas evaluadas | Login, alta de conductor, edición, eliminación lógica, logout |
| Puntuación media | **63,0 / 100** |
| Desviación típica | 13,88 |
| Error estándar | 4,39 |
| IC 95 % (t = 2,262, gl = 9) | **[53,07; 72,93]** |
| Calificación (escala adjetiva de Bangor et al., 2009) | **Bueno** (rango 52,7–72,5) |
| Zona de aceptabilidad (Bangor et al.) | Marginal (50–70) |

## Puntuación por participante

La puntuación SUS se calculó con la regla estándar: ítems impares (q1, q3, q5, q7, q9)
contribuyen `respuesta − 1`; ítems pares (q2, q4, q6, q8, q10) contribuyen `5 − respuesta`;
la suma se multiplica por 2,5.

| Código | Edad | Sexo | Exp. web | q1 | q2 | q3 | q4 | q5 | q6 | q7 | q8 | q9 | q10 | SUS |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P01 | 23 | Masculino | Baja | 3 | 3 | 4 | 4 | 4 | 3 | 4 | 4 | 1 | 3 | 47,5 |
| P02 | 21 | Femenino | Media | 5 | 5 | 4 | 4 | 3 | 3 | 4 | 2 | 2 | 3 | 52,5 |
| P03 | 25 | Masculino | Alta | 3 | 3 | 3 | 3 | 3 | 2 | 1 | 2 | 4 | 4 | 50,0 |
| P04 | 23 | Femenino | Media | 4 | 3 | 3 | 3 | 3 | 3 | 2 | 2 | 3 | 3 | 52,5 |
| P05 | 20 | Femenino | Media | 3 | 4 | 3 | 2 | 4 | 2 | 3 | 2 | 3 | 4 | 55,0 |
| P06 | 22 | Masculino | Media | 3 | 1 | 4 | 2 | 4 | 3 | 4 | 2 | 4 | 3 | 70,0 |
| P07 | 20 | Femenino | Media | 4 | 2 | 4 | 1 | 5 | 2 | 5 | 1 | 5 | 1 | 90,0 |
| P08 | 20 | Femenino | Media | 4 | 2 | 3 | 3 | 4 | 4 | 4 | 2 | 4 | 2 | 65,0 |
| P09 | 21 | Masculino | Alta | 4 | 2 | 3 | 1 | 4 | 2 | 4 | 2 | 4 | 1 | 77,5 |
| P10 | 19 | Masculino | Media | 3 | 2 | 3 | 1 | 3 | 3 | 4 | 1 | 4 | 2 | 70,0 |

## Contexto de la medición

- Fecha: 30 de julio de 2026 (sesiones moderadas en el equipo de la evaluadora)
- Modo: cada participante operó la aplicación SGROAS y respondió el formulario SUS por sí mismo
- Aplicación accesada en `http://localhost:4200` (frontend) con backend en `http://localhost:8080`
- Matriz de datos crudos: `docs/mediciones/sus/sus-raw.csv`
- Interpretación: la media (63,0) corresponde a la calificación adjetiva *Bueno* de Bangor et al.
  (2009); se ubica en la zona de aceptabilidad *marginal* (50–70) y por debajo del umbral de 70
  que Bangor et al. asocian a sistemas aceptables, por lo que se documenta como área de mejora.
