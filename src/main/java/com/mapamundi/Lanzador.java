package com.mapamundi;

/**
 * Punto de entrada que no extiende Application, para poder ejecutar el juego con el botón
 * "Run" de VS Code (o con java -cp) sin configurar el module-path de JavaFX.
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
