"""Compone y sintetiza la música de fondo del juego (obra original, sin muestras de terceros).

Genera dos bucles sin corte en src/main/resources/com/mapamundi/musica/:
  menu.wav   alegre, con melodía (104 ppm)
  juego.wav  tranquila, sin melodía principal para no distraer (92 ppm)

Uso: python -I herramientas/crear_musica.py   (necesita numpy)
"""
import os
import wave

import numpy as np

SR = 22050
AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'com', 'mapamundi', 'musica')
azar = np.random.default_rng(2000)  # fija: la música siempre suena igual

ACORDES = {
    'C': (60, 64, 67), 'Am': (57, 60, 64), 'F': (53, 57, 60), 'G': (55, 59, 62),
    'Em': (52, 55, 59), 'Dm': (50, 53, 57),
}


def hz(nota):
    return 440.0 * 2 ** ((nota - 69) / 12)


def envolvente(n, ataque, caida, sostener, soltar):
    t = np.arange(n) / SR
    env = np.where(t < ataque, t / max(ataque, 1e-4), sostener + (1 - sostener) * np.exp(-(t - ataque) / max(caida, 1e-4)))
    cola = int(soltar * SR)
    if cola and n > cola:
        env[-cola:] *= np.linspace(1, 0, cola)
    return env


def bajo(nota, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = hz(nota)
    triangular = 2 / np.pi * np.arcsin(np.sin(2 * np.pi * f * t))
    return (0.8 * triangular + 0.4 * np.sin(2 * np.pi * f / 2 * t)) * envolvente(n, 0.008, 0.25, 0.55, 0.06)


def marimba(nota, dur):
    n = int(max(dur, 0.6) * SR)
    t = np.arange(n) / SR
    f = hz(nota)
    tono = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * f * 3.98 * t) * np.exp(-t * 18)
    return tono * np.exp(-t * 4.5) * envolvente(n, 0.004, 1, 1, 0.02)


def campana(nota, dur):
    n = int(max(dur, 1.4) * SR)
    t = np.arange(n) / SR
    f = hz(nota)
    tono = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 3) + 0.2 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 6)
    return tono * np.exp(-t * 2.2) * envolvente(n, 0.003, 1, 1, 0.05)


def melodia(nota, dur):
    """Voz principal: pulso suave con pocos armónicos y un vibrato que entra tarde."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = hz(nota) * (1 + 0.004 * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - 0.15) / 0.2, 0, 1))
    fase = 2 * np.pi * np.cumsum(f) / SR
    tono = sum(np.sin(k * fase) * (1 / k) * (0.9 if k % 4 else 0.2) for k in range(1, 7))
    return tono * envolvente(n, 0.015, 0.3, 0.7, min(0.08, dur / 3))


def bombo(_nota, _dur):
    n = int(0.22 * SR)
    t = np.arange(n) / SR
    f = 45 + 80 * np.exp(-t * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 14)


def platillo(_nota, _dur):
    n = int(0.05 * SR)
    ruido = np.diff(azar.uniform(-1, 1, n + 1))
    return ruido * np.exp(-np.arange(n) / SR * 90)


def caja(_nota, _dur):
    n = int(0.16 * SR)
    t = np.arange(n) / SR
    return (0.6 * azar.uniform(-1, 1, n) + 0.5 * np.sin(2 * np.pi * 185 * t)) * np.exp(-t * 26)


class Pista:
    def __init__(self, ppm, compases):
        self.pulso = 60 / ppm
        self.n = int(round(compases * 4 * self.pulso * SR))
        self.audio = np.zeros(self.n)

    def tocar(self, voz, nota, inicio_pulsos, dur_pulsos, volumen):
        sonido = voz(nota, dur_pulsos * self.pulso) * volumen
        i = int(round(inicio_pulsos * self.pulso * SR))
        # Suma circular: lo que se pasa del final suena al principio, así el bucle no tiene corte.
        idx = (np.arange(len(sonido)) + i) % self.n
        np.add.at(self.audio, idx, sonido)

    def eco(self, retardo, ganancia):
        d = int(retardo * self.pulso * SR)
        self.audio = self.audio + ganancia * np.roll(self.audio, d)

    def guardar(self, nombre):
        datos = self.audio / np.max(np.abs(self.audio)) * 0.89
        os.makedirs(SALIDA, exist_ok=True)
        ruta = os.path.join(SALIDA, nombre)
        with wave.open(ruta, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((datos * 32767).astype('<i2').tobytes())
        print(f'{nombre}: {self.n / SR:.1f} s, {os.path.getsize(ruta) / 1e6:.1f} MB')


def tema_menu():
    p = Pista(104, 16)
    progresion = ['C', 'Am', 'F', 'G', 'C', 'Am', 'F', 'G', 'F', 'G', 'Em', 'Am', 'Dm', 'G', 'C', 'G']
    # (nota, pulsos); None = silencio
    tema = [
        [(76, 1), (79, .5), (76, .5), (72, 1), (74, 1)], [(72, 1.5), (69, .5), (72, 1), (76, 1)],
        [(77, 1), (76, .5), (74, .5), (72, 1), (69, 1)], [(71, 1), (74, 1), (67, 2)],
        [(76, 1), (79, .5), (76, .5), (72, 1), (76, 1)], [(81, 1.5), (79, .5), (76, 1), (72, 1)],
        [(74, 1), (77, 1), (81, 1), (79, .5), (77, .5)], [(76, 1), (74, 1), (74, 2)],
        [(81, 1), (79, .5), (77, .5), (76, 1), (72, 1)], [(74, 1), (71, .5), (72, .5), (74, 2)],
        [(76, 1), (79, 1), (83, 1), (79, 1)], [(81, 2), (76, 2)],
        [(77, 1), (76, .5), (74, .5), (69, 1), (74, 1)], [(71, 1), (72, .5), (74, .5), (79, 2)],
        [(76, 1), (74, .5), (72, .5), (67, 1), (76, 1)], [(74, 2), (None, 2)],
    ]
    for compas, acorde in enumerate(progresion):
        b = compas * 4
        r, t3, q5 = ACORDES[acorde]
        # Arpegio de marimba en corcheas: 1-3-5-8-5-3-1-3
        for k, nota in enumerate([r, t3, q5, r + 12, q5, t3, r, t3]):
            p.tocar(marimba, nota + 12, b + k * 0.5, 0.5, 0.22)
        # Bajo: tónica en 1 y 3, quinta de paso en el 4
        p.tocar(bajo, r - 24, b, 1.8, 0.55)
        p.tocar(bajo, r - 24, b + 2, 1.3, 0.5)
        p.tocar(bajo, q5 - 24, b + 3.5, 0.5, 0.4)
        # Batería suave
        p.tocar(bombo, 0, b, 1, 0.55)
        p.tocar(bombo, 0, b + 2, 1, 0.45)
        p.tocar(caja, 0, b + 1, 1, 0.16)
        p.tocar(caja, 0, b + 3, 1, 0.16)
        for k in range(8):
            p.tocar(platillo, 0, b + k * 0.5, 0.5, 0.10 if k % 2 else 0.06)
        # Melodía
        t = b
        for nota, dur in tema[compas]:
            if nota:
                p.tocar(melodia, nota, t, dur * 0.95, 0.20)
            t += dur
    p.eco(0.75, 0.22)
    p.guardar('menu.wav')


def tema_juego():
    p = Pista(92, 16)
    progresion = ['Am', 'F', 'C', 'G'] * 4
    pentatonica = [69, 72, 74, 76, 79, 81]
    for compas, acorde in enumerate(progresion):
        b = compas * 4
        r, t3, q5 = ACORDES[acorde]
        for k, nota in enumerate([r, q5, t3 + 12, q5, r + 12, q5, t3 + 12, q5]):
            p.tocar(marimba, nota + 12, b + k * 0.5, 0.5, 0.14 if k % 2 else 0.18)
        p.tocar(bajo, r - 24, b, 3.5, 0.45)
        p.tocar(bombo, 0, b, 1, 0.3)
        for k in range(4):
            p.tocar(platillo, 0, b + k + 0.5, 0.5, 0.05)
        # Una campanita de vez en cuando, para que no sea monótono
        if compas % 2 == 1:
            p.tocar(campana, int(azar.choice(pentatonica)) + 12, b + 2, 2, 0.12)
    p.eco(0.75, 0.25)
    p.guardar('juego.wav')


if __name__ == '__main__':
    tema_menu()
    tema_juego()
