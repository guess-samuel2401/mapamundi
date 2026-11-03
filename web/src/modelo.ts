// Países, continentes y modos de juego (paquetes com.mapamundi.modelo y com.mapamundi.juego de Windows).
import { Forma, Region } from './geo'
import { ingles, t } from './i18n'

export type Continente = 'AFRICA' | 'AMERICA' | 'EUROPA' | 'ASIA' | 'OCEANIA'

/** Los cinco continentes del juego original, en su orden (teclas 1 a 5). */
export const CONTINENTES: Continente[] = ['AFRICA', 'AMERICA', 'EUROPA', 'ASIA', 'OCEANIA']

/** La parte del mapa que muestra cada continente. */
export const VISTAS: Record<Continente, Region> = {
  AFRICA: Region.deGrados(-26, 60, -37, 38),
  AMERICA: Region.deGrados(-170, -25, -57, 74),
  EUROPA: Region.deGrados(-26, 62, 33, 72),
  ASIA: Region.deGrados(25, 150, -11, 56),
  OCEANIA: Region.deGrados(110, 200, -50, 10),
}

export const nombreContinente = (c: Continente): string => t(`continente.${c}`)

/** Los modos del formulario «Nivel» del original, del más fácil al más difícil. */
export type Modo = 'MAPA_PAIS' | 'MAPA_CAPITAL' | 'PAIS' | 'CAPITAL' | 'DATOS'
export const MODOS: Modo[] = ['MAPA_PAIS', 'MAPA_CAPITAL', 'PAIS', 'CAPITAL', 'DATOS']

/** Si se ve la forma real del país o, en su lugar, un círculo de color. */
export const muestraForma = (m: Modo): boolean => m === 'MAPA_PAIS' || m === 'MAPA_CAPITAL'
export const porCapital = (m: Modo): boolean => m === 'MAPA_CAPITAL' || m === 'CAPITAL'
export const nombreModo = (m: Modo): string => t(`modo.${m}.nombre`)

interface TextosEn {
  nombre?: string
  capital?: string
  idioma?: string
  moneda?: string
  religion?: string
  exporta?: string
}

/** Un país del archivo paises.json (generado por herramientas/construir_datos.py). */
interface PaisJson {
  iso: string
  nombre: string
  continente: Continente
  capital: string
  poblacion: string
  superficie: string
  idioma: string
  moneda: string
  pibPerCapita: string
  religion: string
  exporta: string
  en?: TextosEn
  color: number
  anillos: number[][]
}

const elegir = (es: string, en: string | undefined): string => (ingles() && en ? en : es)

/** "41.454.761 habs. (2023)" → "41,454,761 inhabitants (2023)"; "0,49 km²" → "0.49 km²". */
export function cifraEnIngles(texto: string): string {
  if (!texto) return texto
  return texto
    .replace(/\d[\d.,]*\d|\d/g, (n) => n.replace(/\./g, '\u0001').replace(/,/g, '.').replace(/\u0001/g, ','))
    .replace('habs.', 'inhabitants')
    .replace('aprox.', 'approx.')
    .replace('No Disponible', 'Not available')
}

export class Pais {
  readonly forma: Forma

  constructor(private readonly j: PaisJson) {
    this.forma = new Forma(j.anillos)
  }

  get iso(): string { return this.j.iso }
  get continente(): Continente { return this.j.continente }
  /** Índice 1-7 de color, para que dos vecinos no coincidan (MAPCOLOR7). */
  get color(): number { return this.j.color }
  get nombre(): string { return elegir(this.j.nombre, this.j.en?.nombre) }
  get capital(): string { return elegir(this.j.capital, this.j.en?.capital) }
  get idioma(): string { return elegir(this.j.idioma, this.j.en?.idioma) }
  get moneda(): string { return elegir(this.j.moneda, this.j.en?.moneda) }
  get religion(): string { return elegir(this.j.religion, this.j.en?.religion) }
  get exporta(): string { return elegir(this.j.exporta, this.j.en?.exporta) }
  get poblacion(): string { return ingles() ? cifraEnIngles(this.j.poblacion) : this.j.poblacion }
  get superficie(): string { return ingles() ? cifraEnIngles(this.j.superficie) : this.j.superficie }
  get pibPerCapita(): string { return ingles() ? cifraEnIngles(this.j.pibPerCapita) : this.j.pibPerCapita }
}

/** Todos los países y los territorios no jugables (que se dibujan en gris). */
export class Atlas {
  constructor(
    readonly paises: Pais[],
    readonly otrasTierras: Forma[],
    readonly datosActualizados: string,
  ) {}

  private islasCalculadas: Set<Pais> | null = null

  /**
   * Los países sin frontera terrestre (San Cristóbal y Nieves, Santa Lucía, Japón…): los que no comparten ningún
   * vértice de su contorno con otro país.
   */
  get islas(): Set<Pais> {
    if (!this.islasCalculadas) {
      const clave = (u: number, v: number) => `${Math.round(u * 1e7)},${Math.round(v * 1e7)}`
      const duenos = new Map<string, Pais | null>()
      for (const p of this.paises)
        for (const a of p.forma.anillos)
          for (let i = 0; i < a.puntos; i++) {
            const k = clave(a.u[i], a.v[i])
            const d = duenos.get(k)
            if (d === undefined) duenos.set(k, p)
            else if (d !== p) duenos.set(k, null) // vértice compartido: frontera
          }
      this.islasCalculadas = new Set(
        this.paises.filter((p) => p.forma.anillos.every((a) => {
          for (let i = 0; i < a.puntos; i++) if (duenos.get(clave(a.u[i], a.v[i])) === null) return false
          return true
        })),
      )
    }
    return this.islasCalculadas
  }

  private readonly separaciones = new Map<Pais, number>()

  /**
   * Distancia (en unidades Mercator) desde el centro de una isla hasta la tierra ajena más cercana: hasta dónde
   * se puede ampliar sin taparse con sus vecinas (las Antillas Menores están a un paso unas de otras).
   */
  separacion(isla: Pais): number {
    let d = this.separaciones.get(isla)
    if (d === undefined) {
      const u = isla.forma.anclaU, v = isla.forma.anclaV
      d = Infinity
      const otras = [...this.paises.filter((p) => p !== isla).map((p) => p.forma), ...this.otrasTierras]
      for (const f of otras)
        for (const a of f.anillos) {
          const c = a.caja
          for (const vueltas of [-1, 0, 1]) {
            const du = Math.max(0, c.uMin + vueltas - u, u - c.uMax - vueltas), dv = Math.max(0, c.vMin - v, v - c.vMax)
            if (Math.hypot(du, dv) >= d) continue
            for (let i = 0; i < a.puntos; i++) d = Math.min(d, Math.hypot(a.u[i] + vueltas - u, a.v[i] - v))
          }
        }
      this.separaciones.set(isla, d)
    }
    return d
  }

  static desdeJson(json: { paises: PaisJson[]; otrasTierras: number[][][]; datosActualizados: string }): Atlas {
    return new Atlas(
      json.paises.map((p) => new Pais(p)),
      json.otrasTierras.map((a) => new Forma(a)),
      json.datosActualizados,
    )
  }

  static async cargar(url: string): Promise<Atlas> {
    const r = await fetch(url)
    return Atlas.desdeJson(await r.json())
  }

  de(c: Continente): Pais[] {
    return this.paises.filter((p) => p.continente === c)
  }

  pais(iso: string): Pais | undefined {
    return this.paises.find((p) => p.iso === iso)
  }
}
