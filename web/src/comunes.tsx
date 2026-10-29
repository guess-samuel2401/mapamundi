// Piezas que comparten varias pantallas: cabecera, récords, miniaturas de mapa y ficha del país.
import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react'
import { entero, t } from './i18n'
import { CONTINENTES, nombreContinente, type Atlas, type Continente, type Pais } from './modelo'
import { dibujarCirculo, dibujarContinente, dibujarSilueta } from './pintor'
import type { Region } from './geo'
import { leerRecords } from './preferencias'
import { admitePantallaCompleta, alternarPantallaCompleta } from './movil'

/** El tamaño de un elemento, que se actualiza al cambiar (girar el teléfono, cambiar la ventana). */
export function useTamano<T extends HTMLElement>(): [React.RefObject<T | null>, { w: number; h: number }] {
  const ref = useRef<T>(null)
  const [tam, setTam] = useState({ w: 0, h: 0 })
  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const medir = () => setTam({ w: Math.round(el.clientWidth), h: Math.round(el.clientHeight) })
    medir()
    const ro = new ResizeObserver(medir)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])
  return [ref, tam]
}

/** "Todo el mundo" o los continentes elegidos: "Europa", "África y Asia", "América, Europa y Asia". */
export function zona(continentes: Continente[]): string {
  if (continentes.length === 0 || continentes.length === CONTINENTES.length) return t('eleccion.mundo')
  const nombres = CONTINENTES.filter((c) => continentes.includes(c)).map(nombreContinente)
  if (nombres.length === 1) return nombres[0]
  return `${nombres.slice(0, -1).join(', ')} ${t('eleccion.y')} ${nombres[nombres.length - 1]}`
}

export function Logotipo({ grande }: { grande?: boolean }) {
  return <div className={grande ? 'logotipo grande' : 'logotipo'}>Mapamundi</div>
}

/** Arriba: el logotipo (y «Volver»), y a la derecha récords y música. */
export function Cabecera({ volver, musica, onMusica }: { volver?: () => void; musica: boolean; onMusica: () => void }) {
  const [records, setRecords] = useState(false)
  return (
    <header className="cabecera">
      <div className="cabecera-izquierda">
        <Logotipo />
        {volver && (
          <button className="boton-fantasma" onClick={volver}>
            ‹ {t('eleccion.volver')}
          </button>
        )}
      </div>
      <div className="cabecera-derecha">
        <button className="boton-fantasma" onClick={() => setRecords(true)}>
          🏆 {t('menu.records')}
        </button>
        <button className="boton-fantasma icono" onClick={onMusica} title={t('ajustes.musica')} aria-label={t('ajustes.musica')}>
          {musica ? '🎵' : '🔇'}
        </button>
        {admitePantallaCompleta() && (
          <button className="boton-fantasma icono" onClick={() => void alternarPantallaCompleta()} title={t('web.pantallaCompleta')} aria-label={t('web.pantallaCompleta')}>
            ⛶
          </button>
        )}
      </div>
      {records && <Records onCerrar={() => setRecords(false)} />}
    </header>
  )
}

export function Dialogo({ titulo, onCerrar, children }: { titulo: string; onCerrar: () => void; children: ReactNode }) {
  return (
    <div className="capa-dialogo" onClick={onCerrar}>
      <div className="panel dialogo" onClick={(e) => e.stopPropagation()}>
        <h2 className="titulo-dialogo">{titulo}</h2>
        {children}
        <button className="boton-principal" onClick={onCerrar}>
          {t('web.cerrar')}
        </button>
      </div>
    </div>
  )
}

function Records({ onCerrar }: { onCerrar: () => void }) {
  const lista = leerRecords()
  const medallas = ['🥇', '🥈', '🥉']
  return (
    <Dialogo titulo={t('menu.records.titulo')} onCerrar={onCerrar}>
      {lista.length === 0 ? (
        <p className="texto-suave">{t('web.sinRecords')}</p>
      ) : (
        <table className="tabla-records">
          <tbody>
            {lista.map((e, i) => (
              <tr key={i}>
                <td>{medallas[i] ?? i + 1}</td>
                <td className="negrita">{e.nombre}</td>
                <td>{e.nivel}</td>
                <td className="puntos">{entero(e.puntaje)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Dialogo>
  )
}

/** Un trozo de mapa (continente o mundo) que llena su recuadro, con los países resaltados en color. */
export function MiniMapa({ atlas, vista, resaltar }: { atlas: Atlas; vista: Region; resaltar: (p: Pais) => boolean }) {
  const [ref, tam] = useTamano<HTMLDivElement>()
  const canvas = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    if (canvas.current && tam.w > 0 && tam.h > 0) dibujarContinente(canvas.current, atlas, vista, resaltar, tam.w, tam.h)
  }, [atlas, vista, resaltar, tam.w, tam.h])
  return (
    <div className="minimapa" ref={ref}>
      <canvas ref={canvas} />
    </div>
  )
}

/** La silueta del país, o el círculo de color en los modos sin forma. */
export function Pieza({ pais, forma, ancho, alto }: { pais: Pais; forma: boolean; ancho: number; alto: number }) {
  const canvas = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    if (!canvas.current) return
    if (forma) dibujarSilueta(canvas.current, pais, ancho, alto)
    else dibujarCirculo(canvas.current, pais, Math.min(ancho, alto) * 0.4)
  }, [pais, forma, ancho, alto])
  return <canvas ref={canvas} className="pieza-canvas" />
}

/** Filas {clave, valor} en dos columnas, con la fuente de los datos al pie. */
function Ficha({ filas, fecha }: { filas: [string, string][]; fecha?: string }) {
  return (
    <div className="ficha">
      <dl>
        {filas
          .filter(([, v]) => v && v.trim())
          .map(([k, v]) => (
            <div key={k} className="ficha-fila">
              <dt>{k}:</dt>
              <dd>{v}</dd>
            </div>
          ))}
      </dl>
      {fecha && <p className="ficha-fuente">{t('ficha.fuente', fecha)}</p>}
    </div>
  )
}

/** La ficha del país (capital, superficie, población, idioma, moneda, PIB, religión y exportaciones). */
export function FichaDatos({ pais, conNombreYCapital, fecha }: { pais: Pais; conNombreYCapital: boolean; fecha?: string }) {
  const filas: [string, string][] = []
  if (conNombreYCapital) {
    filas.push([t('ficha.pais'), pais.nombre], [t('ficha.capital'), pais.capital])
  }
  filas.push(
    [t('ficha.superficie'), pais.superficie],
    [t('ficha.poblacion'), pais.poblacion],
    [t('ficha.idioma'), pais.idioma],
    [t('ficha.moneda'), pais.moneda],
    [t('ficha.pib'), pais.pibPerCapita],
    [t('ficha.religion'), pais.religion],
    [t('ficha.exporta'), pais.exporta],
  )
  return <Ficha filas={filas} fecha={fecha} />
}

/** «Solo datos del país», primer intento: sin capital, moneda ni idioma, que delatarían el país. */
export function Cifras({ pais }: { pais: Pais }) {
  return (
    <Ficha
      filas={[
        [t('ficha.superficie'), pais.superficie],
        [t('ficha.poblacion'), pais.poblacion],
        [t('ficha.pib'), pais.pibPerCapita],
        [t('ficha.religion'), pais.religion],
        [t('ficha.exporta'), pais.exporta],
      ]}
    />
  )
}

/** Abajo a la derecha, siempre a la vista: aprender jugando (la partida de práctica). */
export function BotonAprender({ onClick }: { onClick: () => void }) {
  return (
    <button className="boton-aprender" onClick={onClick}>
      🎓 {t('eleccion.aprende')}
    </button>
  )
}
