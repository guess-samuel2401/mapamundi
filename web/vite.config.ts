import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// base relativa: el build funciona abierto desde cualquier carpeta, en GitHub Pages y dentro de Capacitor.
export default defineConfig({
  base: './',
  plugins: [react()],
  test: { environment: 'node' },
})
