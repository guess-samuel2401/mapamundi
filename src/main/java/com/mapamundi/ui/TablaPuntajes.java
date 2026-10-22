package com.mapamundi.ui;

import com.mapamundi.i18n.Textos;

import com.mapamundi.juego.Puntajes;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

import java.util.List;

/** "Puntuaciones más altas…" del menú original. Los tres primeros llevan medalla de oro, plata y cobre. */
final class TablaPuntajes extends VBox {

    /** Colores de cada medalla: brillo, cuerpo y borde. El nombre del jugador usa la misma familia de color. */
    private enum Medalla {
        ORO("oro", "#fff1a8", "#d9a400", "#8a6508"),
        PLATA("plata", "#ffffff", "#a3aab5", "#5f6672"),
        COBRE("cobre", "#f7c79c", "#b5652d", "#7a3f17");

        final String clase;
        final Color brillo, cuerpo, borde;

        Medalla(String clase, String brillo, String cuerpo, String borde) {
            this.clase = clase;
            this.brillo = Color.web(brillo);
            this.cuerpo = Color.web(cuerpo);
            this.borde = Color.web(borde);
        }

        static Medalla delPuesto(int indice) {
            return indice < values().length ? values()[indice] : null;
        }
    }

    TablaPuntajes(List<Puntajes.Entrada> lista, Puntajes.Entrada nueva) {
        super(8);
        getStyleClass().addAll("panel", "tabla-puntajes");
        getChildren().add(FichaPais.etiqueta(Textos.t("menu.records.titulo").toUpperCase(Textos.idioma().locale()), "subtitulo-rojo"));
        if (lista.isEmpty()) {
            getChildren().add(FichaPais.etiqueta(Textos.t("records.vacio"), "texto-suave"));
            return;
        }
        GridPane g = new GridPane();
        g.setHgap(0);
        g.setVgap(2);
        ColumnConstraints nombre = new ColumnConstraints();
        nombre.setHgrow(Priority.ALWAYS);
        ColumnConstraints derecha = new ColumnConstraints();
        derecha.setHalignment(HPos.RIGHT);
        // Columna del puesto: medalla de 24 px + borde + relleno de la celda.
        ColumnConstraints puesto = new ColumnConstraints(52);
        puesto.setHalignment(HPos.CENTER);
        g.getColumnConstraints().addAll(puesto, nombre, derecha, new ColumnConstraints(), new ColumnConstraints());
        g.addRow(0, cab("#"), cab(Textos.t("records.nombre")), cab(Textos.t("fin.puntaje")), cab(Textos.t("hud.nivel")),
                cab(Textos.t("records.fecha")));
        for (int i = 0; i < lista.size(); i++) {
            Puntajes.Entrada e = lista.get(i);
            Medalla medalla = Medalla.delPuesto(i);

            Label nombreJugador = FichaPais.etiqueta(e.nombre(), medalla != null ? "nombre-" + medalla.clase : "negrita");
            Node[] celdas = {
                    puesto(i, medalla),
                    nombreJugador,
                    FichaPais.etiqueta(String.valueOf(e.puntaje()), "hud-puntaje"),
                    FichaPais.etiqueta(e.nivel(), ""),
                    FichaPais.etiqueta(e.fecha(), "texto-suave"),
            };
            // La fila recién guardada se resalta por encima de los tintes de medalla.
            String fila = e.equals(nueva) ? "fila-nueva"
                    : medalla != null ? "fila-" + medalla.clase
                    : i % 2 == 1 ? "fila-par" : null;
            for (Node c : celdas) {
                Region r = (Region) c;
                r.getStyleClass().add("celda-puntaje");
                r.setMaxWidth(Double.MAX_VALUE);
                r.setMaxHeight(Double.MAX_VALUE);
                if (fila != null) r.getStyleClass().add(fila);
            }
            g.addRow(i + 1, celdas);
        }
        getChildren().add(g);
    }

    /** El número del puesto; en los tres primeros, dentro de una medalla. */
    private static Node puesto(int indice, Medalla medalla) {
        String numero = String.valueOf(indice + 1);
        if (medalla == null) {
            Label l = FichaPais.etiqueta(numero, "numero-puesto");
            l.setAlignment(Pos.CENTER);
            return l;
        }
        Circle disco = new Circle(12);
        disco.setFill(new RadialGradient(0, 0, 0.35, 0.3, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0, medalla.brillo), new Stop(0.55, medalla.cuerpo), new Stop(1, medalla.borde)));
        disco.setStroke(medalla.borde);
        disco.setStrokeWidth(1.5);
        Label cifra = FichaPais.etiqueta(numero, "cifra-medalla");
        cifra.setTextFill(Color.WHITE);
        StackPane p = new StackPane(disco, cifra);
        p.getStyleClass().addAll("medalla", "medalla-" + medalla.clase);
        p.setMinWidth(Region.USE_PREF_SIZE);
        return p;
    }

    private static Label cab(String texto) {
        Label l = FichaPais.etiqueta(texto.toUpperCase(), "pista-tipo");
        l.getStyleClass().add("celda-puntaje");
        return l;
    }
}
