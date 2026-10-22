package com.mapamundi.i18n;

import java.util.Locale;

/** Idiomas del juego. */
public enum Idioma {
    ES("es", "Español"),
    EN("en", "English");

    private final String codigo;
    private final String nombre;

    Idioma(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public String codigo() {
        return codigo;
    }

    /** El nombre del idioma en ese mismo idioma ("Español", "English"). */
    public String nombre() {
        return nombre;
    }

    public Locale locale() {
        return Locale.of(codigo);
    }

    public static Idioma deCodigo(String codigo) {
        for (Idioma i : values()) if (i.codigo.equalsIgnoreCase(codigo)) return i;
        return ES;
    }
}
