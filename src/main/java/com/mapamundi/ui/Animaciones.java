package com.mapamundi.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** Pequeñas animaciones de la interfaz. */
final class Animaciones {

    private static final Duration ENTRADA = Duration.millis(180);
    private static final Duration SALIDA = Duration.millis(140);

    /**
     * Curva "back out": se pasa un poco del final y vuelve, como un rebote suave.
     * Las curvas SPLINE de JavaFX no pueden salirse de 0..1, por eso se calcula aquí.
     */
    private static final Interpolator REBOTE = new Interpolator() {
        private static final double C1 = 1.70158, C3 = C1 + 1;

        @Override
        protected double curve(double t) {
            double x = t - 1;
            return 1 + C3 * x * x * x + C1 * x * x;
        }
    };

    private Animaciones() {
    }

    /** Al pasar el cursor, el nodo crece un poco y se eleva; al salir vuelve a su sitio. */
    static void alPasar(Node nodo, double escala, double elevacion) {
        alPasar(nodo, escala, elevacion, null);
    }

    /**
     * Igual que {@link #alPasar(Node, double, double)}, pero detiene {@code enReposo} (por ejemplo, el latido
     * del botón Jugar) mientras el cursor está encima, para que las dos animaciones no se peleen.
     */
    static void alPasar(Node nodo, double escala, double elevacion, Animation enReposo) {
        ParallelTransition entrar = transicion(nodo, escala, -elevacion, ENTRADA, REBOTE);
        ParallelTransition salir = transicion(nodo, 1, 0, SALIDA, Interpolator.EASE_OUT);
        salir.setOnFinished(e -> {
            if (enReposo != null && !nodo.isHover()) enReposo.play();
        });
        nodo.hoverProperty().addListener((o, antes, encima) -> {
            if (encima) {
                if (enReposo != null) enReposo.pause();
                salir.stop();
                // Por encima de sus vecinos, para que al crecer no quede tapado.
                nodo.setViewOrder(-1);
                entrar.playFromStart();
            } else {
                entrar.stop();
                nodo.setViewOrder(0);
                salir.playFromStart();
            }
        });
    }

    private static ParallelTransition transicion(Node nodo, double escala, double y, Duration duracion, Interpolator curva) {
        ScaleTransition s = new ScaleTransition(duracion, nodo);
        s.setToX(escala);
        s.setToY(escala);
        s.setInterpolator(curva);
        TranslateTransition t = new TranslateTransition(duracion, nodo);
        t.setToY(y);
        t.setInterpolator(curva);
        return new ParallelTransition(s, t);
    }
}
