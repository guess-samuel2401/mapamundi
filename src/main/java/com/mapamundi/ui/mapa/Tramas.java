package com.mapamundi.ui.mapa;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;

/** Tramas del modo daltónico: cada índice de color lleva un dibujo distinto, para no depender solo del color. */
final class Tramas {

    private static final int LADO = 10;

    private Tramas() {
    }

    static Paint crear(int indice, Color fondo) {
        Canvas c = new Canvas(LADO, LADO);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(fondo);
        gc.fillRect(0, 0, LADO, LADO);
        gc.setStroke(fondo.deriveColor(0, 1.1, 0.55, 0.75));
        gc.setFill(gc.getStroke());
        gc.setLineWidth(1.3);
        switch (indice % 7) {
            case 0 -> { } // liso
            case 1 -> { // rayas diagonales /
                gc.strokeLine(-1, LADO + 1, LADO + 1, -1);
                gc.strokeLine(-1, 1, 1, -1);
                gc.strokeLine(LADO - 1, LADO + 1, LADO + 1, LADO - 1);
            }
            case 2 -> gc.fillOval(LADO / 2.0 - 1.6, LADO / 2.0 - 1.6, 3.2, 3.2); // puntos
            case 3 -> gc.strokeLine(0, LADO / 2.0, LADO, LADO / 2.0); // rayas horizontales
            case 4 -> { // rayas diagonales \
                gc.strokeLine(-1, -1, LADO + 1, LADO + 1);
                gc.strokeLine(LADO - 1, -1, LADO + 1, 1);
                gc.strokeLine(-1, LADO - 1, 1, LADO + 1);
            }
            case 5 -> { // cuadrícula
                gc.strokeLine(0, LADO / 2.0, LADO, LADO / 2.0);
                gc.strokeLine(LADO / 2.0, 0, LADO / 2.0, LADO);
            }
            default -> gc.strokeLine(LADO / 2.0, 0, LADO / 2.0, LADO); // rayas verticales
        }
        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        WritableImage img = c.snapshot(p, null);
        return new ImagePattern(img, 0, 0, LADO, LADO, false);
    }
}
