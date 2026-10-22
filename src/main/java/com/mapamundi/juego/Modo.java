package com.mapamundi.juego;

import com.mapamundi.i18n.Textos;

/** Los modos del formulario "Nivel" del juego original. */
public enum Modo {
    MAPA_PAIS(true),
    MAPA_CAPITAL(true),
    PAIS(false),
    CAPITAL(false),
    DATOS(false);

    private final boolean muestraForma;

    Modo(boolean muestraForma) {
        this.muestraForma = muestraForma;
    }

    public String nombre() {
        return Textos.t("modo." + name() + ".nombre");
    }

    public String ayuda() {
        return Textos.t("modo." + name() + ".ayuda");
    }

    /** Si se ve la forma real del país (en colores) o solo una sombra. */
    public boolean muestraForma() {
        return muestraForma;
    }

    public boolean porCapital() {
        return this == MAPA_CAPITAL || this == CAPITAL;
    }
}
