// Dibuja el mapa en un <canvas> con los colores del juego (como Pintor.java y Miniatura.java de Windows).
import { Region, Transformacion, type Anillo, type Forma } from './geo'
import type { Atlas, Pais } from './modelo'

export const COLOR = {
  AGUA: '#74bebc',
  TIERRA: '#fffdf8',
  OTRA_TIERRA: '#e8e2d4',
  RESTO_DEL_MUNDO: '#e4e3dc',
  BORDE: '#2a3552',
  GRIS_SILUETA: '#ece8df',
  TINTA: '#1d2b4f',
  ROJO: '#e3263b',
  SOL: '#ffd23f',
  CORAL: '#ff8f6b',
}

/** Siete tonos cálidos, sin celestes que se confundan con el mar (el índice viene de MAPCOLOR7). */
const PALETA = ['#f4a99b', '#f3d27a', '#a9d38c', '#eda7c9', '#c4b0e6', '#f6b878', '#d2bf94']
/** Modo daltónico: paleta Okabe-Ito aclarada, más una trama distinta por índice. */
const PALETA_DALTONICO = ['#f5b94a', '#f2e86b', '#d9a0c4', '#ee8f5f', '#7aa6d8', '#c9ad84', '#b5cf7f']

let daltonico = false
const tramas: (CanvasPattern | null)[] = new Array(7).fill(null)

export function usarDaltonico(activo: boolean): void {
  daltonico = activo
}

export type Relleno = string | CanvasPattern

/** Relleno de un país: su color de la paleta o, en modo daltónico, color + trama. */
export function colorDe(p: Pais, gc: CanvasRenderingContext2D): Relleno {
  const i = (((p.color - 1) % 7) + 7) % 7
  if (!daltonico) return PALETA[i]
  if (!tramas[i]) tramas[i] = crearTrama(i, PALETA_DALTONICO[i], gc)
  return tramas[i] ?? PALETA_DALTONICO[i]
}

function crearTrama(indice: number, fondo: string, gc: CanvasRenderingContext2D): CanvasPattern | null {
  const L = 10
  const c = document.createElement('canvas')
  c.width = L
  c.height = L
  const g = c.getContext('2d')!
  g.fillStyle = fondo
  g.fillRect(0, 0, L, L)
  g.strokeStyle = 'rgba(40, 40, 60, 0.38)'
  g.fillStyle = g.strokeStyle
  g.lineWidth = 1.3
  const linea = (x1: number, y1: number, x2: number, y2: number) => {
    g.beginPath()
    g.moveTo(x1, y1)
    g.lineTo(x2, y2)
    g.stroke()
  }
  switch (indice % 7) {
    case 1:
      linea(-1, L + 1, L + 1, -1)
      linea(-1, 1, 1, -1)
      linea(L - 1, L + 1, L + 1, L - 1)
      break
    case 2:
      g.beginPath()
      g.arc(L / 2, L / 2, 1.6, 0, Math.PI * 2)
      g.fill()
      break
    case 3:
      linea(0, L / 2, L, L / 2)
      break
    case 4:
      linea(-1, -1, L + 1, L + 1)
      linea(L - 1, -1, L + 1, 1)
      linea(-1, L - 1, 1, L + 1)
      break
    case 5:
      linea(0, L / 2, L, L / 2)
      linea(L / 2, 0, L / 2, L)
      break
    case 6:
      linea(L / 2, 0, L / 2, L)
      break
  }
  return gc.createPattern(c, 'repeat')
}

/** Vueltas al mundo para que la forma caiga cerca de lo que se ve (cruces del antimeridiano). */
export const vueltas = (visible: Region, f: Forma): number => visible.vueltasPara(f.anclaU)

/** Rellena y contornea un grupo de anillos desplazados (du, dv). Los huecos se respetan (par-impar). */
export function forma(
  gc: CanvasRenderingContext2D,
  anillos: Anillo[],
  du: number,
  dv: number,
  t: Transformacion,
  relleno: Relleno | null,
  borde: string | null,
  grosor: number,
): void {
  const visible = t.visible()
  gc.beginPath()
  let alguno = false
  for (const a of anillos) {
    const c = a.caja
    if (!visible.seCruzaCon(new Region(c.uMin + du, c.vMin + dv, c.uMax + du, c.vMax + dv))) continue
    alguno = true
    gc.moveTo(t.x(a.u[0] + du), t.y(a.v[0] + dv))
    for (let i = 1; i < a.puntos; i++) gc.lineTo(t.x(a.u[i] + du), t.y(a.v[i] + dv))
    gc.closePath()
  }
  if (!alguno) return
  if (relleno) {
    gc.fillStyle = relleno
    gc.fill('evenodd')
  }
  if (borde && grosor > 0) {
    gc.strokeStyle = borde
    gc.lineWidth = grosor
    gc.lineJoin = 'round'
    gc.stroke()
  }
}

/** Agua, territorios no jugables y países; relleno decide el color de cada país (null = blanco). */
export function mapa(
  gc: CanvasRenderingContext2D,
  t: Transformacion,
  atlas: Atlas,
  relleno: (p: Pais) => Relleno | null,
  grosor: number,
): void {
  gc.fillStyle = COLOR.AGUA
  gc.fillRect(0, 0, t.ancho, t.alto)
  const visible = t.visible()
  for (const f of atlas.otrasTierras) forma(gc, f.anillos, vueltas(visible, f), 0, t, COLOR.OTRA_TIERRA, COLOR.BORDE, grosor * 0.7)
  for (const p of atlas.paises) {
    forma(gc, p.forma.anillos, vueltas(visible, p.forma), 0, t, relleno(p) ?? COLOR.TIERRA, COLOR.BORDE, grosor)
  }
}

/** En los modos sin forma la pieza es un círculo del color del país, para no delatar su silueta. */
export function figura(gc: CanvasRenderingContext2D, p: Pais, cx: number, cy: number, lado: number, borde: string | null, grosor: number): void {
  gc.beginPath()
  gc.arc(cx, cy, lado / 2, 0, Math.PI * 2)
  gc.fillStyle = colorDe(p, gc)
  gc.fill()
  if (borde && grosor > 0) {
    gc.strokeStyle = borde
    gc.lineWidth = grosor
    gc.stroke()
  }
}

/**
 * Rectángulo con esquinas redondeadas (como gc.roundRect, que no existe en iPhone con iOS 15 o anterior ni en
 * Chrome anterior a 2022).
 */
export function rectanguloRedondeado(gc: CanvasRenderingContext2D, x: number, y: number, ancho: number, alto: number, radio: number): void {
  const r = Math.min(radio, ancho / 2, alto / 2)
  gc.beginPath()
  gc.moveTo(x + r, y)
  gc.arcTo(x + ancho, y, x + ancho, y + alto, r)
  gc.arcTo(x + ancho, y + alto, x, y + alto, r)
  gc.arcTo(x, y + alto, x, y, r)
  gc.arcTo(x, y, x + ancho, y, r)
  gc.closePath()
}

/** Prepara un canvas para dibujar a la resolución real de la pantalla (nítido en móviles). */
export function preparar(canvas: HTMLCanvasElement, ancho: number, alto: number): CanvasRenderingContext2D {
  const dpr = Math.min(window.devicePixelRatio || 1, 3)
  canvas.width = Math.max(1, Math.round(ancho * dpr))
  canvas.height = Math.max(1, Math.round(alto * dpr))
  canvas.style.width = `${ancho}px`
  canvas.style.height = `${alto}px`
  const gc = canvas.getContext('2d')!
  gc.setTransform(dpr, 0, 0, dpr, 0, 0)
  return gc
}

/** Un continente con sus países en colores y el resto del mundo en gris, como los botones del original. */
export function dibujarContinente(canvas: HTMLCanvasElement, atlas: Atlas, vista: Region, resaltar: (p: Pais) => boolean, ancho: number, alto: number): void {
  const gc = preparar(canvas, ancho, alto)
  const t = Transformacion.cubriendo(vista, ancho, alto)
  mapa(gc, t, atlas, (p) => (resaltar(p) ? colorDe(p, gc) : COLOR.RESTO_DEL_MUNDO), 0.4)
}

/** Silueta gris del país, centrada en el recuadro. */
export function dibujarSilueta(canvas: HTMLCanvasElement, p: Pais, ancho: number, alto: number): void {
  const gc = preparar(canvas, ancho, alto)
  const m = 6
  const caja = p.forma.cajaNucleo
  const escala = Math.min((ancho - 2 * m) / caja.ancho, (alto - 2 * m) / caja.alto)
  const t = new Transformacion(caja.centroU, caja.centroV, escala, ancho, alto)
  forma(gc, p.forma.nucleo, 0, 0, t, COLOR.GRIS_SILUETA, COLOR.TINTA, 1.8)
}

/** El círculo de color que hace de pieza en los modos sin forma. */
export function dibujarCirculo(canvas: HTMLCanvasElement, p: Pais, lado: number): void {
  const gc = preparar(canvas, lado * 1.5, lado * 1.5)
  figura(gc, p, lado * 0.75, lado * 0.75, lado, COLOR.TINTA, 2.5)
}
