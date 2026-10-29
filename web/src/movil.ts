// Ayudas para jugar en el teléfono: pantalla completa en horizontal, y saber si hay pantalla táctil.

/** Si el dispositivo se maneja con el dedo (teléfono o tableta). */
export const esTactil = (): boolean => typeof window !== 'undefined' && !!window.matchMedia?.('(pointer: coarse)').matches

const agente = (): string => (typeof navigator === 'undefined' ? '' : navigator.userAgent)

export const esAndroid = (): boolean => /Android/i.test(agente())

/** iPhone o iPad (el iPad moderno se presenta como Mac, pero con pantalla táctil). */
export const esIOS = (): boolean =>
  /iPhone|iPad|iPod/.test(agente()) || (typeof navigator !== 'undefined' && navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)

/**
 * El navegador interno de WhatsApp, Facebook, Instagram, TikTok… (al tocar un enlace dentro de esas apps). Ahí
 * suelen fallar el sonido, la pantalla completa y la instalación, y los récords no siempre se guardan.
 */
export const navegadorInterno = (): boolean =>
  /FBAN|FBAV|FB_IAB|FBIOS|Instagram|Messenger|WhatsApp|Line\/|TikTok|musical_ly|Snapchat|Twitter/i.test(agente()) ||
  (esAndroid() && /; wv\)/.test(agente()))

/** En Android, un enlace que abre esta misma página en Chrome. */
export function enlaceChrome(): string {
  const u = new URL(location.href)
  return `intent://${u.host}${u.pathname}${u.search}#Intent;scheme=${u.protocol.replace(':', '')};package=com.android.chrome;end`
}

/** Si ya se está jugando desde la app instalada (pantalla de inicio). */
export const instalada = (): boolean =>
  typeof window !== 'undefined' &&
  (window.matchMedia?.('(display-mode: standalone)').matches ||
    window.matchMedia?.('(display-mode: fullscreen)').matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true)

// ------------------------------------------------------------------ pantalla encendida y vibración

type Bloqueo = { release: () => Promise<void> }
type ConBloqueo = Navigator & { wakeLock?: { request: (tipo: 'screen') => Promise<Bloqueo> } }

/**
 * Mantiene la pantalla encendida mientras dura la partida (al jugar no siempre se toca la pantalla, y el teléfono
 * la oscurecería). Devuelve la función que la libera. El teléfono la suelta solo al salir de la app; por eso se
 * vuelve a pedir al volver.
 */
export function mantenerPantallaEncendida(): () => void {
  const nav = navigator as ConBloqueo
  if (!nav.wakeLock) return () => {}
  let bloqueo: Bloqueo | null = null
  let activo = true
  const pedir = () => {
    if (!activo || document.hidden) return
    nav.wakeLock!.request('screen').then(
      (b) => {
        if (activo) bloqueo = b
        else void b.release()
      },
      () => {},
    )
  }
  const alVolver = () => {
    if (!document.hidden) pedir()
  }
  pedir()
  document.addEventListener('visibilitychange', alVolver)
  return () => {
    activo = false
    document.removeEventListener('visibilitychange', alVolver)
    void bloqueo?.release().catch(() => {})
  }
}

/** Una vibración corta (en Android; iPhone no deja vibrar desde una página). */
export function vibrar(patron: number | number[]): void {
  try {
    navigator.vibrate?.(patron)
  } catch {
    // Sin soporte: no pasa nada.
  }
}

// ------------------------------------------------------------------ instalar como app

/** El aviso de instalación de Chrome/Android, guardado para mostrarlo cuando el jugador toque «Instalar». */
type EventoInstalar = Event & { prompt: () => Promise<void> }
let eventoInstalar: EventoInstalar | null = null
const oyentes = new Set<() => void>()
const avisar = () => oyentes.forEach((f) => f())

/** Hay que llamarlo al arrancar: Chrome avisa una sola vez, enseguida, de que la página se puede instalar. */
export function escucharInstalacion(): void {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault()
    eventoInstalar = e as EventoInstalar
    avisar()
  })
  window.addEventListener('appinstalled', () => {
    eventoInstalar = null
    avisar()
  })
}

export function alCambiarInstalacion(f: () => void): () => void {
  oyentes.add(f)
  return () => {
    oyentes.delete(f)
  }
}

/** Si mostrar «Instalar»: Chrome lo ofrece, o es un iPhone/iPad con Safari (ahí se instala a mano). */
export const puedeInstalar = (): boolean => !instalada() && (eventoInstalar !== null || (esIOS() && !navegadorInterno()))

/** Si el navegador instala solo (Chrome, Edge, Android); si no, hay que explicar los pasos de Safari. */
export const instalacionDirecta = (): boolean => eventoInstalar !== null

export async function instalar(): Promise<void> {
  if (!eventoInstalar) return
  await eventoInstalar.prompt()
  eventoInstalar = null
  avisar()
}

/** Si el navegador deja poner la página en pantalla completa (en iPhone, Safari no lo permite). */
export const admitePantallaCompleta = (): boolean => typeof document !== 'undefined' && !!document.documentElement.requestFullscreen

export const enPantallaCompleta = (): boolean => !!document.fullscreenElement

/**
 * En el teléfono: pantalla completa (sin la barra del navegador). Tiene que llamarse desde un toque del jugador;
 * si el navegador no lo permite (Safari en iPhone), no pasa nada y el juego sigue igual. La orientación no se
 * fija: Mapamundi se juega en horizontal o en vertical.
 */
export async function pantallaCompletaEnElTelefono(): Promise<void> {
  if (!esTactil() || !admitePantallaCompleta() || enPantallaCompleta()) return
  try {
    await document.documentElement.requestFullscreen({ navigationUI: 'hide' })
  } catch {
    // Sin permiso o sin soporte: se juega en la ventana del navegador.
  }
}

/** El botón de pantalla completa de la cabecera. */
export async function alternarPantallaCompleta(): Promise<void> {
  try {
    if (enPantallaCompleta()) await document.exitFullscreen()
    else await document.documentElement.requestFullscreen({ navigationUI: 'hide' })
  } catch {
    // Sin soporte: no pasa nada.
  }
}
