// Copia a public/ los recursos que comparte con la versión de Windows (datos de los países, textos,
// sonidos, música y tipografías), para que haya una sola fuente de verdad: src/main/resources del proyecto Java.
import { cpSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const web = join(dirname(fileURLToPath(import.meta.url)), '..')
const java = join(web, '..', 'src', 'main', 'resources', 'com', 'mapamundi')
const publico = join(web, 'public', 'recursos')

const copias = [
  ['datos/paises.json', 'paises.json'],
  ['i18n/textos_es.properties', 'textos_es.properties'],
  ['i18n/textos_en.properties', 'textos_en.properties'],
  ['sonidos', 'sonidos'],
  ['musica', 'musica'],
  ['fuentes', 'fuentes'],
  ['icono-256.png', 'icono.png'],
]
mkdirSync(publico, { recursive: true })
for (const [origen, destino] of copias) {
  cpSync(join(java, origen), join(publico, destino), { recursive: true })
}
console.log('recursos copiados a public/recursos')
