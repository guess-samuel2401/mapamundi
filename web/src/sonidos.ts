// Efectos y música de fondo: los mismos archivos que la versión de Windows.
import type { Sonido } from './partida'

const ARCHIVOS: Record<Sonido, string> = {
  TIC: 'tic',
  CONTINENTE_CORRECTO: 'continente_correcto',
  CONTINENTE_EQUIVOCADO: 'continente_error',
  UBICADO: 'ubicado',
  MAL_UBICADO: 'mal_ubicado',
  FIN_TIEMPO: 'fin_tiempo',
  FIN_PARTIDA: 'fin_partida',
  NIVEL_SUPERADO: 'nivel_superado',
}

export type Musica = 'menu' | 'juego'

const VOLUMEN_MUSICA = 0.4
const VOLUMEN_EFECTOS = 0.8

export class Sonidos {
  private readonly efectos = new Map<Sonido, HTMLAudioElement>()
  private readonly musicas = new Map<Musica, HTMLAudioElement>()
  private sonando: Musica | null = null
  musicaActiva = true
  efectosActivos = true

  constructor(private readonly base: string) {}

  reproducir(s: Sonido): void {
    if (!this.efectosActivos) return
    let a = this.efectos.get(s)
    if (!a) {
      a = new Audio(`${this.base}sonidos/${ARCHIVOS[s]}.wav`)
      a.volume = VOLUMEN_EFECTOS * (s === 'TIC' ? 0.5 : 1)
      this.efectos.set(s, a)
    }
    a.currentTime = 0
    void a.play().catch(() => {})
  }

  /**
   * Cambia la música de fondo. Los navegadores no dejan sonar nada hasta el primer toque del jugador: por eso
   * la primera pantalla la pide al elegir el idioma.
   */
  musica(m: Musica): void {
    if (this.sonando === m) {
      this.aplicar()
      return
    }
    if (this.sonando) this.musicas.get(this.sonando)?.pause()
    this.sonando = m
    let a = this.musicas.get(m)
    if (!a) {
      a = new Audio(`${this.base}musica/${m}.wav`)
      a.loop = true
      a.volume = VOLUMEN_MUSICA
      this.musicas.set(m, a)
    }
    a.currentTime = 0
    this.aplicar()
  }

  /** Aplica musicaActiva (tras pulsar el botón de música). */
  aplicar(): void {
    const a = this.sonando ? this.musicas.get(this.sonando) : undefined
    if (!a) return
    if (this.musicaActiva) void a.play().catch(() => {})
    else a.pause()
  }
}
