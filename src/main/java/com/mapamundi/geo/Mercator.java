package com.mapamundi.geo;

/**
 * Proyección Web Mercator (la de OpenStreetMap) en unidades normalizadas:
 * u = 0 en lon -180 y 1 en lon 180 (puede salirse de ese rango al cruzar el antimeridiano),
 * v = 0 arriba (lat ~85) y 1 abajo (lat ~-85).
 */
public final class Mercator {

    public static final double LAT_MAX = 85.05112878;
    public static final double RADIO_TIERRA_KM = 6371.0088;

    private Mercator() {
    }

    public static double u(double lon) {
        return (lon + 180.0) / 360.0;
    }

    public static double v(double lat) {
        double phi = Math.toRadians(Math.max(-LAT_MAX, Math.min(LAT_MAX, lat)));
        return (1.0 - Math.log(Math.tan(phi) + 1.0 / Math.cos(phi)) / Math.PI) / 2.0;
    }

    public static double lon(double u) {
        return u * 360.0 - 180.0;
    }

    public static double lat(double v) {
        return Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1.0 - 2.0 * v))));
    }

    /** Distancia sobre la esfera (fórmula del haversine), en km. */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /** Distancia en km entre dos puntos dados en coordenadas Mercator (u, v). */
    public static double distanciaKmUV(double u1, double v1, double u2, double v2) {
        return distanciaKm(lat(v1), lon(u1), lat(v2), lon(u2));
    }
}
