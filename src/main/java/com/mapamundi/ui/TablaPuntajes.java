package com.mapamundi.ui;

import com.mapamundi.juego.Puntajes;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** "Puntuaciones más altas…" del menú original. */
final class TablaPuntajes extends VBox {

    TablaPuntajes(List<Puntajes.Entrada> lista, Puntajes.Entrada nueva) {
        super(8);
        getStyleClass().addAll("panel", "tabla-puntajes");
        getChildren().add(FichaPais.etiqueta("PUNTUACIONES MÁS ALTAS", "subtitulo-rojo"));
        if (lista.isEmpty()) {
            getChildren().add(FichaPais.etiqueta("Todavía no hay puntuaciones. ¡Sé el primero!", "texto-suave"));
            return;
        }
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(2);
        ColumnConstraints nombre = new ColumnConstraints();
        nombre.setHgrow(Priority.ALWAYS);
        ColumnConstraints derecha = new ColumnConstraints();
        derecha.setHalignment(HPos.RIGHT);
        g.getColumnConstraints().addAll(new ColumnConstraints(), nombre, derecha, new ColumnConstraints(), new ColumnConstraints());
        g.addRow(0, cab("#"), cab("Nombre"), cab("Puntaje"), cab("Nivel"), cab("Fecha"));
        for (int i = 0; i < lista.size(); i++) {
            Puntajes.Entrada e = lista.get(i);
            Label[] celdas = {FichaPais.etiqueta(String.valueOf(i + 1), ""), FichaPais.etiqueta(e.nombre(), "negrita"),
                    FichaPais.etiqueta(String.valueOf(e.puntaje()), "hud-puntaje"), FichaPais.etiqueta(e.nivel(), ""),
                    FichaPais.etiqueta(e.fecha(), "texto-suave")};
            String clase = e.equals(nueva) ? "fila-nueva" : i % 2 == 1 ? "fila-par" : null;
            for (Label c : celdas) {
                c.setMaxWidth(Double.MAX_VALUE);
                if (clase != null) c.getStyleClass().add(clase);
            }
            g.addRow(i + 1, celdas);
        }
        getChildren().add(g);
    }

    private static Label cab(String texto) {
        return FichaPais.etiqueta(texto.toUpperCase(), "pista-tipo");
    }
}
