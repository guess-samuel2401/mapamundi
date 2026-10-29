// Ayudas para jugar en el teléfono: pantalla completa en horizontal, y saber si hay pantalla táctil.

/** Si el dispositivo se maneja con el dedo (teléfono o tableta). */
export const esTactil = (): boolean => typeof window !== 'undefined' && !!window.matchMedia?.('(pointer: coarse)').matches

/** Si el navegador deja poner la página en pantalla completa (en iPhone, Safari no lo permite). */
export const admitePantallaCompleta = (): boolean => typeof document !== 'undefined' && !!document.documentElement.requestFullscreen

export const enPantallaCompleta = (): boolean => !!document.fullscreenElement

/** Safari (iPhone) no tiene screen.orientation.lock: se trata como opcional. */
type OrientacionBloqueable = { lock?: (orientacion: OrientationLockType) => Promise<void> }

/**
 * En el teléfono: pantalla completa y, donde se puede (Android), fijar la orientación horizontal. Tiene que
 * llamarse desde un toque del jugador; si el navegador no lo permite, no pasa nada y el juego sigue igual.
 */
export async function pantallaCompletaHorizontal(): Promise<void> {
  if (!esTactil()) return
  try {
    if (admitePantallaCompleta() && !enPantallaCompleta()) await document.documentElement.requestFullscreen({ navigationUI: 'hide' })
  } catch {
    // Sin permiso o sin soporte: se juega en la ventana del navegador.
  }
  try {
    await (screen.orientation as OrientacionBloqueable | undefined)?.lock?.('landscape')
  } catch {
    // Solo funciona en pantalla completa y en algunos navegadores; si no, aparece el aviso de girar el teléfono.
  }
}

/** El botón de pantalla completa de la cabecera. */
export async function alternarPantallaCompleta(): Promise<void> {
  try {
    if (enPantallaCompleta()) {
      await document.exitFullscreen()
      return
    }
    await document.documentElement.requestFullscreen({ navigationUI: 'hide' })
    if (esTactil()) await (screen.orientation as OrientacionBloqueable | undefined)?.lock?.('landscape')
  } catch {
    // Sin soporte: no pasa nada.
  }
}
