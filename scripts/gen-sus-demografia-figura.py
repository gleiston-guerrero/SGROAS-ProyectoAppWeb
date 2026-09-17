#!/usr/bin/env python3
"""Regenerates dataset/sus/fig-sus-demografia.png in English (P7 fix).
No generator script for this figure exists in the repo (orphan file, added
directly as a binary in commit 7e49da9); this recreates it from the same
source data (dataset/sus/sus-raw.csv) with English labels, matching the
original 3-panel layout (gender bar chart, web-experience bar chart, age vs
SUS score scatter).
"""
import csv
import os
import sys
import matplotlib.pyplot as plt

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(ROOT, "dataset", "sus", "sus-raw.csv")
DEFAULT_OUT = os.path.join(ROOT, "dataset", "sus", "fig-sus-demografia.png")

ROWS = list(csv.DictReader(open(CSV_PATH, encoding="utf-8")))

GENDER_MAP = {"Masculino": "Male", "Femenino": "Female"}
EXP_MAP = {"Baja": "Low", "Media": "Medium", "Alta": "High"}
EXP_ORDER = ["Low", "Medium", "High"]

genders = [GENDER_MAP[r["sexo"]] for r in ROWS]
n_male = genders.count("Male")
n_female = genders.count("Female")

exps = [EXP_MAP[r["experiencia_web"]] for r in ROWS]
exp_counts = {e: exps.count(e) for e in EXP_ORDER}

ages = [int(r["edad"]) for r in ROWS]
scores = [float(r["sus_score"]) for r in ROWS]
colors = ["#4c72b0" if g == "Male" else "#c44e52" for g in genders]

fig, axes = plt.subplots(1, 3, figsize=(15, 4.2))
fig.suptitle("SUS participant demographics (P01-P15)", fontsize=13)

ax = axes[0]
bars = ax.bar(["Male", "Female"], [n_male, n_female], color=["#4c72b0", "#c44e52"])
ax.set_title(f"Gender ({n_male}M / {n_female}F)")
for b in bars:
    ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 0.1, str(int(b.get_height())),
            ha="center", va="bottom")
ax.set_ylim(0, 11)

ax = axes[1]
vals = [exp_counts[e] for e in EXP_ORDER]
bars = ax.bar(EXP_ORDER, vals, color="#55a868")
ax.set_title(f"Web experience (L{exp_counts['Low']} / M{exp_counts['Medium']} / H{exp_counts['High']})")
for b in bars:
    ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 0.1, str(int(b.get_height())),
            ha="center", va="bottom")
ax.set_ylim(0, 12)

ax = axes[2]
for g, color, label in [("Male", "#4c72b0", "Male"), ("Female", "#c44e52", "Female")]:
    xs = [a for a, gg in zip(ages, genders) if gg == g]
    ys = [s for s, gg in zip(scores, genders) if gg == g]
    ax.scatter(xs, ys, color=color, label=label)
ax.set_title("Age vs SUS")
ax.set_xlabel("Age")
ax.set_ylabel("SUS")
ax.legend()

plt.tight_layout()
plt.savefig(sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT, dpi=150)
print("done")
