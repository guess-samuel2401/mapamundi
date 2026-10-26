import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { cargarTextos, usarIdioma } from './i18n'
import { Atlas } from './modelo'
import { cargarPreferencias } from './preferencias'
import './estilos.css'

/** Los recursos compartidos con la versión de Windows (ver scripts/copiar-recursos.mjs). */
const BASE = `${import.meta.env.BASE_URL}recursos/`

async function arrancar(): Promise<void> {
  const raiz = createRoot(document.getElementById('raiz')!)
  await cargarTextos(BASE)
  usarIdioma(cargarPreferencias().idioma)
  const atlas = await Atlas.cargar(`${BASE}paises.json`)
  raiz.render(
    <StrictMode>
      <App atlas={atlas} base={BASE} />
    </StrictMode>,
  )
}

void arrancar()
