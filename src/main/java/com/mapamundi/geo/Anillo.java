package com.mapamundi.geo;

/** Un contorno cerrado (isla, territorio o hueco) ya proyectado a Mercator. */
public final class Anillo {

    private final double[] u;
    private final double[] v;
    private final Region caja;
    private final double area;
    private final double centroU;
    private final double centroV;

    /** @param lonLat coordenadas planas [lon0, lat0, lon1, lat1, …] */
    public Anillo(double[] lonLat) {
        this(lonLat, 0);
    }

    private Anillo(double[] lonLat, double desplazamientoLon) {
        int n = lonLat.length / 2;
        u = new double[n];
        v = new double[n];
        double uMin = Double.MAX_VALUE, vMin = Double.MAX_VALUE, uMax = -Double.MAX_VALUE, vMax = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            u[i] = Mercator.u(lonLat[2 * i] + desplazamientoLon);
            v[i] = Mercator.v(lonLat[2 * i + 1]);
            uMin = Math.min(uMin, u[i]);
            uMax = Math.max(uMax, u[i]);
            vMin = Math.min(vMin, v[i]);
            vMax = Math.max(vMax, v[i]);
        }
        caja = new Region(uMin, vMin, uMax, vMax);
        // Área y centroide con la fórmula del polígono (shoelace) en el plano Mercator.
        double a = 0, cx = 0, cy = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double cruz = u[i] * v[j] - u[j] * v[i];
            a += cruz;
            cx += (u[i] + u[j]) * cruz;
            cy += (v[i] + v[j]) * cruz;
        }
        area = Math.abs(a / 2);
        if (Math.abs(a) > 1e-15) {
            centroU = cx / (3 * a);
            centroV = cy / (3 * a);
        } else {
            centroU = caja.centroU();
            centroV = caja.centroV();
        }
    }

    /** Copia desplazada en longitud (±360°) para unirse con el resto de su país al otro lado del antimeridiano. */
    public static Anillo desplazado(double[] lonLat, double desplazamientoLon) {
        return new Anillo(lonLat, desplazamientoLon);
    }

    public int puntos() {
        return u.length;
    }

    public double u(int i) {
        return u[i];
    }

    public double v(int i) {
        return v[i];
    }

    public Region caja() {
        return caja;
    }

    public double area() {
        return area;
    }

    public double centroU() {
        return centroU;
    }

    public double centroV() {
        return centroV;
    }
}
