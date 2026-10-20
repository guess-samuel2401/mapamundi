package com.mapamundi.juego;

/** Los modos del formulario "Nivel" del juego original. */
public enum Modo {
    MAPA_PAIS("Mapa y nombre del país", "Ves la forma y el nombre del país.", true),
    MAPA_CAPITAL("Mapa y nombre de la capital", "Ves la forma y la capital.", true),
    PAIS("Solo nombre del país", "Sin forma: la pieza es una sombra.", false),
    CAPITAL("Solo nombre de la capital", "Deduce el país por su capital.", false),
    DATOS("Solo datos del país", "Modo experto: moneda, idioma, población…", false);

    private final String nombre;
    private final String ayuda;
    private final boolean muestraForma;

    Modo(String nombre, String ayuda, boolean muestraForma) {
        this.nombre = nombre;
        this.ayuda = ayuda;
        this.muestraForma = muestraForma;
    }

    public String nombre() {
        return nombre;
    }

    public String ayuda() {
        return ayuda;
    }

    /** Si se ve la forma real del país (en colores) o solo una sombra. */
    public boolean muestraForma() {
        return muestraForma;
    }

    public boolean porCapital() {
        return this == MAPA_CAPITAL || this == CAPITAL;
    }
}
