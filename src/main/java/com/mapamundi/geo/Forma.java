package com.mapamundi.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * La geometría completa de un país (o territorio) y su "núcleo": la parte que forma la pieza
 * que mueve el jugador. Las islas lejanas (Guayana Francesa para Francia, Hawái para EE. UU.)
 * se dibujan en el mapa, pero no forman parte de la pieza.
 */
public final class Forma {

    /** Una isla pertenece al núcleo si está a menos de esta distancia de otra parte del núcleo. */
    static final double SEPARACION_MAX_KM = 1100;

    private final List<Anillo> anillos;
    private final List<Anillo> nucleo;
    private final Region caja;
    private final Region cajaNucleo;
    private final double anclaU;
    private final double anclaV;

    public Forma(List<double[]> anillosLonLat) {
        if (anillosLonLat.isEmpty()) throw new IllegalArgumentException("Forma vacía");
        int principal = 0;
        double mayor = -1;
        for (int i = 0; i < anillosLonLat.size(); i++) {
            double a = areaGrados(anillosLonLat.get(i));
            if (a > mayor) {
                mayor = a;
                principal = i;
            }
        }
        // Los anillos al otro lado del antimeridiano se desplazan ±360° para quedar junto al principal.
        double centroPrincipal = centroLon(anillosLonLat.get(principal));
        anillos = new ArrayList<>();
        for (double[] r : anillosLonLat) {
            double d = centroLon(r) - centroPrincipal;
            double desplazamiento = d > 180 ? -360 : d < -180 ? 360 : 0;
            anillos.add(Anillo.desplazado(r, desplazamiento));
        }
        nucleo = calcularNucleo(anillos, anillos.get(principal));
        caja = cajaDe(anillos);
        cajaNucleo = cajaDe(nucleo);

        double area = 0, su = 0, sv = 0;
        for (Anillo a : nucleo) {
            area += a.area();
            su += a.centroU() * a.area();
            sv += a.centroV() * a.area();
        }
        anclaU = area > 0 ? su / area : cajaNucleo.centroU();
        anclaV = area > 0 ? sv / area : cajaNucleo.centroV();
    }

    private static List<Anillo> calcularNucleo(List<Anillo> todos, Anillo principal) {
        List<Anillo> nucleo = new ArrayList<>(List.of(principal));
        List<Anillo> pendientes = new ArrayList<>(todos);
        pendientes.remove(principal);
        boolean agregado = true;
        while (agregado) {
            agregado = false;
            for (int i = 0; i < pendientes.size(); i++) {
                Anillo candidato = pendientes.get(i);
                for (Anillo parte : nucleo) {
                    if (separacionKm(candidato.caja(), parte.caja()) <= SEPARACION_MAX_KM) {
                        nucleo.add(candidato);
                        pendientes.remove(i--);
                        agregado = true;
                        break;
                    }
                }
            }
        }
        return nucleo;
    }

    /** Separación aproximada entre dos cajas, en km (0 si se tocan). */
    static double separacionKm(Region a, Region b) {
        double du = Math.max(0, Math.max(a.uMin() - b.uMax(), b.uMin() - a.uMax()));
        double dv = Math.max(0, Math.max(a.vMin() - b.vMax(), b.vMin() - a.vMax()));
        double lat = Mercator.lat((Math.max(a.vMin(), b.vMin()) + Math.min(a.vMax(), b.vMax())) / 2);
        double kmPorU = 2 * Math.PI * Mercator.RADIO_TIERRA_KM * Math.cos(Math.toRadians(lat));
        // En Mercator, v se estira igual que u a esa latitud.
        return Math.hypot(du * kmPorU, dv * kmPorU);
    }

    private static Region cajaDe(List<Anillo> lista) {
        Region r = lista.getFirst().caja();
        for (Anillo a : lista) r = r.union(a.caja());
        return r;
    }

    private static double areaGrados(double[] r) {
        double a = 0;
        int n = r.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            a += r[2 * i] * r[2 * j + 1] - r[2 * j] * r[2 * i + 1];
        }
        return Math.abs(a / 2);
    }

    private static double centroLon(double[] r) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < r.length; i += 2) {
            min = Math.min(min, r[i]);
            max = Math.max(max, r[i]);
        }
        return (min + max) / 2;
    }

    /** Distancia (en unidades Mercator, ~2 km) por debajo de la cual dos fronteras se consideran la misma. */
    private static final double TOQUE = 0.00006;

    /**
     * Si el núcleo de esta forma toca alguna parte de {@code otra} (comparten frontera). Solo se comparan
     * los puntos que caen cerca de la otra forma, para que países enormes como Rusia no sean lentos.
     */
    public boolean tocaA(Forma otra) {
        Region aqui = cajaNucleo.ampliadaFija(TOQUE), alla = otra.caja.ampliadaFija(TOQUE);
        if (!aqui.seCruzaCon(alla)) return false;
        List<double[]> mios = puntosDentro(nucleo, alla), suyos = puntosDentro(otra.anillos, aqui);
        for (double[] a : mios) {
            for (double[] b : suyos) {
                if (Math.abs(a[0] - b[0]) <= TOQUE && Math.abs(a[1] - b[1]) <= TOQUE) return true;
            }
        }
        return false;
    }

    private static List<double[]> puntosDentro(List<Anillo> anillos, Region caja) {
        List<double[]> puntos = new ArrayList<>();
        for (Anillo a : anillos) {
            if (!a.caja().seCruzaCon(caja)) continue;
            for (int i = 0; i < a.puntos(); i++) {
                double u = a.u(i), v = a.v(i);
                if (u >= caja.uMin() && u <= caja.uMax() && v >= caja.vMin() && v <= caja.vMax()) puntos.add(new double[]{u, v});
            }
        }
        return puntos;
    }

    public List<Anillo> anillos() {
        return anillos;
    }

    public List<Anillo> nucleo() {
        return nucleo;
    }

    public Region caja() {
        return caja;
    }

    public Region cajaNucleo() {
        return cajaNucleo;
    }

    /** Punto de referencia de la pieza (centro de masa del núcleo): de aquí se mide la distancia al acertar. */
    public double anclaU() {
        return anclaU;
    }

    public double anclaV() {
        return anclaV;
    }
}
