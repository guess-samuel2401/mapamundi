// Service worker: deja Mapamundi guardado en el teléfono para jugar sin conexión después de la primera visita.
// - La página (index.html) se pide primero a la red, para tener siempre la última versión; sin red, la guardada.
// - Lo demás (programa, datos de los países, sonidos, música, tipografías e íconos) se sirve desde lo guardado
//   y, si falta, se descarga y se guarda.
// Al publicar una versión nueva conviene subir VERSION, para que se borre lo guardado de la anterior.
const VERSION = 'mapamundi-v3'

const SONIDOS = ['tic', 'continente_correcto', 'continente_error', 'ubicado', 'mal_ubicado', 'fin_tiempo', 'fin_partida', 'nivel_superado', 'logro']
const FUENTES = ['Baloo2-Bold', 'Baloo2-ExtraBold', 'Nunito-Regular', 'Nunito-SemiBold', 'Nunito-Bold', 'Nunito-ExtraBold']

/** Todo lo que hace falta para jugar sin conexión (unos 2 MB). */
const ESENCIALES = [
  './',
  './index.html',
  './manifest.webmanifest',
  './recursos/paises.json',
  './recursos/textos_es.properties',
  './recursos/textos_en.properties',
  './recursos/musica/menu.mp3',
  './recursos/musica/juego.mp3',
  './iconos/icono-192.png',
  './iconos/icono-512.png',
  ...SONIDOS.map((s) => `./recursos/sonidos/${s}.wav`),
  ...FUENTES.map((f) => `./recursos/fuentes/${f}.ttf`),
]

/** El programa (JS y CSS con su huella en el nombre) se lee de index.html, para guardarlo también. */
async function programa() {
  const html = await (await fetch('./index.html', { cache: 'no-store' })).text()
  return [...html.matchAll(/(?:src|href)="(\.\/assets\/[^"]+)"/g)].map((m) => m[1])
}

self.addEventListener('install', (e) => {
  e.waitUntil(
    (async () => {
      const c = await caches.open(VERSION)
      await c.addAll(ESENCIALES)
      await c.addAll(await programa())
      await self.skipWaiting()
    })(),
  )
})

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches
      .keys()
      .then((nombres) => Promise.all(nombres.filter((n) => n !== VERSION).map((n) => caches.delete(n))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (e) => {
  const pedido = e.request
  if (pedido.method !== 'GET' || new URL(pedido.url).origin !== self.location.origin) return

  if (pedido.mode === 'navigate') {
    e.respondWith(
      fetch(pedido)
        .then((r) => {
          const copia = r.clone()
          caches.open(VERSION).then((c) => c.put('./index.html', copia))
          return r
        })
        .catch(() => caches.match('./index.html')),
    )
    return
  }

  e.respondWith(
    caches.match(pedido).then(
      (guardado) =>
        guardado ||
        fetch(pedido).then((r) => {
          if (r.ok) {
            const copia = r.clone()
            caches.open(VERSION).then((c) => c.put(pedido, copia))
          }
          return r
        }),
    ),
  )
})
