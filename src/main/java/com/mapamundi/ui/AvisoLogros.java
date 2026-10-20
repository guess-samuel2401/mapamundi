package com.mapamundi.ui;

import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.juego.Sonido;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/** Aviso "¡Insignia conseguida!" que entra por la derecha, se queda unos segundos y se desvanece. */
final class AvisoLogros extends VBox {

    private final Sonidos sonidos;

    AvisoLogros(Sonidos sonidos) {
        super(8);
        this.sonidos = sonidos;
        setAlignment(Pos.TOP_RIGHT);
        setPickOnBounds(false);
        setMouseTransparent(true);
        setPadding(new Insets(120, 22, 0, 0));
        StackPane.setAlignment(this, Pos.TOP_RIGHT);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    void mostrar(List<Logro> logros) {
        for (int i = 0; i < logros.size(); i++) {
            mostrar(logros.get(i), Duration.millis(i * 700));
        }
    }

    private void mostrar(Logro l, Duration retraso) {
        Insignia insignia = new Insignia(l, true, 22);
        VBox texto = new VBox(0, FichaPais.etiqueta("¡INSIGNIA CONSEGUIDA!", "titulo-seccion"),
                FichaPais.etiqueta(l.nombre(), "logro-nombre"));
        HBox tarjeta = new HBox(10, insignia, texto);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.getStyleClass().add("aviso-logro");
        tarjeta.setOpacity(0);
        getChildren().add(tarjeta);

        TranslateTransition entrar = new TranslateTransition(Duration.millis(380), tarjeta);
        entrar.setFromX(320);
        entrar.setToX(0);
        FadeTransition aparecer = new FadeTransition(Duration.millis(250), tarjeta);
        aparecer.setFromValue(0);
        aparecer.setToValue(1);
        ScaleTransition latido = new ScaleTransition(Duration.millis(220), insignia);
        latido.setFromX(0.4);
        latido.setFromY(0.4);
        latido.setToX(1);
        latido.setToY(1);
        FadeTransition desaparecer = new FadeTransition(Duration.millis(450), tarjeta);
        desaparecer.setToValue(0);

        SequentialTransition todo = new SequentialTransition(
                new PauseTransition(retraso),
                new ParallelTransition(entrar, aparecer),
                latido,
                new PauseTransition(Duration.seconds(3)),
                desaparecer);
        todo.getChildren().get(0).setOnFinished(e -> sonidos.reproducir(Sonido.LOGRO));
        todo.setOnFinished(e -> getChildren().remove(tarjeta));
        todo.play();
    }
}
