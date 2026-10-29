// Lo primero: elegir entre español e inglés (los textos van en los dos idiomas), y el modo daltónico.
import { useEffect, useState } from 'react'
import { Logotipo } from '../comunes'
import type { Idioma } from '../i18n'

interface Props {
  inicial: Idioma
  daltonico: boolean
  onDaltonico: (activo: boolean) => void
  onElegir: (idioma: Idioma) => void
}

export function PantallaIdioma({ inicial, daltonico, onDaltonico, onElegir }: Props) {
  const [marcado, setMarcado] = useState<Idioma>(inicial)

  useEffect(() => {
    const tecla = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') setMarcado((m) => (m === 'es' ? 'en' : 'es'))
      else if (e.key === 'Enter') onElegir(marcado)
      else if (e.key === '1') onElegir('es')
      else if (e.key === '2') onElegir('en')
    }
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [marcado, onElegir])

  const tarjeta = (idioma: Idioma, codigo: string, nombre: string, detalle: string, tecla: number) => (
    <button
      className={`tarjeta tarjeta-idioma${marcado === idioma ? ' seleccionada' : ''}`}
      onMouseEnter={() => setMarcado(idioma)}
      onClick={() => onElegir(idioma)}
    >
      <span className="numero-tecla">{tecla}</span>
      <span className="idioma-codigo">{codigo}</span>
      <span className="idioma-nombre">{nombre}</span>
      <span className="idioma-detalle">{detalle}</span>
    </button>
  )

  return (
    <main className="pantalla pantalla-centrada">
      <Logotipo grande />
      <h1 className="titulo-claro">Elige tu idioma · Choose your language</h1>
      <div className="fila-tarjetas">
        {tarjeta('es', 'ES', 'Español', 'Jugar en español', 1)}
        {tarjeta('en', 'EN', 'English', 'Play in English', 2)}
      </div>
      <p className="ayuda-clara solo-teclado">← → · Enter · 1 / 2</p>
      <button className={`interruptor-daltonico${daltonico ? ' activo' : ''}`} onClick={() => onDaltonico(!daltonico)}>
        {daltonico ? '✓' : '◐'} Modo daltónico · Colour-blind mode
      </button>
    </main>
  )
}
