"""Dibuja el icono del programa: un globo terráqueo con los colores del juego.

Genera empaquetado/mapamundi.ico (16–256 px, para el .exe) y los PNG de la ventana.
Uso: python -I herramientas/crear_icono.py
"""
import json
import math
import os

from PIL import Image, ImageDraw

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
DATOS = os.path.join(RAIZ, 'src', 'main', 'resources', 'com', 'mapamundi', 'datos', 'paises.json')
RECURSOS = os.path.join(RAIZ, 'src', 'main', 'resources', 'com', 'mapamundi')

UVA, MAR, TINTA, SOL = '#3a2f66', '#74bebc', '#1d2b4f', '#ffd23f'
PALETA = ['#f4a99b', '#f3d27a', '#a9d38c', '#eda7c9', '#c4b0e6', '#f6b878', '#d2bf94']
LON0, LAT0 = -45, 12  # centro del globo: América y la costa de África

N = 1024  # se dibuja grande y se reduce, para que los bordes queden suaves


def ortografica(lon, lat, r, cx, cy):
    l, p = math.radians(lon - LON0), math.radians(lat)
    p0 = math.radians(LAT0)
    cos_c = math.sin(p0) * math.sin(p) + math.cos(p0) * math.cos(p) * math.cos(l)
    x = math.cos(p) * math.sin(l)
    y = math.cos(p0) * math.sin(p) - math.sin(p0) * math.cos(p) * math.cos(l)
    return cx + r * x, cy - r * y, cos_c >= 0


def main():
    datos = json.load(open(DATOS, encoding='utf-8'))
    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, N - 1, N - 1), radius=N * 0.22, fill=UVA)

    cx = cy = N / 2
    r = N * 0.36
    # Sombra amarilla desplazada, como el logotipo
    d.ellipse((cx - r + N * 0.03, cy - r + N * 0.035, cx + r + N * 0.03, cy + r + N * 0.035), fill=SOL)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=MAR)

    globo = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    g = ImageDraw.Draw(globo)

    def anillo(coords, color):
        pts = [ortografica(coords[i], coords[i + 1], r, cx, cy) for i in range(0, len(coords), 2)]
        visibles = [(x, y) for x, y, v in pts if v]
        if len(visibles) < 3 or len(visibles) < len(pts) * 0.6:
            return
        g.polygon(visibles, fill=color, outline=TINTA, width=3)

    for otras in datos['otrasTierras']:
        for a in otras:
            anillo(a, '#e8e2d4')
    for p in datos['paises']:
        for a in p['anillos']:
            anillo(a, PALETA[(p['color'] - 1) % 7])

    # Recortar al círculo del globo
    mascara = Image.new('L', (N, N), 0)
    ImageDraw.Draw(mascara).ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
    img.paste(globo, (0, 0), Image.composite(globo, Image.new('RGBA', (N, N)), mascara).split()[3])
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=TINTA, width=int(N * 0.028))
    # Brillo
    d.arc((cx - r * 0.82, cy - r * 0.82, cx + r * 0.82, cy + r * 0.82), 200, 250, fill=(255, 255, 255, 170), width=int(N * 0.03))

    os.makedirs(os.path.join(RAIZ, 'empaquetado'), exist_ok=True)
    ico = os.path.join(RAIZ, 'empaquetado', 'mapamundi.ico')
    img.resize((256, 256), Image.LANCZOS).save(ico, sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    for tam in (32, 64, 256):
        nombre = 'icono.png' if tam == 64 else f'icono-{tam}.png'
        img.resize((tam, tam), Image.LANCZOS).save(os.path.join(RECURSOS, nombre))
    img.resize((512, 512), Image.LANCZOS).save(os.path.join(RAIZ, 'empaquetado', 'icono-512.png'))
    print('ok', ico)


if __name__ == '__main__':
    main()
