"""Genera src/main/resources/com/mapamundi/datos/paises.json para el juego en Java.

Combina:
  - Fronteras de Natural Earth 1:50m (dominio público): herramientas/fuentes/ne_50m_admin_0_countries.geojson
  - Datos del Mapamundi original (2000-2003): paises.DBF/FPT (idioma, religión, moneda, exportaciones…)

Uso: python -I herramientas/construir_datos.py "D:\\Fuente Globalizacion\\mapamundi\\paises.DBF"
"""
import json, math, os, sys, unicodedata

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from lector_dbf import read_dbf  # noqa: E402

PAISES_NE = os.path.join(AQUI, 'fuentes', 'ne_50m_admin_0_countries.geojson')
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'com', 'mapamundi', 'datos', 'paises.json')

# Territorios que se dibujan como tierra pero no se juegan.
NO_JUGABLES = {'Western Sahara'}
# Regiones separatistas que se funden con el país al que pertenecen.
FUSIONAR = {'Somaliland': 'Somalia', 'Northern Cyprus': 'Cyprus'}
# Sin soberanía propia en Natural Earth, pero se juegan.
JUGABLES_EXTRA = {'Palestine', 'Taiwan', 'Kosovo'}

CONTINENTES = {'Africa': 'AFRICA', 'Europe': 'EUROPA', 'Asia': 'ASIA', 'Oceania': 'OCEANIA',
               'North America': 'AMERICA', 'South America': 'AMERICA'}
MAR_ABIERTO = {'Maldives': 'ASIA', 'Mauritius': 'AFRICA', 'Seychelles': 'AFRICA'}

# Nombres a mostrar cuando el de Natural Earth no es el habitual en español.
NOMBRES = {'Republic of China': 'Taiwán', 'Taiwan': 'Taiwán', 'eSwatini': 'Esuatini', 'Myanmar': 'Myanmar (Birmania)',
           'Czechia': 'Chequia', 'Timor-Leste': 'Timor Oriental'}

# Nombre en el juego original -> nombre de Natural Earth (NAME_ES).
ALIAS_ORIGINAL = {
    'Botswana': 'Botsuana', 'Congo': 'República del Congo', 'Zaire': 'República Democrática del Congo',
    'Djibuti': 'Yibuti', 'Guinea Bissau': 'Guinea-Bisáu', 'Kenya': 'Kenia', 'Lesotho': 'Lesoto', 'Malawi': 'Malaui',
    'Tunicia': 'Túnez', 'Suriname': 'Surinam', 'Bosnia-Herzegovina': 'Bosnia y Herzegovina',
    'Macedonia': 'Macedonia del Norte', 'Rusia (Europea)': 'Rusia', 'Arabia Saudí': 'Arabia Saudita',
    'Bahrein': 'Baréin', 'Bangladesh': 'Bangladés', 'China Popular': 'China', 'Kazajstán': 'Kazajistán',
    'Kirguizistan': 'Kirguistán', 'Malaysia': 'Malasia', 'Myanmar': 'Birmania', 'Qatar': 'Catar',
    'República de Corea': 'Corea del Sur', 'República Democrática Popular Laos': 'Laos',
    'República Popular Democrática Corea': 'Corea del Norte', 'Taiwan': 'República de China', 'Fiji': 'Fiyi',
    'Marshall': 'Islas Marshall', 'Micronesia': 'Estados Federados de Micronesia', 'Palau': 'Palaos',
    'Papúa - Nueva Guinea': 'Papúa Nueva Guinea', 'Salomon': 'Islas Salomón', 'Samoa Occidental': 'Samoa',
    'Guinea Continental': 'Guinea',
}
# Un registro original que sirve para dos países actuales.
COMPARTIDOS = {'Serbia y Montenegro': ['Serbia', 'Montenegro']}

# Capitales para los países nuevos o cuya capital cambió desde 2003.
CAPITALES = {
    'Kazajistán': 'Astaná', 'Birmania': 'Naipyidó', 'Sudán del Sur': 'Yuba', 'Montenegro': 'Podgorica',
    'Serbia': 'Belgrado', 'Kosovo': 'Pristina', 'Eritrea': 'Asmara', 'Timor Oriental': 'Dili',
    'Palestina': 'Ramala', 'Nigeria': 'Abuya', 'Costa de Marfil': 'Yamusukro', 'Tanzania': 'Dodoma',
    'Sri Lanka': 'Sri Jayawardenapura Kotte', 'Palaos': 'Ngerulmud', 'Bolivia': 'Sucre (constitucional) y La Paz (sede de gobierno)',
    'Esuatini': 'Mbabane', 'Nauru': 'Yaren', 'Tuvalu': 'Funafuti', 'Kiribati': 'Tarawa del Sur',
    'Islas Marshall': 'Majuro', 'Estados Federados de Micronesia': 'Palikir', 'Brunéi': 'Bandar Seri Begawan',
    'Andorra': 'Andorra la Vieja', 'Liechtenstein': 'Vaduz', 'Mónaco': 'Mónaco', 'San Marino': 'San Marino',
    'Ciudad del Vaticano': 'Ciudad del Vaticano', 'Malta': 'La Valeta', 'Comoras': 'Moroni', 'Cabo Verde': 'Praia',
    'Santo Tomé y Príncipe': 'Santo Tomé', 'Mauricio': 'Port Louis', 'Seychelles': 'Victoria', 'Maldivas': 'Malé',
    'Singapur': 'Singapur', 'Baréin': 'Manama', 'Bahamas': 'Nasáu', 'Barbados': 'Bridgetown', 'Dominica': 'Roseau',
    'Granada': "Saint George's", 'Santa Lucía': 'Castries', 'San Vicente y las Granadinas': 'Kingstown',
    'San Cristóbal y Nieves': 'Basseterre', 'Antigua y Barbuda': "Saint John's", 'Trinidad y Tobago': 'Puerto España',
    'Jamaica': 'Kingston', 'Tonga': "Nukualofa", 'Samoa': 'Apia', 'Vanuatu': 'Port Vila', 'Fiyi': 'Suva',
    'Islas Salomón': 'Honiara', 'Papúa Nueva Guinea': 'Port Moresby', 'Belice': 'Belmopán', 'Lesoto': 'Maseru',
    'Botsuana': 'Gaborone', 'Malaui': 'Lilongüe', 'Yibuti': 'Yibuti', 'Guinea Ecuatorial': 'Malabo',
    'Guinea': 'Conakri', 'Luxemburgo': 'Luxemburgo', 'Haití': 'Puerto Príncipe', 'Cuba': 'La Habana',
    'República Dominicana': 'Santo Domingo',
}


def norm(s):
    return ''.join(c for c in unicodedata.normalize('NFD', s.lower()) if unicodedata.category(c) != 'Mn').strip()


def simplificar(puntos, tol):
    """Douglas-Peucker sobre una lista de (lon, lat)."""
    if len(puntos) < 5:
        return puntos
    def dist(p, a, b):
        if a == b:
            return math.hypot(p[0] - a[0], p[1] - a[1])
        dx, dy = b[0] - a[0], b[1] - a[1]
        t = max(0, min(1, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / (dx * dx + dy * dy)))
        return math.hypot(p[0] - a[0] - t * dx, p[1] - a[1] - t * dy)
    keep = [False] * len(puntos)
    keep[0] = keep[-1] = True
    pila = [(0, len(puntos) - 1)]
    while pila:
        i, j = pila.pop()
        mejor, idx = 0, -1
        for k in range(i + 1, j):
            d = dist(puntos[k], puntos[i], puntos[j])
            if d > mejor:
                mejor, idx = d, k
        if mejor > tol:
            keep[idx] = True
            pila += [(i, idx), (idx, j)]
    return [p for p, k in zip(puntos, keep) if k]


def anillos(geom, tol=0.02):
    polys = geom['coordinates'] if geom['type'] == 'MultiPolygon' else [geom['coordinates']]
    salida = []
    for poly in polys:
        for ring in poly:
            pts = [(round(x, 3), round(y, 3)) for x, y in ring]
            simple = simplificar(pts, tol)
            if len(simple) < 4:  # islas diminutas: se conservan tal cual
                simple = pts
            plano = []
            for x, y in simple:
                plano += [x, y]
            salida.append(plano)
    return salida


def area_km2(rings_planos):
    """Área geodésica aproximada (fórmula esférica), con signo por anillo para restar huecos."""
    R = 6371.0088
    total = 0.0
    for r in rings_planos:
        pts = list(zip(r[0::2], r[1::2]))
        s = 0.0
        for (x1, y1), (x2, y2) in zip(pts, pts[1:] + pts[:1]):
            s += math.radians(x2 - x1) * (2 + math.sin(math.radians(y1)) + math.sin(math.radians(y2)))
        total += s * R * R / 2
    return abs(total)


def miles(n):
    return f'{int(round(n)):,}'.replace(',', '.')


def main():
    ne = json.load(open(PAISES_NE, encoding='utf-8'))['features']
    _, originales = read_dbf(sys.argv[1])
    por_nombre = {}
    for r in originales:
        if r['_deleted']:
            continue
        nombre = r['NOMBRE'].strip()
        for destino in COMPARTIDOS.get(nombre, [ALIAS_ORIGINAL.get(nombre, nombre)]):
            por_nombre.setdefault(norm(destino), r)

    fusiones = {}
    for f in ne:
        p = f['properties']
        if p['ADMIN'] in FUSIONAR:
            fusiones.setdefault(FUSIONAR[p['ADMIN']], []).append(f['geometry'])

    paises, otras, sin_datos = [], [], []
    for f in ne:
        p = f['properties']
        admin = p['ADMIN']
        if p['CONTINENT'] == 'Antarctica' or admin in FUSIONAR:
            continue
        geoms = [f['geometry']] + fusiones.get(admin, [])
        rings = [r for g in geoms for r in anillos(g)]
        jugable = (admin == p['SOVEREIGNT'] or admin in JUGABLES_EXTRA) and admin not in NO_JUGABLES
        if not jugable:
            otras.append(rings)
            continue

        nombre_ne = p['NAME_ES']
        nombre = NOMBRES.get(p['NAME_EN'], NOMBRES.get(nombre_ne, nombre_ne))
        original = por_nombre.get(norm(nombre_ne))
        continente = MAR_ABIERTO.get(admin) or CONTINENTES[p['CONTINENT']]
        capital = CAPITALES.get(nombre_ne) or (original['CAPITAL'].strip() if original else '')
        if not capital:
            sin_datos.append(nombre)
        superficie = original['SUPERFICIE'].strip() if original and original['SUPERFICIE'].strip() else f'≈ {miles(area_km2(rings))} km2'
        texto = lambda campo: original[campo].strip() if original else ''
        paises.append({
            'iso': p['ISO_A2_EH'] if p['ISO_A2_EH'] != '-99' else p['ADM0_A3'],
            'nombre': nombre,
            'continente': continente,
            'sudamerica': p['CONTINENT'] == 'South America',
            'capital': capital,
            'poblacion': f"{miles(p['POP_EST'])} habs. ({p['POP_YEAR']})",
            'superficie': superficie.replace('km2', 'km²'),
            'idioma': texto('IDIOMA'),
            'religion': texto('RELIGION'),
            'moneda': texto('MONEDA'),
            'exporta': texto('EXPORTA'),
            'datosOriginales': original is not None,
            'color': p['MAPCOLOR7'],
            'anillos': rings,
        })

    paises.sort(key=lambda x: norm(x['nombre']))
    os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
    with open(SALIDA, 'w', encoding='utf-8') as fh:
        json.dump({'fuente': 'Natural Earth 1:50m (dominio público) + Mapamundi 2000', 'paises': paises, 'otrasTierras': otras},
                  fh, ensure_ascii=False, separators=(',', ':'))

    usados = {id(r) for r in por_nombre.values()}
    print(f'{len(paises)} países jugables, {len(otras)} territorios de fondo')
    print(f"{sum(p['datosOriginales'] for p in paises)} con datos del juego original")
    for c in sorted({p['continente'] for p in paises}):
        print(f"  {c}: {sum(1 for p in paises if p['continente'] == c)}")
    print('Sin capital:', sin_datos)
    asignados = {id(por_nombre[k]) for k in por_nombre if any(norm(x['nombre']) == k for x in paises)}
    print('Originales sin usar:', [r['NOMBRE'] for r in originales if not r['_deleted'] and id(r) not in usados | asignados])
    print(f'Tamaño: {os.path.getsize(SALIDA) / 1e6:.2f} MB')


if __name__ == '__main__':
    main()
