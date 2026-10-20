package com.mapamundi.geo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoTest {

    @Test
    void mercatorIdaYVuelta() {
        assertEquals(-78.5, Mercator.lon(Mercator.u(-78.5)), 1e-9);
        assertEquals(-0.22, Mercator.lat(Mercator.v(-0.22)), 1e-9);
        assertEquals(0.5, Mercator.v(0), 1e-12);
    }

    @Test
    void distanciaQuitoGuayaquil() {
        // ~270 km en línea recta.
        double km = Mercator.distanciaKm(-0.18, -78.47, -2.19, -79.89);
        assertEquals(272, km, 6);
    }

    @Test
    void lasIslasLejanasNoFormanParteDeLaPieza() {
        double[] continente = cuadrado(0, 45, 5);
        double[] islaCercana = cuadrado(7, 45, 1);   // ~150 km
        double[] islaLejana = cuadrado(-53, 4, 2);   // como la Guayana Francesa para Francia
        Forma f = new Forma(List.of(continente, islaCercana, islaLejana));
        assertEquals(3, f.anillos().size());
        assertEquals(2, f.nucleo().size());
        assertTrue(f.cajaNucleo().uMin() > Mercator.u(-1));
    }

    @Test
    void lasPartesAlOtroLadoDelAntimeridianoSeJuntan() {
        // Como Fiyi: una isla en 179° y otra en -179°.
        Forma f = new Forma(List.of(cuadrado(178, -17, 1.5), cuadrado(-180, -17, 0.5)));
        assertTrue(f.caja().ancho() < Mercator.u(-176) - Mercator.u(-180), "la forma no debe dar la vuelta al mundo");
        assertEquals(2, f.nucleo().size());
    }

    private static double[] cuadrado(double lon, double lat, double lado) {
        return new double[]{lon, lat, lon + lado, lat, lon + lado, lat + lado, lon, lat + lado};
    }
}
