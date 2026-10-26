// El juego tal como era en el Mapamundi original (2000): la misma regla que PartidaClasica.java.
//
//  1. Caída: el país cae un paso por tic; hay que elegir su continente antes de que llegue abajo. Equivocarse
//     o dejarlo caer es un error, y el mismo país vuelve a caer.
//  2. Mapa: la pieza nunca se para: avanza sola (al principio hacia abajo), las flechas solo cambian su
//     dirección y rebota en los bordes. «Soltar» (Enter) la deja. Mientras, una barra de tiempo se llena.
//
// Hay 5 errores para toda la partida. Cada país bien ubicado suma de 10 a 50 puntos según el modo. Cuando todos
// los países de un continente están ubicados, ese continente se completa («Mapa completo»).
import { Mercator, Region } from './geo'
import type { Atlas, Continente, Modo, Pais } from './modelo'
import { CONTINENTES, MODOS, VISTAS } from './modelo'

export const ERRORES_MAX = 5
/** Pasos que da el país al caer (en el original, de 17 a 330 píxeles de 7 en 7). */
export const PASOS_CAIDA = 45
/** Tics de la barra de tiempo del mapa. */
export const PASOS_RELOJ = 252
/** Paso de la pieza en el mapa, como fracción del ancho de la vista. */
const PASO = 5 / 740
/** Milisegundos entre tics: el país cae un paso cada 450 ms, la pieza avanza cada 130 ms, la barra cada 150 ms. */
export const RITMO = { caida: 450, movimiento: 130, reloj: 150 }
/** Margen de acierto: vale soltar la pieza dentro del país o a menos de esto de su lugar. */
export const MARGEN_KM = 250

export type Fase = 'CAIDA' | 'UBICACION' | 'RESULTADO' | 'FIN'
export type Direccion = 'ARRIBA' | 'IZQUIERDA' | 'ABAJO' | 'DERECHA'
export type Motivo = 'UBICADO' | 'LEJOS' | 'TIEMPO_CAIDA' | 'CONTINENTE_EQUIVOCADO' | 'TIEMPO_MAPA'
export type Sonido = 'TIC' | 'CONTINENTE_CORRECTO' | 'CONTINENTE_EQUIVOCADO' | 'UBICADO' | 'MAL_UBICADO' | 'FIN_TIEMPO' | 'FIN_PARTIDA' | 'NIVEL_SUPERADO'

const PASOS: Record<Direccion, [number, number]> = { ARRIBA: [0, -1], IZQUIERDA: [-1, 0], ABAJO: [0, 1], DERECHA: [1, 0] }
const OPUESTA: Record<Direccion, Direccion> = { ARRIBA: 'ABAJO', ABAJO: 'ARRIBA', IZQUIERDA: 'DERECHA', DERECHA: 'IZQUIERDA' }

export interface Resultado {
  acierto: boolean
  motivo: Motivo
  /** -1 si no hay distancia (fallo en la caída). */
  distanciaKm: number
  puntos: number
  completado: Continente | null
}

/** Puntos por país: 10 en «Mapa y nombre del país» … 50 en «Solo datos del país». */
export const puntosPorPais = (m: Modo): number => (MODOS.indexOf(m) + 1) * 10

export interface Ajustes {
  modo: Modo
  /** Los continentes con los que se juega (vacío = todos). */
  continentes: Continente[]
}

export class PartidaClasica {
  readonly enJuego: Set<Continente>
  private readonly todos: Pais[]
  readonly completos = new Set<Continente>()
  readonly ubicados = new Set<Pais>()

  actual!: Pais
  fase: Fase = 'CAIDA'
  errores = 0
  /** Fallos con el país actual: vuelve a cero con cada país nuevo. */
  fallosEnEstePais = 0
  puntaje = 0
  private caidaPasos = 0
  private relojPasos = 0
  pausa = false
  vista: Region | null = null
  vueltas = 0
  despU = 0
  despV = 0
  direccion: Direccion = 'ABAJO'
  resultado: Resultado | null = null
  ganada = false

  alCambiar: () => void = () => {}
  alSonar: (s: Sonido) => void = () => {}

  /** @param paises los países del atlas; se juega con los de los continentes elegidos */
  constructor(
    readonly ajustes: Ajustes,
    paises: Pais[],
    private readonly azar: () => number = Math.random,
  ) {
    this.enJuego = new Set(ajustes.continentes.length === 0 ? CONTINENTES : ajustes.continentes)
    this.todos = paises.filter((p) => this.enJuego.has(p.continente))
    this.nuevoPais()
  }

  static deAtlas(ajustes: Ajustes, atlas: Atlas, azar?: () => number): PartidaClasica {
    return new PartidaClasica(ajustes, atlas.paises, azar)
  }

  // ------------------------------------------------------------------ fase 1: la caída

  ticCaida(): void {
    if (this.fase !== 'CAIDA' || this.pausa) return
    this.caidaPasos++
    if (this.caidaPasos >= PASOS_CAIDA) {
      this.caidaPasos = PASOS_CAIDA
      this.fallar({ acierto: false, motivo: 'TIEMPO_CAIDA', distanciaKm: -1, puntos: 0, completado: null }, 'FIN_TIEMPO')
    } else {
      this.sonar('TIC')
      this.cambio()
    }
  }

  /** Los continentes ya completos (o no elegidos) no se pueden elegir: en el original salían en gris. */
  elegible(c: Continente): boolean {
    return this.enJuego.has(c) && !this.completos.has(c)
  }

  elegirContinente(c: Continente): void {
    if (this.fase !== 'CAIDA' || this.pausa || !this.elegible(c)) return
    if (c === this.actual.continente) {
      this.sonar('CONTINENTE_CORRECTO')
      this.fase = 'UBICACION'
      this.prepararMapa()
      this.cambio()
    } else {
      this.fallar({ acierto: false, motivo: 'CONTINENTE_EQUIVOCADO', distanciaKm: -1, puntos: 0, completado: null }, 'CONTINENTE_EQUIVOCADO')
    }
  }

  // ------------------------------------------------------------------ fase 2: el mapa

  /** Las flechas: la pieza sigue moviéndose, pero hacia otro lado. */
  cambiarDireccion(d: Direccion): void {
    if (this.fase !== 'UBICACION' || this.pausa) return
    this.direccion = d
    this.cambio()
  }

  /** Un paso de la pieza en su dirección; si se saldría del mapa, rebota. */
  ticMovimiento(): void {
    if (this.fase !== 'UBICACION' || this.pausa || !this.vista) return
    const paso = this.vista.ancho * PASO
    const f = this.actual.forma
    const [du, dv] = PASOS[this.direccion]
    const u = f.anclaU + this.vueltas + this.despU + du * paso
    const v = f.anclaV + this.despV + dv * paso
    if (u < this.vista.uMin || u > this.vista.uMax || v < this.vista.vMin || v > this.vista.vMax) {
      this.direccion = OPUESTA[this.direccion]
    } else {
      this.despU += du * paso
      this.despV += dv * paso
    }
    this.cambio()
  }

  /** La barra de tiempo del mapa: cuando se llena, se acabó el tiempo. */
  ticReloj(): void {
    if (this.fase !== 'UBICACION' || this.pausa) return
    this.relojPasos++
    if (this.relojPasos >= PASOS_RELOJ) {
      this.relojPasos = PASOS_RELOJ
      this.fallar({ acierto: false, motivo: 'TIEMPO_MAPA', distanciaKm: this.distanciaKm(), puntos: 0, completado: null }, 'FIN_TIEMPO')
    } else {
      if (this.relojPasos % Math.max(1, Math.round(400 / RITMO.reloj)) === 0) this.sonar('TIC')
      this.cambio()
    }
  }

  /** Soltar la pieza: vale si su centro cae dentro del país o a menos de MARGEN_KM. */
  confirmar(): void {
    if (this.fase !== 'UBICACION' || this.pausa) return
    const f = this.actual.forma
    const km = this.distanciaKm()
    const acierto = f.contiene(f.anclaU + this.despU, f.anclaV + this.despV) || km <= MARGEN_KM
    if (!acierto) {
      this.fallar({ acierto: false, motivo: 'LEJOS', distanciaKm: km, puntos: 0, completado: null }, 'MAL_UBICADO')
      return
    }
    const puntos = puntosPorPais(this.ajustes.modo)
    this.puntaje += puntos
    this.ubicados.add(this.actual)
    const c = this.actual.continente
    let completado: Continente | null = null
    if (this.todos.filter((p) => p.continente === c).every((p) => this.ubicados.has(p))) {
      this.completos.add(c)
      completado = c
    }
    this.ganada = [...this.enJuego].every((x) => this.completos.has(x))
    this.resultado = { acierto: true, motivo: 'UBICADO', distanciaKm: km, puntos, completado }
    this.fase = 'RESULTADO'
    this.sonar(this.ganada || completado ? 'NIVEL_SUPERADO' : 'UBICADO')
    this.cambio()
  }

  alternarPausa(): void {
    if (this.fase === 'RESULTADO' || this.fase === 'FIN') return
    this.pausa = !this.pausa
    this.cambio()
  }

  /** Tras el mensaje: otro país (si acertó), el mismo otra vez (si falló) o el final. */
  siguiente(): void {
    if (this.fase !== 'RESULTADO') return
    if (this.terminada) this.fase = 'FIN'
    else if (this.resultado?.acierto) this.nuevoPais()
    else this.reiniciarCaida()
    this.cambio()
  }

  // ------------------------------------------------------------------ consultas

  /** Cuánto ha caído el país, de 0 (arriba) a 1 (abajo). */
  get caida(): number {
    return this.caidaPasos / PASOS_CAIDA
  }

  /** Cuánto se ha llenado la barra de tiempo del mapa, de 0 a 1. */
  get reloj(): number {
    return this.relojPasos / PASOS_RELOJ
  }

  /** Países de los continentes elegidos (para el avance). */
  get totalPaises(): number {
    return this.todos.length
  }

  /** Sin errores disponibles o con todos los continentes completos. */
  get terminada(): boolean {
    return this.errores >= ERRORES_MAX || this.ganada
  }

  /**
   * Si al fallar se muestra dónde estaba el país. En «Solo datos del país» el primer fallo con un país no lo
   * revela: el mismo país vuelve a caer con todos sus datos; al segundo fallo, sí. En los demás modos, siempre.
   */
  get revelaElLugar(): boolean {
    return this.ajustes.modo !== 'DATOS' || this.fallosEnEstePais >= 2
  }

  /** En «Solo datos del país», tras el primer fallo con un país se ven todos sus datos (menos nombre y capital). */
  get datosCompletos(): boolean {
    return this.ajustes.modo === 'DATOS' && this.fallosEnEstePais >= 1
  }

  // ------------------------------------------------------------------ internos

  private fallar(r: Resultado, sonido: Sonido): void {
    this.errores++
    this.fallosEnEstePais++
    this.resultado = r
    this.fase = 'RESULTADO'
    this.sonar(this.errores >= ERRORES_MAX ? 'FIN_PARTIDA' : sonido)
    this.cambio()
  }

  /** Un país al azar de los continentes que faltan, que todavía no se haya ubicado. */
  private nuevoPais(): void {
    const pendientes = this.todos.filter((p) => !this.ubicados.has(p) && !this.completos.has(p.continente))
    this.actual = pendientes.length === 0 ? this.todos[0] : pendientes[Math.floor(this.azar() * pendientes.length)]
    this.fallosEnEstePais = 0
    this.reiniciarCaida()
  }

  private reiniciarCaida(): void {
    this.fase = 'CAIDA'
    this.caidaPasos = 0
    this.resultado = null
  }

  /** El mapa del continente, con margen; la pieza empieza arriba y en el centro, bajando. */
  private prepararMapa(): void {
    const f = this.actual.forma
    const base = VISTAS[this.actual.continente]
    this.vueltas = base.vueltasPara(f.anclaU)
    // Con margen alrededor, para que los países del borde (Palaos, Malta…) y su nombre se vean enteros.
    this.vista = base.union(f.cajaNucleo.desplazada(this.vueltas).ampliada(0.05)).ampliada(0.08)
    this.despU = this.vista.centroU - f.anclaU - this.vueltas
    this.despV = this.vista.vMin + this.vista.alto * 0.06 - f.anclaV
    this.direccion = 'ABAJO'
    this.relojPasos = 0
  }

  private distanciaKm(): number {
    const f = this.actual.forma
    return Mercator.distanciaKmUV(f.anclaU, f.anclaV, f.anclaU + this.despU, f.anclaV + this.despV)
  }

  private sonar(s: Sonido): void {
    this.alSonar(s)
  }

  private cambio(): void {
    this.alCambiar()
  }
}
