// Textos en español e inglés: los mismos .properties que la versión de Windows (copiados a public/recursos),
// más unas pocas claves propias de la versión web (controles táctiles, instalar…).

export type Idioma = 'es' | 'en'

const diccionarios: Record<Idioma, Record<string, string>> = { es: {}, en: {} }
let idioma: Idioma = 'es'

/** Claves que solo usa la versión web. */
const PROPIAS: Record<Idioma, Record<string, string>> = {
  es: {
    'web.soltar': 'Soltar',
    'web.cargando': 'Cargando el mapa…',
    'web.ayuda.caida': 'Toca el continente donde está el país antes de que llegue abajo.',
    'web.ayuda.mapa': 'La pieza se mueve sola. Guíala con las flechas y pulsa Soltar cuando esté sobre su lugar.',
    'web.practica.caida': '¿En qué continente está {0}? Toca «{1}» antes de que llegue abajo.',
    'web.practica.mapa': 'La pieza se mueve sola. Guíala con las flechas hasta el círculo amarillo y pulsa Soltar.',
    'web.sinRecords': 'Aún no hay récords. ¡Juega una partida y sé el primero!',
    'web.cerrar': 'Cerrar',
    'web.pantallaCompleta': 'Pantalla completa',
    'web.instalar': 'Instalar',
    'web.instalar.titulo': 'Instalar Mapamundi en tu iPhone',
    'web.instalar.paso1': 'En Safari, toca el botón Compartir (el cuadrado con una flecha hacia arriba).',
    'web.instalar.paso2': 'Busca y elige «Añadir a pantalla de inicio».',
    'web.instalar.paso3': 'Toca «Añadir». Mapamundi aparecerá con su ícono, se abrirá a pantalla completa y funcionará también sin conexión.',
  },
  en: {
    'web.soltar': 'Drop',
    'web.cargando': 'Loading the map…',
    'web.ayuda.caida': 'Tap the continent the country is in before it reaches the bottom.',
    'web.ayuda.mapa': 'The piece moves by itself. Steer it with the arrows and press Drop when it is over its place.',
    'web.practica.caida': 'Which continent is {0} in? Tap "{1}" before it reaches the bottom.',
    'web.practica.mapa': 'The piece moves by itself. Steer it with the arrows to the yellow circle and press Drop.',
    'web.sinRecords': 'No records yet. Play a game and be the first!',
    'web.cerrar': 'Close',
    'web.pantallaCompleta': 'Full screen',
    'web.instalar': 'Install',
    'web.instalar.titulo': 'Install Mapamundi on your iPhone',
    'web.instalar.paso1': 'In Safari, tap the Share button (the square with an arrow pointing up).',
    'web.instalar.paso2': 'Find and choose "Add to Home Screen".',
    'web.instalar.paso3': 'Tap "Add". Mapamundi will appear with its icon, open full screen and work offline too.',
  },
}

/** Lee un .properties en UTF-8 (clave=valor, # comentarios), como lo hace la versión de Windows. */
export function leerProperties(texto: string): Record<string, string> {
  const d: Record<string, string> = {}
  for (const linea of texto.split(/\r?\n/)) {
    if (!linea.trim() || linea.trimStart().startsWith('#')) continue
    const i = linea.indexOf('=')
    if (i < 0) continue
    d[linea.slice(0, i).trim()] = linea.slice(i + 1)
  }
  return d
}

export async function cargarTextos(base: string): Promise<void> {
  for (const i of ['es', 'en'] as Idioma[]) {
    const r = await fetch(`${base}textos_${i}.properties`)
    diccionarios[i] = { ...leerProperties(await r.text()), ...PROPIAS[i] }
  }
}

/** Para las pruebas: carga los diccionarios ya leídos. */
export function usarDiccionarios(es: Record<string, string>, en: Record<string, string>): void {
  diccionarios.es = { ...es, ...PROPIAS.es }
  diccionarios.en = { ...en, ...PROPIAS.en }
}

export function usarIdioma(nuevo: Idioma): void {
  idioma = nuevo
  if (typeof document !== 'undefined') document.documentElement.lang = nuevo
}

export function idiomaActual(): Idioma {
  return idioma
}

export function ingles(): boolean {
  return idioma === 'en'
}

/** El texto de clave en el idioma actual, con {0}, {1}… sustituidos por los argumentos. */
export function t(clave: string, ...args: (string | number)[]): string {
  let texto = diccionarios[idioma][clave] ?? diccionarios.es[clave] ?? `¡${clave}!`
  args.forEach((a, i) => {
    texto = texto.split(`{${i}}`).join(String(a))
  })
  return texto
}

/** Un entero con el separador de miles del idioma (12.345 / 12,345). */
export function entero(n: number): string {
  // Como Java: en español también se agrupan los miles de 4 cifras (1.234), cosa que es-ES de Intl no hace.
  const ingles = Math.round(n).toLocaleString('en-US')
  return idioma === 'en' ? ingles : ingles.split(',').join('.')
}
