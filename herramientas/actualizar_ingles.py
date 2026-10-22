"""Descarga de Wikidata (CC0) el nombre en inglés de la capital, la moneda y los idiomas oficiales de cada país.

Genera herramientas/datos_wikidata_en.json, que construir_datos.py usa para la versión en inglés del juego.
No toca los datos en español (esos los descarga actualizar_datos.py).

Uso: python herramientas/actualizar_ingles.py
"""
import datetime
import json
import os
import time

from actualizar_datos import AQUI, PAISES, SIN_ETIQUETA, consultar, mayuscula, valor

SALIDA = os.path.join(AQUI, 'datos_wikidata_en.json')
# Entidades que el servicio de etiquetas devuelve sin nombre.
NOMBRES_CONOCIDOS = {'Q4916': 'Euro'}
ETIQUETAS = 'SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }'
CONSULTAS = {
    'capital': f'SELECT ?iso ?vLabel WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p wdt:P36 ?v . {ETIQUETAS} }}',
    'moneda': f'SELECT ?iso ?vLabel WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p wdt:P38 ?v . {ETIQUETAS} }}',
    'idioma': f'SELECT ?iso ?vLabel WHERE {{ VALUES ?iso {{ %s }} ?p wdt:P297 ?iso . ?p wdt:P37 ?v . {ETIQUETAS} }}',
}


def unir(valores):
    vistos = []
    for v in valores:
        v = NOMBRES_CONOCIDOS.get(v, v)
        if v and v not in vistos and not SIN_ETIQUETA.match(v):
            vistos.append(v)
    if len(vistos) <= 1:
        return vistos[0] if vistos else ''
    return ', '.join(vistos[:-1]) + ' and ' + vistos[-1]


def main():
    paises = json.load(open(PAISES, encoding='utf-8'))['paises']
    isos = ' '.join(f'"{p["iso"]}"' for p in paises)
    filas = {}
    for dato, plantilla in CONSULTAS.items():
        print(f'Consultando {dato}…')
        filas[dato] = consultar(plantilla % isos)
        time.sleep(1)
    resultado = {}
    for p in paises:
        de = {dato: [f for f in filas[dato] if valor(f, 'iso') == p['iso']] for dato in CONSULTAS}
        r = {dato: unir(mayuscula(valor(f, 'vLabel')) for f in de[dato]) for dato in CONSULTAS}
        resultado[p['iso']] = {k: v for k, v in r.items() if v}
    salida = {'fuente': 'Wikidata (CC0)', 'consultado': datetime.date.today().isoformat(), 'paises': resultado}
    with open(SALIDA, 'w', encoding='utf-8') as fh:
        json.dump(salida, fh, ensure_ascii=False, indent=1, sort_keys=True)
    for campo in CONSULTAS:
        faltan = [p['iso'] for p in paises if campo not in resultado[p['iso']]]
        print(f'  sin {campo}: {len(faltan)} {faltan[:10]}')


if __name__ == '__main__':
    main()
