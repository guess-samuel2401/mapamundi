"""Convierte un .wav MS-ADPCM (formato 2) a PCM de 16 bits, que es lo que reproduce JavaFX.

Uso: python -I herramientas/adpcm_a_pcm.py entrada.wav salida.wav
"""
import struct
import sys
import wave

ADAPT = [230, 230, 230, 230, 307, 409, 512, 614, 768, 614, 512, 409, 307, 230, 230, 230]


def leer_chunks(datos):
    pos, chunks = 12, {}
    while pos + 8 <= len(datos):
        nombre, largo = datos[pos:pos + 4], struct.unpack('<I', datos[pos + 4:pos + 8])[0]
        chunks[nombre] = datos[pos + 8:pos + 8 + largo]
        pos += 8 + largo + (largo & 1)
    return chunks


def decodificar(fmt, data):
    formato, canales, hz, _, bloque, _ = struct.unpack('<HHIIHH', fmt[:16])
    if formato != 2:
        raise ValueError('No es MS-ADPCM')
    n_coef = struct.unpack('<H', fmt[20:22])[0]
    coefs = [struct.unpack('<hh', fmt[22 + 4 * i:26 + 4 * i]) for i in range(n_coef)]
    muestras = []
    for inicio in range(0, len(data), bloque):
        b = data[inicio:inicio + bloque]
        if len(b) < 7 * canales:
            break
        p = 0
        pred = list(b[p:p + canales]); p += canales
        delta = list(struct.unpack('<' + 'h' * canales, b[p:p + 2 * canales])); p += 2 * canales
        s1 = list(struct.unpack('<' + 'h' * canales, b[p:p + 2 * canales])); p += 2 * canales
        s2 = list(struct.unpack('<' + 'h' * canales, b[p:p + 2 * canales])); p += 2 * canales
        for c in range(canales):
            muestras.append(s2[c])
        for c in range(canales):
            muestras.append(s1[c])
        canal = 0
        for byte in b[p:]:
            for nib in (byte >> 4, byte & 0x0F):
                c1, c2 = coefs[pred[canal]]
                predicho = (s1[canal] * c1 + s2[canal] * c2) // 256
                firmado = nib - 16 if nib & 8 else nib
                muestra = max(-32768, min(32767, predicho + firmado * delta[canal]))
                muestras.append(muestra)
                s2[canal], s1[canal] = s1[canal], muestra
                delta[canal] = max(16, ADAPT[nib] * delta[canal] // 256)
                canal = (canal + 1) % canales
    return canales, hz, muestras


def main():
    datos = open(sys.argv[1], 'rb').read()
    chunks = leer_chunks(datos)
    canales, hz, muestras = decodificar(chunks[b'fmt '], chunks[b'data'])
    with wave.open(sys.argv[2], 'wb') as w:
        w.setnchannels(canales)
        w.setsampwidth(2)
        w.setframerate(hz)
        w.writeframes(struct.pack('<' + 'h' * len(muestras), *muestras))
    print(f'{len(muestras) // canales} muestras, {canales} canales, {hz} Hz')


if __name__ == '__main__':
    main()
