import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { cargarTextos, usarIdioma } from './i18n'
import { Atlas } from './modelo'
import { cargarPreferencias } from './preferencias'
import { escucharInstalacion } from './movil'
import './estilos.css'

/** Los recursos compartidos con la versión de Windows (ver scripts/copiar-recursos.mjs). */
const BASE = `${import.meta.env.BASE_URL}recursos/`

// Chrome avisa enseguida de que se puede instalar: se guarda para el botón «Instalar».
escucharInstalacion()

// En el teléfono, mantener el dedo sobre el mapa o un botón no abre el menú del navegador.
window.addEventListener('contextmenu', (e) => e.preventDefault())

// Guardado en el teléfono para jugar sin conexión (solo en la versión publicada, no al desarrollar).
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    void navigator.serviceWorker.register('./sw.js').catch(() => {})
  })
}

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
