// La partida (PantallaClasica + MapaClasico de Windows): arriba el panel con puntaje, errores y la barra de
// tiempo; en el centro, el país que cae sobre los cinco continentes o el mapa del continente; a la derecha (o
// debajo), los datos del país. Una cruceta y el botón «Soltar», sobre el mapa, guían la pieza.
import { useCallback, useEffect, useLayoutEffect, useReducer, useRef, useState } from 'react'
import { Cifras, FichaDatos, MiniMapa, Pieza, useTamano, zona } from '../comunes'
import { Transformacion, type Anillo, type Forma, type Region } from '../geo'
import { entero, t } from '../i18n'
import { CONTINENTES, VISTAS, muestraForma, nombreContinente, nombreModo, porCapital, type Atlas, type Continente, type Pais } from '../modelo'
import { ERRORES_MAX, PartidaClasica, RITMO, vistaDelMapa, type Direccion } from '../partida'
import { COLOR, colorDe, figura, forma as pintarForma, mapa as pintarMapa, preparar, rectanguloRedondeado, type Relleno } from '../pintor'
import type { Preferencias } from '../preferencias'
import type { Sonidos } from '../sonidos'
import { mantenerPantallaEncendida, vibrar } from '../movil'

/** A la deriva la pieza va más despacio que en el original; con una flecha pulsada, bastante más rápido. */
const VELOCIDAD_DERIVA = 0.6
const VELOCIDAD_CON_FLECHA = 3.2
const VELOCIDAD_CTRL = RITMO.movimiento / 30
/** Tamaño mínimo de la pieza en el mapa: solo los microestados, que serían un punto, se amplían hasta esto. */
const LADO_MINIMO = 12
/**
 * Las islas diminutas (Tonga, Maldivas, Cabo Verde…) se amplían algo más, en el mapa y en la pieza; las que tienen
 * tierra muy cerca (las Antillas Menores), solo hasta donde no se tapan con ella.
 */
const LADO_MINIMO_ISLA = 18
const LADO_CIRCULO = 22
/** Separación de la cruceta y «Soltar» respecto del borde del mapa (como .controles-mapa en estilos.css). */
const MARGEN_CONTROLES = 10
/** Lado con que se ve una isla en el mapa, a esta escala (píxeles por unidad Mercator). */
const ladoIsla = (atlas: Atlas, p: Pais, escala: number): number => Math.min(LADO_MINIMO_ISLA, 0.9 * atlas.separacion(p) * escala)
/** Lado mínimo de la pieza (y del país al revelarlo): las islas, como en el mapa, pero nunca menos de LADO_MINIMO. */
const ladoMinimo = (atlas: Atlas, p: Pais, escala: number): number => (atlas.islas.has(p) ? Math.max(LADO_MINIMO, ladoIsla(atlas, p, escala)) : LADO_MINIMO)
const TECLAS_DIRECCION: Record<string, Direccion> = { ArrowUp: 'ARRIBA', ArrowDown: 'ABAJO', ArrowLeft: 'IZQUIERDA', ArrowRight: 'DERECHA' }
const tactil = typeof window !== 'undefined' && window.matchMedia?.('(pointer: coarse)').matches

interface Props {
  atlas: Atlas
  partida: PartidaClasica
  prefs: Preferencias
  sonidos: Sonidos
  /** Partida de práctica guiada (globo con instrucciones, flecha y círculo de ayuda). */
  practica: boolean
  onPrefs: (p: Preferencias) => void
  onSalir: () => void
  onFin: (p: PartidaClasica) => void
}

export function PantallaJuego({ atlas, partida, prefs, sonidos, practica, onPrefs, onSalir, onFin }: Props) {
  const [, refrescar] = useReducer((n: number) => n + 1, 0)
  const velocidad = useRef(VELOCIDAD_DERIVA)
  const [seleccion, setSeleccion] = useState(0)
  const mapaRef = useRef<{ linea: number[] | null }>({ linea: null })
  const [mensajeVisible, setMensajeVisible] = useState(false)
  // La cruceta siempre está en la página (invisible fuera del mapa) para medir el rincón que ocupa.
  const controles = useRef<HTMLDivElement>(null)
  const hueco = useCallback((): Hueco => ({ w: controles.current?.offsetWidth ?? 0, h: controles.current?.offsetHeight ?? 0 }), [])
  // Una vez medida la cruceta, se vuelve a dibujar el país que cae con su tamaño en el mapa.
  useLayoutEffect(() => refrescar(), [])

  // ------------------------------------------------------------------ relojes y avisos de la partida
  // En una referencia, para que los relojes no se reinicien si la pantalla de arriba pasa otra función.
  const alTerminar = useRef(onFin)
  alTerminar.current = onFin
  useEffect(() => {
    partida.alSonar = (s) => {
      sonidos.reproducir(s)
      // En el teléfono, además, una vibración corta al acertar y al fallar (si los efectos están activados).
      if (!sonidos.efectosActivos) return
      if (s === 'UBICADO' || s === 'CONTINENTE_CORRECTO' || s === 'NIVEL_SUPERADO') vibrar(35)
      else if (s === 'MAL_UBICADO' || s === 'CONTINENTE_EQUIVOCADO' || s === 'FIN_TIEMPO') vibrar([70, 50, 70])
      else if (s === 'FIN_PARTIDA') vibrar(300)
    }
    partida.alCambiar = () => {
      if (partida.fase === 'FIN') alTerminar.current(partida)
      else refrescar()
    }
    const caida = setInterval(() => partida.ticCaida(), RITMO.caida)
    const reloj = setInterval(() => partida.ticReloj(), RITMO.reloj)
    let movimiento = 0
    const mover = () => {
      partida.ticMovimiento()
      movimiento = window.setTimeout(mover, RITMO.movimiento / velocidad.current)
    }
    movimiento = window.setTimeout(mover, RITMO.movimiento / velocidad.current)
    return () => {
      clearInterval(caida)
      clearInterval(reloj)
      clearTimeout(movimiento)
      partida.alCambiar = () => {}
      partida.alSonar = () => {}
    }
  }, [partida, sonidos])

  // La pantalla no se apaga mientras dura la partida.
  useEffect(() => mantenerPantallaEncendida(), [])

  // Si el jugador sale de la aplicación (otra app, una llamada, pantalla apagada), la partida se pausa sola.
  useEffect(() => {
    const oculta = () => {
      if (document.hidden && !partida.pausa && partida.fase !== 'RESULTADO' && partida.fase !== 'FIN') partida.alternarPausa()
    }
    document.addEventListener('visibilitychange', oculta)
    return () => document.removeEventListener('visibilitychange', oculta)
  }, [partida])

  const r = partida.resultado
  const fase = partida.fase
  // Al fallar en el mapa (y si se revela el lugar) primero se ve la línea hasta la ★ y, un momento después, el recuadro.
  const marcaElLugar = fase === 'RESULTADO' && !!r && !r.acierto && r.distanciaKm >= 0 && partida.revelaElLugar
  useEffect(() => {
    if (fase !== 'RESULTADO') {
      setMensajeVisible(false)
      return
    }
    if (!marcaElLugar) {
      setMensajeVisible(true)
      return
    }
    setMensajeVisible(false)
    const espera = setTimeout(() => setMensajeVisible(true), 1800)
    return () => clearTimeout(espera)
  }, [fase, r, marcaElLugar])

  // ------------------------------------------------------------------ teclado
  const elegir = useCallback((c: Continente) => partida.elegirContinente(c), [partida])
  const primeraElegible = useCallback(
    (desde: number, paso: number) => {
      const n = CONTINENTES.length
      for (let i = 0; i < n; i++) {
        const c = (((desde + i * paso) % n) + n) % n
        if (partida.elegible(CONTINENTES[c])) return c
      }
      return desde
    },
    [partida],
  )

  useEffect(() => {
    const abajo = (e: KeyboardEvent) => {
      if (e.key === 'F5' || e.key === 'p' || e.key === 'P' || e.key === 'Escape') {
        partida.alternarPausa()
        e.preventDefault()
        return
      }
      if (partida.pausa) return
      if (partida.fase === 'CAIDA') {
        if (e.key === 'ArrowRight' || e.key === 'ArrowDown') setSeleccion((s) => primeraElegible(s + 1, 1))
        else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') setSeleccion((s) => primeraElegible(s - 1, -1))
        else if (e.key === 'Enter' || e.key === ' ') elegir(CONTINENTES[seleccion])
        else if (/^[1-5]$/.test(e.key)) elegir(CONTINENTES[Number(e.key) - 1])
        else return
        e.preventDefault()
      } else if (partida.fase === 'UBICACION') {
        const d = TECLAS_DIRECCION[e.key]
        if (d) {
          velocidad.current = e.ctrlKey ? VELOCIDAD_CTRL : VELOCIDAD_CON_FLECHA
          partida.cambiarDireccion(d)
        } else if (e.key === 'Enter' || e.key === ' ') partida.confirmar()
        else return
        e.preventDefault()
      } else if (partida.fase === 'RESULTADO' && (e.key === 'Enter' || e.key === ' ') && mensajeVisible) {
        partida.siguiente()
        e.preventDefault()
      }
    }
    const arriba = (e: KeyboardEvent) => {
      if (TECLAS_DIRECCION[e.key] || e.key === 'Control') velocidad.current = VELOCIDAD_DERIVA
    }
    window.addEventListener('keydown', abajo)
    window.addEventListener('keyup', arriba)
    return () => {
      window.removeEventListener('keydown', abajo)
      window.removeEventListener('keyup', arriba)
    }
  }, [partida, seleccion, elegir, primeraElegible, mensajeVisible])

  // Al empezar cada caída, la selección del teclado va al primer continente que se puede elegir.
  useEffect(() => {
    if (fase === 'CAIDA') setSeleccion((s) => primeraElegible(s, 1))
  }, [fase, partida.actual, primeraElegible])

  // ------------------------------------------------------------------ cruceta táctil
  const pulsarFlecha = (d: Direccion) => {
    velocidad.current = VELOCIDAD_CON_FLECHA
    partida.cambiarDireccion(d)
  }
  const soltarFlecha = () => {
    velocidad.current = VELOCIDAD_DERIVA
  }

  // El resultado de un intento en el mapa se sigue viendo sobre el mapa; el de la caída, sobre la caída.
  const enMapa = fase === 'UBICACION' || (fase === 'RESULTADO' && (r?.motivo === 'LEJOS' || r?.motivo === 'TIEMPO_MAPA' || r?.motivo === 'UBICADO'))
  const progreso = enMapa ? 1 - partida.reloj : 1 - partida.caida

  return (
    <main className="pantalla pantalla-juego">
      <Hud partida={partida} prefs={prefs} onPrefs={onPrefs} progreso={progreso} enMapa={enMapa} />
      <div className="cuerpo-juego">
        <div className="centro-juego">
          {enMapa ? (
            <MapaJuego atlas={atlas} partida={partida} guia={practica} hueco={hueco} ref={mapaRef} />
          ) : (
            <VistaCaida atlas={atlas} partida={partida} seleccion={seleccion} practica={practica} hueco={hueco} onElegir={elegir} />
          )}
          {fase === 'RESULTADO' && r && mensajeVisible && (
            <Mensaje partida={partida} practica={practica} junto={marcaElLugar} linea={mapaRef.current.linea} />
          )}
          {/* La cruceta y «Soltar», sobre el mapa (abajo a la izquierda, en el mar): así el mapa ocupa todo el ancho.
              Fuera del mapa quedan invisibles, pero en su sitio, para medir el rincón que ocupan. */}
          <div className={`controles-mapa${fase === 'UBICACION' && !partida.pausa ? '' : ' invisible'}`} ref={controles} aria-hidden={fase !== 'UBICACION' || partida.pausa}>
            <div className="cruceta">
              {(['ARRIBA', 'IZQUIERDA', 'DERECHA', 'ABAJO'] as Direccion[]).map((d) => (
                <button
                  key={d}
                  className={`flecha flecha-${d.toLowerCase()}`}
                  aria-label={d}
                  onPointerDown={(e) => {
                    e.preventDefault()
                    pulsarFlecha(d)
                  }}
                  onPointerUp={soltarFlecha}
                  onPointerLeave={soltarFlecha}
                  onPointerCancel={soltarFlecha}
                >
                  {{ ARRIBA: '▲', IZQUIERDA: '◀', DERECHA: '▶', ABAJO: '▼' }[d]}
                </button>
              ))}
            </div>
            <button className="boton-principal boton-soltar" onClick={() => partida.confirmar()}>
              {t('web.soltar')}
            </button>
          </div>
        </div>
        <aside className="lateral">
          <Lateral atlas={atlas} partida={partida} prefs={prefs} practica={practica} onPrefs={onPrefs} />
          {practica && fase !== 'RESULTADO' && <GuiaPractica partida={partida} onSaltar={onSalir} />}
        </aside>
      </div>
      {partida.pausa && (
        <div className="capa-pausa">
          <div className="panel caja-pausa">
            <h2 className="titulo-grande">{t('pausa.titulo')}</h2>
            <p>{t('pausa.detenido')}</p>
            <button className="boton-principal" onClick={() => partida.alternarPausa()}>
              {t('pausa.continuar')}
            </button>
            <button className="boton-secundario" onClick={onSalir}>
              {t('pausa.salir')}
            </button>
          </div>
        </div>
      )}
    </main>
  )
}

// ====================================================================== panel superior

function Hud({ partida, prefs, onPrefs, progreso, enMapa }: { partida: PartidaClasica; prefs: Preferencias; onPrefs: (p: Preferencias) => void; progreso: number; enMapa: boolean }) {
  const sonido = prefs.musica || prefs.efectos
  return (
    <header className="hud">
      <div className="hud-fila">
        <span className="titulo-hud">Mapamundi</span>
        <span className="insignia-zona">{zona([...partida.enJuego])}</span>
        <span className="hud-par">
          <span className="hud-clave">{t('hud.puntaje')}</span> <span className="hud-puntaje">{String(partida.puntaje).padStart(4, '0')}</span>
        </span>
        <span className="hud-par">
          <span className="hud-clave">{t('hud.errores')}</span>{' '}
          <span className={`hud-valor${partida.errores >= ERRORES_MAX - 1 ? ' peligro' : ''}`}>
            {partida.errores}/{ERRORES_MAX}
          </span>
        </span>
        <span className="hud-par oculto-pequeno">
          <span className="hud-clave">{t('hud.avance')}</span>{' '}
          <span className="hud-valor">
            {partida.ubicados.size}/{partida.totalPaises}
          </span>
        </span>
        <span className="hud-hueco" />
        <span className="hud-modo oculto-pequeno">{nombreModo(partida.ajustes.modo)}</span>
        <button className="boton-hud" onClick={() => onPrefs({ ...prefs, musica: !sonido, efectos: !sonido })} aria-label="sonido">
          {sonido ? '🔊' : '🔇'}
        </button>
        <button className="boton-hud" onClick={() => partida.alternarPausa()}>
          {tactil ? '⏸' : '⏸ F5'}
        </button>
      </div>
      <div className="barra-tiempo">
        <div className={`barra-relleno${progreso <= 0.25 ? ' poco' : ''}`} style={{ width: `${Math.max(0, progreso) * 100}%` }} />
        <span className="barra-texto">{t(enMapa ? 'clasico.tiempoMapa' : 'clasico.tiempoCaida')}</span>
      </div>
    </header>
  )
}

// ====================================================================== la caída

function VistaCaida({ atlas, partida, seleccion, practica, hueco, onElegir }: { atlas: Atlas; partida: PartidaClasica; seleccion: number; practica: boolean; hueco: () => Hueco; onElegir: (c: Continente) => void }) {
  const [cielo, tam] = useTamano<HTMLDivElement>()
  const [vista, tamVista] = useTamano<HTMLDivElement>()
  const p = partida.actual
  const modo = partida.ajustes.modo
  const rotulo = modo === 'DATOS' ? null : porCapital(modo) ? p.capital : p.nombre
  let altoPieza = Math.min(110, Math.max(50, tam.h * 0.42))
  let anchoPieza = altoPieza * 1.35
  if (muestraForma(modo) && tamVista.w > 1) {
    // La silueta cae exactamente del tamaño que tendrá en el mapa del continente (que ocupa este mismo espacio,
    // menos su borde): los países grandes, grandes, y los pequeños, pequeños, ampliados igual que en el mapa.
    const caja = p.forma.cajaNucleo
    const escala = encuadre(atlas, p.continente, vistaDelMapa(p), tamVista.w - 6, tamVista.h - 6, hueco()).escala
    const lado = Math.max(caja.ancho, caja.alto) * escala
    const minimo = ladoMinimo(atlas, p, escala)
    const k = Math.min(lado < minimo ? Math.min(40, minimo / Math.max(lado, 0.5)) : 1, (anchoPieza - 12) / (caja.ancho * escala), (altoPieza - 12) / (caja.alto * escala))
    anchoPieza = caja.ancho * escala * k + 12
    altoPieza = caja.alto * escala * k + 12
  }
  const top = 8 + partida.caida * Math.max(0, tam.h - altoPieza - 40)
  const resaltadores = useRef(CONTINENTES.map((c) => (x: Pais) => x.continente === c))
  return (
    <div className="vista-caida" ref={vista}>
      <div className="cielo" ref={cielo}>
        <div className="pieza-cayendo" style={{ top }}>
          {rotulo && <span className="rotulo-caida">{rotulo}</span>}
          <Pieza pais={p} forma={muestraForma(modo)} ancho={anchoPieza} alto={altoPieza} />
        </div>
      </div>
      <div className="fila-continentes">
        {CONTINENTES.map((c, i) => {
          const elegible = partida.elegible(c)
          return (
            <button
              key={c}
              className={`tarjeta-continente${i === seleccion && elegible ? ' seleccionada' : ''}${elegible ? '' : ' apagada'}`}
              disabled={!elegible}
              onClick={() => onElegir(c)}
            >
              <MiniMapa atlas={atlas} vista={VISTAS[c]} resaltar={resaltadores.current[i]} />
              <span className="tarjeta-continente-nombre">{nombreContinente(c)}</span>
              <span className="numero-tecla">{i + 1}</span>
              {partida.completos.has(c) && <span className="mapa-completo">{t('clasico.mapaCompleto')}</span>}
              {practica && c === p.continente && partida.fase === 'CAIDA' && <span className="flecha-practica">▼</span>}
            </button>
          )
        })}
      </div>
    </div>
  )
}

// ====================================================================== el mapa del continente

interface MapaRef {
  linea: number[] | null
}

/** El rincón (abajo a la izquierda) que ocupan la cruceta y «Soltar», en píxeles. */
interface Hueco {
  w: number
  h: number
}

const encuadres = new Map<string, Transformacion>()

/**
 * Dónde se dibuja el mapa del continente: lo más grande posible, salvo que la tierra del continente quede debajo
 * de la cruceta y «Soltar». Entonces el continente sube (o se corre a la derecha) y deja ese rincón al mar, como
 * el Pacífico en América. Lo usan el mapa y el país que cae, para que tenga el mismo tamaño.
 */
function encuadre(atlas: Atlas, continente: Continente, vista: Region, w: number, h: number, hueco: Hueco): Transformacion {
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
  const arriba = metida(vista, w, h - rh, w, h, 0)
  const derecha = metida(vista, w - rw, h, w, h, rw)
  const libres = [entero, arriba, derecha].filter((t) => t.escala > 0 && !tapaTierra(atlas, continente, t, rw, rh))
  if (libres.length > 0) return libres.reduce((a, b) => (b.escala > a.escala ? b : a))
  return arriba.escala >= derecha.escala ? arriba : derecha
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
function tapaTierra(atlas: Atlas, continente: Continente, t: Transformacion, rw: number, rh: number): boolean {
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

/**
 * El mapa del continente: solo ese continente en blanco (los ya ubicados, en color) y el resto del mundo en gris;
 * encima, la pieza, que se mueve sola. El fondo se dibuja una vez (y al cambiar de tamaño o de país ubicado); la
 * pieza, en cada cuadro.
 */
const MapaJuego = ({ atlas, partida, guia, hueco, ref }: { atlas: Atlas; partida: PartidaClasica; guia: boolean; hueco: () => Hueco; ref: React.RefObject<MapaRef> }) => {
  const [caja, tam] = useTamano<HTMLDivElement>()
  const canvas = useRef<HTMLCanvasElement>(null)
  const fondo = useRef<{ clave: string; lienzo: HTMLCanvasElement } | null>(null)

  useLayoutEffect(() => {
    const el = canvas.current
    if (!el || tam.w < 2 || tam.h < 2) return
    const gc = preparar(el, tam.w, tam.h)
    let cuadro = 0
    const dibujar = () => {
      cuadro = requestAnimationFrame(dibujar)
      const vista = partida.vista
      if (!vista) return
      const continente = partida.actual.continente
      const t = encuadre(atlas, continente, vista, tam.w, tam.h, hueco())
      // Fondo: mar, países (los del continente en blanco o, si ya están ubicados, en color) y fronteras.
      const clave = `${continente},${t.centroU},${t.centroV},${t.escala},${tam.w},${tam.h},${partida.ubicados.size}`
      if (fondo.current?.clave !== clave) {
        const lienzo = document.createElement('canvas')
        const g = preparar(lienzo, tam.w, tam.h)
        const relleno = (p: Pais) => (p.continente !== continente ? COLOR.RESTO_DEL_MUNDO : partida.ubicados.has(p) ? colorDe(p, g) : COLOR.TIERRA)
        pintarMapa(g, t, atlas, relleno, 0.9)
        // Las islas diminutas del continente, un poco ampliadas para que no sean un punto.
        const visible = t.visible()
        for (const p of atlas.islas) {
          if (p.continente === continente) formaVisible(g, t, p.forma, p.forma.nucleo, visible.vueltasPara(p.forma.anclaU), 0, relleno(p), 0.9, ladoIsla(atlas, p, t.escala))
        }
        fondo.current = { clave, lienzo }
      }
      gc.clearRect(0, 0, tam.w, tam.h)
      gc.drawImage(fondo.current.lienzo, 0, 0, tam.w, tam.h)
      ref.current.linea = dibujarEncima(gc, t, atlas, partida, guia)
    }
    dibujar()
    return () => cancelAnimationFrame(cuadro)
  }, [atlas, partida, guia, hueco, tam.w, tam.h, ref])

  return (
    <div className="mapa-juego" ref={caja}>
      <canvas ref={canvas} />
    </div>
  )
}

/** La pieza, el anillo de la práctica y, al fallar, el país real con la línea y la flecha que lo señala. Devuelve la línea. */
function dibujarEncima(gc: CanvasRenderingContext2D, t: Transformacion, atlas: Atlas, partida: PartidaClasica, guia: boolean): number[] | null {
  const actual = partida.actual
  const f = actual.forma
  const v = partida.vueltas
  const r = partida.resultado
  const resultado = partida.fase === 'RESULTADO'
  const acierto = resultado && !!r?.acierto
  const minimo = ladoMinimo(atlas, actual, t.escala)
  // En «Solo datos del país» el primer fallo no revela dónde estaba.
  const revelar = resultado && !acierto && partida.revelaElLugar
  let ladoReal = 0
  if (revelar) {
    gc.shadowColor = COLOR.SOL
    gc.shadowBlur = 10
    const k = formaVisible(gc, t, f, f.anillos, v, 0, colorDe(actual, gc), 1.6, minimo)
    gc.shadowBlur = 0
    ladoReal = Math.max(t.x(f.cajaNucleo.uMax) - t.x(f.cajaNucleo.uMin), t.y(f.cajaNucleo.vMax) - t.y(f.cajaNucleo.vMin)) * k
  }
  if (acierto) formaVisible(gc, t, f, f.nucleo, v, 0, colorDe(actual, gc), 1.4, minimo)
  if (guia && partida.fase === 'UBICACION') {
    const gx = t.x(f.anclaU + v), gy = t.y(f.anclaV)
    const radio = 26 + 6 * Math.sin(performance.now() / 160)
    gc.strokeStyle = COLOR.SOL
    gc.lineWidth = 4
    gc.setLineDash([10, 6])
    gc.beginPath()
    gc.arc(gx, gy, radio, 0, Math.PI * 2)
    gc.stroke()
    gc.setLineDash([])
  }
  if (!acierto) dibujarPieza(gc, t, partida, actual, f, v + partida.despU, partida.despV, resultado, minimo)
  if (revelar && r && r.distanciaKm >= 0) {
    const rx = t.x(f.anclaU + v), ry = t.y(f.anclaV)
    const gx = t.x(f.anclaU + v + partida.despU), gy = t.y(f.anclaV + partida.despV)
    // La flecha llega hasta el borde de los países pequeños (para no taparlos) y al centro de los grandes.
    let dx = rx - gx, dy = ry - gy
    let largo = Math.hypot(dx, dy)
    if (largo < 40) {
      // Si la pieza quedó casi encima, la flecha baja desde arriba.
      dx = 0
      dy = 1
      largo = 1
    }
    dx /= largo
    dy /= largo
    const separacion = ladoReal < 40 ? ladoReal / 2 + 3 : 0
    const px = rx - dx * separacion, py = ry - dy * separacion
    const desde = Math.hypot(rx - gx, ry - gy) < 40 ? [px, py - 46] : [gx, gy]
    gc.strokeStyle = COLOR.ROJO
    gc.lineWidth = 2.5
    gc.setLineDash([8, 6])
    gc.beginPath()
    gc.moveTo(desde[0], desde[1])
    gc.lineTo(px - dx * 14, py - dy * 14)
    gc.stroke()
    gc.setLineDash([])
    flecha(gc, px, py, dx, dy)
    return [desde[0], desde[1], px, py]
  }
  return null
}

/** Punta de flecha roja con la punta en (x, y), apuntando en la dirección (dx, dy) (de largo 1). */
function flecha(gc: CanvasRenderingContext2D, x: number, y: number, dx: number, dy: number): void {
  const largo = 18, ancho = 9
  const bx = x - dx * largo, by = y - dy * largo
  gc.beginPath()
  gc.moveTo(x, y)
  gc.lineTo(bx - dy * ancho, by + dx * ancho)
  gc.lineTo(bx + dy * ancho, by - dx * ancho)
  gc.closePath()
  gc.fillStyle = COLOR.ROJO
  gc.fill()
  gc.strokeStyle = COLOR.TINTA
  gc.lineWidth = 1.5
  gc.lineJoin = 'round'
  gc.stroke()
}

/** Dibuja los anillos desplazados; si quedarían más pequeños que minimo, los amplía alrededor de su referencia. */
function formaVisible(gc: CanvasRenderingContext2D, t: Transformacion, f: Forma, anillos: Anillo[], du: number, dv: number, relleno: Relleno, grosor: number, minimo: number): number {
  const c = f.cajaNucleo
  const lado = Math.max(Math.abs(t.x(c.uMax) - t.x(c.uMin)), Math.abs(t.y(c.vMax) - t.y(c.vMin)))
  const k = lado >= minimo ? 1 : Math.min(40, minimo / Math.max(lado, 0.5))
  const px = t.x(f.anclaU + du), py = t.y(f.anclaV + dv)
  gc.save()
  gc.translate(px, py)
  gc.scale(k, k)
  gc.translate(-px, -py)
  pintarForma(gc, anillos, du, dv, t, relleno, COLOR.BORDE, grosor / k)
  gc.restore()
  return k
}

function dibujarPieza(gc: CanvasRenderingContext2D, t: Transformacion, partida: PartidaClasica, pais: Pais, f: Forma, du: number, dv: number, quieta: boolean, minimo: number): void {
  const cx = t.x(f.anclaU + du), cy = t.y(f.anclaV + dv)
  const modo = partida.ajustes.modo
  let arriba: number
  gc.globalAlpha = quieta ? 0.6 : 1
  if (muestraForma(modo)) {
    gc.shadowColor = 'rgba(29, 43, 79, 0.45)'
    gc.shadowBlur = 4
    gc.shadowOffsetX = 2
    gc.shadowOffsetY = 3
    const k = formaVisible(gc, t, f, f.nucleo, du, dv, colorDe(pais, gc), 1.3, minimo)
    gc.shadowBlur = 0
    gc.shadowOffsetX = 0
    gc.shadowOffsetY = 0
    arriba = Math.min(cy - 10, cy - (cy - t.y(f.cajaNucleo.vMin + dv)) * k)
  } else {
    figura(gc, pais, cx, cy, LADO_CIRCULO, COLOR.TINTA, 2)
    arriba = cy - LADO_CIRCULO
  }
  gc.globalAlpha = 1
  if (quieta) return
  const rotulo = modo === 'DATOS' ? null : porCapital(modo) ? pais.capital : pais.nombre
  if (!rotulo) return
  gc.font = '800 14px Nunito, sans-serif'
  const ancho = gc.measureText(rotulo).width + 16
  const y = Math.max(4, arriba - 30)
  gc.fillStyle = 'rgba(29, 43, 79, 0.9)'
  rectanguloRedondeado(gc, cx - ancho / 2, y, ancho, 23, 10)
  gc.fill()
  gc.fillStyle = 'white'
  gc.textAlign = 'center'
  gc.textBaseline = 'middle'
  gc.fillText(rotulo, cx, y + 11.5)
}

// ====================================================================== panel lateral

/**
 * Debajo del mapa (o a su derecha), sin pregunta: el jugador entiende solo qué hacer. Solo «Ver datos del país»
 * (o los datos, en «Solo datos del país»); con los datos ocultos, el panel es pequeño y el mapa más grande.
 */
function Lateral({ atlas, partida, prefs, practica, onPrefs }: { atlas: Atlas; partida: PartidaClasica; prefs: Preferencias; practica: boolean; onPrefs: (p: Preferencias) => void }) {
  const p = partida.actual
  const modo = partida.ajustes.modo
  const r = partida.resultado
  let contenido = null
  if (modo === 'DATOS') {
    const revelado = partida.fase === 'RESULTADO' && !!r && !r.acierto && partida.revelaElLugar
    contenido = revelado ? <FichaDatos pais={p} conNombreYCapital fecha={atlas.datosActualizados} /> : partida.datosCompletos ? <FichaDatos pais={p} conNombreYCapital={false} /> : <Cifras pais={p} />
  } else if (!practica) {
    contenido = (
      <>
        <button className="boton-discreto" onClick={() => onPrefs({ ...prefs, datosDelPais: !prefs.datosDelPais })}>
          {t(prefs.datosDelPais ? 'clasico.datos.ocultar' : 'clasico.datos.ver')}
        </button>
        {prefs.datosDelPais && <FichaDatos pais={p} conNombreYCapital={!porCapital(modo)} fecha={atlas.datosActualizados} />}
      </>
    )
  }
  // En la práctica, el globo de la guía ya lo explica todo.
  return contenido && <div className="panel panel-lateral">{contenido}</div>
}

function GuiaPractica({ partida, onSaltar }: { partida: PartidaClasica; onSaltar: () => void }) {
  const p = partida.actual
  return (
    <div className="panel practica-globo">
      <span className="practica-paso">{t('practica.paso', partida.ubicados.size + 1, partida.totalPaises)}</span>
      <p className="practica-texto">{partida.fase === 'CAIDA' ? t('web.practica.caida', p.nombre, nombreContinente(p.continente)) : t('web.practica.mapa')}</p>
      <button className="boton-discreto" onClick={onSaltar}>
        {t('practica.saltar')}
      </button>
    </div>
  )
}

// ====================================================================== el mensaje de cada intento

/** El acierto, en medio; el fallo en el mapa, un recuadro compacto en la esquina del mapa que no tapa la línea. */
function Mensaje({ partida, practica, junto, linea }: { partida: PartidaClasica; practica: boolean; junto: boolean; linea: number[] | null }) {
  const r = partida.resultado!
  const p = partida.actual
  const caja = useRef<HTMLDivElement>(null)
  const [pos, setPos] = useState<{ left: number; top: number } | null>(null)

  useLayoutEffect(() => {
    if (!junto || !caja.current) return
    const contenedor = caja.current.parentElement!
    const w = contenedor.clientWidth, h = contenedor.clientHeight
    const bw = caja.current.offsetWidth, bh = caja.current.offsetHeight, m = 12
    const esquinas: [number, number][] = [[w - bw - m, h - bh - m], [m, h - bh - m], [w - bw - m, m], [m, m]]
    const toca = (x: number, y: number) => {
      if (!linea) return false
      for (let i = 0; i <= 60; i++) {
        const px = linea[0] + ((linea[2] - linea[0]) * i) / 60, py = linea[1] + ((linea[3] - linea[1]) * i) / 60
        if (px >= x - 12 && px <= x + bw + 12 && py >= y - 12 && py <= y + bh + 12) return true
      }
      return false
    }
    const elegida = esquinas.find(([x, y]) => !toca(x, y)) ?? esquinas[0]
    setPos({ left: Math.max(0, elegida[0]), top: Math.max(0, elegida[1]) })
  }, [junto, linea])

  const fin = partida.terminada
  const clave = fin ? (practica ? 'practica.terminar' : 'resultado.verFinal') : 'clasico.aceptar'
  const estilo = junto ? (pos ? { left: pos.left, top: pos.top } : { visibility: 'hidden' as const }) : undefined
  return (
    <div className={`panel mensaje${junto ? ' junto-a-la-linea' : ' centrado'}`} ref={caja} style={estilo}>
      <h2 className={r.acierto ? 'acierto' : 'fallo'}>
        {r.acierto ? '😃 ' : '😞 '}
        {t(`clasico.motivo.${r.motivo}`)}
      </h2>
      {(r.acierto || partida.revelaElLugar) && <p className="titulo-mediano">{p.nombre}</p>}
      {r.distanciaKm >= 0 && partida.revelaElLugar && <p className="negrita">{t('resultado.distancia', entero(Math.round(r.distanciaKm)))}</p>}
      {r.acierto && <p className="puntos-mensaje">+{r.puntos}</p>}
      {!r.acierto && partida.ajustes.modo === 'DATOS' && !partida.revelaElLugar && <p className="negrita">{t('clasico.masDatos')}</p>}
      {r.completado && !practica && <p className="acierto">{t('clasico.completado', nombreContinente(r.completado))}</p>}
      {!r.acierto && <p className="negrita">{fin ? t('clasico.sinErrores') : t('clasico.quedanErrores', ERRORES_MAX - partida.errores)}</p>}
      <button className="boton-principal" onClick={() => partida.siguiente()}>
        {t(clave)}
        {tactil ? '' : ' (Enter)'}
      </button>
    </div>
  )
}
