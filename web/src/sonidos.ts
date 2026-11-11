// Efectos y música de fondo: los mismos archivos que la versión de Windows.
//
// Se reproducen con Web Audio y no con <audio>, porque así funcionan también en iPhone y Android: los
// navegadores de teléfono solo dejan sonar algo después de un toque del jugador, y Web Audio queda desbloqueado
// para toda la partida con ese primer toque (ver desbloquear()). Además, los efectos cortos suenan sin retraso.
import type { Sonido } from './partida'
import { esIOS } from './movil'

const ARCHIVOS: Record<Sonido, string> = {
  TIC: 'tic',
  CONTINENTE_CORRECTO: 'continente_correcto',
  CONTINENTE_EQUIVOCADO: 'continente_error',
  UBICADO: 'ubicado',
  MAL_UBICADO: 'mal_ubicado',
  FIN_TIEMPO: 'fin_tiempo',
  FIN_PARTIDA: 'fin_partida',
  NIVEL_SUPERADO: 'nivel_superado',
  GANO: 'gano',
}

export type Musica = 'menu' | 'juego'

const VOLUMEN_MUSICA = 0.4
/** Los efectos, a todo volumen (los originales más bajos ya vienen subidos); el tic del reloj, a la mitad. */
const VOLUMEN_EFECTOS = 1

type ContextoAudio = AudioContext

/** Medio segundo de silencio en WAV (8 kHz, 8 bits), creado aquí para no descargar nada. */
function silencio(): string {
  const n = 4000
  const d = new DataView(new ArrayBuffer(44 + n))
  const texto = (o: number, s: string) => {
    for (let i = 0; i < s.length; i++) d.setUint8(o + i, s.charCodeAt(i))
  }
  texto(0, 'RIFF')
  d.setUint32(4, 36 + n, true)
  texto(8, 'WAVEfmt ')
  d.setUint32(16, 16, true)
  d.setUint16(20, 1, true) // PCM
  d.setUint16(22, 1, true) // mono
  d.setUint32(24, 8000, true)
  d.setUint32(28, 8000, true)
  d.setUint16(32, 1, true)
  d.setUint16(34, 8, true)
  texto(36, 'data')
  d.setUint32(40, n, true)
  for (let i = 0; i < n; i++) d.setUint8(44 + i, 128)
  return URL.createObjectURL(new Blob([d.buffer], { type: 'audio/wav' }))
}

export class Sonidos {
  private ctx: ContextoAudio | null = null
  private readonly buffers = new Map<string, Promise<AudioBuffer | null>>()
  private fuenteMusica: AudioBufferSourceNode | null = null
  private volumenMusica: GainNode | null = null
  /** Mientras se ve si el jugador acertó o falló, la música se calla (ver callarMusica()). */
  private callada = false
  private sonando: Musica | null = null
  /** iPhone anterior a iOS 16.4: el <audio> mudo que hace sonar el juego con el interruptor de silencio puesto. */
  private mudo: HTMLAudioElement | null = null
  musicaActiva = true
  efectosActivos = true

  constructor(private readonly base: string) {
    if (typeof document === 'undefined') return
    // Al salir del juego (otra aplicación, llamada, pantalla apagada) se calla; al volver, sigue.
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) this.mudo?.pause()
      else void this.mudo?.play().catch(() => {})
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
    this.ignorarModoSilencio()
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

  /**
   * En iPhone, Web Audio obedece al interruptor de silencio del costado (como los sonidos de las teclas), y con
   * él activado el juego no se oye. Como en las apps de juegos, debe sonar igual: Safari 16.4 o más nuevo lo
   * permite pidiendo audio de «reproducción»; en los anteriores, un <audio> mudo en bucle pone la página en ese
   * modo.
   */
  private ignorarModoSilencio(): void {
    const sesion = (navigator as unknown as { audioSession?: { type: string } }).audioSession
    if (sesion) {
      sesion.type = 'playback'
      return
    }
    if (!esIOS()) return
    if (!this.mudo) {
      this.mudo = new Audio(silencio())
      this.mudo.loop = true
      this.mudo.setAttribute('playsinline', '')
    }
    if (this.mudo.paused && !document.hidden) void this.mudo.play().catch(() => {})
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

  /**
   * Calla la música un momento (mientras se ve si el jugador acertó o falló, para que se oiga bien el sonido del
   * resultado) o la devuelve, suave. La música sigue corriendo en silencio y vuelve donde va.
   */
  callarMusica(callar: boolean): void {
    if (this.callada === callar) return
    this.callada = callar
    const ctx = this.ctx, volumen = this.volumenMusica
    if (!ctx || !volumen) return
    const ahora = ctx.currentTime
    volumen.gain.cancelScheduledValues(ahora)
    volumen.gain.setValueAtTime(volumen.gain.value, ahora)
    volumen.gain.linearRampToValueAtTime(callar ? 0 : VOLUMEN_MUSICA, ahora + (callar ? 0.25 : 0.9))
  }

  /** Aplica musicaActiva (tras pulsar el botón de música). */
  aplicar(): void {
    if (!this.musicaActiva) this.pararMusica()
    else if (this.sonando && !this.fuenteMusica) this.empezarMusica(this.sonando)
  }

  private empezarMusica(m: Musica): void {
    if (!this.ctx) return
    const ctx = this.ctx
    void this.cargar(`musica/${m}.mp3`).then((buffer) => {
      // Si mientras cargaba cambió la música o se quitó, no se empieza.
      if (!buffer || this.sonando !== m || !this.musicaActiva || this.fuenteMusica) return
      const fuente = ctx.createBufferSource()
      fuente.buffer = buffer
      fuente.loop = true
      const volumen = ctx.createGain()
      volumen.gain.setValueAtTime(0, ctx.currentTime)
      volumen.gain.linearRampToValueAtTime(this.callada ? 0 : VOLUMEN_MUSICA, ctx.currentTime + 0.9)
      fuente.connect(volumen).connect(ctx.destination)
      fuente.start()
      this.fuenteMusica = fuente
      this.volumenMusica = volumen
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
    this.volumenMusica = null
  }
}
