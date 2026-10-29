import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// base relativa: el build funciona abierto desde cualquier carpeta, en GitHub Pages y dentro de Capacitor.
// target: el programa se traduce a JavaScript que entienden también teléfonos de 2019-2020 (iPhone con iOS 13,
// Android con Chrome 80), no solo los navegadores más nuevos.
export default defineConfig({
  base: './',
  plugins: [react()],
  build: { target: ['es2019', 'chrome80', 'safari13', 'firefox78', 'edge88'] },
  test: { environment: 'node' },
})
