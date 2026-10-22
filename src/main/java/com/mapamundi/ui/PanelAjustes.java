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

import static com.mapamundi.i18n.Textos.t;

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
        CheckBox musica = new CheckBox(t("ajustes.musica"));
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
        CheckBox completa = new CheckBox(t("ajustes.pantallaCompleta") + " (F11)");
        completa.setSelected(ctx.escenario().isFullScreen());
        completa.selectedProperty().addListener((o, a, b) -> {
            ctx.escenario().setFullScreen(b);
            pref.pantallaCompleta = b;
            pref.guardar();
        });
        ctx.escenario().fullScreenProperty().addListener((o, a, b) -> completa.setSelected(b));

        // Daltónico
        CheckBox daltonico = new CheckBox(t("ajustes.daltonico"));
        daltonico.setSelected(pref.daltonico);
        Label ayudaDaltonico = FichaPais.etiqueta(t("ajustes.daltonico.ayuda"), "texto-suave");
        ayudaDaltonico.setWrapText(true);
        daltonico.selectedProperty().addListener((o, a, b) -> {
            aplicarDaltonico(ctx, b);
            alCambiarAspecto.run();
        });

        getChildren().addAll(
                seccion(t("ajustes.sonido")),
                musica, fila(t("ajustes.volumenMusica"), volumenMusica),
                fila(t("ajustes.volumenEfectos"), volumenEfectos),
                seccion(t("ajustes.pantalla")),
                completa,
                seccion(t("ajustes.accesibilidad")),
                daltonico, ayudaDaltonico);
        if (verTutorial != null) {
            Button tutorial = new Button("🎓  " + t("ajustes.tutorial"));
            tutorial.setOnAction(e -> verTutorial.run());
            getChildren().addAll(seccion(t("ajustes.ayuda")), tutorial);
        }
        setPrefWidth(460);
    }

    /** Activa o quita el modo daltónico y lo guarda (desde Ajustes o desde la pantalla de idioma). */
    static void aplicarDaltonico(Contexto ctx, boolean activo) {
        Preferencias pref = ctx.preferencias();
        pref.daltonico = activo;
        pref.guardar();
        Pintor.daltonico(activo);
        if (ctx.escenario().getScene() != null) {
            ctx.escenario().getScene().getRoot().getStyleClass().remove("daltonico");
            if (activo) ctx.escenario().getScene().getRoot().getStyleClass().add("daltonico");
        }
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
