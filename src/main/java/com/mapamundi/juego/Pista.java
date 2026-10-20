package com.mapamundi.juego;

/** Ayudas que el jugador puede pedir durante una ronda; su costo se descuenta de los puntos de esa ronda. */
public enum Pista {
    DESCARTAR("Descartar 2", "Tacha dos continentes equivocados.", 50),
    ZONA("Zona", "Marca en el mapa un círculo donde está el país.", 100),
    VECINOS("Vecinos", "Pinta los países que lo rodean.", 150);

    private final String nombre;
    private final String descripcion;
    private final int costo;

    Pista(String nombre, String descripcion, int costo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.costo = costo;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public int costo() {
        return costo;
    }

    /** En qué fase se puede pedir: descartar continentes en la primera, el resto sobre el mapa. */
    public Partida.Fase fase() {
        return this == DESCARTAR ? Partida.Fase.CONTINENTE : Partida.Fase.UBICACION;
    }
}
