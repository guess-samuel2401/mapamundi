package com.mapamundi.ui;

import com.mapamundi.juego.Logros.Categoria;
import com.mapamundi.juego.Logros.Logro;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

/** La medalla de un logro: bronce, plata u oro con su símbolo; gris mientras está bloqueado. */
final class Insignia extends StackPane {

    Insignia(Logro logro, boolean conseguida, double radio) {
        Color brillo, cuerpo, borde;
        if (!conseguida) {
            brillo = Color.web("#f2f0ea");
            cuerpo = Color.web("#cfcbc2");
            borde = Color.web("#9a958b");
        } else if (logro.categoria() == Categoria.ORO) {
            brillo = Color.web("#fff1a8");
            cuerpo = Color.web("#e0a800");
            borde = Color.web("#8a6508");
        } else if (logro.categoria() == Categoria.PLATA) {
            brillo = Color.web("#ffffff");
            cuerpo = Color.web("#a3aab5");
            borde = Color.web("#5f6672");
        } else {
            brillo = Color.web("#f7c79c");
            cuerpo = Color.web("#b5652d");
            borde = Color.web("#7a3f17");
        }
        Circle anillo = new Circle(radio);
        anillo.setFill(borde);
        Circle disco = new Circle(radio * 0.86);
        disco.setFill(new RadialGradient(0, 0, 0.35, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, brillo), new Stop(0.6, cuerpo), new Stop(1, borde)));
        Label icono = new Label(logro.icono());
        icono.getStyleClass().add("icono-insignia");
        icono.setStyle("-fx-font-size: " + Math.round(radio * (logro.icono().length() > 2 ? 0.62 : 0.85)) + "px;");
        icono.setTextFill(conseguida ? Color.WHITE : Color.web("#8e897f"));
        if (conseguida) icono.setEffect(new DropShadow(2, 0, 1, Color.web("#000000", 0.55)));
        getChildren().addAll(anillo, disco, icono);
        setMinSize(radio * 2, radio * 2);
        setMaxSize(radio * 2, radio * 2);
        if (conseguida) setEffect(new DropShadow(6, 0, 2, Color.web("#000000", 0.25)));
    }
}
