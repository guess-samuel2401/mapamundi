package com.mapamundi.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Ventana superpuesta (Récords, Cómo se juega, Ajustes, Logros…) que se cierra con Esc, el botón o un clic fuera. */
final class Dialogo {

    private Dialogo() {
    }

    static StackPane mostrar(StackPane contenedor, String titulo, Node contenido, double ancho, Runnable alCerrar) {
        Button cerrar = new Button(com.mapamundi.i18n.Textos.t("dialogo.cerrar"));
        cerrar.getStyleClass().add("boton-principal");
        cerrar.setCancelButton(true);
        HBox pie = new HBox(cerrar);
        pie.setAlignment(Pos.CENTER_RIGHT);
        VBox caja = new VBox(14, FichaPais.etiqueta(titulo, "titulo-config"), contenido, pie);
        caja.getStyleClass().add("panel");
        caja.setMaxSize(ancho, Region.USE_PREF_SIZE);
        caja.setOnMouseClicked(e -> e.consume());

        StackPane capa = new StackPane(caja);
        capa.getStyleClass().add("capa-dialogo");
        Runnable quitar = () -> {
            if (contenedor.getChildren().remove(capa) && alCerrar != null) alCerrar.run();
        };
        cerrar.setOnAction(e -> quitar.run());
        capa.setOnMouseClicked(e -> quitar.run());
        capa.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                quitar.run();
                e.consume();
            }
        });
        contenedor.getChildren().add(capa);
        FadeTransition entrada = new FadeTransition(Duration.millis(160), capa);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.play();
        cerrar.requestFocus();
        return capa;
    }

    static StackPane mostrar(StackPane contenedor, String titulo, Node contenido) {
        return mostrar(contenedor, titulo, contenido, 620, null);
    }
}
