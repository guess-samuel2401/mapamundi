package com.mapamundi.ui;

import com.mapamundi.i18n.Textos;
import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.PartidaClasica;
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

import static com.mapamundi.i18n.Textos.t;

/**
 * Fin de la partida, sencillo: un mensaje según cómo fue, el puntaje en grande, los países ubicados y dos
 * botones (jugar otra vez o cambiar los continentes). Solo si el puntaje entra en el top 10 se pide el nombre
 * (FrmIngresoDatos del original).
 */
final class PantallaFin extends VBox {

    /**
     * Lo que se muestra al final, venga de una partida moderna o clásica.
     *
     * @param ganada      se completaron todos los mapas (estilo clásico)
     * @param nivelRecord lo que se guarda como "nivel" en la tabla de récords
     */
    record Resumen(int puntaje, int ubicados, boolean ganada, String nivelRecord) {

        static Resumen de(Partida p) {
            String nivel = p.nivel().nombre();
            return new Resumen(p.puntaje(), p.ubicados().size(), false,
                    nivel + (p.ajustes().contrarreloj() ? " · " + t("regla.contrarreloj").toLowerCase() : ""));
        }

        static Resumen de(PartidaClasica p) {
            return new Resumen(p.puntaje(), p.ubicados().size(), p.ganada(),
                    PantallaEleccion.zona(p.enJuego()) + " · " + p.ajustes().modo().nombre());
        }

        /** Un mensaje (y su dibujo) según lo bien que fue. */
        String[] mensaje() {
            if (ganada) return new String[]{"🏆", t("clasico.fin.ganada")};
            if (ubicados == 0) return new String[]{"💪", t("fin.mensaje.0")};
            if (ubicados < 5) return new String[]{"🙂", t("fin.mensaje.1")};
            if (ubicados < 15) return new String[]{"😃", t("fin.mensaje.2")};
            return new String[]{"🌟", t("fin.mensaje.3")};
        }
    }

    PantallaFin(Resumen r, Puntajes puntajes, List<Logro> insignias, Runnable cambiar, Runnable otraVez) {
        super(14);
        setPadding(new Insets(24, 32, 24, 32));
        setAlignment(Pos.CENTER);
        setMaxWidth(560);

        String[] mensaje = r.mensaje();
        Label dibujo = FichaPais.etiqueta(mensaje[0], "");
        dibujo.setStyle("-fx-font-size: 60px;");
        Label titulo = FichaPais.etiqueta(mensaje[1], "titulo-mapa-grande");
        titulo.setWrapText(true);
        Label puntos = FichaPais.etiqueta(Textos.entero(r.puntaje()), "fin-puntaje");
        Label clave = FichaPais.etiqueta(t("fin.puntaje").toUpperCase(Textos.idioma().locale()), "hud-clave");
        Label ubicados = FichaPais.etiqueta(t("fin.paisesUbicados", r.ubicados()), "negrita");
        VBox tarjeta = new VBox(10, dibujo, titulo, clave, puntos, ubicados);
        tarjeta.getStyleClass().add("panel");
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(24, 34, 24, 34));
        if (!insignias.isEmpty()) {
            FlowPane lista = new FlowPane(14, 8);
            lista.setAlignment(Pos.CENTER);
            for (Logro l : insignias) {
                HBox h = new HBox(6, new Insignia(l, true, 16), FichaPais.etiqueta(l.nombre(), "logro-nombre"));
                h.setAlignment(Pos.CENTER_LEFT);
                lista.getChildren().add(h);
            }
            tarjeta.getChildren().addAll(FichaPais.etiqueta(t("fin.insignias"), "titulo-seccion"), lista);
        }

        if (puntajes.entraEnLaTabla(r.puntaje())) {
            Label record = FichaPais.etiqueta("🏆 " + t("fin.nuevoRecord"), "acierto");
            TextField nombre = new TextField();
            nombre.setPromptText(t("fin.tuNombre"));
            HBox.setHgrow(nombre, Priority.ALWAYS);
            nombre.textProperty().addListener((o, a, b) -> {
                if (b.length() > 20) nombre.setText(b.substring(0, 20));
            });
            Button guardar = new Button(t("fin.guardar"));
            guardar.getStyleClass().add("boton-principal");
            HBox fila = new HBox(8, nombre, guardar);
            VBox pedir = new VBox(6, record, fila);
            pedir.setAlignment(Pos.CENTER);
            Runnable accion = () -> {
                String texto = nombre.getText().isBlank() ? t("fin.anonimo") : nombre.getText().trim();
                puntajes.guardar(Puntajes.Entrada.nueva(texto, r.puntaje(), r.nivelRecord()));
                pedir.getChildren().setAll(FichaPais.etiqueta("✓ " + t("fin.guardado"), "acierto"));
            };
            guardar.setOnAction(e -> accion.run());
            nombre.setOnAction(e -> accion.run());
            tarjeta.getChildren().add(pedir);
            Platform.runLater(nombre::requestFocus);
        }

        Button otra = new Button("▶  " + t("fin.otraVez"));
        otra.getStyleClass().addAll("boton-principal", "boton-continuar-eleccion");
        otra.setOnAction(e -> otraVez.run());
        Button volver = new Button(t("fin.cambiar"));
        volver.setOnAction(e -> cambiar.run());
        VBox botones = new VBox(10, otra, volver);
        botones.setAlignment(Pos.CENTER);
        tarjeta.getChildren().add(botones);

        getChildren().add(tarjeta);
    }
}
