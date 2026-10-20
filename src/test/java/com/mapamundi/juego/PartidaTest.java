package com.mapamundi.juego;

import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartidaTest {

    private static Atlas atlas;

    @BeforeAll
    static void cargar() {
        atlas = Atlas.cargar();
    }

    private Partida nueva(int nivel) {
        Ajustes a = new Ajustes(Velocidad.NORMAL, Modo.MAPA_PAIS, nivel, EnumSet.allOf(Continente.class), true);
        return new Partida(a, Nivel.progresion(atlas), Nivel.personalizado(atlas, a.continentes()), new Random(7));
    }

    private static Continente otroContinente(Pais p) {
        return p.continente() == Continente.ASIA ? Continente.EUROPA : Continente.ASIA;
    }

    @Test
    void elAtlasTieneLosPaisesActuales() {
        assertEquals(197, atlas.paises().size());
        assertTrue(atlas.paises().stream().anyMatch(p -> p.nombre().equals("Sudán del Sur")));
        assertEquals(12, atlas.paises().stream().filter(Pais::sudamerica).count());
    }

    @Test
    void continenteCorrectoSumaPuntosYArrancaElCronometro() {
        Partida p = nueva(0);
        p.elegirContinente(p.actual().continente());
        assertEquals(Partida.Fase.UBICACION, p.fase());
        assertEquals(Partida.PUNTOS_CONTINENTE, p.puntaje());
        assertEquals(Velocidad.NORMAL.segundos(), p.segundos());
    }

    @Test
    void cincoErroresDeContinenteCuestanUnaVida() {
        Partida p = nueva(0);
        for (int i = 0; i < Partida.INTENTOS_CONTINENTE - 1; i++) p.elegirContinente(otroContinente(p.actual()));
        assertEquals(Partida.Fase.CONTINENTE, p.fase());
        p.elegirContinente(otroContinente(p.actual()));
        assertEquals(Partida.Fase.RESULTADO, p.fase());
        assertEquals(Resultado.Motivo.CONTINENTE, p.resultado().motivo());
        assertEquals(Partida.VIDAS - 1, p.vidas());
    }

    @Test
    void piezaEnSuLugarEsAcierto() {
        Partida p = nueva(0);
        p.elegirContinente(p.actual().continente());
        p.colocar(0, 0);
        p.confirmar();
        assertTrue(p.resultado().acierto());
        assertEquals(0, p.resultado().distanciaKm(), 1e-6);
        int esperado = Partida.PUNTOS_CONTINENTE + Partida.PUNTOS_UBICACION + Velocidad.NORMAL.segundos() * Velocidad.NORMAL.bonusPorSegundo();
        assertEquals(esperado, p.puntaje());
        assertTrue(p.ubicados().contains(p.actual()));
    }

    @Test
    void laPiezaEmpiezaLejosYFallarCuestaUnaVida() {
        Partida p = nueva(0);
        p.elegirContinente(p.actual().continente());
        p.confirmar();
        assertFalse(p.resultado().acierto());
        assertTrue(p.resultado().distanciaKm() > Velocidad.NORMAL.toleranciaKm());
        assertEquals(Partida.VIDAS - 1, p.vidas());
    }

    @Test
    void seAcabaElTiempo() {
        Partida p = nueva(0);
        p.elegirContinente(p.actual().continente());
        for (int i = 0; i < Velocidad.NORMAL.segundos(); i++) p.tic();
        assertEquals(Resultado.Motivo.TIEMPO, p.resultado().motivo());
    }

    @Test
    void laPausaDetieneElCronometro() {
        Partida p = nueva(0);
        p.elegirContinente(p.actual().continente());
        p.alternarPausa();
        p.tic();
        assertEquals(Velocidad.NORMAL.segundos(), p.segundos());
    }

    @Test
    void seisAciertosPasanAlSiguienteNivel() {
        Partida p = nueva(0);
        List<Sonido> sonidos = new ArrayList<>();
        p.alSonar(sonidos::add);
        for (int i = 0; i < Partida.PAISES_POR_NIVEL; i++) {
            p.elegirContinente(p.actual().continente());
            p.colocar(0, 0);
            p.confirmar();
            p.siguiente();
        }
        assertEquals(2, p.numeroNivel());
        assertEquals("América", p.nivelSuperado());
        assertTrue(sonidos.contains(Sonido.NIVEL_SUPERADO));
    }

    @Test
    void sinVidasTerminaLaPartida() {
        Partida p = nueva(0);
        for (int i = 0; i < Partida.VIDAS; i++) {
            p.elegirContinente(p.actual().continente());
            p.confirmar();
            p.siguiente();
        }
        assertEquals(Partida.Fase.FIN, p.fase());
    }

    @Test
    void oceaniaCruzaElAntimeridianoSinProblemas() {
        Pais tonga = atlas.paises().stream().filter(x -> x.nombre().equals("Tonga")).findFirst().orElseThrow();
        Ajustes a = new Ajustes(Velocidad.LENTA, Modo.MAPA_PAIS, -1, EnumSet.of(Continente.OCEANIA), true);
        for (int semilla = 0; semilla < 200; semilla++) {
            Partida p = new Partida(a, Nivel.progresion(atlas), Nivel.personalizado(atlas, a.continentes()), new Random(semilla));
            if (p.actual() != tonga) continue;
            double u = tonga.forma().anclaU() + p.vueltas();
            assertTrue(u >= p.vista().uMin() && u <= p.vista().uMax(), "Tonga debe verse dentro del mapa de Oceanía");
            assertNotEquals(0, p.vueltas());
            return;
        }
    }
}
