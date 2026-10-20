package com.mapamundi.juego;

import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogrosTest {

    private static Atlas atlas;

    @TempDir
    Path carpeta;

    @BeforeAll
    static void cargar() {
        atlas = Atlas.cargar();
    }

    private Partida partida(Velocidad velocidad, Modo modo) {
        Ajustes a = new Ajustes(velocidad, modo, 0, EnumSet.allOf(Continente.class), true);
        return new Partida(a, Nivel.progresion(atlas), Nivel.personalizado(atlas, a.continentes()), new Random(3));
    }

    /** Juega una ronda: acierta o falla la ubicación, y revisa los logros. */
    private static List<Logro> ronda(Partida p, Logros logros, boolean acertar) {
        p.elegirContinente(p.actual().continente());
        if (acertar) p.colocar(0, 0);
        p.confirmar();
        List<Logro> nuevos = new ArrayList<>(logros.revisar(p));
        p.siguiente();
        nuevos.addAll(logros.revisar(p));
        return nuevos;
    }

    @Test
    void elPrimerAciertoDesbloqueaLogrosYSeGuarda() {
        Path archivo = carpeta.resolve("logros.json");
        Logros logros = new Logros(archivo, atlas);
        List<Logro> nuevos = ronda(partida(Velocidad.RAPIDA, Modo.MAPA_PAIS), logros, true);
        assertTrue(nuevos.contains(Logro.PRIMER_PAIS));
        assertTrue(nuevos.contains(Logro.PUNTERIA), "colocar en (0,0) es puntería perfecta");
        assertTrue(nuevos.contains(Logro.RELAMPAGO), "se confirmó sin gastar tiempo");
        assertTrue(nuevos.contains(Logro.VELOCISTA));
        // Al volver a abrir el juego, el logro sigue ahí y no se repite.
        Logros otraVez = new Logros(archivo, atlas);
        assertTrue(otraVez.desbloqueado(Logro.PRIMER_PAIS));
        assertFalse(ronda(partida(Velocidad.RAPIDA, Modo.MAPA_PAIS), otraVez, true).contains(Logro.PRIMER_PAIS));
    }

    @Test
    void unaRachaDeCincoYSubirDeNivelSinPerderCorazones() {
        Logros logros = new Logros(carpeta.resolve("logros.json"), atlas);
        Partida p = partida(Velocidad.NORMAL, Modo.MAPA_PAIS);
        List<Logro> todos = new ArrayList<>();
        for (int i = 0; i < Partida.PAISES_POR_NIVEL; i++) todos.addAll(ronda(p, logros, true));
        assertTrue(todos.contains(Logro.RACHA_5));
        assertTrue(todos.contains(Logro.SIN_RASGUNO), "6 aciertos sin fallar suben de nivel sin perder corazones");
        assertFalse(todos.contains(Logro.VELOCISTA), "en velocidad Normal no cuenta");
    }

    @Test
    void fallarNoDesbloqueaNadaYCortaLaRacha() {
        Logros logros = new Logros(carpeta.resolve("logros.json"), atlas);
        Partida p = partida(Velocidad.NORMAL, Modo.MAPA_PAIS);
        assertTrue(ronda(p, logros, false).isEmpty());
        ronda(p, logros, true);
        assertEquals(1, p.racha());
        ronda(p, logros, false);
        assertEquals(0, p.racha());
    }

    @Test
    void elProgresoDeLosLogrosAcumulativos() {
        Logros logros = new Logros(carpeta.resolve("logros.json"), atlas);
        Partida p = partida(Velocidad.NORMAL, Modo.CAPITAL);
        ronda(p, logros, true);
        ronda(p, logros, true);
        assertArrayEquals(new int[]{2, 10}, logros.progreso(Logro.CAPITALES));
        assertArrayEquals(new int[]{2, 25}, logros.progreso(Logro.VIAJERO));
        assertEquals(12, logros.progreso(Logro.SUDAMERICA)[1]);
        assertEquals(null, logros.progreso(Logro.PUNTERIA));
    }
}
