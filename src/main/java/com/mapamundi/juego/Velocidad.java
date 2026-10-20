package com.mapamundi.juego;

/** Velocidades del formulario "Velocidad" del original, con el tiempo y margen del nuevo diseño. */
public enum Velocidad {
    LENTA("Lenta", 45, 250, 10),
    NORMAL("Normal", 30, 150, 20),
    RAPIDA("Rápida", 15, 75, 30);

    private final String nombre;
    private final int segundos;
    private final int toleranciaKm;
    private final int bonusPorSegundo;

    Velocidad(String nombre, int segundos, int toleranciaKm, int bonusPorSegundo) {
        this.nombre = nombre;
        this.segundos = segundos;
        this.toleranciaKm = toleranciaKm;
        this.bonusPorSegundo = bonusPorSegundo;
    }

    public String nombre() {
        return nombre;
    }

    public int segundos() {
        return segundos;
    }

    public int toleranciaKm() {
        return toleranciaKm;
    }

    public int bonusPorSegundo() {
        return bonusPorSegundo;
    }
}
