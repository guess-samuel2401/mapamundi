package com.mapamundi.juego;

/** Los efectos de sonido del juego (sintetizados con herramientas/crear_efectos.py). */
public enum Sonido {
    CONTINENTE_CORRECTO("continente_correcto"),
    CONTINENTE_EQUIVOCADO("continente_error"),
    UBICADO("ubicado"),
    MAL_UBICADO("mal_ubicado"),
    FIN_TIEMPO("fin_tiempo"),
    FIN_PARTIDA("fin_partida"),
    NIVEL_SUPERADO("nivel_superado"),
    LOGRO("logro"),
    /** El "bip" del modo clásico: cada paso de la caída y el reloj del mapa. */
    TIC("tic");

    private final String archivo;

    Sonido(String archivo) {
        this.archivo = archivo;
    }

    public String archivo() {
        return archivo;
    }
}
