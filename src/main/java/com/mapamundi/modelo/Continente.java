package com.mapamundi.modelo;

import com.mapamundi.geo.Region;
import com.mapamundi.i18n.Textos;

/** Los cinco continentes del juego original, con la parte del mapa que muestra cada uno. */
public enum Continente {
    AFRICA(Region.deGrados(-26, 60, -37, 38)),
    AMERICA(Region.deGrados(-170, -25, -57, 74)),
    EUROPA(Region.deGrados(-26, 62, 33, 72)),
    ASIA(Region.deGrados(25, 150, -11, 56)),
    OCEANIA(Region.deGrados(110, 200, -50, 10));

    private final Region vista;

    Continente(Region vista) {
        this.vista = vista;
    }

    /** El nombre en el idioma del juego. */
    public String nombre() {
        return Textos.t("continente." + name());
    }

    public Region vista() {
        return vista;
    }

    /** Tecla 1-5 en la fase de elegir continente, en el orden del juego original. */
    public int tecla() {
        return ordinal() + 1;
    }
}
