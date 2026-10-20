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

    private Partida conReglas(boolean contrarreloj, boolean girada) {
        Ajustes a = new Ajustes(Velocidad.NORMAL, Modo.MAPA_PAIS, 0, EnumSet.allOf(Continente.class), true,
                contrarreloj, false, girada);
        return new Partida(a, Nivel.progresion(atlas), Nivel.personalizado(atlas, a.continentes()), new Random(7));
    }

    private static void acertar(Partida p) {
        p.elegirContinente(p.actual().continente());
        p.colocar(0, 0);
        if (p.giro() != 0) p.girar(-p.giro() / Partida.PASO_GIRO);
        p.confirmar();
    }

    @Test
    void elCuartoAciertoSeguidoValeElDoble() {
        Partida p = nueva(0);
        for (int i = 0; i < 3; i++) {
            acertar(p);
            assertEquals(1, p.resultado().multiplicador());
            p.siguiente();
        }
        int antes = p.puntaje();
        acertar(p);
        Resultado r = p.resultado();
        assertEquals(2, r.multiplicador());
        assertEquals(Partida.PUNTOS_CONTINENTE + (r.puntos() + r.bonus()) * 2, p.puntaje() - antes);
        assertEquals(3, Partida.multiplicador(8));
    }

    @Test
    void lasPistasSeDescuentanDeLaRonda() {
        Partida p = nueva(0);
        p.pedirPista(Pista.DESCARTAR);
        assertEquals(2, p.descartados().size(), "tacha dos continentes");
        assertEquals(0, p.erroresContinente(), "sin gastar intentos");
        assertFalse(p.descartados().contains(p.actual().continente()));
        assertFalse(p.puedePedir(Pista.ZONA), "la zona es para el mapa");
        p.elegirContinente(p.actual().continente());
        p.pedirPista(Pista.ZONA);
        p.pedirPista(Pista.ZONA); // una sola vez por ronda
        assertEquals(Pista.DESCARTAR.costo() + Pista.ZONA.costo(), p.penalizacion());
        assertTrue(p.zonaRadio() > 0);
        double desvio = Math.hypot(p.zonaU() - p.actual().forma().anclaU(), p.zonaV() - p.actual().forma().anclaV());
        assertTrue(desvio > 0 && desvio < p.zonaRadio(), "el país cae dentro del círculo, pero no en el centro");
        p.colocar(0, 0);
        p.confirmar();
        Resultado r = p.resultado();
        assertEquals(r.puntos() + r.bonus() - 150, r.total());
    }

    @Test
    void enContrarrelojNoSePierdenCorazonesYElTiempoEsDeLaPartida() {
        Partida p = conReglas(true, false);
        p.elegirContinente(p.actual().continente());
        p.confirmar(); // lejos
        assertFalse(p.resultado().acierto());
        assertEquals(Partida.VIDAS, p.vidas());
        assertFalse(p.terminada());
        p.siguiente();
        p.tic();
        assertEquals(Partida.SEGUNDOS_CONTRARRELOJ - 1, p.segundosPartida());
        acertar(p);
        assertEquals(0, p.resultado().bonus(), "sin bonus por ronda");
        assertEquals(Partida.SEGUNDOS_CONTRARRELOJ - 1 + Partida.SEGUNDOS_POR_ACIERTO, p.segundosPartida());
        p.siguiente();
        for (int i = 0; i < Partida.SEGUNDOS_CONTRARRELOJ + Partida.SEGUNDOS_POR_ACIERTO; i++) p.tic();
        assertEquals(Resultado.Motivo.FIN_CONTRARRELOJ, p.resultado().motivo());
        assertTrue(p.terminada());
        p.siguiente();
        assertEquals(Partida.Fase.FIN, p.fase());
    }

    @Test
    void laPiezaGiradaHayQueEnderezarla() {
        Partida p = conReglas(false, true);
        int giro = p.giro();
        assertTrue(Math.abs(giro) >= 45 && giro % Partida.PASO_GIRO == 0, "empieza girada: " + giro);
        p.elegirContinente(p.actual().continente());
        p.colocar(0, 0);
        p.confirmar();
        assertEquals(Resultado.Motivo.GIRADA, p.resultado().motivo(), "bien puesta pero torcida no vale");
        p.siguiente();
        acertar(p);
        assertTrue(p.resultado().acierto());
        assertEquals(Partida.PUNTOS_UBICACION + Partida.PUNTOS_GIRO, p.resultado().puntos());
        assertEquals(0, nueva(0).giro(), "sin el modo experto no hay giro");
    }

    @Test
    void losVecinosDeEcuadorYDeUnaIsla() {
        Pais ecuador = atlas.paises().stream().filter(x -> x.iso().equals("EC")).findFirst().orElseThrow();
        assertEquals(List.of("CO", "PE"), atlas.vecinos(ecuador).stream().map(Pais::iso).sorted().toList());
        Pais brasil = atlas.paises().stream().filter(x -> x.iso().equals("BR")).findFirst().orElseThrow();
        // Francia también: la Guayana Francesa tiene frontera con Brasil.
        assertEquals(List.of("AR", "BO", "CO", "FR", "GY", "PE", "PY", "SR", "UY", "VE"),
                atlas.vecinos(brasil).stream().map(Pais::iso).sorted().toList());
        Pais japon = atlas.paises().stream().filter(x -> x.iso().equals("JP")).findFirst().orElseThrow();
        assertEquals(3, atlas.vecinos(japon).size(), "una isla recibe sus tres países más cercanos");
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
