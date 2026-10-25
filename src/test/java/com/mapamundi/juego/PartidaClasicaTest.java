package com.mapamundi.juego;

import com.mapamundi.juego.PartidaClasica.Direccion;
import com.mapamundi.juego.PartidaClasica.Fase;
import com.mapamundi.juego.PartidaClasica.Motivo;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartidaClasicaTest {

    private static Atlas atlas;

    @BeforeAll
    static void cargar() {
        atlas = Atlas.cargar();
    }

    private static PartidaClasica nueva(Modo modo, EnumSet<Continente> continentes) {
        Ajustes a = new Ajustes(Velocidad.NORMAL, modo, -1, continentes, true, false, false, false, true);
        return new PartidaClasica(a, atlas.paises(), new Random(4));
    }

    private static Continente otro(Pais p) {
        return p.continente() == Continente.ASIA ? Continente.EUROPA : Continente.ASIA;
    }

    /** Como un jugador: cambia la dirección con las "flechas" hasta que la pieza está sobre su lugar. */
    private static void guiar(PartidaClasica p) {
        double paso = p.vista().ancho() * PartidaClasica.PASO;
        for (int i = 0; i < 20000 && Math.hypot(p.despU(), p.despV()) > paso; i++) {
            Direccion d = Math.abs(p.despU()) > Math.abs(p.despV())
                    ? (p.despU() > 0 ? Direccion.IZQUIERDA : Direccion.DERECHA)
                    : (p.despV() > 0 ? Direccion.ARRIBA : Direccion.ABAJO);
            p.cambiarDireccion(d);
            p.ticMovimiento();
        }
    }

    private static void ubicar(PartidaClasica p) {
        p.elegirContinente(p.actual().continente());
        guiar(p);
        p.confirmar();
    }

    @Test
    void siElPaisLlegaAbajoEsUnErrorYVuelveACaer() {
        PartidaClasica p = nueva(Modo.MAPA_PAIS, EnumSet.allOf(Continente.class));
        Pais pais = p.actual();
        for (int i = 0; i < PartidaClasica.PASOS_CAIDA - 1; i++) p.ticCaida();
        assertEquals(Fase.CAIDA, p.fase());
        p.ticCaida();
        assertEquals(Motivo.TIEMPO_CAIDA, p.resultado().motivo());
        assertEquals(1, p.errores());
        p.siguiente();
        assertSame(pais, p.actual(), "el mismo país vuelve a caer");
        assertEquals(0, p.caida());
    }

    @Test
    void elContinenteEquivocadoEsUnErrorConElMismoPais() {
        PartidaClasica p = nueva(Modo.MAPA_PAIS, EnumSet.allOf(Continente.class));
        Pais pais = p.actual();
        p.elegirContinente(otro(pais));
        assertEquals(Motivo.CONTINENTE_EQUIVOCADO, p.resultado().motivo());
        p.siguiente();
        assertSame(pais, p.actual());
        p.elegirContinente(pais.continente());
        assertEquals(Fase.UBICACION, p.fase());
    }

    @Test
    void laPiezaNoSeDetieneYRebotaEnLosBordes() {
        PartidaClasica p = nueva(Modo.MAPA_PAIS, EnumSet.allOf(Continente.class));
        p.elegirContinente(p.actual().continente());
        assertEquals(Direccion.ABAJO, p.direccion(), "empieza bajando, como en el original");
        double antes = p.despV();
        p.ticMovimiento();
        assertTrue(p.despV() > antes);
        p.cambiarDireccion(Direccion.ARRIBA);
        for (int i = 0; i < 40; i++) p.ticMovimiento();
        assertEquals(Direccion.ABAJO, p.direccion(), "al llegar arriba rebota");
    }

    @Test
    void guiadaHastaSuLugarEsUnAciertoConLosPuntosDelModo() {
        PartidaClasica p = nueva(Modo.DATOS, EnumSet.allOf(Continente.class));
        ubicar(p);
        assertTrue(p.resultado().acierto(), "a " + p.resultado().distanciaKm() + " km");
        assertEquals(50, p.puntaje(), "«Solo datos» vale 50 por país");
        assertEquals(10, PartidaClasica.puntosPorPais(Modo.MAPA_PAIS));
        Pais ubicado = p.actual();
        p.siguiente();
        assertFalse(ubicado == p.actual(), "tras acertar sale otro país");
    }

    @Test
    void soltarloLejosOSinTiempoSonErrores() {
        PartidaClasica p = nueva(Modo.MAPA_PAIS, EnumSet.allOf(Continente.class));
        p.elegirContinente(p.actual().continente());
        p.confirmar(); // sigue arriba del todo
        assertEquals(Motivo.LEJOS, p.resultado().motivo());
        p.siguiente();
        p.elegirContinente(p.actual().continente());
        for (int i = 0; i < PartidaClasica.PASOS_RELOJ; i++) p.ticReloj();
        assertEquals(Motivo.TIEMPO_MAPA, p.resultado().motivo());
        assertEquals(2, p.errores());
    }

    @Test
    void cincoErroresTerminanLaPartida() {
        PartidaClasica p = nueva(Modo.MAPA_PAIS, EnumSet.allOf(Continente.class));
        for (int i = 0; i < PartidaClasica.ERRORES_MAX; i++) {
            p.elegirContinente(otro(p.actual()));
            p.siguiente();
        }
        assertEquals(Fase.FIN, p.fase());
        assertFalse(p.ganada());
    }

    @Test
    void completarOceaniaEsMapaCompletoYGanaLaPartida() {
        PartidaClasica p = nueva(Modo.MAPA_PAIS, EnumSet.of(Continente.OCEANIA));
        assertFalse(p.elegible(Continente.ASIA), "solo se juega con los continentes elegidos");
        int total = p.totalPaises();
        for (int i = 0; i < total; i++) {
            ubicar(p);
            assertTrue(p.resultado().acierto(), p.actual().nombre() + " a " + p.resultado().distanciaKm() + " km");
            if (i < total - 1) {
                assertNull(p.resultado().completado());
                p.siguiente();
            }
        }
        assertEquals(Continente.OCEANIA, p.resultado().completado());
        assertTrue(p.ganada());
        assertFalse(p.elegible(Continente.OCEANIA), "un continente completo se apaga");
        p.siguiente();
        assertEquals(Fase.FIN, p.fase());
        assertEquals(total * 10, p.puntaje());
    }

    @Test
    void enSoloDatosElPrimerFalloDaMasDatosYElSegundoRevelaElLugar() {
        PartidaClasica p = nueva(Modo.DATOS, EnumSet.allOf(Continente.class));
        Pais pais = p.actual();
        assertFalse(p.datosCompletos());
        p.elegirContinente(otro(pais));
        assertFalse(p.revelaElLugar(), "el primer fallo no dice dónde estaba");
        assertTrue(p.datosCompletos(), "tras el primer fallo se ven todos los datos");
        p.siguiente();
        assertSame(pais, p.actual(), "vuelve a caer el mismo país");
        p.elegirContinente(otro(pais));
        assertTrue(p.revelaElLugar(), "el segundo fallo sí lo revela");

        PartidaClasica normal = nueva(Modo.MAPA_PAIS, EnumSet.allOf(Continente.class));
        normal.elegirContinente(otro(normal.actual()));
        assertTrue(normal.revelaElLugar(), "en los demás modos se revela al primer fallo");
    }
}
