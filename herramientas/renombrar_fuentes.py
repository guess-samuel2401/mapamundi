"""Corrige los nombres internos de las fuentes descargadas de Fontsource para que JavaFX las encuentre.

Los TTF estáticos de Nunito vienen instanciados de la fuente variable con familias como
"Nunito ExtraLight SemiBold". JavaFX solo combina Regular/Bold dentro de una familia, así que:
  Nunito-Regular / Nunito-Bold  -> familia "Nunito", estilos Regular / Bold
  Nunito-SemiBold               -> familia "Nunito SemiBold"
  Nunito-ExtraBold              -> familia "Nunito ExtraBold"
  Baloo2-Bold                   -> familia "Baloo 2" (Bold)
  Baloo2-ExtraBold              -> familia "Baloo 2 ExtraBold"
La licencia OFL permite modificar las fuentes; los nombres reservados no cambian.

Uso: python -I herramientas/renombrar_fuentes.py src/main/resources/com/mapamundi/fuentes
"""
import os
import sys

from fontTools.ttLib import TTFont

NOMBRES = {
    'Nunito-Regular.ttf': ('Nunito', 'Regular'),
    'Nunito-Bold.ttf': ('Nunito', 'Bold'),
    'Nunito-SemiBold.ttf': ('Nunito SemiBold', 'Regular'),
    'Nunito-ExtraBold.ttf': ('Nunito ExtraBold', 'Regular'),
    'Baloo2-Bold.ttf': ('Baloo 2', 'Bold'),
    'Baloo2-ExtraBold.ttf': ('Baloo 2 ExtraBold', 'Regular'),
}


def main():
    carpeta = sys.argv[1]
    for archivo, (familia, estilo) in NOMBRES.items():
        ruta = os.path.join(carpeta, archivo)
        fuente = TTFont(ruta)
        tabla = fuente['name']
        completo = familia if estilo == 'Regular' else f'{familia} {estilo}'
        postscript = completo.replace(' ', '')
        for registro in list(tabla.names):
            # Quitar nombres tipográficos preferentes (16/17), que confunden la agrupación de familias.
            if registro.nameID in (16, 17, 21, 22):
                tabla.removeNames(nameID=registro.nameID)
        tabla.setName(familia, 1, 3, 1, 0x409)
        tabla.setName(estilo, 2, 3, 1, 0x409)
        tabla.setName(completo, 4, 3, 1, 0x409)
        tabla.setName(postscript, 6, 3, 1, 0x409)
        # Bit de negrita coherente con el estilo, para que -fx-font-weight: bold elija el archivo Bold.
        negrita = estilo == 'Bold'
        fuente['OS/2'].fsSelection = (fuente['OS/2'].fsSelection & ~0b1100001) | (0b100000 if negrita else 0b1000000)
        fuente['head'].macStyle = 1 if negrita else 0
        fuente['OS/2'].usWeightClass = 700 if negrita else 400
        fuente.save(ruta)
        print(f'{archivo}: "{familia}" {estilo}')


if __name__ == '__main__':
    main()
