# Mapamundi 2.0 (Java)

Nueva versión del juego **Mapamundi** (Visual FoxPro, año 2000), ahora como aplicación de escritorio en
**Java 25 + JavaFX**, con un **mapa propio dibujado a partir de las fronteras reales** de Natural Earth
(sin internet y sin nombres que delaten la respuesta) y los **197 países actuales**.

![Menú principal](docs/captura-menu.jpg)

## Descargar e instalar (Windows 10/11, 64 bits)

1. Entra en **[Descargas (Releases)](https://github.com/guess-samuel2401/mapamundi/releases/latest)**.
2. Descarga una de las dos opciones:
   - **`Mapamundi-2.1.0-instalador.exe`** (recomendado): ábrelo y sigue el asistente. Crea accesos directos
     en el menú Inicio (y, si quieres, en el Escritorio) y se puede desinstalar desde
     *Configuración → Aplicaciones*. No pide permisos de administrador.
   - **`Mapamundi-2.1.0-windows-portable.zip`**: descomprímelo donde quieras y abre `Mapamundi\Mapamundi.exe`.
     No instala nada.
3. No hace falta tener Java: el programa lo lleva incluido. Tampoco necesita internet para jugar.

> **Aviso de Windows SmartScreen.** Como el programa no está firmado con un certificado comercial, Windows
> puede mostrar *"Windows protegió su PC"*. Pulsa **Más información → Ejecutar de todas formas**.

![Partida en curso](docs/captura-juego.jpg)

## Cómo se juega

1. **Continente:** aparece un país (nombre + silueta). Elige su continente con las flechas + Enter,
   las teclas 1–5 o un clic. Tienes 5 intentos: +100 puntos.
2. **Ubicación:** arrastra la pieza del país sobre el mapa en blanco y confirma antes de que acabe el tiempo.
   - Ratón: arrastrar la pieza. Arrastrar el mar mueve el mapa; la rueda hace zoom.
   - Teclado: flechas (1 px), Shift + flechas (8 px), Enter para confirmar.
   - Cruceta en pantalla (mantener pulsado repite).
3. La distancia se mide **en km reales** entre la pieza y su lugar. Dentro del margen: +500 y un bonus por
   los segundos sobrantes. Si fallas o se acaba el tiempo, pierdes un corazón ♥ (se rompe en el panel superior). Con los 5 corazones perdidos termina la partida.
4. Tras cada ronda se ve tu posición (●), la real (★) y los datos del país.
5. **F5**, P o Esc: pausa.

| Velocidad | Tiempo | Margen  | Bonus por segundo |
|-----------|--------|---------|-------------------|
| Lenta     | 45 s   | ±250 km | 10                |
| Normal    | 30 s   | ±150 km | 20                |
| Rápida    | 15 s   | ±75 km  | 30                |

**Niveles:** Sudamérica → América → Europa → África → Asia → Oceanía → Todo el mundo
(cada 6 países bien ubicados se sube de nivel), o escoger los continentes a mano.

**Modos** (los del original): Mapa y nombre del país · Mapa y nombre de la capital · Solo nombre del país ·
Solo nombre de la capital · Solo datos del país.

La primera vez que se abre el juego aparece un **tutorial** de 5 pasos (se puede volver a ver desde el botón
**?** del menú o desde Ajustes), y durante la primera partida salen consejos breves en la parte de abajo.

### Logros e insignias

23 insignias de bronce, plata y oro: rachas de 5, 10 y 20 aciertos seguidos, puntería (a menos de 25 km),
rapidez, capitales, modo "solo datos", completar un nivel sin perder corazones, llegar a niveles y puntuaciones,
ubicar 25, 100 o los 197 países y completar cada continente. Se ven en **★ Logros** del menú, con el avance de
las que faltan; al conseguir una aparece un aviso durante la partida y un resumen al final.

### Ajustes (⚙)

- **Música de fondo** (una para el menú y otra, más tranquila, para la partida) y volumen de música y efectos
  por separado. El botón de sonido del panel superior silencia todo.
- **Pantalla completa** (también con **F11**).
- **Modo daltónico:** paleta de colores distinguibles con daltonismo (Okabe-Ito) y una trama distinta en cada
  color de país, para no depender solo del color. El verde y el rojo de acierto/fallo pasan a azul y naranja.

Todo se guarda en `%USERPROFILE%\.mapamundi\`: `preferencias.json` (ajustes y tutorial visto), `logros.json`
y `puntajes.json` (las 10 mejores puntuaciones).

## Ejecutar

Requiere un JDK 25 (el proyecto trae Maven Wrapper, no hace falta instalar Maven).

```bash
.\mvnw.cmd clean compile javafx:run
```

(`javafx:run` por sí solo no compila. Y si VS Code y Maven compilan a la vez en `target/classes`
puede quedar una clase corrupta — "Extra bytes at the end of class file" —, por eso conviene el `clean`.)

En **VS Code** (con el *Extension Pack for Java*):
- Pestaña *Ejecutar y depurar* → **Jugar Mapamundi**, o
- `Ctrl+Shift+B` → tarea **Jugar (Maven)**.

Pruebas: `.\mvnw.cmd test` (o la tarea **Pruebas**).

Para probar con datos ficticios sin tocar los reales, apunta estas variables de entorno a otros archivos JSON
antes de abrir el juego: `MAPAMUNDI_PUNTAJES` (récords), `MAPAMUNDI_LOGROS` (insignias) y
`MAPAMUNDI_PREFERENCIAS` (ajustes; con un archivo nuevo vuelve a salir el tutorial):

```bash
$env:MAPAMUNDI_PUNTAJES = "C:\ruta\puntajes-de-prueba.json"; .\mvnw.cmd compile javafx:run
```

## Programa de escritorio (Windows)

```bash
.\empaquetado\empaquetar.ps1 -Instalar
```

1. Compila el juego y reúne sus librerías en `target\programa`.
2. Con **jpackage** (incluido en el JDK) crea `target\instalador\Mapamundi\Mapamundi.exe`, con su propio Java
   recortado a los módulos que usa (~89 MB): funciona en equipos sin Java instalado.
3. Con `-Instalar`, lo copia a `%LOCALAPPDATA%\Programs\Mapamundi` (no necesita permisos de administrador)
   y crea accesos directos en el Escritorio y en el menú Inicio.

Para llevarlo a otro equipo basta con copiar la carpeta `target\instalador\Mapamundi` entera.

Para publicar una versión nueva (requiere [Inno Setup 6](https://jrsoftware.org/isinfo.php)):

```bash
.\empaquetado\empaquetar.ps1 -Publicar
gh release create v2.1.0 target\publicar\* --title "Mapamundi 2.1.0"
```

`-Publicar` deja en `target\publicar` el instalador (`empaquetado/instalador.iss`) y el ZIP portable.
Para desinstalarlo: borrar esa carpeta de Programs y los dos accesos directos (los puntajes están en `%USERPROFILE%\.mapamundi`).

El icono (`empaquetado/mapamundi.ico`) se genera con `python -I herramientas/crear_icono.py`.

## Estructura

```
src/main/java/com/mapamundi/
  App.java, Lanzador.java     arranque (Lanzador permite el botón Run de VS Code)
  geo/                        Mercator (proyección de OpenStreetMap), regiones y formas de los países
  modelo/                     Continente, Pais, Atlas (carga datos/paises.json)
  juego/                      reglas y estado de la partida (sin interfaz: se prueba con JUnit)
  ui/                         pantallas JavaFX (menú, fases, resultado, fin)
  ui/mapa/                    dibujo del mapa en Canvas, zoom, arrastre
src/main/resources/com/mapamundi/
  datos/paises.json           generado (fronteras + datos)
  sonidos/                    los .wav del juego original
  estilos.css                 colores del original
herramientas/                 scripts de Python para generar los datos
```

### Detalles del mapa

- Proyección **Web Mercator**, la misma de OpenStreetMap.
- Los países que cruzan el antimeridiano (Rusia, Fiyi, Kiribati…) se recomponen para que no se partan.
- La pieza de cada país es su "núcleo": se excluyen las islas a más de 1.100 km del resto
  (por ejemplo, la Guayana Francesa no forma parte de la pieza de Francia), aunque sí se dibujan en el mapa.
- Sáhara Occidental y las dependencias (Groenlandia, Puerto Rico…) se dibujan como tierra, pero no se juegan.

## Regenerar los datos

Necesita Python 3. Descarga las fronteras de Natural Earth en `herramientas/fuentes/`:

- `ne_50m_admin_0_countries.geojson` de <https://github.com/nvkelso/natural-earth-vector/tree/master/geojson>

y ejecuta (la ruta es la tabla del juego original):

```bash
python -I herramientas/actualizar_datos.py      # datos actuales de Wikidata -> herramientas/datos_wikidata.json
python -I herramientas/construir_datos.py "D:\Fuente Globalizacion\mapamundi\paises.DBF"
```

`actualizar_datos.py` consulta Wikidata (licencia CC0) y guarda capital, población, superficie, idiomas oficiales,
moneda y PIB nominal (el PIB per cápita se calcula con la población). Si a un país le falta un dato, se usa el del
juego original. La religión y las exportaciones siguen siendo las del juego de 2003 y la ficha lo indica.

La música de fondo (`src/main/resources/com/mapamundi/musica/`) es original, sintetizada con
`python herramientas/crear_musica.py` (necesita numpy); los bucles están mezclados para repetirse sin corte.

`herramientas/adpcm_a_pcm.py` convierte sonidos MS-ADPCM (como `FinTiempo.wav`) a PCM, que es lo que reproduce JavaFX.

## Créditos

- Fronteras: [Natural Earth](https://www.naturalearthdata.com/) 1:50m, dominio público.
- Capital, población, superficie, idiomas, moneda y PIB: [Wikidata](https://www.wikidata.org/) (CC0).
- Religión, exportaciones e imágenes de referencia: Mapamundi original (2000–2003).
- Tipografías: [Baloo 2](https://github.com/EkType/Baloo2) (títulos y números) y
  [Nunito](https://github.com/googlefonts/nunito) (texto), licencia SIL Open Font License 1.1
  (`src/main/resources/com/mapamundi/fuentes/OFL-*.txt`). Los archivos estáticos vienen de Fontsource y
  `herramientas/renombrar_fuentes.py` ajusta sus nombres internos para que JavaFX los reconozca.

## Sistema visual

| Uso | Color |
|---|---|
| Fondo de la app ("uva") / mar del mapa | `#3A2F66` / `#74BEBC` |
| Paneles (papel) | `#FFF7EC` |
| Texto / texto secundario | `#1D2B4F` / `#4A5878` |
| Logotipo | coral `#FF8F6B`, contorno `#1A1433`, sombra `#FFD23F` |
| Títulos / rojo para texto pequeño | `#E3263B` / `#B3132A` |
| Acción principal y selección | `#FFD23F` (claro `#FFF1B8`) |
| Acento (botones secundarios, panel superior) | `#FF8F6B` |
| Países (atlas, sin celestes para no confundirse con el mar) | `#F4A99B` `#F3D27A` `#A9D38C` `#EDA7C9` `#C4B0E6` `#F6B878` `#D2BF94` |

Escala tipográfica: 12 · 14 · 16 · 20 · 28 · 40 · 64 px.

`docs/opciones-de-color.png` muestra las cuatro combinaciones de fondo y título que se compararon (se eligió la C, "Uva").
