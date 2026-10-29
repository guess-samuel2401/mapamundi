# Mapamundi Sencillo — versión web (y base para Android/iPhone)

La misma partida que **Mapamundi Sencillo** de Windows (rama `version-sencilla`), hecha para el navegador y
pensada para el teléfono en horizontal. Es el primer paso para llevar el juego a móviles: después se empaqueta
como app de Android/iPhone con [Capacitor](https://capacitorjs.com/) sin reescribirla.

## Qué incluye

- **Idioma** (español o inglés) y **modo daltónico**, en la primera pantalla.
- La primera vez, la **partida de práctica** guiada (Brasil, Egipto y Australia), que también se repite con
  «Aprende a jugar».
- **Continentes como casillas** (uno o varios, o «Todo el mundo») y **retos** («¿Con qué nivel o dificultad te
  gustaría jugar con …?»).
- La **partida del original**:
  - el país cae mientras se elige su continente;
  - la pieza del mapa nunca se detiene;
  - hay 5 errores;
  - las mismas reglas de «Solo datos del país».
  - Al fallar, primero se ven la línea y la ★ y luego un recuadro compacto en la esquina libre del mapa.
- **Controles táctiles:** tocar el continente; en el mapa, una **cruceta** (mientras se mantiene una flecha, la
  pieza va más rápido) y el botón **«Soltar»**, en la columna derecha para no tapar el mapa. También funciona con
  teclado (flechas, Enter, 1–5, F5/P/Esc).
- **Pantalla final** sencilla, récords guardados en el navegador, música y efectos con botón para quitarlos.
- Se juega **en horizontal o en vertical**: en vertical, el mapa va arriba y la pregunta, los datos y la cruceta, debajo.

## En el teléfono

- **Pantalla completa:**
  - En Android, al elegir el idioma la página pasa a pantalla completa.
  - En cualquier momento se puede poner o quitar con el botón ⛶ de la cabecera.
  - En iPhone, Safari no lo permite; se juega en la ventana del navegador o instalando la app.
- **Horizontal o vertical:** el jugador elige; se puede girar el teléfono en plena partida.
- **Se instala como una app:**
  - Botón **📲 Instalar** en la cabecera: en Android y Chrome abre la instalación; en iPhone muestra los pasos de Safari («Compartir» → «Añadir a pantalla de inicio»). No aparece si ya está instalada.
  - Tiene ícono propio y se abre a pantalla completa.
  - **Funciona sin conexión**: el service worker (`public/sw.js`) guarda el programa, los datos de los países, los sonidos, la música y las tipografías (unos 2 MB).
  - Al publicar una versión nueva hay que subir `VERSION` en `sw.js`.
- **Abierto desde WhatsApp, Facebook, Instagram…:** esas apps usan su propio navegador, donde fallan el sonido, la pantalla completa y la instalación. El juego lo detecta y avisa que conviene abrirlo en el navegador; en Android, con un botón «Abrir en Chrome».
- **Sonido con Web Audio:**
  - se desbloquea con el primer toque del jugador, como exigen iPhone y Android;
  - la música y los efectos se callan al salir de la app;
  - la música se convierte a **MP3** al compilar (0,6 MB en lugar de 3,4 MB del WAV de Windows).
- **Vibración** corta al acertar y al fallar (Android; con los efectos activados).
- **La pantalla no se apaga** durante la partida (Screen Wake Lock).
- **Idioma del teléfono** preseleccionado la primera vez.
- **Botón Atrás de Android:** vuelve a la pantalla anterior en vez de cerrar la página; durante la partida, la pausa.
- La partida también **se pausa sola** si el jugador sale de la app (otra aplicación, una llamada, pantalla apagada).
- **Teléfonos antiguos:** el programa se compila para navegadores de 2019-2020 en adelante (iPhone con iOS 13, Android con Chrome 80), sin funciones que solo existen en los más nuevos.
- **Diseño para pantallas pequeñas:**
  - probado en vertical (390×844) y en horizontal (568×320, 640×360 y 844×390);
  - usa la altura real de la pantalla (`dvh`) y respeta la muesca (`safe-area`);
  - sin zoom con doble toque, sin rebote al deslizar y sin menú al mantener el dedo;
  - en pantallas táctiles se ocultan las ayudas de teclado.

## Una sola fuente de datos

Los países (fronteras y fichas), los textos en español e inglés, los sonidos, la música y las tipografías **no
se duplican**: `npm run recursos` (que corre solo con `dev` y `build`) los copia desde
`../src/main/resources/com/mapamundi` a `public/recursos`. Si se cambia un texto o un dato para Windows, la web lo
toma en la siguiente compilación. Las reglas del juego (`src/partida.ts`) son las de `PartidaClasica.java`
traducidas a TypeScript, con sus pruebas (`src/partida.test.ts`).

## Comandos

Requiere Node.js 20 o más nuevo.

```bash
npm install
npm run dev        # http://localhost:5173, con recarga al guardar
npm test           # pruebas de las reglas (vitest)
npm run build      # versión final en dist/ (rutas relativas: sirve en cualquier carpeta)
```

## Publicar

`dist/` es una página estática: se puede subir a GitHub Pages, Netlify o cualquier hosting. Para GitHub Pages, la
forma más simple es publicar el contenido de `dist/` en una rama `gh-pages`, o usar una acción de GitHub que
ejecute `npm ci && npm run build` dentro de `web/`.

## Próximo paso: app de Android/iPhone con Capacitor

```bash
npm install @capacitor/core @capacitor/cli @capacitor/android
npx cap init "Mapamundi Sencillo" com.mapamundi.sencillo --web-dir dist
npm run build && npx cap add android && npx cap sync
npx cap open android   # abre Android Studio para generar el APK
```

Para iPhone se necesitan una Mac con Xcode y una cuenta de Apple Developer.
