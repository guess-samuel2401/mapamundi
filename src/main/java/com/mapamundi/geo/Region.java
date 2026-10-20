package com.mapamundi.geo;

/**
 * Rectángulo del mapa en coordenadas Mercator (u, v). lonMin puede ser mayor que 180 en
 * regiones que cruzan el antimeridiano (Oceanía), por eso se guarda en u sin recortar.
 */
public record Region(double uMin, double vMin, double uMax, double vMax) {

    public static Region deGrados(double lonMin, double lonMax, double latMin, double latMax) {
        return new Region(Mercator.u(lonMin), Mercator.v(latMax), Mercator.u(lonMax), Mercator.v(latMin));
    }

    public double ancho() {
        return uMax - uMin;
    }

    public double alto() {
        return vMax - vMin;
    }

    public double centroU() {
        return (uMin + uMax) / 2;
    }

    public double centroV() {
        return (vMin + vMax) / 2;
    }

    public Region union(Region otra) {
        return new Region(Math.min(uMin, otra.uMin), Math.min(vMin, otra.vMin), Math.max(uMax, otra.uMax), Math.max(vMax, otra.vMax));
    }

    public Region ampliada(double fraccion) {
        double du = ancho() * fraccion, dv = alto() * fraccion;
        return new Region(uMin - du, Math.max(0, vMin - dv), uMax + du, Math.min(1, vMax + dv));
    }

    /** Ampliada un margen fijo (en unidades Mercator) por cada lado. */
    public Region ampliadaFija(double margen) {
        return new Region(uMin - margen, vMin - margen, uMax + margen, vMax + margen);
    }

    public Region desplazada(double du) {
        return new Region(uMin + du, vMin, uMax + du, vMax);
    }

    public boolean seCruzaCon(Region otra) {
        return uMin <= otra.uMax && otra.uMin <= uMax && vMin <= otra.vMax && otra.vMin <= vMax;
    }

    /**
     * Cuántas vueltas al mundo (±1 en u) hay que desplazar algo centrado en {@code u}
     * para que quede dentro del rango horizontal de esta región, o lo más cerca posible.
     */
    public int vueltasPara(double u) {
        double centro = centroU();
        return (int) Math.round(centro - u);
    }
}
