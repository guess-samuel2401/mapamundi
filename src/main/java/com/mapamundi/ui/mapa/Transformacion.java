package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Region;

/**
 * Pasa de coordenadas Mercator (u, v) a píxeles de pantalla y al revés.
 *
 * @param centroU centro de la vista en u
 * @param centroV centro de la vista en v
 * @param escala  píxeles por unidad Mercator
 */
public record Transformacion(double centroU, double centroV, double escala, double ancho, double alto) {

    /** La región entera, lo más grande posible dentro de ancho × alto. */
    public static Transformacion ajustada(Region region, double ancho, double alto) {
        double escala = Math.min(ancho / region.ancho(), alto / region.alto());
        return new Transformacion(region.centroU(), region.centroV(), escala, ancho, alto);
    }

    /** La región llenando todo ancho × alto; lo que no cabe queda fuera (como background-size: cover). */
    public static Transformacion cubriendo(Region region, double ancho, double alto) {
        double escala = Math.max(ancho / region.ancho(), alto / region.alto());
        return new Transformacion(region.centroU(), region.centroV(), escala, ancho, alto);
    }

    public double x(double u) {
        return (u - centroU) * escala + ancho / 2;
    }

    public double y(double v) {
        return (v - centroV) * escala + alto / 2;
    }

    public double u(double x) {
        return (x - ancho / 2) / escala + centroU;
    }

    public double v(double y) {
        return (y - alto / 2) / escala + centroV;
    }

    /** Lo que se ve en pantalla, como región Mercator. */
    public Region visible() {
        return new Region(u(0), v(0), u(ancho), v(alto));
    }

    public Transformacion con(double centroU, double centroV, double escala) {
        return new Transformacion(centroU, centroV, escala, ancho, alto);
    }
}
