package com.mapamundi.juego;

import com.mapamundi.i18n.Textos;

/** Ayudas que el jugador puede pedir durante una ronda; su costo se descuenta de los puntos de esa ronda. */
public enum Pista {
    DESCARTAR(50),
    ZONA(100),
    VECINOS(150);

    private final int costo;

    Pista(int costo) {
        this.costo = costo;
    }

    public String nombre() {
        return Textos.t("pista." + name() + ".nombre");
    }

    public String descripcion() {
        return Textos.t("pista." + name() + ".descripcion");
    }

    public int costo() {
        return costo;
    }

    /** En qué fase se puede pedir: descartar continentes en la primera, el resto sobre el mapa. */
    public Partida.Fase fase() {
        return this == DESCARTAR ? Partida.Fase.CONTINENTE : Partida.Fase.UBICACION;
    }
}
