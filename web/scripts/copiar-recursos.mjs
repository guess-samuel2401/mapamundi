// Copia a public/ los recursos que comparte con la versión de Windows (datos de los países, textos,
// sonidos y tipografías), para que haya una sola fuente de verdad: src/main/resources del proyecto Java.
// La música se convierte de WAV a MP3 (unas 5 veces más liviana), para que cargue rápido en el teléfono.
import { cpSync, existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs'
import { basename, dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { Mp3Encoder } from '@breezystack/lamejs'

const web = join(dirname(fileURLToPath(import.meta.url)), '..')
const java = join(web, '..', 'src', 'main', 'resources', 'com', 'mapamundi')
const publico = join(web, 'public', 'recursos')

const copias = [
  ['datos/paises.json', 'paises.json'],
  ['i18n/textos_es.properties', 'textos_es.properties'],
  ['i18n/textos_en.properties', 'textos_en.properties'],
  ['sonidos', 'sonidos'],
  ['fuentes', 'fuentes'],
  ['icono-256.png', 'icono.png'],
]
mkdirSync(publico, { recursive: true })
for (const [origen, destino] of copias) {
  cpSync(join(java, origen), join(publico, destino), { recursive: true })
}

/** Lee un WAV PCM de 16 bits: devuelve sus muestras (mezcladas a mono) y su frecuencia. */
function leerWav(ruta) {
  const b = readFileSync(ruta)
  let pos = 12
  let canales = 1, frecuencia = 22050, bits = 16, datos = null
  while (pos + 8 <= b.length) {
    const id = b.toString('ascii', pos, pos + 4)
    const tam = b.readUInt32LE(pos + 4)
    if (id === 'fmt ') {
      canales = b.readUInt16LE(pos + 10)
      frecuencia = b.readUInt32LE(pos + 12)
      bits = b.readUInt16LE(pos + 22)
    } else if (id === 'data') {
      datos = b.subarray(pos + 8, pos + 8 + tam)
    }
    pos += 8 + tam + (tam % 2)
  }
  if (!datos || bits !== 16) throw new Error(`${ruta}: se esperaba un WAV PCM de 16 bits`)
  const n = datos.length / 2 / canales
  const mono = new Int16Array(n)
  for (let i = 0; i < n; i++) {
    let suma = 0
    for (let c = 0; c < canales; c++) suma += datos.readInt16LE((i * canales + c) * 2)
    mono[i] = suma / canales
  }
  return { muestras: mono, frecuencia }
}

function aMp3(wav, mp3) {
  const { muestras, frecuencia } = leerWav(wav)
  const codificador = new Mp3Encoder(1, frecuencia, 64)
  const partes = []
  for (let i = 0; i < muestras.length; i += 1152) {
    const trozo = codificador.encodeBuffer(muestras.subarray(i, i + 1152))
    if (trozo.length) partes.push(Buffer.from(trozo))
  }
  const final = codificador.flush()
  if (final.length) partes.push(Buffer.from(final))
  writeFileSync(mp3, Buffer.concat(partes))
}

const musica = join(java, 'musica')
mkdirSync(join(publico, 'musica'), { recursive: true })
for (const archivo of readdirSync(musica).filter((a) => a.endsWith('.wav'))) {
  const wav = join(musica, archivo)
  const mp3 = join(publico, 'musica', basename(archivo, '.wav') + '.mp3')
  // Solo si cambió la música (convertir tarda unos segundos).
  if (!existsSync(mp3) || statSync(mp3).mtimeMs < statSync(wav).mtimeMs) aMp3(wav, mp3)
}
console.log('recursos copiados a public/recursos')
