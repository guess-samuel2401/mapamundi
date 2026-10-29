// Efectos y música de fondo: los mismos archivos que la versión de Windows.
//
// Se reproducen con Web Audio y no con <audio>, porque así funcionan también en iPhone y Android: los
// navegadores de teléfono solo dejan sonar algo después de un toque del jugador, y Web Audio queda desbloqueado
// para toda la partida con ese primer toque (ver desbloquear()). Además, los efectos cortos suenan sin retraso.
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

type ContextoAudio = AudioContext

export class Sonidos {
  private ctx: ContextoAudio | null = null
  private readonly buffers = new Map<string, Promise<AudioBuffer | null>>()
  private fuenteMusica: AudioBufferSourceNode | null = null
  private sonando: Musica | null = null
  musicaActiva = true
  efectosActivos = true

  constructor(private readonly base: string) {
    if (typeof document === 'undefined') return
    // Al salir del juego (otra aplicación, llamada, pantalla apagada) se calla; al volver, sigue.
    document.addEventListener('visibilitychange', () => {
      if (!this.ctx) return
      if (document.hidden) void this.ctx.suspend()
      else void this.ctx.resume()
    })
  }

  /**
   * Hay que llamarlo desde un toque o una tecla del jugador: crea (o reanuda) el audio. Después ya puede sonar
   * todo lo demás, también desde los relojes del juego.
   */
  desbloquear(): void {
    if (!this.ctx) {
      const Clase = window.AudioContext ?? (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
      if (!Clase) return
      this.ctx = new Clase()
      // Los efectos se cargan ya, para que el primero no llegue tarde.
      for (const archivo of Object.values(ARCHIVOS)) void this.cargar(`sonidos/${archivo}.wav`)
    }
    if (this.ctx.state === 'suspended' && !document.hidden) void this.ctx.resume()
    if (this.sonando && this.musicaActiva && !this.fuenteMusica) this.empezarMusica(this.sonando)
  }

  private cargar(ruta: string): Promise<AudioBuffer | null> {
    let p = this.buffers.get(ruta)
    if (!p) {
      p = fetch(this.base + ruta)
        .then((r) => r.arrayBuffer())
        .then((datos) => this.ctx!.decodeAudioData(datos))
        .catch(() => null)
      this.buffers.set(ruta, p)
    }
    return p
  }

  reproducir(s: Sonido): void {
    if (!this.efectosActivos || !this.ctx) return
    const ctx = this.ctx
    void this.cargar(`sonidos/${ARCHIVOS[s]}.wav`).then((buffer) => {
      if (!buffer || !this.efectosActivos) return
      const fuente = ctx.createBufferSource()
      fuente.buffer = buffer
      const volumen = ctx.createGain()
      volumen.gain.value = VOLUMEN_EFECTOS * (s === 'TIC' ? 0.5 : 1)
      fuente.connect(volumen).connect(ctx.destination)
      fuente.start()
    })
  }

  /** Cambia la música de fondo (la del menú o la de la partida). */
  musica(m: Musica): void {
    if (this.sonando === m && this.fuenteMusica) return
    this.pararMusica()
    this.sonando = m
    if (this.musicaActiva) this.empezarMusica(m)
  }

  /** Aplica musicaActiva (tras pulsar el botón de música). */
  aplicar(): void {
    if (!this.musicaActiva) this.pararMusica()
    else if (this.sonando && !this.fuenteMusica) this.empezarMusica(this.sonando)
  }

  private empezarMusica(m: Musica): void {
    if (!this.ctx) return
    const ctx = this.ctx
    void this.cargar(`musica/${m}.wav`).then((buffer) => {
      // Si mientras cargaba cambió la música o se quitó, no se empieza.
      if (!buffer || this.sonando !== m || !this.musicaActiva || this.fuenteMusica) return
      const fuente = ctx.createBufferSource()
      fuente.buffer = buffer
      fuente.loop = true
      const volumen = ctx.createGain()
      volumen.gain.setValueAtTime(0, ctx.currentTime)
      volumen.gain.linearRampToValueAtTime(VOLUMEN_MUSICA, ctx.currentTime + 0.9)
      fuente.connect(volumen).connect(ctx.destination)
      fuente.start()
      this.fuenteMusica = fuente
    })
  }

  private pararMusica(): void {
    if (!this.fuenteMusica) return
    try {
      this.fuenteMusica.stop()
    } catch {
      // Ya estaba parada.
    }
    this.fuenteMusica = null
  }
}
