// Dónde se dibuja el mapa de cada continente en la partida: lo más grande posible, con el rincón de abajo a la
// izquierda (la cruceta y «Soltar») en el mar.
import { Transformacion, type Region } from './geo'
import type { Atlas, Continente } from './modelo'

/** Separación de la cruceta y «Soltar» respecto del borde del mapa (como .controles-mapa en estilos.css). */
const MARGEN_CONTROLES = 10

/** El rincón (abajo a la izquierda) que ocupan la cruceta y «Soltar», en píxeles. */
export interface Hueco {
  w: number
  h: number
}

const encuadres = new Map<string, Transformacion>()

/**
 * Dónde se dibuja el mapa del continente: lo más grande posible, con el rincón de la cruceta y «Soltar» en el
 * mar, como el Pacífico en América. Lo usan el mapa y el país que cae, para que tenga el mismo tamaño.
 */
export function encuadre(atlas: Atlas, continente: Continente, vista: Region, w: number, h: number, hueco: Hueco): Transformacion {
  const clave = `${continente},${vista.uMin},${vista.vMin},${vista.uMax},${vista.vMax},${w},${h},${hueco.w},${hueco.h}`
  let t = encuadres.get(clave)
  if (!t) {
    t = elegirEncuadre(atlas, continente, vista, w, h, hueco)
    encuadres.set(clave, t)
  }
  return t
}

function elegirEncuadre(atlas: Atlas, continente: Continente, vista: Region, w: number, h: number, hueco: Hueco): Transformacion {
  const entero = Transformacion.ajustada(vista, w, h)
  if (hueco.w < 1 || hueco.h < 1 || w < 2 || h < 2) return entero
  const rw = hueco.w + 2 * MARGEN_CONTROLES, rh = hueco.h + 2 * MARGEN_CONTROLES
  // Del tamaño más grande al más pequeño: en cada uno, el continente se corre lo justo hacia la derecha y hacia
  // arriba (sin salirse del lienzo) hasta que el rincón quede en el mar. África, que se angosta al sur, deja el
  // Atlántico a la izquierda casi sin achicarse.
  for (let f = 1; f >= 0.5; f -= 0.04) {
    const escala = entero.escala * f
    const sobraX = w - vista.ancho * escala, sobraY = h - vista.alto * escala
    for (let paso = 0; paso <= 8; paso++) {
      for (let i = Math.max(0, paso - 4); i <= Math.min(paso, 4); i++) {
        const x0 = (sobraX / 2) * (1 + i / 4), y0 = (sobraY / 2) * (1 - (paso - i) / 4)
        const t = new Transformacion(vista.uMin - (x0 - w / 2) / escala, vista.vMin - (y0 - h / 2) / escala, escala, w, h)
        if (!tapaTierra(atlas, continente, t, rw, rh)) return t
      }
    }
  }
  // Si no hay manera, el continente arriba del rincón.
  return metida(vista, w, h - rh, w, h, 0)
}

/** La vista, lo más grande posible dentro del rectángulo (x0, 0)–(x0 + aw, ah) de un lienzo de w × h. */
function metida(vista: Region, aw: number, ah: number, w: number, h: number, x0: number): Transformacion {
  const escala = Math.max(0, Math.min(aw / vista.ancho, ah / vista.alto))
  if (escala === 0) return new Transformacion(vista.centroU, vista.centroV, 0, w, h)
  return new Transformacion(vista.centroU - (x0 + aw / 2 - w / 2) / escala, vista.centroV - (ah / 2 - h / 2) / escala, escala, w, h)
}

/**
 * Si algún país del continente queda en el rincón de abajo a la izquierda de rw × rh píxeles. Las islitas de los
 * países continentales (Galápagos, Canarias, Hawái) no cuentan; los países isla (Cabo Verde), sí.
 */
export function tapaTierra(atlas: Atlas, continente: Continente, t: Transformacion, rw: number, rh: number): boolean {
  const y0 = t.alto - rh
  const visible = t.visible()
  for (const p of atlas.paises) {
    if (p.continente !== continente) continue
    const f = p.forma
    const du = visible.vueltasPara(f.anclaU)
    const isla = atlas.islas.has(p)
    for (const a of f.anillos) {
      const c = a.caja
      if (t.x(c.uMin + du) > rw || t.x(c.uMax + du) < 0 || t.y(c.vMax) < y0 || t.y(c.vMin) > t.alto) continue
      if (!isla && Math.max(c.ancho, c.alto) * t.escala < 10) continue
      for (let i = 0; i < a.puntos; i++) {
        const x = t.x(a.u[i] + du), y = t.y(a.v[i])
        if (x >= 0 && x <= rw && y >= y0 && y <= t.alto) return true
      }
    }
    // Un país grande puede cubrir todo el rincón sin que su borde pase por él.
    if (f.contiene(t.u(rw / 2) - du, t.v(y0 + rh / 2))) return true
  }
  return false
}
