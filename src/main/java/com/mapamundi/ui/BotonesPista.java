package com.mapamundi.ui;

import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Pista;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/** Los botones de pistas de una fase, con su costo; cada pista se puede pedir una vez por ronda. */
final class BotonesPista extends VBox {

    private final Partida partida;
    private final Map<Pista, Button> botones = new EnumMap<>(Pista.class);

    BotonesPista(Partida partida, Pista... pistas) {
        super(4);
        this.partida = partida;
        HBox fila = new HBox(6);
        fila.setAlignment(Pos.CENTER_LEFT);
        for (Pista p : pistas) {
            Button b = new Button();
            b.getStyleClass().add("boton-pista");
            b.setFocusTraversable(false);
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            Tooltip ayuda = new Tooltip(p.descripcion() + " Cuesta " + p.costo() + " puntos de esta ronda.");
            ayuda.setShowDelay(Duration.millis(300));
            b.setTooltip(ayuda);
            b.setOnAction(e -> partida.pedirPista(p));
            botones.put(p, b);
            fila.getChildren().add(b);
        }
        getChildren().addAll(FichaPais.etiqueta("PISTAS · CUESTAN PUNTOS", "titulo-seccion"), fila);
        actualizar();
    }

    void actualizar() {
        botones.forEach((p, b) -> {
            boolean usada = partida.pistas().contains(p);
            b.setText(usada ? "✔ " + p.nombre() : p.nombre() + "  −" + p.costo());
            b.setDisable(!partida.puedePedir(p));
            b.getStyleClass().remove("usada");
            if (usada) b.getStyleClass().add("usada");
        });
    }
}
