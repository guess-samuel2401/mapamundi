package com.mapamundi.modelo;

import com.mapamundi.geo.Region;

/** Los cinco continentes del juego original, con la parte del mapa que muestra cada uno. */
public enum Continente {
    AFRICA("África", Region.deGrados(-26, 60, -37, 38)),
    AMERICA("América", Region.deGrados(-170, -25, -57, 74)),
    EUROPA("Europa", Region.deGrados(-26, 62, 33, 72)),
    ASIA("Asia", Region.deGrados(25, 150, -11, 56)),
    OCEANIA("Oceanía", Region.deGrados(110, 200, -50, 10));

    private final String nombre;
    private final Region vista;

    Continente(String nombre, Region vista) {
        this.nombre = nombre;
        this.vista = vista;
    }

    public String nombre() {
        return nombre;
    }

    public Region vista() {
        return vista;
    }

    /** Tecla 1-5 en la fase de elegir continente, en el orden del juego original. */
    public int tecla() {
        return ordinal() + 1;
    }
}
