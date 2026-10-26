// Lo que el jugador elige y se recuerda entre partidas (en el navegador, con localStorage).
import type { Idioma } from './i18n'

export interface Preferencias {
  idioma: Idioma
  musica: boolean
  efectos: boolean
  daltonico: boolean
  /** Los datos del país en el panel de la partida (se pueden ocultar con un botón). */
  datosDelPais: boolean
  /** Si ya se hizo la partida de práctica de la primera vez. */
  practicaHecha: boolean
}

const CLAVE = 'mapamundi-sencillo.preferencias'
const PORDEFECTO: Preferencias = { idioma: 'es', musica: true, efectos: true, daltonico: false, datosDelPais: true, practicaHecha: false }

export function cargarPreferencias(): Preferencias {
  try {
    return { ...PORDEFECTO, ...JSON.parse(localStorage.getItem(CLAVE) ?? '{}') }
  } catch {
    return { ...PORDEFECTO }
  }
}

export function guardarPreferencias(p: Preferencias): void {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(p))
  } catch {
    // Navegación privada o almacenamiento lleno: se juega igual, sin recordar.
  }
}

/** Un puntaje de la tabla de récords (las 10 mejores partidas). */
export interface Entrada {
  nombre: string
  puntaje: number
  nivel: string
  fecha: string
}

const CLAVE_RECORDS = 'mapamundi-sencillo.records'
export const MAX_RECORDS = 10

export function leerRecords(): Entrada[] {
  try {
    const lista = JSON.parse(localStorage.getItem(CLAVE_RECORDS) ?? '[]') as Entrada[]
    return Array.isArray(lista) ? lista : []
  } catch {
    return []
  }
}

export function entraEnLaTabla(puntaje: number): boolean {
  if (puntaje <= 0) return false
  const lista = leerRecords()
  return lista.length < MAX_RECORDS || puntaje > lista[lista.length - 1].puntaje
}

export function guardarRecord(e: Entrada): Entrada[] {
  const lista = [...leerRecords(), e].sort((a, b) => b.puntaje - a.puntaje).slice(0, MAX_RECORDS)
  try {
    localStorage.setItem(CLAVE_RECORDS, JSON.stringify(lista))
  } catch {
    // Sin almacenamiento: el récord se ve en esta pantalla, pero no se guarda.
  }
  return lista
}
