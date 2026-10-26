// «¿Con qué nivel o dificultad te gustaría jugar con …?»: los cinco modos, con un ejemplo de cómo se ve la pieza.
// Al elegir uno empieza la partida.
import { useEffect } from 'react'
import { BotonAprender, Cabecera, Pieza, zona } from '../comunes'
import { t } from '../i18n'
import { CONTINENTES, MODOS, muestraForma, nombreModo, porCapital, type Atlas, type Continente, type Modo } from '../modelo'
import { puntosPorPais } from '../partida'

/** Un país conocido de cada continente, para el ejemplo. */
const EJEMPLO: Record<Continente, string> = { AFRICA: 'EG', AMERICA: 'PE', EUROPA: 'FR', ASIA: 'JP', OCEANIA: 'AU' }

interface Props {
  atlas: Atlas
  zona: Continente[]
  musica: boolean
  onMusica: () => void
  onElegir: (m: Modo) => void
  onVolver: () => void
  onAprender: () => void
}

export function PantallaRetos({ atlas, zona: continentes, musica, onMusica, onElegir, onVolver, onAprender }: Props) {
  const mundo = continentes.length === CONTINENTES.length
  const ejemplo = atlas.pais(mundo ? 'PE' : EJEMPLO[CONTINENTES.find((c) => continentes.includes(c))!]) ?? atlas.paises[0]
  const titulo = mundo ? t('eleccion.reto.tituloMundo') : t('eleccion.reto.titulo', zona(continentes))

  useEffect(() => {
    const tecla = (e: KeyboardEvent) => {
      const n = Number(e.key)
      if (n >= 1 && n <= 5) onElegir(MODOS[n - 1])
      else if (e.key === 'Escape') onVolver()
    }
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [onElegir, onVolver])

  return (
    <main className="pantalla">
      <Cabecera volver={onVolver} musica={musica} onMusica={onMusica} />
      <div className="centro-eleccion">
        <h1 className="titulo-claro">{titulo}</h1>
        <div className="fila-retos">
          {MODOS.map((m, i) => {
            const rotulo = m === 'DATOS' ? '?' : porCapital(m) ? ejemplo.capital : ejemplo.nombre
            return (
              <button key={m} className="tarjeta tarjeta-reto" onClick={() => onElegir(m)}>
                <span className="numero-tecla">{i + 1}</span>
                <span className="eleccion-nombre">{nombreModo(m)}</span>
                <span className="rotulo-caida">{rotulo}</span>
                <Pieza pais={ejemplo} forma={muestraForma(m)} ancho={100} alto={70} />
                <span className="eleccion-detalle">
                  <span className="dificultad">{'●'.repeat(i + 1) + '○'.repeat(4 - i)}</span> +{puntosPorPais(m)} {t('eleccion.porPais')}
                </span>
              </button>
            )
          })}
        </div>
      </div>
      <BotonAprender onClick={onAprender} />
    </main>
  )
}
