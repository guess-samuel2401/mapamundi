// Geometría del mapa: la misma que la versión de Windows (paquete com.mapamundi.geo).

/**
 * Proyección Web Mercator en unidades normalizadas: u = 0 en lon -180 y 1 en lon 180 (puede salirse de ese
 * rango al cruzar el antimeridiano); v = 0 arriba (lat ~85) y 1 abajo (lat ~-85).
 */
export const Mercator = {
  LAT_MAX: 85.05112878,
  RADIO_TIERRA_KM: 6371.0088,
  u(lon: number): number {
    return (lon + 180) / 360
  },
  v(lat: number): number {
    const phi = (Math.max(-this.LAT_MAX, Math.min(this.LAT_MAX, lat)) * Math.PI) / 180
    return (1 - Math.log(Math.tan(phi) + 1 / Math.cos(phi)) / Math.PI) / 2
  },
  lon(u: number): number {
    return u * 360 - 180
  },
  lat(v: number): number {
    return (Math.atan(Math.sinh(Math.PI * (1 - 2 * v))) * 180) / Math.PI
  },
  /** Distancia sobre la esfera (haversine), en km. */
  distanciaKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const rad = Math.PI / 180
    const dLat = (lat2 - lat1) * rad
    const dLon = (lon2 - lon1) * rad
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLon / 2) ** 2
    return 2 * this.RADIO_TIERRA_KM * Math.asin(Math.min(1, Math.sqrt(a)))
  },
  /** Distancia en km entre dos puntos dados en coordenadas Mercator (u, v). */
  distanciaKmUV(u1: number, v1: number, u2: number, v2: number): number {
    return this.distanciaKm(this.lat(v1), this.lon(u1), this.lat(v2), this.lon(u2))
  },
}

/** Rectángulo del mapa en coordenadas Mercator (u, v). */
export class Region {
  constructor(
    readonly uMin: number,
    readonly vMin: number,
    readonly uMax: number,
    readonly vMax: number,
  ) {}

  static deGrados(lonMin: number, lonMax: number, latMin: number, latMax: number): Region {
    return new Region(Mercator.u(lonMin), Mercator.v(latMax), Mercator.u(lonMax), Mercator.v(latMin))
  }

  get ancho(): number {
    return this.uMax - this.uMin
  }

  get alto(): number {
    return this.vMax - this.vMin
  }

  get centroU(): number {
    return (this.uMin + this.uMax) / 2
  }

  get centroV(): number {
    return (this.vMin + this.vMax) / 2
  }

  union(o: Region): Region {
    return new Region(Math.min(this.uMin, o.uMin), Math.min(this.vMin, o.vMin), Math.max(this.uMax, o.uMax), Math.max(this.vMax, o.vMax))
  }

  ampliada(fraccion: number): Region {
    const du = this.ancho * fraccion
    const dv = this.alto * fraccion
    return new Region(this.uMin - du, Math.max(0, this.vMin - dv), this.uMax + du, Math.min(1, this.vMax + dv))
  }

  desplazada(du: number): Region {
    return new Region(this.uMin + du, this.vMin, this.uMax + du, this.vMax)
  }

  seCruzaCon(o: Region): boolean {
    return this.uMin <= o.uMax && o.uMin <= this.uMax && this.vMin <= o.vMax && o.vMin <= this.vMax
  }

  /** Cuántas vueltas al mundo (±1 en u) hay que desplazar algo centrado en u para que quede en esta región. */
  vueltasPara(u: number): number {
    return Math.round(this.centroU - u)
  }
}

/** Un contorno cerrado (isla, territorio o hueco) ya proyectado a Mercator. */
export class Anillo {
  readonly u: Float64Array
  readonly v: Float64Array
  readonly caja: Region
  readonly area: number
  readonly centroU: number
  readonly centroV: number

  /** @param lonLat coordenadas planas [lon0, lat0, lon1, lat1, …]; @param desplazamientoLon ±360 al cruzar el antimeridiano */
  constructor(lonLat: number[], desplazamientoLon = 0) {
    const n = lonLat.length / 2
    this.u = new Float64Array(n)
    this.v = new Float64Array(n)
    let uMin = Infinity, vMin = Infinity, uMax = -Infinity, vMax = -Infinity
    for (let i = 0; i < n; i++) {
      const u = Mercator.u(lonLat[2 * i] + desplazamientoLon)
      const v = Mercator.v(lonLat[2 * i + 1])
      this.u[i] = u
      this.v[i] = v
      uMin = Math.min(uMin, u)
      uMax = Math.max(uMax, u)
      vMin = Math.min(vMin, v)
      vMax = Math.max(vMax, v)
    }
    this.caja = new Region(uMin, vMin, uMax, vMax)
    // Área y centroide con la fórmula del polígono (shoelace) en el plano Mercator.
    let a = 0, cx = 0, cy = 0
    for (let i = 0; i < n; i++) {
      const j = (i + 1) % n
      const cruz = this.u[i] * this.v[j] - this.u[j] * this.v[i]
      a += cruz
      cx += (this.u[i] + this.u[j]) * cruz
      cy += (this.v[i] + this.v[j]) * cruz
    }
    this.area = Math.abs(a / 2)
    if (Math.abs(a) > 1e-15) {
      this.centroU = cx / (3 * a)
      this.centroV = cy / (3 * a)
    } else {
      this.centroU = this.caja.centroU
      this.centroV = this.caja.centroV
    }
  }

  get puntos(): number {
    return this.u.length
  }
}

/** Una isla pertenece al núcleo si está a menos de esta distancia de otra parte del núcleo. */
const SEPARACION_MAX_KM = 1100

/**
 * La geometría completa de un país y su «núcleo»: la parte que forma la pieza que mueve el jugador. Las islas
 * lejanas (Guayana Francesa para Francia, Hawái para EE. UU.) se dibujan en el mapa, pero no son parte de la pieza.
 */
export class Forma {
  readonly anillos: Anillo[]
  readonly nucleo: Anillo[]
  readonly caja: Region
  readonly cajaNucleo: Region
  /** Punto de referencia de la pieza (centro de masa del núcleo): de aquí se mide la distancia al acertar. */
  readonly anclaU: number
  readonly anclaV: number

  constructor(anillosLonLat: number[][]) {
    if (anillosLonLat.length === 0) throw new Error('Forma vacía')
    let principal = 0
    let mayor = -1
    anillosLonLat.forEach((r, i) => {
      const a = areaGrados(r)
      if (a > mayor) {
        mayor = a
        principal = i
      }
    })
    // Los anillos al otro lado del antimeridiano se desplazan ±360° para quedar junto al principal.
    const centroPrincipal = centroLon(anillosLonLat[principal])
    this.anillos = anillosLonLat.map((r) => {
      const d = centroLon(r) - centroPrincipal
      return new Anillo(r, d > 180 ? -360 : d < -180 ? 360 : 0)
    })
    this.nucleo = calcularNucleo(this.anillos, this.anillos[principal])
    this.caja = cajaDe(this.anillos)
    this.cajaNucleo = cajaDe(this.nucleo)
    let area = 0, su = 0, sv = 0
    for (const a of this.nucleo) {
      area += a.area
      su += a.centroU * a.area
      sv += a.centroV * a.area
    }
    this.anclaU = area > 0 ? su / area : this.cajaNucleo.centroU
    this.anclaV = area > 0 ? sv / area : this.cajaNucleo.centroV
  }

  /** Si el punto (u, v) cae dentro del núcleo (regla par-impar, para respetar lagos y huecos). */
  contiene(u: number, v: number): boolean {
    const c = this.cajaNucleo
    if (u < c.uMin || u > c.uMax || v < c.vMin || v > c.vMax) return false
    let dentro = false
    for (const a of this.nucleo) {
      const k = a.caja
      if (u < k.uMin || u > k.uMax || v < k.vMin || v > k.vMax) continue
      const n = a.puntos
      for (let i = 0, j = n - 1; i < n; j = i++) {
        const ui = a.u[i], vi = a.v[i], uj = a.u[j], vj = a.v[j]
        if (vi > v !== vj > v && u < ((uj - ui) * (v - vi)) / (vj - vi) + ui) dentro = !dentro
      }
    }
    return dentro
  }
}

function calcularNucleo(todos: Anillo[], principal: Anillo): Anillo[] {
  const nucleo = [principal]
  const pendientes = todos.filter((a) => a !== principal)
  let agregado = true
  while (agregado) {
    agregado = false
    for (let i = 0; i < pendientes.length; i++) {
      const candidato = pendientes[i]
      if (nucleo.some((parte) => separacionKm(candidato.caja, parte.caja) <= SEPARACION_MAX_KM)) {
        nucleo.push(candidato)
        pendientes.splice(i--, 1)
        agregado = true
      }
    }
  }
  return nucleo
}

/** Separación aproximada entre dos cajas, en km (0 si se tocan). */
function separacionKm(a: Region, b: Region): number {
  const du = Math.max(0, Math.max(a.uMin - b.uMax, b.uMin - a.uMax))
  const dv = Math.max(0, Math.max(a.vMin - b.vMax, b.vMin - a.vMax))
  const lat = Mercator.lat((Math.max(a.vMin, b.vMin) + Math.min(a.vMax, b.vMax)) / 2)
  const kmPorU = 2 * Math.PI * Mercator.RADIO_TIERRA_KM * Math.cos((lat * Math.PI) / 180)
  return Math.hypot(du * kmPorU, dv * kmPorU)
}

function cajaDe(lista: Anillo[]): Region {
  return lista.reduce((r, a) => r.union(a.caja), lista[0].caja)
}

function areaGrados(r: number[]): number {
  let a = 0
  const n = r.length / 2
  for (let i = 0; i < n; i++) {
    const j = (i + 1) % n
    a += r[2 * i] * r[2 * j + 1] - r[2 * j] * r[2 * i + 1]
  }
  return Math.abs(a / 2)
}

function centroLon(r: number[]): number {
  let min = Infinity, max = -Infinity
  for (let i = 0; i < r.length; i += 2) {
    min = Math.min(min, r[i])
    max = Math.max(max, r[i])
  }
  return (min + max) / 2
}

/** Pasa de coordenadas Mercator (u, v) a píxeles y al revés (escala = píxeles por unidad Mercator). */
export class Transformacion {
  constructor(
    readonly centroU: number,
    readonly centroV: number,
    readonly escala: number,
    readonly ancho: number,
    readonly alto: number,
  ) {}

  /** La región entera, lo más grande posible dentro de ancho × alto. */
  static ajustada(r: Region, ancho: number, alto: number): Transformacion {
    return new Transformacion(r.centroU, r.centroV, Math.min(ancho / r.ancho, alto / r.alto), ancho, alto)
  }

  /** La región llenando todo ancho × alto; lo que no cabe queda fuera (como background-size: cover). */
  static cubriendo(r: Region, ancho: number, alto: number): Transformacion {
    return new Transformacion(r.centroU, r.centroV, Math.max(ancho / r.ancho, alto / r.alto), ancho, alto)
  }

  x(u: number): number {
    return (u - this.centroU) * this.escala + this.ancho / 2
  }

  y(v: number): number {
    return (v - this.centroV) * this.escala + this.alto / 2
  }

  u(x: number): number {
    return (x - this.ancho / 2) / this.escala + this.centroU
  }

  v(y: number): number {
    return (y - this.alto / 2) / this.escala + this.centroV
  }

  /** Lo que se ve en pantalla, como región Mercator. */
  visible(): Region {
    return new Region(this.u(0), this.v(0), this.u(this.ancho), this.v(this.alto))
  }
}
