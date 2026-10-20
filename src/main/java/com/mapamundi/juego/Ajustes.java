package com.mapamundi.juego;

import com.mapamundi.modelo.Continente;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lo que el jugador elige en el menú.
 *
 * @param nivelInicial índice en {@link Nivel#progresion}, o -1 para jugar con los continentes elegidos
 */
public record Ajustes(Velocidad velocidad, Modo modo, int nivelInicial, Set<Continente> continentes, boolean mostrarDatos) {

    public static Ajustes porDefecto() {
        return new Ajustes(Velocidad.NORMAL, Modo.MAPA_PAIS, 0, EnumSet.allOf(Continente.class), true);
    }

    public boolean personalizado() {
        return nivelInicial < 0;
    }
}
