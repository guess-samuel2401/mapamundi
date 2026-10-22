package com.mapamundi.juego;

import com.mapamundi.i18n.Textos;

/** Velocidades del formulario "Velocidad" del original, con el tiempo y margen del nuevo diseño. */
public enum Velocidad {
    LENTA(45, 250, 10),
    NORMAL(30, 150, 20),
    RAPIDA(15, 75, 30);

    private final int segundos;
    private final int toleranciaKm;
    private final int bonusPorSegundo;

    Velocidad(int segundos, int toleranciaKm, int bonusPorSegundo) {
        this.segundos = segundos;
        this.toleranciaKm = toleranciaKm;
        this.bonusPorSegundo = bonusPorSegundo;
    }

    public String nombre() {
        return Textos.t("velocidad." + name());
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
