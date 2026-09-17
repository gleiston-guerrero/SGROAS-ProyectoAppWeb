#!/usr/bin/env python3
"""Regenera los diagramas C4 de nivel 1 y 2 (P7 fix, 2026-09-17).

Las figuras C4 (docs/arquitectura/c4-nivel1-contexto.png y
c4-nivel2-contenedores.png, copiadas tambien a docs/informe-final/
c4-level1-context.png y c4-level2-containers.png) decian "Vue.js Frontend"
/ "[Container: Vue.js 3]" -- el frontend real es Angular 20
(frontend/package.json: "@angular/core": "^20.3.0"). No hay Structurizr
(la herramienta original que genero estos PNG, ver los .dsl en
docs/arquitectura/) disponible en el entorno de esta auditoria, asi que
este script recrea el mismo layout con matplotlib, leyendo el texto
correcto directamente de los .dsl (fuente de verdad) en vez de repetirlo
hardcodeado aqui.

Uso: python3 scripts/gen-c4-diagramas.py
"""
import os
import re
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ARCH = os.path.join(ROOT, "docs", "arquitectura")
INFORME = os.path.join(ROOT, "docs", "informe-final")

GRAY = "#444444"
BOXGRAY = "#666666"


def _frontend_label():
    """Lee el nombre y version del contenedor frontend directamente del
    .dsl (fuente de verdad), para no repetir el nombre de la tecnologia
    hardcodeado en este script."""
    dsl = open(os.path.join(ARCH, "c4-nivel2-contenedores.dsl"), encoding="utf-8").read()
    m = re.search(r'frontend = container "([^"]+)" "([^"]+)" "([^"]+)"', dsl)
    if not m:
        raise RuntimeError("No se pudo leer el container 'frontend' de c4-nivel2-contenedores.dsl")
    name, desc, tech = m.groups()
    return name, desc, tech


def box(ax, x, y, w, h, title, subtitle, body, title_fs=15, sub_fs=10, body_fs=13):
    rect = mpatches.FancyBboxPatch(
        (x, y), w, h, boxstyle="round,pad=0,rounding_size=0.01",
        linewidth=1.4, edgecolor=BOXGRAY, facecolor="white"
    )
    ax.add_patch(rect)
    ax.text(x + w / 2, y + h * 0.86, title, ha="center", va="center",
             fontsize=title_fs, fontweight="bold", color="#222222")
    ax.text(x + w / 2, y + h * 0.68, subtitle, ha="center", va="center",
             fontsize=sub_fs, color="#333333")
    ax.text(x + w / 2, y + h * 0.32, body, ha="center", va="center",
             fontsize=body_fs, color="#222222")


def arrow(ax, x1, y1, x2, y2, label, sublabel=""):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="-|>", linestyle=(0, (4, 3)), color=GRAY, lw=1.3))
    mx, my = (x1 + x2) / 2, (y1 + y2) / 2
    ax.text(mx + 0.01, my + 0.01, label, ha="left", va="bottom", fontsize=11, color="#222222")
    if sublabel:
        ax.text(mx + 0.01, my - 0.025, sublabel, ha="left", va="top", fontsize=9, color="#333333")


def gen_level1(out_paths):
    _, _, tech = _frontend_label()
    fig, ax = plt.subplots(figsize=(5.0, 5.3))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")

    ax.text(0.5, 0.99, "System Context View: SGROAS API", ha="center", va="top",
            fontsize=14, fontweight="bold", color="#222222")
    ax.text(0.5, 0.955, "Level 1 - System Context Diagram", ha="center", va="top",
            fontsize=14, fontweight="bold", color="#222222")

    box(ax, 0.06, 0.62, 0.88, 0.27, "User", "[Person]",
        "System operator (admin,\ncoordinator, security)", body_fs=13)
    box(ax, 0.06, 0.06, 0.88, 0.27, "SGROAS API", "[Software System]",
        "Management platform for routes,\ndrivers, vehicles and incidents", body_fs=13)

    arrow(ax, 0.5, 0.62, 0.5, 0.33, "Uses the platform via", f"[REST API / {tech.split()[0]} frontend]")

    plt.tight_layout()
    for p in out_paths:
        plt.savefig(p, dpi=150, facecolor="white")
    plt.close(fig)


def gen_level2(out_paths):
    name, desc, tech = _frontend_label()
    fig, ax = plt.subplots(figsize=(9.5, 10.2))
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1.10)
    ax.axis("off")

    ax.text(0.5, 1.085, "Container View: SGROAS", ha="center", va="top",
            fontsize=16, fontweight="bold", color="#222222")
    ax.text(0.5, 1.045, "Level 2 - Container Diagram", ha="center", va="top",
            fontsize=16, fontweight="bold", color="#222222")

    box(ax, 0.32, 0.90, 0.36, 0.115, "User", "[Person]", "System operator", body_fs=12)

    outer = mpatches.FancyBboxPatch(
        (0.02, 0.03), 0.96, 0.82, boxstyle="round,pad=0,rounding_size=0.005",
        linewidth=1.4, edgecolor=BOXGRAY, facecolor="none"
    )
    ax.add_patch(outer)
    ax.text(0.5, 0.815, "SGROAS", ha="center", va="top", fontsize=14, fontweight="bold", color="#222222")
    ax.text(0.5, 0.788, "[Software System]", ha="center", va="top", fontsize=10, color="#333333")

    box(ax, 0.28, 0.615, 0.44, 0.135, name, f"[Container: {tech}]", desc, body_fs=12)
    box(ax, 0.20, 0.395, 0.60, 0.150, "Spring Boot REST API", "[Container: Spring Boot 3.5 + Java 21]", "Java backend", body_fs=12)
    box(ax, 0.05, 0.09, 0.27, 0.170, "PostgreSQL Database", "[Container: PostgreSQL 16]", "Primary\nstorage", body_fs=11)
    box(ax, 0.37, 0.09, 0.24, 0.170, "Redis", "[Container: Redis 7]", "Distributed\ncache", body_fs=11)
    box(ax, 0.66, 0.09, 0.29, 0.170, "JWT Service", "[Container: jjwt 0.12.6]", "Authentication\nand authorization", body_fs=11)

    arrow(ax, 0.5, 0.90, 0.5, 0.755, "Browses")
    arrow(ax, 0.5, 0.615, 0.5, 0.548, "HTTP requests", "[JSON]")
    arrow(ax, 0.36, 0.395, 0.22, 0.263, "Read/Write", "[SQL]")
    arrow(ax, 0.49, 0.395, 0.49, 0.263, "Query cache", "[Redis Protocol]")
    arrow(ax, 0.63, 0.395, 0.79, 0.263, "Token validation")

    plt.tight_layout()
    for p in out_paths:
        plt.savefig(p, dpi=150, facecolor="white")
    plt.close(fig)


if __name__ == "__main__":
    gen_level1([
        os.path.join(ARCH, "c4-nivel1-contexto.png"),
        os.path.join(INFORME, "c4-level1-context.png"),
    ])
    gen_level2([
        os.path.join(ARCH, "c4-nivel2-contenedores.png"),
        os.path.join(INFORME, "c4-level2-containers.png"),
    ])
    print("done")
