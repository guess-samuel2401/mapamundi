package com.mapamundi.ui;

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

/** Fin de la partida: si el puntaje entra en el top 10 se pide el nombre (FrmIngresoDatos del original). */
final class PantallaFin extends VBox {

    /**
     * Lo que se muestra al final, venga de una partida moderna o clásica.
     *
     * @param tercera     la tercera cifra (mejor racha en la moderna, errores en la clásica): {clave, valor}
     * @param nivelRecord lo que se guarda como "nivel" en la tabla de récords
     */
    record Resumen(String titulo, int puntaje, int ubicados, String[] tercera, String nivel, String nivelRecord) {

        static Resumen de(Partida p) {
            String nivel = p.nivel().nombre();
            return new Resumen(t("fin.titulo"), p.puntaje(), p.ubicados().size(),
                    new String[]{t("fin.mejorRacha"), String.valueOf(p.mejorRacha())}, nivel,
                    nivel + (p.ajustes().contrarreloj() ? " · " + t("regla.contrarreloj").toLowerCase() : ""));
        }

        static Resumen de(PartidaClasica p) {
            String estilo = t("clasico.nombre");
            return new Resumen(t(p.ganada() ? "clasico.fin.ganada" : "fin.titulo"), p.puntaje(), p.ubicados().size(),
                    new String[]{t("hud.errores"), p.errores() + "/" + PartidaClasica.ERRORES_MAX}, estilo,
                    estilo + " · " + p.ajustes().modo().nombre());
        }
    }

    PantallaFin(Resumen r, Puntajes puntajes, List<Logro> insignias, Runnable menu, Runnable otraVez) {
        super(14);
        setPadding(new Insets(20, 32, 24, 32));
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(720);

        Label globo = FichaPais.etiqueta("🌍", "");
        globo.setStyle("-fx-font-size: 54px;");
        HBox cifras = new HBox(10,
                cifra(t("fin.puntaje"), String.valueOf(r.puntaje())),
                cifra(t("fin.ubicados"), String.valueOf(r.ubicados())),
                cifra(r.tercera()[0], r.tercera()[1]),
                cifra(t("hud.nivel"), r.nivel()));
        cifras.setAlignment(Pos.CENTER);

        Label titulo = FichaPais.etiqueta(r.titulo(), "titulo-mapa-grande");
        titulo.setWrapText(true);
        VBox tarjeta = new VBox(12, globo, titulo, cifras);
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
            tarjeta.getChildren().addAll(FichaPais.etiqueta(t("fin.insignias"), "titulo-seccion"), lista);
        }

        VBox tabla = new VBox(new TablaPuntajes(puntajes.leer(), null));

        if (puntajes.entraEnLaTabla(r.puntaje())) {
            TextField nombre = new TextField();
            nombre.setPromptText(t("fin.nuevoRecord"));
            HBox.setHgrow(nombre, Priority.ALWAYS);
            nombre.textProperty().addListener((o, a, b) -> {
                if (b.length() > 20) nombre.setText(b.substring(0, 20));
            });
            Button guardar = new Button(t("fin.guardar"));
            guardar.getStyleClass().add("boton-principal");
            HBox fila = new HBox(8, nombre, guardar);
            Runnable accion = () -> {
                String texto = nombre.getText().isBlank() ? t("fin.anonimo") : nombre.getText().trim();
                Puntajes.Entrada e = Puntajes.Entrada.nueva(texto, r.puntaje(), r.nivelRecord());
                tabla.getChildren().setAll(new TablaPuntajes(puntajes.guardar(e), e));
                tarjeta.getChildren().remove(fila);
            };
            guardar.setOnAction(e -> accion.run());
            nombre.setOnAction(e -> accion.run());
            tarjeta.getChildren().add(fila);
            Platform.runLater(nombre::requestFocus);
        }

        Button otra = new Button("▶ " + t("fin.otraVez"));
        otra.getStyleClass().add("boton-principal");
        otra.setOnAction(e -> otraVez.run());
        Button volver = new Button(t("fin.menu"));
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
