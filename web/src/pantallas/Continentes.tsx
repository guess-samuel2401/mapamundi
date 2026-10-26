// «Marca los continentes con los que quieres jugar»: casillas que empiezan vacías y, al final y aparte,
// «Todo el mundo». «Continuar» pasa a los retos cuando hay algo marcado.
import { useCallback, useEffect, useMemo, useState } from 'react'
import { BotonAprender, Cabecera, MiniMapa } from '../comunes'
import { Region } from '../geo'
import { t } from '../i18n'
import { CONTINENTES, VISTAS, nombreContinente, type Atlas, type Continente, type Pais } from '../modelo'

const MUNDO = Region.deGrados(-170, 190, -56, 78)
/** Índice de la última tarjeta, «Todo el mundo». */
const TODOS = CONTINENTES.length

interface Props {
  atlas: Atlas
  musica: boolean
  onMusica: () => void
  onContinuar: (zona: Continente[]) => void
  onAprender: () => void
}

export function PantallaContinentes({ atlas, musica, onMusica, onContinuar, onAprender }: Props) {
  const [marcadas, setMarcadas] = useState<Set<number>>(new Set())

  const alternar = useCallback((i: number) => {
    setMarcadas((antes) => {
      const s = new Set(antes)
      if (i === TODOS) {
        // «Todo el mundo» va aparte: al marcarlo se quitan los continentes sueltos.
        const estaba = s.has(TODOS)
        s.clear()
        if (!estaba) s.add(TODOS)
      } else {
        s.delete(TODOS)
        if (!s.delete(i)) s.add(i)
      }
      return s
    })
  }, [])

  const continuar = useCallback(() => {
    if (marcadas.size === 0) return
    onContinuar(marcadas.has(TODOS) ? [...CONTINENTES] : CONTINENTES.filter((_, i) => marcadas.has(i)))
  }, [marcadas, onContinuar])

  useEffect(() => {
    const tecla = (e: KeyboardEvent) => {
      const n = Number(e.key)
      if (n >= 1 && n <= 6) alternar(n - 1)
      else if (e.key === 'Enter') continuar()
    }
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [alternar, continuar])

  // Funciones estables para que los minimapas no se redibujen en cada clic.
  const resaltadores = useMemo(() => CONTINENTES.map((c) => (p: Pais) => p.continente === c), [])
  const todos = useCallback(() => true, [])

  const tarjeta = (i: number, nombre: string, n: number, vista: Region, resaltar: (p: Pais) => boolean) => (
    <button key={i} className={`tarjeta tarjeta-eleccion${marcadas.has(i) ? ' seleccionada' : ''}`} onClick={() => alternar(i)}>
      <span className="numero-tecla">{i + 1}</span>
      <span className={`casilla${marcadas.has(i) ? ' marcada' : ''}`}>✓</span>
      <MiniMapa atlas={atlas} vista={vista} resaltar={resaltar} />
      <span className="eleccion-nombre">{nombre}</span>
      <span className="eleccion-detalle">{t('menu.paises', n)}</span>
    </button>
  )

  return (
    <main className="pantalla">
      <Cabecera musica={musica} onMusica={onMusica} />
      <div className="centro-eleccion">
        <h1 className="titulo-claro">{t('eleccion.continente.titulo')}</h1>
        <div className="rejilla-continentes">
          {CONTINENTES.map((c, i) => tarjeta(i, nombreContinente(c), atlas.de(c).length, VISTAS[c], resaltadores[i]))}
          {tarjeta(TODOS, t('eleccion.mundo'), atlas.paises.length, MUNDO, todos)}
        </div>
        <button className="boton-principal boton-continuar" disabled={marcadas.size === 0} onClick={continuar}>
          {t('eleccion.continuar')} ›
        </button>
      </div>
      <BotonAprender onClick={onAprender} />
    </main>
  )
}
