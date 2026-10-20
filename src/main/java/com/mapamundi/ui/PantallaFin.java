package com.mapamundi.ui;

import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Puntajes;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Fin de la partida: si el puntaje entra en el top 10 se pide el nombre (FrmIngresoDatos del original). */
final class PantallaFin extends VBox {

    PantallaFin(Partida partida, Puntajes puntajes, List<Logro> insignias, Runnable menu, Runnable otraVez) {
        super(14);
        setPadding(new Insets(20, 32, 24, 32));
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(720);

        Label globo = FichaPais.etiqueta("🌍", "");
        globo.setStyle("-fx-font-size: 54px;");
        HBox cifras = new HBox(10,
                cifra("Puntaje", String.valueOf(partida.puntaje())),
                cifra("Países ubicados", String.valueOf(partida.ubicados().size())),
                cifra("Mejor racha", String.valueOf(partida.mejorRacha())),
                cifra("Nivel", partida.nivel().nombre()));
        cifras.setAlignment(Pos.CENTER);

        VBox tarjeta = new VBox(12, globo, FichaPais.etiqueta("Fin del juego", "titulo-mapa-grande"), cifras);
        tarjeta.getStyleClass().add("panel");
        tarjeta.setAlignment(Pos.CENTER);
        if (!insignias.isEmpty()) {
            FlowPane lista = new FlowPane(14, 8);
            lista.setAlignment(Pos.CENTER);
            for (Logro l : insignias) {
                HBox h = new HBox(6, new Insignia(l, true, 16), FichaPais.etiqueta(l.nombre(), "logro-nombre"));
                h.setAlignment(Pos.CENTER_LEFT);
                lista.getChildren().add(h);
            }
            tarjeta.getChildren().addAll(FichaPais.etiqueta("INSIGNIAS CONSEGUIDAS EN ESTA PARTIDA", "titulo-seccion"), lista);
        }

        VBox tabla = new VBox(new TablaPuntajes(puntajes.leer(), null));

        if (puntajes.entraEnLaTabla(partida.puntaje())) {
            TextField nombre = new TextField();
            nombre.setPromptText("¡Nuevo récord! Escribe tu nombre");
            HBox.setHgrow(nombre, Priority.ALWAYS);
            nombre.textProperty().addListener((o, a, b) -> {
                if (b.length() > 20) nombre.setText(b.substring(0, 20));
            });
            Button guardar = new Button("Guardar");
            guardar.getStyleClass().add("boton-principal");
            HBox fila = new HBox(8, nombre, guardar);
            Runnable accion = () -> {
                String texto = nombre.getText().isBlank() ? "Anónimo" : nombre.getText().trim();
                String nivel = partida.nivel().nombre() + (partida.ajustes().contrarreloj() ? " · contrarreloj" : "");
                Puntajes.Entrada e = Puntajes.Entrada.nueva(texto, partida.puntaje(), nivel);
                tabla.getChildren().setAll(new TablaPuntajes(puntajes.guardar(e), e));
                tarjeta.getChildren().remove(fila);
            };
            guardar.setOnAction(e -> accion.run());
            nombre.setOnAction(e -> accion.run());
            tarjeta.getChildren().add(fila);
            Platform.runLater(nombre::requestFocus);
        }

        Button otra = new Button("▶ Jugar de nuevo");
        otra.getStyleClass().add("boton-principal");
        otra.setOnAction(e -> otraVez.run());
        Button volver = new Button("Menú");
        volver.setOnAction(e -> menu.run());
        HBox botones = new HBox(10, otra, volver);
        botones.setAlignment(Pos.CENTER);
        tarjeta.getChildren().add(botones);

        getChildren().addAll(tarjeta, tabla);
    }

    private static VBox cifra(String clave, String valor) {
        VBox v = new VBox(2, FichaPais.etiqueta(clave.toUpperCase(), "hud-clave"), FichaPais.etiqueta(valor, "titulo-mapa"));
        v.setAlignment(Pos.CENTER);
        v.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 8 18 8 18;");
        return v;
    }
}
