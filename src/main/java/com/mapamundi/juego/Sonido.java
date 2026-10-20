package com.mapamundi.juego;

/** Los sonidos .wav del juego original. */
public enum Sonido {
    CONTINENTE_CORRECTO("figuracorrecta"),
    CONTINENTE_EQUIVOCADO("escogemal"),
    UBICADO("gano"),
    MAL_UBICADO("perdio"),
    FIN_TIEMPO("fintiempo"),
    FIN_PARTIDA("perdiotreserrores"),
    NIVEL_SUPERADO("figuracompleta"),
    LOGRO("figuracompleta");

    private final String archivo;

    Sonido(String archivo) {
        this.archivo = archivo;
    }

    public String archivo() {
        return archivo;
    }
}
