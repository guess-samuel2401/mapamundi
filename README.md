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
.\mvnw.cmd compile javafx:run
```

(`javafx:run` por sí solo no compila: si `target/classes` está desactualizado, falla con
"no se ha encontrado la clase principal".)

En **VS Code** (con el *Extension Pack for Java*):
- Pestaña *Ejecutar y depurar* → **Jugar Mapamundi**, o
- `Ctrl+Shift+B` → tarea **Jugar (Maven)**.

Pruebas: `.\mvnw.cmd test` (o la tarea **Pruebas**).

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
