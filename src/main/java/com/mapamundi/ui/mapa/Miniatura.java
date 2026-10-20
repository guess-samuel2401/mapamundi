package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

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
                p -> p.continente() == c ? Pintor.colorDe(p) : Color.web("#cfd8d8"), 0.5);
        return canvas;
    }

    /** Silueta gris del país (como las imágenes *01.jpg del original), centrada en el recuadro. */
    public static Canvas silueta(Pais pais, double ancho, double alto) {
        Canvas canvas = new Canvas(ancho, alto);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double margen = 8;
        Pintor.ajustada(gc, pais.forma().nucleo(), pais.forma().cajaNucleo(), margen, margen, ancho - 2 * margen, alto - 2 * margen,
                Pintor.GRIS_SILUETA, Color.web("#333333"), 1.5);
        return canvas;
    }
}
