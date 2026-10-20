package com.mapamundi.juego;

import com.mapamundi.modelo.Continente;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lo que el jugador elige en el menú.
 *
 * @param nivelInicial índice en {@link Nivel#progresion}, o -1 para jugar con los continentes elegidos
 * @param contrarreloj un solo cronómetro para toda la partida y sin corazones
 * @param frioCaliente la pieza cambia de color según lo cerca que esté de su lugar
 * @param piezaGirada  modo experto: la pieza aparece girada y hay que enderezarla
 */
public record Ajustes(Velocidad velocidad, Modo modo, int nivelInicial, Set<Continente> continentes, boolean mostrarDatos,
                      boolean contrarreloj, boolean frioCaliente, boolean piezaGirada) {

    /** Partida clásica, sin reglas extra. */
    public Ajustes(Velocidad velocidad, Modo modo, int nivelInicial, Set<Continente> continentes, boolean mostrarDatos) {
        this(velocidad, modo, nivelInicial, continentes, mostrarDatos, false, false, false);
    }

    public static Ajustes porDefecto() {
        return new Ajustes(Velocidad.NORMAL, Modo.MAPA_PAIS, 0, EnumSet.allOf(Continente.class), true);
    }

    public boolean personalizado() {
        return nivelInicial < 0;
    }
}
