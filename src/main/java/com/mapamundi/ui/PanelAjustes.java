package com.mapamundi.ui;

import com.mapamundi.ui.mapa.Pintor;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Ajustes del juego: música, efectos, pantalla completa, modo daltónico y tutorial. Se aplican al momento. */
final class PanelAjustes extends VBox {

    /**
     * @param alCambiarAspecto se llama al activar o desactivar el modo daltónico, para redibujar
     * @param verTutorial      abre el tutorial (null si no se ofrece desde aquí)
     */
    PanelAjustes(Contexto ctx, Runnable alCambiarAspecto, Runnable verTutorial) {
        super(12);
        Preferencias pref = ctx.preferencias();
        Sonidos sonidos = ctx.sonidos();

        // Música
        CheckBox musica = new CheckBox("Música de fondo");
        musica.setSelected(pref.musica);
        Slider volumenMusica = deslizador(pref.volumenMusica);
        volumenMusica.disableProperty().bind(musica.selectedProperty().not());
        musica.selectedProperty().addListener((o, a, b) -> {
            pref.musica = b;
            sonidos.aplicarVolumenMusica();
            pref.guardar();
        });
        volumenMusica.valueProperty().addListener((o, a, b) -> {
            pref.volumenMusica = b.doubleValue() / 100;
            sonidos.aplicarVolumenMusica();
        });
        volumenMusica.valueChangingProperty().addListener((o, a, cambiando) -> {
            if (!cambiando) pref.guardar();
        });

        // Efectos
        Slider volumenEfectos = deslizador(pref.volumenEfectos);
        volumenEfectos.valueProperty().addListener((o, a, b) -> pref.volumenEfectos = b.doubleValue() / 100);
        volumenEfectos.valueChangingProperty().addListener((o, a, cambiando) -> {
            if (!cambiando) {
                pref.guardar();
                sonidos.reproducir(com.mapamundi.juego.Sonido.CONTINENTE_CORRECTO); // para oír el volumen elegido
            }
        });

        // Pantalla completa
        CheckBox completa = new CheckBox("Pantalla completa (F11)");
        completa.setSelected(ctx.escenario().isFullScreen());
        completa.selectedProperty().addListener((o, a, b) -> {
            ctx.escenario().setFullScreen(b);
            pref.pantallaCompleta = b;
            pref.guardar();
        });
        ctx.escenario().fullScreenProperty().addListener((o, a, b) -> completa.setSelected(b));

        // Daltónico
        CheckBox daltonico = new CheckBox("Modo daltónico");
        daltonico.setSelected(pref.daltonico);
        Label ayudaDaltonico = FichaPais.etiqueta(
                "Colores pensados para el daltonismo y una trama distinta en cada país, para no depender solo del color.",
                "texto-suave");
        ayudaDaltonico.setWrapText(true);
        daltonico.selectedProperty().addListener((o, a, b) -> {
            pref.daltonico = b;
            pref.guardar();
            Pintor.daltonico(b);
            if (ctx.escenario().getScene() != null) {
                ctx.escenario().getScene().getRoot().getStyleClass().remove("daltonico");
                if (b) ctx.escenario().getScene().getRoot().getStyleClass().add("daltonico");
            }
            alCambiarAspecto.run();
        });

        getChildren().addAll(
                seccion("SONIDO"),
                musica, fila("Volumen de la música", volumenMusica),
                fila("Volumen de los efectos", volumenEfectos),
                seccion("PANTALLA"),
                completa,
                seccion("ACCESIBILIDAD"),
                daltonico, ayudaDaltonico);
        if (verTutorial != null) {
            Button tutorial = new Button("🎓  Ver el tutorial otra vez");
            tutorial.setOnAction(e -> verTutorial.run());
            getChildren().addAll(seccion("AYUDA"), tutorial);
        }
        setPrefWidth(460);
    }

    private static Slider deslizador(double valor) {
        Slider s = new Slider(0, 100, Math.round(valor * 100));
        s.setPrefWidth(220);
        return s;
    }

    private static HBox fila(String texto, Slider deslizador) {
        Label nombre = FichaPais.etiqueta(texto, "");
        Label porcentaje = FichaPais.etiqueta("", "negrita");
        porcentaje.setMinWidth(44);
        porcentaje.textProperty().bind(deslizador.valueProperty().map(v -> Math.round(v.doubleValue()) + " %"));
        HBox.setHgrow(deslizador, Priority.ALWAYS);
        HBox h = new HBox(10, nombre, deslizador, porcentaje);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Label seccion(String texto) {
        return FichaPais.etiqueta(texto, "titulo-seccion");
    }
}
