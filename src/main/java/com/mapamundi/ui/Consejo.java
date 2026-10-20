package com.mapamundi.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Consejo de la primera partida: una franja abajo que se va sola o con «Entendido». */
final class Consejo extends HBox {

    private Consejo(String texto, Runnable alCerrar) {
        super(12);
        // Los emoji en color no existen en las fuentes de JavaFX: una "i" en un círculo amarillo.
        Label icono = FichaPais.etiqueta("i", "consejo-icono");
        icono.setMinSize(30, 30);
        icono.setAlignment(Pos.CENTER);
        Label mensaje = FichaPais.etiqueta(texto, "consejo-texto");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(560);
        Button entendido = new Button("Entendido");
        entendido.getStyleClass().add("boton-principal");
        entendido.setFocusTraversable(false);
        entendido.setOnAction(e -> alCerrar.run());
        getChildren().addAll(icono, mensaje, entendido);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("consejo");
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        StackPane.setAlignment(this, Pos.BOTTOM_CENTER);
        StackPane.setMargin(this, new Insets(0, 0, 22, 0));
    }

    /** Muestra el consejo en el contenedor; desaparece solo a los 9 segundos. */
    static Consejo mostrar(StackPane contenedor, String texto) {
        Consejo[] c = new Consejo[1];
        c[0] = new Consejo(texto, () -> quitar(contenedor, c[0]));
        contenedor.getChildren().add(c[0]);
        TranslateTransition subir = new TranslateTransition(Duration.millis(300), c[0]);
        subir.setFromY(80);
        subir.setToY(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(300), c[0]);
        aparecer.setFromValue(0);
        aparecer.setToValue(1);
        subir.play();
        aparecer.play();
        PauseTransition espera = new PauseTransition(Duration.seconds(9));
        espera.setOnFinished(e -> quitar(contenedor, c[0]));
        espera.play();
        return c[0];
    }

    static void quitar(StackPane contenedor, Consejo c) {
        if (c == null || !contenedor.getChildren().contains(c)) return;
        FadeTransition f = new FadeTransition(Duration.millis(250), c);
        f.setToValue(0);
        f.setOnFinished(e -> contenedor.getChildren().remove(c));
        new SequentialTransition(f).play();
    }
}
