import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { beforeAll, describe, expect, it } from 'vitest'
import { encuadre, tapaTierra, type Hueco } from './encuadre'
import { Transformacion } from './geo'
import { Atlas, CONTINENTES } from './modelo'
import { vistaDelMapa } from './partida'

const recursos = join(__dirname, '..', '..', 'src', 'main', 'resources', 'com', 'mapamundi')
let atlas: Atlas

beforeAll(() => {
  atlas = Atlas.desdeJson(JSON.parse(readFileSync(join(recursos, 'datos', 'paises.json'), 'utf8')))
})

/** Mapas de teléfono (ancho × alto del mapa) y el rincón que ocupan la cruceta y «Soltar» en cada uno. */
const CASOS: { nombre: string; w: number; h: number; hueco: Hueco }[] = [
  { nombre: 'vertical, datos ocultos', w: 376, h: 560, hueco: { w: 140, h: 194 } },
  { nombre: 'vertical, datos abiertos', w: 376, h: 400, hueco: { w: 140, h: 194 } },
  { nombre: 'horizontal', w: 420, h: 280, hueco: { w: 106, h: 149 } },
]

describe('encuadre del mapa del continente', () => {
  for (const c of CASOS) {
    it(`${c.nombre}: la tierra del continente no queda bajo la cruceta y el continente se ve grande`, () => {
      for (const continente of CONTINENTES) {
        // Un país cualquiera del continente: la vista es la del continente con su margen.
        const pais = atlas.paises.find((p) => p.continente === continente)!
        const vista = vistaDelMapa(pais)
        const t = encuadre(atlas, continente, vista, c.w, c.h, c.hueco)
        const entero = Transformacion.ajustada(vista, c.w, c.h)
        const rw = c.hueco.w + 20, rh = c.hueco.h + 20
        expect(tapaTierra(atlas, continente, t, rw, rh), `${continente} tapado`).toBe(false)
        // La vista entera sigue dentro del mapa: la pieza nunca sale de lo que se ve.
        expect(t.x(vista.uMin)).toBeGreaterThanOrEqual(-0.5)
        expect(t.x(vista.uMax)).toBeLessThanOrEqual(c.w + 0.5)
        expect(t.y(vista.vMin)).toBeGreaterThanOrEqual(-0.5)
        expect(t.y(vista.vMax)).toBeLessThanOrEqual(c.h + 0.5)
        // Se corre antes que achicarse: nunca queda a menos del 70 % del tamaño máximo (casi siempre, al 100 %).
        expect(t.escala / entero.escala, `${continente} demasiado pequeño`).toBeGreaterThanOrEqual(0.7)
      }
    })
  }
})
