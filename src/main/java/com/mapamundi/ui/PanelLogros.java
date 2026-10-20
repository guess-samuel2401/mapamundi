package com.mapamundi.ui;

import com.mapamundi.juego.Logros;
import com.mapamundi.juego.Logros.Logro;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Todas las insignias: las conseguidas en color, las pendientes en gris con su avance. */
final class PanelLogros extends VBox {

    private static final int COLUMNAS = 2;

    PanelLogros(Logros logros) {
        super(10);
        int total = Logro.values().length;
        int hechos = logros.cantidadDesbloqueados();
        ProgressBar general = new ProgressBar((double) hechos / total);
        general.getStyleClass().add("barra-logros");
        general.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(general, Priority.ALWAYS);
        HBox resumen = new HBox(12, FichaPais.etiqueta(hechos + " de " + total + " insignias", "negrita"), general);
        resumen.setAlignment(Pos.CENTER_LEFT);

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        rejilla.setPadding(new Insets(0, 4, 0, 0));
        for (int c = 0; c < COLUMNAS; c++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(100.0 / COLUMNAS);
            columna.setHgrow(Priority.ALWAYS);
            rejilla.getColumnConstraints().add(columna);
        }
        int i = 0;
        for (Logro l : Logro.values()) {
            rejilla.add(tarjeta(logros, l), i % COLUMNAS, i / COLUMNAS);
            i++;
        }
        ScrollPane desplazable = new ScrollPane(rejilla);
        desplazable.setFitToWidth(true);
        desplazable.setPrefViewportHeight(430);
        getChildren().addAll(resumen, desplazable);
        setPrefWidth(660);
    }

    private static HBox tarjeta(Logros logros, Logro l) {
        boolean conseguida = logros.desbloqueado(l);
        Label nombre = FichaPais.etiqueta(l.nombre(), conseguida ? "logro-nombre" : "logro-nombre-bloqueado");
        Label descripcion = FichaPais.etiqueta(l.descripcion(), "texto-suave");
        descripcion.setWrapText(true);
        VBox texto = new VBox(2, nombre, descripcion);
        if (conseguida) {
            texto.getChildren().add(FichaPais.etiqueta("Conseguida el " + logros.fecha(l), "logro-fecha"));
        } else {
            int[] avance = logros.progreso(l);
            if (avance != null) {
                ProgressBar barra = new ProgressBar((double) Math.min(avance[0], avance[1]) / avance[1]);
                barra.getStyleClass().add("barra-logros");
                barra.setPrefWidth(150);
                HBox fila = new HBox(8, barra, FichaPais.etiqueta(Math.min(avance[0], avance[1]) + "/" + avance[1], "texto-suave"));
                fila.setAlignment(Pos.CENTER_LEFT);
                texto.getChildren().add(fila);
            }
        }
        HBox.setHgrow(texto, Priority.ALWAYS);
        HBox h = new HBox(12, new Insignia(l, conseguida, 24), texto);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().addAll("tarjeta-logro", conseguida ? "conseguida" : "bloqueada");
        h.setMaxWidth(Double.MAX_VALUE);
        h.setMaxHeight(Double.MAX_VALUE);
        return h;
    }
}
