"""Sintetiza los efectos de sonido del juego (obra original, sin muestras de terceros).

Usan los mismos timbres y la misma tonalidad (do mayor) que la música de fondo, para que todo suene
de una pieza. Genera en src/main/resources/com/mapamundi/sonidos/:

  continente_correcto.wav  dos notas ascendentes de marimba
  continente_error.wav     dos notas graves y apagadas, que bajan
  ubicado.wav              arpegio alegre con campanitas
  mal_ubicado.wav          tres notas que caen, con un "uah" final
  fin_tiempo.wav           tic-tac que se acelera y un golpe grave
  fin_partida.wav          frase descendente en menor, más larga
  nivel_superado.wav       pequeña fanfarria con acorde final
  logro.wav                destello de campanas que sube
  tic.wav                  "bip" corto del modo clásico (cada paso de la caída)

Uso: python herramientas/crear_efectos.py   (necesita numpy)
"""
import os
import wave

import numpy as np

SR = 22050
AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'com', 'mapamundi', 'sonidos')
azar = np.random.default_rng(7)  # fija: los efectos siempre suenan igual


def hz(nota):
    return 440.0 * 2 ** ((nota - 69) / 12)


def tiempo(dur):
    return np.arange(int(dur * SR)) / SR


def suavizar(sonido, ataque=0.004, cola=0.03):
    """Rampa de entrada y de salida para que no haya chasquidos."""
    n = len(sonido)
    a, c = int(ataque * SR), int(cola * SR)
    if a:
        sonido[:a] *= np.linspace(0, 1, a)
    if c and n > c:
        sonido[-c:] *= np.linspace(1, 0, c)
    return sonido


# ------------------------------------------------------------------ timbres

def marimba(nota, dur=0.5):
    t = tiempo(max(dur, 0.35))
    f = hz(nota)
    tono = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 3.98 * t) * np.exp(-t * 20)
    return suavizar(tono * np.exp(-t * 6), 0.002)


def campana(nota, dur=0.9):
    t = tiempo(dur)
    f = hz(nota)
    tono = (np.sin(2 * np.pi * f * t) + 0.4 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 4)
            + 0.2 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 8))
    return suavizar(tono * np.exp(-t * 3.2), 0.002, 0.08)


def metal(nota, dur):
    """Voz de "trompeta" suave: armónicos que se abren al principio, para la fanfarria."""
    t = tiempo(dur)
    f = hz(nota) * (1 + 0.003 * np.sin(2 * np.pi * 5.5 * t))
    fase = 2 * np.pi * np.cumsum(f) / SR
    brillo = 0.35 + 0.65 * np.clip(t / 0.05, 0, 1)
    tono = sum(np.sin(k * fase) * (brillo ** (k - 1)) / k for k in range(1, 8))
    env = np.minimum(1, t / 0.02) * (0.75 + 0.25 * np.exp(-t * 6))
    return suavizar(tono * env, 0.005, min(0.08, dur / 3))


def sorda(nota, dur):
    """Nota grave y redonda (triangular filtrada): para los errores, que no suenen agresivos."""
    t = tiempo(dur)
    f = hz(nota)
    tri = 2 / np.pi * np.arcsin(np.sin(2 * np.pi * f * t))
    return suavizar((0.7 * tri + 0.5 * np.sin(2 * np.pi * f * t)) * np.exp(-t * 7), 0.004, 0.04)


def deslizar(nota_desde, nota_hasta, dur, vibrato=0.0):
    """Tono que se desliza de una nota a otra (el "uah" del fallo)."""
    t = tiempo(dur)
    notas = nota_desde + (nota_hasta - nota_desde) * (t / dur) ** 0.7
    f = hz(notas) * (1 + vibrato * np.sin(2 * np.pi * 6 * t))
    fase = 2 * np.pi * np.cumsum(f) / SR
    tono = np.sin(fase) + 0.35 * np.sin(2 * fase) + 0.15 * np.sin(3 * fase)
    return suavizar(tono * (1 - 0.6 * t / dur), 0.01, 0.12)


def tic(agudo=True):
    t = tiempo(0.05)
    f = 2400 if agudo else 1800
    ruido = azar.uniform(-1, 1, len(t)) * 0.3
    return suavizar((np.sin(2 * np.pi * f * t) + ruido) * np.exp(-t * 120), 0.001, 0.01)


def gong(nota, dur):
    t = tiempo(dur)
    f = hz(nota)
    tono = (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.02 * t) * np.exp(-t * 5)
            + 0.3 * np.sin(2 * np.pi * f * 3.1 * t) * np.exp(-t * 9))
    golpe = azar.uniform(-1, 1, len(t)) * np.exp(-t * 60) * 0.4
    return suavizar((tono + golpe) * np.exp(-t * 3), 0.002, 0.1)


def destello(dur=0.6):
    """Brillo de fondo: ruido muy agudo que se apaga, como un "cling"."""
    t = tiempo(dur)
    ruido = np.diff(azar.uniform(-1, 1, len(t) + 1))
    return ruido * np.exp(-t * 9) * 0.25


# ------------------------------------------------------------------ mezcla

class Efecto:
    def __init__(self, dur):
        self.audio = np.zeros(int(dur * SR))

    def poner(self, sonido, inicio, volumen):
        i = int(inicio * SR)
        fin = min(len(self.audio), i + len(sonido))
        self.audio[i:fin] += sonido[:fin - i] * volumen
        return self

    def guardar(self, nombre, pico=0.8):
        datos = self.audio / np.max(np.abs(self.audio)) * pico
        datos = suavizar(datos, 0.001, 0.02)
        os.makedirs(SALIDA, exist_ok=True)
        ruta = os.path.join(SALIDA, nombre)
        with wave.open(ruta, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((datos * 32767).astype('<i2').tobytes())
        print(f'{nombre}: {len(self.audio) / SR:.2f} s')


def continente_correcto():
    # Do6 → Sol6: un "¡bien!" corto y brillante.
    Efecto(0.45).poner(marimba(84), 0, 0.8).poner(marimba(91), 0.09, 1).poner(campana(103, 0.35), 0.09, 0.12) \
        .guardar('continente_correcto.wav', 0.7)


def continente_error():
    # Dos notas graves que bajan medio tono: avisa sin regañar.
    Efecto(0.4).poner(sorda(52, 0.18), 0, 1).poner(sorda(51, 0.22), 0.13, 0.9).guardar('continente_error.wav', 0.6)


def ubicado():
    # Arpegio Do-Mi-Sol-Do de marimba con campanas encima y un destello.
    e = Efecto(1.1)
    for k, nota in enumerate([72, 76, 79, 84]):
        e.poner(marimba(nota), k * 0.07, 0.8)
    for k, nota in enumerate([88, 91, 96]):
        e.poner(campana(nota, 0.7), 0.28 + k * 0.06, 0.35)
    e.poner(destello(0.6), 0.28, 1)
    e.guardar('ubicado.wav')


def mal_ubicado():
    # Sol-Fa#-Fa y un "uah" que se cae, a lo dibujo animado, suave.
    e = Efecto(1.0)
    for k, nota in enumerate([67, 66, 65]):
        e.poner(sorda(nota, 0.2), k * 0.16, 0.8)
    e.poner(deslizar(64, 57, 0.5, vibrato=0.012), 0.48, 0.55)
    e.guardar('mal_ubicado.wav', 0.65)


def fin_tiempo():
    # Tic-tac que se acelera y un gong grave.
    e = Efecto(1.4)
    momentos = [0, 0.16, 0.30, 0.42, 0.52, 0.60]
    for k, m in enumerate(momentos):
        e.poner(tic(k % 2 == 0), m, 0.9)
    e.poner(gong(43, 0.8), 0.7, 0.9)
    e.guardar('fin_tiempo.wav', 0.7)


def fin_partida():
    # Frase descendente en do menor (Sol-Mib-Do... Si-Do) con un acorde final apagado.
    e = Efecto(2.0)
    frase = [(67, 0.0, 0.25), (63, 0.24, 0.25), (60, 0.48, 0.25), (59, 0.72, 0.18), (60, 0.9, 0.9)]
    for nota, inicio, dur in frase:
        e.poner(metal(nota, dur), inicio, 0.5)
        e.poner(marimba(nota - 12, dur), inicio, 0.35)
    for nota in (48, 51, 55):
        e.poner(sorda(nota, 0.9), 0.9, 0.35)
    e.guardar('fin_partida.wav', 0.7)


def nivel_superado():
    # Fanfarria: Sol-Do-Mi-Sol en corcheas rápidas y acorde de Do mayor sostenido, con campanas.
    e = Efecto(1.9)
    for k, nota in enumerate([67, 72, 76, 79]):
        e.poner(metal(nota, 0.14), k * 0.11, 0.55)
    for nota in (72, 76, 79, 84):
        e.poner(metal(nota, 1.1), 0.46, 0.3)
    e.poner(marimba(48, 0.8), 0.46, 0.6)
    for k, nota in enumerate([96, 100, 103, 108]):
        e.poner(campana(nota, 0.8), 0.5 + k * 0.08, 0.15)
    e.poner(destello(0.9), 0.46, 1.2)
    e.guardar('nivel_superado.wav')


def logro():
    # Destello de campanas que sube (pentatónica) y un "cling" final: distinto del nivel superado.
    e = Efecto(1.3)
    for k, nota in enumerate([84, 86, 88, 91, 93, 96]):
        e.poner(campana(nota, 0.7), k * 0.05, 0.5)
    e.poner(marimba(72, 0.6), 0, 0.5)
    e.poner(campana(100, 1.0), 0.32, 0.6)
    e.poner(destello(0.8), 0.3, 1.4)
    e.guardar('logro.wav', 0.75)


def tic_clasico():
    # El "bip" del modo clásico, que suena en cada paso: corto, suave y de madera, para que no canse.
    Efecto(0.09).poner(marimba(96, 0.09), 0, 1).poner(tic(), 0, 0.25).guardar('tic.wav', 0.35)


if __name__ == '__main__':
    continente_correcto()
    continente_error()
    ubicado()
    mal_ubicado()
    fin_tiempo()
    fin_partida()
    nivel_superado()
    logro()
    tic_clasico()
