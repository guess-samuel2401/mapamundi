package com.mapamundi.ui;

import com.mapamundi.ui.mapa.Pintor;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Las vidas del panel superior, como corazones. Al perder una, el corazón se infla, tiembla y se parte
 * en dos mitades que caen; queda un corazón vacío. Con la última vida, el corazón que queda late.
 */
final class Corazones extends HBox {

    /** Corazón en una caja de 24 × 24. */
    private static final String FORMA = "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09"
            + "C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z";
    private static final LinearGradient ROJO = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ff5a6e")), new Stop(1, Color.web("#c81d33")));

    private final List<StackPane> huecos = new ArrayList<>();
    private final List<SVGPath> llenos = new ArrayList<>();
    private int mostradas = -1;
    private ScaleTransition latido;

    Corazones(int total) {
        super(3);
        setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < total; i++) {
            SVGPath lleno = corazonLleno();
            StackPane hueco = new StackPane(corazonVacio(), lleno);
            hueco.setMinSize(24, 24);
            hueco.setPrefSize(24, 24);
            huecos.add(hueco);
            llenos.add(lleno);
            getChildren().add(hueco);
        }
    }

    /** Muestra {@code vidas} corazones llenos; si son menos que antes, anima los que se pierden. */
    void mostrar(int vidas) {
        vidas = Math.max(0, Math.min(vidas, llenos.size()));
        if (vidas == mostradas) return;
        boolean perdidas = mostradas >= 0 && vidas < mostradas;
        for (int i = 0; i < llenos.size(); i++) {
            if (perdidas && i >= vidas && i < mostradas) romper(i);
            else llenos.get(i).setVisible(i < vidas);
        }
        mostradas = vidas;
        actualizarLatido(vidas);
    }

    private void romper(int i) {
        SVGPath lleno = llenos.get(i);
        StackPane hueco = huecos.get(i);
        lleno.setVisible(true);
        hueco.setViewOrder(-1); // que las mitades caigan por encima de los vecinos

        // 1. Se infla de golpe y tiembla.
        ScaleTransition inflar = new ScaleTransition(Duration.millis(140), lleno);
        inflar.setToX(1.5);
        inflar.setToY(1.5);
        inflar.setInterpolator(Interpolator.EASE_OUT);
        RotateTransition temblar = new RotateTransition(Duration.millis(45), lleno);
        temblar.setFromAngle(-16);
        temblar.setToAngle(16);
        temblar.setCycleCount(6);
        temblar.setAutoReverse(true);

        SequentialTransition secuencia = new SequentialTransition(inflar, temblar);
        secuencia.setOnFinished(e -> {
            lleno.setVisible(false);
            lleno.setScaleX(1);
            lleno.setScaleY(1);
            lleno.setRotate(0);
            partirEnDos(hueco);
        });
        secuencia.play();
    }

    /** 2. Dos mitades del corazón caen girando hacia lados opuestos y se desvanecen. */
    private void partirEnDos(StackPane hueco) {
        List<ParallelTransition> caidas = new ArrayList<>();
        for (int lado = -1; lado <= 1; lado += 2) {
            SVGPath mitad = corazonLleno();
            mitad.setClip(lado < 0 ? new Rectangle(0, 0, 12, 24) : new Rectangle(12, 0, 12, 24));
            mitad.setScaleX(1.5);
            mitad.setScaleY(1.5);
            hueco.getChildren().add(mitad);

            ScaleTransition encoger = new ScaleTransition(Duration.millis(520), mitad);
            encoger.setToX(0.9);
            encoger.setToY(0.9);
            TranslateTransition caer = new TranslateTransition(Duration.millis(520), mitad);
            caer.setByX(9 * lado);
            caer.setByY(22);
            caer.setInterpolator(Interpolator.EASE_IN);
            RotateTransition girar = new RotateTransition(Duration.millis(520), mitad);
            girar.setByAngle(35 * lado);
            FadeTransition apagar = new FadeTransition(Duration.millis(520), mitad);
            apagar.setToValue(0);
            ParallelTransition caida = new ParallelTransition(encoger, caer, girar, apagar);
            caida.setOnFinished(e -> hueco.getChildren().remove(mitad));
            caidas.add(caida);
        }
        // 3. El corazón vacío aparece suavemente.
        hueco.getChildren().getFirst().setOpacity(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(350), hueco.getChildren().getFirst());
        aparecer.setToValue(1);
        ParallelTransition todo = new ParallelTransition(caidas.get(0), caidas.get(1), aparecer);
        todo.setOnFinished(e -> hueco.setViewOrder(0));
        todo.play();
    }

    /** Con una sola vida, el corazón que queda late como aviso. */
    private void actualizarLatido(int vidas) {
        if (latido != null) {
            latido.stop();
            latido.getNode().setScaleX(1);
            latido.getNode().setScaleY(1);
            latido = null;
        }
        if (vidas != 1) return;
        latido = new ScaleTransition(Duration.millis(420), llenos.getFirst());
        latido.setFromX(1);
        latido.setFromY(1);
        latido.setToX(1.22);
        latido.setToY(1.22);
        latido.setAutoReverse(true);
        latido.setCycleCount(Timeline.INDEFINITE);
        latido.setInterpolator(Interpolator.EASE_BOTH);
        latido.play();
    }

    private static SVGPath corazonLleno() {
        SVGPath s = new SVGPath();
        s.setContent(FORMA);
        s.setFill(ROJO);
        s.setStroke(Pintor.TINTA);
        s.setStrokeWidth(1.4);
        return s;
    }

    private static SVGPath corazonVacio() {
        SVGPath s = new SVGPath();
        s.setContent(FORMA);
        s.setFill(Color.web("#e8e2d4"));
        s.setStroke(Color.web("#4a5878", 0.55));
        s.setStrokeWidth(1.4);
        s.getStrokeDashArray().setAll(3.0, 2.0);
        return s;
    }
}
