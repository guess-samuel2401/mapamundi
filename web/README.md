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
- En un teléfono en vertical pide girarlo: el juego es en horizontal.

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
