"""Descarga de Wikidata (licencia CC0) los datos actuales de los países del juego.

Genera herramientas/datos_wikidata.json, que construir_datos.py combina con las fronteras.
Datos: capital, población, superficie, idiomas oficiales, moneda y PIB per cápita
(calculado con el PIB nominal y la población, porque el per cápita casi no está cargado).

Uso: python -I herramientas/actualizar_datos.py
"""
import datetime
import json
import os
import re
import time
import urllib.parse
import urllib.request

AQUI = os.path.dirname(os.path.abspath(__file__))
PAISES = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'com', 'mapamundi', 'datos', 'paises.json')
SALIDA = os.path.join(AQUI, 'datos_wikidata.json')
URL = 'https://query.wikidata.org/sparql'
AGENTE = 'Mapamundi/2.0 (juego educativo; https://github.com/guess-samuel2401/mapamundi)'
USD = 'http://www.wikidata.org/entity/Q4917'

ETIQUETAS = 'SERVICE wikibase:label { bd:serviceParam wikibase:language "es,en". }'
NO_OBSOLETO = '?st wikibase:rank ?rango . FILTER(?rango != wikibase:DeprecatedRank)'

# Una consulta por dato: así no se multiplican las filas.
CONSULTAS = {
    'capital': f'SELECT ?iso ?vLabel WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p wdt:P36 ?v . {ETIQUETAS} }}',
    'moneda': f'SELECT ?iso ?vLabel WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p wdt:P38 ?v . {ETIQUETAS} }}',
    'idioma': f'SELECT ?iso ?vLabel WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p wdt:P37 ?v . {ETIQUETAS} }}',
    'poblacion': f'SELECT ?iso ?v ?fecha ?rango WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p p:P1082 ?st . '
                 f'?st ps:P1082 ?v . OPTIONAL {{ ?st pq:P585 ?fecha }} {NO_OBSOLETO} }}',
    'superficie': f'SELECT ?iso ?v ?rango WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p p:P2046 ?st . '
                  f'?st psn:P2046/wikibase:quantityAmount ?v . {NO_OBSOLETO} }}',
    # PIB nominal total (P2131): el per cápita (P2132) casi no está cargado, así que se calcula con la población.
    'pib': f'SELECT ?iso ?v ?unidad ?fecha ?rango WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p p:P2131 ?st . '
           f'?st psv:P2131 ?q . ?q wikibase:quantityAmount ?v ; wikibase:quantityUnit ?unidad . '
           f'OPTIONAL {{ ?st pq:P585 ?fecha }} {NO_OBSOLETO} }}',
}
API = 'https://www.wikidata.org/w/api.php'
SIN_ETIQUETA = re.compile(r'^Q\d+$')
# Entidades cuyo nombre no devuelve Wikidata en español ni en inglés.
NOMBRES_CONOCIDOS = {'Q4916': 'euro'}


def consultar(sparql):
    datos = urllib.parse.urlencode({'query': sparql, 'format': 'json'}).encode()
    peticion = urllib.request.Request(URL, data=datos, headers={'User-Agent': AGENTE, 'Accept': 'application/sparql-results+json'})
    for intento in range(4):
        try:
            with urllib.request.urlopen(peticion, timeout=120) as r:
                return json.load(r)['results']['bindings']
        except Exception as e:  # Wikidata a veces responde 429/503: esperar y reintentar
            print(f'   reintento {intento + 1}: {e}')
            time.sleep(5 * (intento + 1))
    raise RuntimeError('Wikidata no respondió')


def valor(fila, clave):
    return fila[clave]['value'] if clave in fila else None


def etiquetas_que_faltan(filas):
    """El servicio de etiquetas a veces devuelve el identificador (p. ej. 'Q4916' para el euro): se piden a la API."""
    ids = sorted({valor(f, 'vLabel') for lista in filas.values() for f in lista
                  if valor(f, 'vLabel') and SIN_ETIQUETA.match(valor(f, 'vLabel'))})
    nombres = {q: n for q, n in NOMBRES_CONOCIDOS.items() if q in ids}
    for i in range(0, len(ids), 50):
        consulta = urllib.parse.urlencode({'action': 'wbgetentities', 'ids': '|'.join(ids[i:i + 50]),
                                            'props': 'labels', 'languages': 'es|en', 'format': 'json'})
        peticion = urllib.request.Request(f'{API}?{consulta}', headers={'User-Agent': AGENTE})
        with urllib.request.urlopen(peticion, timeout=60) as r:
            for qid, entidad in json.load(r).get('entities', {}).items():
                etiquetas = entidad.get('labels', {})
                elegida = etiquetas.get('es') or etiquetas.get('en')
                if elegida:
                    nombres[qid] = elegida['value']
    for lista in filas.values():
        for f in lista:
            v = valor(f, 'vLabel')
            if v in nombres:
                f['vLabel']['value'] = nombres[v]
    return len(ids), len(nombres)


def mas_reciente(filas, preferir_rango=True):
    """Entre varias declaraciones, la de fecha más reciente (o la de rango preferido si no hay fechas)."""
    def orden(f):
        preferido = 1 if valor(f, 'rango', ) and valor(f, 'rango').endswith('PreferredRank') else 0
        return (valor(f, 'fecha') or '', preferido if preferir_rango else 0)
    return max(filas, key=orden)


def miles(n):
    return f'{int(round(n)):,}'.replace(',', '.')


def unir(valores):
    vistos = []
    for v in valores:
        if v and v not in vistos and not SIN_ETIQUETA.match(v):  # Q123 = sin nombre conocido
            vistos.append(v)
    if len(vistos) <= 1:
        return vistos[0] if vistos else ''
    return ', '.join(vistos[:-1]) + ' y ' + vistos[-1]


def mayuscula(texto):
    return texto[:1].upper() + texto[1:] if texto else texto


def main():
    paises = json.load(open(PAISES, encoding='utf-8'))['paises']
    isos = ' '.join(f'"{p["iso"]}"' for p in paises)
    filas = {}
    for dato, plantilla in CONSULTAS.items():
        print(f'Consultando {dato}…')
        filas[dato] = consultar(plantilla % isos)
        time.sleep(1)
    buscadas, encontradas = etiquetas_que_faltan(filas)
    print(f'Nombres recuperados con la API: {encontradas} de {buscadas}')

    resultado = {}
    for p in paises:
        iso = p['iso']
        de = {dato: [f for f in filas[dato] if valor(f, 'iso') == iso] for dato in CONSULTAS}
        r = {}
        r['capital'] = unir(valor(f, 'vLabel') for f in de['capital'])
        r['moneda'] = unir(mayuscula(valor(f, 'vLabel')) for f in de['moneda'])
        r['idioma'] = unir(mayuscula(valor(f, 'vLabel')) for f in de['idioma'])
        habitantes = None
        if de['poblacion']:
            f = mas_reciente(de['poblacion'])
            habitantes = float(valor(f, 'v'))
            anio = (valor(f, 'fecha') or '')[:4]
            r['poblacion'] = f'{miles(habitantes)} habs.' + (f' ({anio})' if anio else '')
        if de['superficie']:
            # Puede haber varias cifras (con o sin islas, aguas…): construir_datos.py elige la más
            # parecida al área de las fronteras del mapa.
            r['superficies'] = sorted({round(float(valor(f, 'v')) / 1e6, 2) for f in de['superficie']})
        usd = [f for f in de['pib'] if valor(f, 'unidad') == USD]
        if usd and habitantes:
            f = mas_reciente(usd)
            anio = (valor(f, 'fecha') or '')[:4]
            por_persona = float(valor(f, 'v')) / habitantes
            r['pibPerCapita'] = f'aprox. {miles(round(por_persona, -1))} USD' + (f' ({anio})' if anio else '')
        resultado[iso] = {k: v for k, v in r.items() if v}

    salida = {'fuente': 'Wikidata (CC0)', 'consultado': datetime.date.today().isoformat(), 'paises': resultado}
    with open(SALIDA, 'w', encoding='utf-8') as fh:
        json.dump(salida, fh, ensure_ascii=False, indent=1, sort_keys=True)

    faltan = {campo: [p['nombre'] for p in paises if campo not in resultado[p['iso']]]
              for campo in ('capital', 'poblacion', 'superficies', 'idioma', 'moneda', 'pibPerCapita')}
    print(f'Listo: {len(resultado)} países -> {SALIDA}')
    for campo, nombres in faltan.items():
        print(f'  sin {campo}: {len(nombres)} {nombres[:8]}')


if __name__ == '__main__':
    main()
