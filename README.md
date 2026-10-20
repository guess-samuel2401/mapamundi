# Mapamundi 2.0 (Java)

Nueva versión del juego **Mapamundi** (Visual FoxPro, año 2000), ahora como aplicación de escritorio en
**Java 25 + JavaFX**, con un **mapa propio dibujado a partir de las fronteras reales** de Natural Earth
(sin internet y sin nombres que delaten la respuesta) y los **197 países actuales**.

## Cómo se juega

1. **Continente:** aparece un país (nombre + silueta). Elige su continente con las flechas + Enter,
   las teclas 1–5 o un clic. Tienes 5 intentos: +100 puntos.
2. **Ubicación:** arrastra la pieza del país sobre el mapa en blanco y confirma antes de que acabe el tiempo.
   - Ratón: arrastrar la pieza. Arrastrar el mar mueve el mapa; la rueda hace zoom.
   - Teclado: flechas (1 px), Shift + flechas (8 px), Enter para confirmar.
   - Cruceta en pantalla (mantener pulsado repite).
3. La distancia se mide **en km reales** entre la pieza y su lugar. Dentro del margen: +500 y un bonus por
   los segundos sobrantes. Si fallas o se acaba el tiempo, pierdes una vida ★. Con 5 vidas perdidas termina la partida.
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

Las 10 mejores puntuaciones se guardan en `%USERPROFILE%\.mapamundi\puntajes.json`.

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
python -I herramientas/construir_datos.py "D:\Fuente Globalizacion\mapamundi\paises.DBF"
```

`herramientas/adpcm_a_pcm.py` convierte sonidos MS-ADPCM (como `FinTiempo.wav`) a PCM, que es lo que reproduce JavaFX.

## Créditos

- Fronteras: [Natural Earth](https://www.naturalearthdata.com/) 1:50m, dominio público.
- Idioma, religión, moneda, exportaciones e imágenes de referencia: Mapamundi original (2000–2003).
- Población: estimaciones de Natural Earth (2019).
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
