#!/usr/bin/env python3
"""Detecta identificadores en espanol dentro de src/main/java y src/test/java.

Reescrito para el punto P5 de la auditoria externa: la version anterior
usaba un regex anclado con \\b al INICIO del identificador
(``^(extraer|...)\\b``), lo que nunca puede casar con un compuesto camelCase
como ``extraerEmail`` o ``mapearAResponse``: entre la 'r' de "extraer" y la
'E' de "Email" no hay ningun limite de \\b (ambos son caracteres de
palabra), asi que el regex nunca detectaba esos casos.

Esta version busca las raices como SUBCADENA en cualquier posicion del
identificador (no solo al inicio), lo cual si detecta camelCase compuesto,
y ademas cuenta tanto metodos como tipos (clases/interfaces/enums/records),
por separado en src/main y src/test y combinados, con umbral <=5%.

Exit code 0 = OK (todas las categorias <=5%), 1 = alguna categoria > 5%.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UMBRAL = 0.05

SPANISH_ROOTS = [
    "extraer", "listar", "crear", "actualizar", "eliminar", "desactivar", "activar",
    "obtener", "buscar", "guardar", "borrar", "editar", "registrar", "cambiar",
    "generar", "validar", "valido", "valida", "enviar", "resetear", "marcar",
    "verificar", "consultar", "devolver", "cargar", "descargar", "inyectar",
    "manejar", "mapear", "usuario", "conductor", "vehiculo", "ruta", "incidente",
    "asignacion", "clave", "correo", "telefono", "direccion", "estado", "nombre",
    "apellido", "cedula", "placa", "marca", "modelo", "anio", "capacidad",
    "codigo", "descripcion", "gravedad", "origen", "destino", "distancia",
    "duracion", "color", "motor", "chasis", "fecha", "contrasena",
    # Anadidos tras auditoria externa (2026-09-18): el lexico original se
    # escribio antes del modulo abd/ (Bases de Datos Avanzadas) y no cubria
    # sus nombres, lo que dejaba pasar identificadores reales en espanol
    # sin detectar (mapa, nivel, provincia, ciudad, rol, sesion, sugerido,
    # disco, numero -- confirmados presentes en nombres de metodo/tipo de
    # src/main/java, no solo en variables locales).
    "mapa", "nivel", "provincia", "ciudad", "rol", "sesion", "sugerido",
    "disco", "numero",
]

SPANISH_TEST_EXTRA = [
    "debe", "hora", "nivel", "tipo", "evidencia", "disco", "unidad",
    "programa", "alerta", "credencial", "intento", "rango", "filtro",
    "parametro", "mantenimiento", "autenticacion", "protegido", "respuesta",
    "encontrado", "inexistente", "nulo", "blanco", "duplicado", "inactivo",
    "correcto", "configurado", "genera", "devuelve", "mapea", "normaliza",
    "conserva", "desbloquea", "permite", "mantiene", "cambia", "borra",
    "activa", "correctamente", "sin", "con",
]

HAS_ACCENT = re.compile(r"[áéíóúñÁÉÍÓÚÑ]", re.UNICODE)

# Divide un identificador camelCase/PascalCase en sus palabras componentes,
# p.ej. "extraerEmail" -> ["extraer", "Email"], "findByEstadoIgnoreCase" ->
# ["find", "By", "Estado", "Ignore", "Case"]. Esto reemplaza el regex
# anclado con \b del checker anterior (que nunca podia casar un compuesto
# camelCase, porque \b no marca limite entre dos caracteres de palabra como
# 'r' y 'E'), sin caer en falsos positivos por sub-cadena cruda (p.ej. "con"
# dentro de "Config", o "genera" dentro de "generate").
_WORD_SPLIT_RE = re.compile(r"[A-Z]?[a-z0-9]+|[A-Z]+(?![a-z])")


def _split_camel_case(identifier):
    return [w.lower() for w in _WORD_SPLIT_RE.findall(identifier)]


MAIN_ROOTS = {r.lower() for r in SPANISH_ROOTS}
TEST_ROOTS = {r.lower() for r in SPANISH_ROOTS + SPANISH_TEST_EXTRA}


def _matches_roots(identifier, roots):
    return any(word in roots for word in _split_camel_case(identifier))


class _RootMatcher:
    def __init__(self, roots):
        self.roots = roots

    def search(self, identifier):
        return _matches_roots(identifier, self.roots)


MAIN_RE = _RootMatcher(MAIN_ROOTS)
TEST_RE = _RootMatcher(TEST_ROOTS)

# Excepciones: identificadores en ingles legitimo que contienen una raiz como
# subcadena por coincidencia (falsos positivos conocidos). Se comparan en
# minuscula, texto completo del identificador.
EXCEPCIONES = {
    "id", "ids",
}

METHOD_PATTERN = re.compile(
    r"^\s*(public|protected)?\s*((static|final|synchronized|abstract|default)\s+)*[\w<>, \.\?\[\]]+\s+(\w+)\s*\(",
    re.IGNORECASE,
)
_JAVA_KEYWORDS_NOT_METHODS = {
    "if", "for", "while", "switch", "catch", "return", "new", "synchronized",
    "else", "do", "throw", "assert",
}
TYPE_PATTERN = re.compile(
    r"^\s*(public|protected)?\s*(static\s+)?(final\s+)?(abstract\s+)?"
    r"(class|interface|enum|record)\s+(\w+)",
)
TEST_ANN_PATTERN = re.compile(r"^\s*@Test")
TEST_METHOD_PATTERN = re.compile(
    r"^\s*(public\s+)?(?:void|boolean|int|String|long|List\S*|ResponseEntity\S*|Map\S*|Optional\S*|[A-Z]\w*)\s+(\w+)\s*\("
)


def is_spanish(name, pattern):
    if name.lower() in EXCEPCIONES:
        return False
    if HAS_ACCENT.search(name):
        return True
    return bool(pattern.search(name))


def _walk_java(base_dir):
    for dirpath, _dirs, files in os.walk(base_dir):
        for fname in files:
            if fname.endswith(".java"):
                yield os.path.join(dirpath, fname)


def _scan_main(main_dir):
    methods_total, methods_bad = 0, []
    types_total, types_bad = 0, []
    for path in _walk_java(main_dir):
        with open(path, "r", encoding="latin-1") as fh:
            lines = fh.readlines()
        rel = os.path.relpath(path, ROOT)
        for i, line in enumerate(lines):
            t = TYPE_PATTERN.match(line)
            if t:
                # Una declaracion de record (p.ej. "public record Foo(Integer x)")
                # tambien casa con METHOD_PATTERN (el nombre del record queda
                # como si fuera un nombre de metodo, con la lista de
                # componentes como si fueran sus argumentos). Se cuenta UNA
                # sola vez, como tipo, nunca ademas como metodo -- si no, el
                # denominador de metodos queda inflado con nombres de tipos
                # duplicados y el porcentaje real de nombres en espanol queda
                # diluido de forma artificial.
                types_total += 1
                name = t.group(6)
                if is_spanish(name, MAIN_RE):
                    types_bad.append(f"{rel}:{i + 1}: {name}")
                continue
            m = METHOD_PATTERN.match(line)
            if m and m.group(4) not in _JAVA_KEYWORDS_NOT_METHODS:
                methods_total += 1
                name = m.group(4)
                if is_spanish(name, MAIN_RE):
                    methods_bad.append(f"{rel}:{i + 1}: {name}")
    return methods_total, methods_bad, types_total, types_bad


def _scan_test(test_dir):
    methods_total, methods_bad = 0, []
    types_total, types_bad = 0, []
    if not os.path.isdir(test_dir):
        return methods_total, methods_bad, types_total, types_bad
    for path in _walk_java(test_dir):
        with open(path, "r", encoding="latin-1") as fh:
            lines = fh.readlines()
        rel = os.path.relpath(path, ROOT)
        for i, line in enumerate(lines):
            t = TYPE_PATTERN.match(line)
            if t:
                types_total += 1
                name = t.group(6)
                if is_spanish(name, TEST_RE):
                    types_bad.append(f"{rel}:{i + 1}: {name}")
            if not TEST_ANN_PATTERN.match(line):
                continue
            for k in range(i, min(i + 6, len(lines))):
                m = TEST_METHOD_PATTERN.match(lines[k])
                if m:
                    methods_total += 1
                    name = m.group(2)
                    if is_spanish(name, TEST_RE):
                        methods_bad.append(f"{rel}:{i + 1}: {name}")
                    break
    return methods_total, methods_bad, types_total, types_bad


def _pct(bad, total):
    return (100.0 * len(bad) / total) if total else 0.0


def _report(label, total, bad):
    pct = _pct(bad, total)
    estado = "OK" if pct <= UMBRAL * 100 else "FAIL"
    print(f"[{estado}] {label}: {len(bad)}/{total} en espanol ({pct:.2f}%, umbral {UMBRAL*100:.0f}%)")
    for item in bad:
        print(f"    {item}")
    return pct <= UMBRAL * 100


def main():
    main_dir = os.path.join(ROOT, "src", "main", "java")
    test_dir = os.path.join(ROOT, "src", "test", "java")

    mm_total, mm_bad, mt_total, mt_bad = _scan_main(main_dir)
    tm_total, tm_bad, tt_total, tt_bad = _scan_test(test_dir)

    ok = True
    ok &= _report("Metodos en src/main", mm_total, mm_bad)
    ok &= _report("Tipos en src/main", mt_total, mt_bad)
    ok &= _report("Metodos en src/test", tm_total, tm_bad)
    ok &= _report("Tipos en src/test", tt_total, tt_bad)
    ok &= _report(
        "Metodos combinados (main+test)",
        mm_total + tm_total,
        mm_bad + tm_bad,
    )
    ok &= _report(
        "Tipos combinados (main+test)",
        mt_total + tt_total,
        mt_bad + tt_bad,
    )

    print()
    if ok:
        print("OK: todas las categorias por debajo del umbral del 5%")
        return 0
    print("FAIL: al menos una categoria supera el umbral del 5%")
    return 1


if __name__ == "__main__":
    sys.exit(main())
