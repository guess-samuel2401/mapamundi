import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { beforeAll, describe, expect, it } from 'vitest'
import { Region } from './geo'
import { leerProperties, t, usarDiccionarios } from './i18n'
import { Atlas, CONTINENTES, type Continente, type Modo, type Pais } from './modelo'
import { ERRORES_MAX, MARGEN_KM, PartidaClasica, puntosPorPais } from './partida'

const recursos = join(__dirname, '..', '..', 'src', 'main', 'resources', 'com', 'mapamundi')
let atlas: Atlas

beforeAll(() => {
  atlas = Atlas.desdeJson(JSON.parse(readFileSync(join(recursos, 'datos', 'paises.json'), 'utf8')))
  usarDiccionarios(
    leerProperties(readFileSync(join(recursos, 'i18n', 'textos_es.properties'), 'utf8')),
    leerProperties(readFileSync(join(recursos, 'i18n', 'textos_en.properties'), 'utf8')),
  )
})

/** Azar fijo, para que las partidas de prueba sean siempre iguales. */
function azar(semilla: number): () => number {
  let s = semilla
  return () => {
    s = (s * 1103515245 + 12345) % 2147483648
    return s / 2147483648
  }
}

const nueva = (modo: Modo = 'MAPA_PAIS', continentes: Continente[] = []): PartidaClasica =>
  new PartidaClasica({ modo, continentes }, atlas.paises, azar(4))

const otro = (p: Pais): Continente => (p.continente === 'ASIA' ? 'EUROPA' : 'ASIA')

/** Pone la pieza exactamente en su lugar, como si el jugador la hubiera guiado. */
function guiar(p: PartidaClasica): void {
  p.despU = 0
  p.despV = 0
}

describe('datos', () => {
  it('carga los 197 países, cada uno con su continente', () => {
    expect(atlas.paises).toHaveLength(197)
    for (const p of atlas.paises) expect(CONTINENTES).toContain(p.continente)
  })

  it('cada país contiene su propio punto de referencia o está muy cerca', () => {
    const peru = atlas.pais('PE')!
    expect(peru.forma.contiene(peru.forma.anclaU, peru.forma.anclaV)).toBe(true)
  })

  it('los textos de español e inglés tienen las claves que usa el juego', () => {
    expect(t('continente.AFRICA')).toBe('África')
    expect(t('clasico.pregunta.caida', 'Perú')).toBe('¿En qué continente está Perú?')
  })
})

describe('partida clásica', () => {
  it('acertar el continente pasa al mapa y ubicar bien suma los puntos del modo', () => {
    const p = nueva('PAIS')
    p.elegirContinente(p.actual.continente)
    expect(p.fase).toBe('UBICACION')
    guiar(p)
    p.confirmar()
    expect(p.resultado?.acierto).toBe(true)
    expect(p.puntaje).toBe(puntosPorPais('PAIS'))
    expect(puntosPorPais('PAIS')).toBe(30)
  })

  it('equivocar el continente es un error y el mismo país vuelve a caer', () => {
    const p = nueva()
    const pais = p.actual
    p.elegirContinente(otro(pais))
    expect(p.errores).toBe(1)
    expect(p.resultado?.motivo).toBe('CONTINENTE_EQUIVOCADO')
    p.siguiente()
    expect(p.actual).toBe(pais)
    expect(p.fase).toBe('CAIDA')
  })

  it('dejar caer el país hasta abajo es un error', () => {
    const p = nueva()
    for (let i = 0; i < 100; i++) p.ticCaida()
    expect(p.resultado?.motivo).toBe('TIEMPO_CAIDA')
    expect(p.errores).toBe(1)
  })

  it(`con ${ERRORES_MAX} errores termina la partida`, () => {
    const p = nueva()
    for (let i = 0; i < ERRORES_MAX; i++) {
      p.elegirContinente(otro(p.actual))
      if (i < ERRORES_MAX - 1) p.siguiente()
    }
    expect(p.terminada).toBe(true)
    p.siguiente()
    expect(p.fase).toBe('FIN')
  })

  it(`soltar lejos (a más de ${MARGEN_KM} km y fuera del país) es un error`, () => {
    const p = nueva('MAPA_PAIS', ['AMERICA'])
    p.elegirContinente(p.actual.continente)
    p.despU = 0.08
    p.despV = 0.08
    p.confirmar()
    expect(p.resultado?.motivo).toBe('LEJOS')
    expect(p.resultado!.distanciaKm).toBeGreaterThan(MARGEN_KM)
  })

  it('la pieza rebota en los bordes y nunca sale del mapa', () => {
    const p = nueva()
    p.elegirContinente(p.actual.continente)
    p.cambiarDireccion('ARRIBA')
    const f = p.actual.forma
    for (let i = 0; i < 500; i++) {
      p.ticMovimiento()
      const v = f.anclaV + p.despV
      expect(v).toBeGreaterThanOrEqual(p.vista!.vMin)
      expect(v).toBeLessThanOrEqual(p.vista!.vMax)
    }
  })

  it('en pausa no avanza nada', () => {
    const p = nueva()
    p.alternarPausa()
    p.ticCaida()
    expect(p.caida).toBe(0)
  })

  it('solo se eligen los continentes de la partida y completar todos gana', () => {
    const una = new PartidaClasica({ modo: 'MAPA_PAIS', continentes: ['AMERICA'] }, [atlas.pais('PE')!, atlas.pais('FR')!], azar(1))
    expect(una.elegible('EUROPA')).toBe(false)
    expect(una.totalPaises).toBe(1)
    una.elegirContinente('AMERICA')
    guiar(una)
    una.confirmar()
    expect(una.resultado?.completado).toBe('AMERICA')
    expect(una.ganada).toBe(true)
  })

  it('el mapa tiene margen: Palaos no queda pegado al borde', () => {
    const palaos = atlas.pais('PW')!
    const p = new PartidaClasica({ modo: 'MAPA_PAIS', continentes: ['OCEANIA'] }, [palaos], azar(1))
    p.elegirContinente('OCEANIA')
    const v: Region = p.vista!
    expect(palaos.forma.anclaV - v.vMin).toBeGreaterThan(v.alto * 0.05)
  })

  it('en «Solo datos» el primer fallo da más datos y el segundo revela el lugar', () => {
    const p = nueva('DATOS')
    const pais = p.actual
    expect(p.datosCompletos).toBe(false)
    p.elegirContinente(otro(pais))
    expect(p.revelaElLugar).toBe(false)
    expect(p.datosCompletos).toBe(true)
    p.siguiente()
    expect(p.actual).toBe(pais)
    p.elegirContinente(otro(pais))
    expect(p.revelaElLugar).toBe(true)

    const normal = nueva('MAPA_PAIS')
    normal.elegirContinente(otro(normal.actual))
    expect(normal.revelaElLugar).toBe(true)
  })
})
