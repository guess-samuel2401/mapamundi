package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.function.Predicate;

/** Dibujos estáticos: el mapa de cada continente en colores y la silueta de un país. */
public final class Miniatura {

    private Miniatura() {
    }

    /** Continente con sus países en colores y el resto del mundo en gris, como los botones del original. */
    public static Canvas continente(Atlas atlas, Continente c, double ancho, double alto) {
        Canvas canvas = new Canvas(ancho, alto);
        Region vista = c.vista();
        Transformacion t = Transformacion.ajustada(vista, ancho, alto);
        Pintor.mapa(canvas.getGraphicsContext2D(), t, atlas,
                p -> p.continente() == c ? Pintor.colorDe(p) : Pintor.RESTO_DEL_MUNDO, 0.5);
        return canvas;
    }

    /**
     * Una región del mapa que llena todo el recuadro (recortando lo que sobre), con los países
     * que cumplen {@code resaltar} en colores y el resto en gris. Para las fichas de nivel del menú.
     */
    public static Canvas region(Atlas atlas, Region region, Predicate<Pais> resaltar, double ancho, double alto) {
        Canvas canvas = new Canvas(ancho, alto);
        Transformacion t = Transformacion.cubriendo(region, ancho, alto);
        Pintor.mapa(canvas.getGraphicsContext2D(), t, atlas,
                p -> resaltar.test(p) ? Pintor.colorDe(p) : Pintor.RESTO_DEL_MUNDO, 0.35);
        return canvas;
    }

    /** Silueta gris del país (como las imágenes *01.jpg del original), centrada en el recuadro. */
    public static Canvas silueta(Pais pais, double ancho, double alto) {
        Canvas canvas = new Canvas(ancho, alto);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double margen = 8;
        Pintor.ajustada(gc, pais.forma().nucleo(), pais.forma().cajaNucleo(), margen, margen, ancho - 2 * margen, alto - 2 * margen,
                Pintor.GRIS_SILUETA, Pintor.TINTA, 1.8);
        return canvas;
    }
}
