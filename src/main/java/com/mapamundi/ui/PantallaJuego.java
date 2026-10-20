package com.mapamundi.ui;

import com.mapamundi.juego.Partida;
import com.mapamundi.modelo.Atlas;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Pantalla de juego: panel superior, la fase actual, la pausa y el cartel de nivel superado. */
final class PantallaJuego extends StackPane {

    interface AlTerminar {
        void fin(Partida partida);
    }

    private final Partida partida;
    private final Atlas atlas;
    private final Sonidos sonidos;
    private final Hud hud = new Hud();
    private final BorderPane cuerpo = new BorderPane();
    private final StackPane capaPausa;
    private final Timeline reloj;
    private FaseContinente faseContinente;
    private FaseMapa faseMapa;
    private Partida.Fase faseAnterior;
    private String ultimoCartel;
    // Una sola instancia, para poder quitarla después con removeEventFilter.
    private final EventHandler<KeyEvent> manejadorTeclas = this::tecla;

    PantallaJuego(Partida partida, Atlas atlas, Sonidos sonidos, Runnable salir, AlTerminar alTerminar) {
        this.partida = partida;
        this.atlas = atlas;
        this.sonidos = sonidos;
        cuerpo.setTop(hud);
        capaPausa = construirPausa(salir);
        getChildren().addAll(cuerpo, capaPausa);

        hud.sonido.setOnAction(e -> {
            sonidos.alternarSilencio();
            actualizar();
        });
        hud.pausa.setOnAction(e -> partida.alternarPausa());

        reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> partida.tic()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();

        partida.alSonar(sonidos::reproducir);
        partida.alCambiar(() -> {
            if (partida.fase() == Partida.Fase.FIN) {
                reloj.stop();
                alTerminar.fin(partida);
            } else {
                actualizar();
            }
        });
        sceneProperty().addListener((o, antes, escena) -> {
            if (antes != null) antes.removeEventFilter(KeyEvent.KEY_PRESSED, manejadorTeclas);
            if (escena != null) escena.addEventFilter(KeyEvent.KEY_PRESSED, manejadorTeclas);
        });
        actualizar();
    }

    private StackPane construirPausa(Runnable salir) {
        Button seguir = new Button("Continuar (F5)");
        seguir.getStyleClass().add("boton-principal");
        seguir.setMaxWidth(Double.MAX_VALUE);
        seguir.setOnAction(e -> partida.alternarPausa());
        Button menu = new Button("Salir al menú");
        menu.setMaxWidth(Double.MAX_VALUE);
        menu.setOnAction(e -> {
            reloj.stop();
            salir.run();
        });
        VBox caja = new VBox(12, FichaPais.etiqueta("Pausa", "titulo-mapa-grande"),
                FichaPais.etiqueta("El cronómetro está detenido.", ""), seguir, menu);
        caja.getStyleClass().add("panel");
        caja.setAlignment(Pos.CENTER);
        caja.setMaxSize(320, USE_PREF_SIZE);
        StackPane capa = new StackPane(caja);
        capa.getStyleClass().add("capa-pausa");
        capa.setVisible(false);
        return capa;
    }

    private void tecla(KeyEvent e) {
        KeyCode k = e.getCode();
        if (k == KeyCode.F5 || k == KeyCode.P || k == KeyCode.ESCAPE) {
            partida.alternarPausa();
            e.consume();
            return;
        }
        if (partida.fase() == Partida.Fase.CONTINENTE && faseContinente != null) faseContinente.tecla(e);
        else if (faseMapa != null) faseMapa.tecla(e);
    }

    private void actualizar() {
        hud.actualizar(partida, sonidos.enSilencio());
        capaPausa.setVisible(partida.enPausa());

        Partida.Fase fase = partida.fase();
        boolean cambioDeFase = fase != faseAnterior && !(fase == Partida.Fase.RESULTADO && faseAnterior == Partida.Fase.UBICACION);
        if (cambioDeFase) {
            Node vista;
            // Si se agotan los intentos del continente, el resultado se muestra sobre el mapa.
            if (fase == Partida.Fase.CONTINENTE) {
                faseContinente = new FaseContinente(partida, atlas);
                faseMapa = null;
                vista = faseContinente;
            } else {
                faseMapa = new FaseMapa(partida, atlas);
                faseContinente = null;
                vista = faseMapa;
            }
            BorderPane.setMargin(vista, new Insets(16));
            cuerpo.setCenter(vista);
            FadeTransition entrada = new FadeTransition(Duration.millis(220), vista);
            entrada.setFromValue(0);
            entrada.setToValue(1);
            entrada.play();
        } else if (faseContinente != null) {
            faseContinente.actualizar();
        } else if (faseMapa != null) {
            faseMapa.actualizar();
        }
        faseAnterior = fase;

        String cartel = partida.nivelSuperado();
        if (cartel != null && !cartel.equals(ultimoCartel)) mostrarCartel(cartel);
        ultimoCartel = cartel;
    }

    private void mostrarCartel(String nivel) {
        VBox caja = new VBox(2, FichaPais.etiqueta("¡NIVEL SUPERADO!", "negrita"), FichaPais.etiqueta(nivel, "titulo-mapa-grande"));
        caja.getStyleClass().add("cartel-nivel");
        caja.setAlignment(Pos.CENTER);
        caja.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        caja.setMouseTransparent(true);
        StackPane.setAlignment(caja, Pos.TOP_CENTER);
        StackPane.setMargin(caja, new Insets(110, 0, 0, 0));
        getChildren().add(caja);
        FadeTransition entra = new FadeTransition(Duration.millis(250), caja);
        entra.setFromValue(0);
        entra.setToValue(1);
        FadeTransition sale = new FadeTransition(Duration.millis(400), caja);
        sale.setToValue(0);
        SequentialTransition s = new SequentialTransition(entra, new PauseTransition(Duration.seconds(2)), sale);
        s.setOnFinished(e -> getChildren().remove(caja));
        s.play();
    }

    /** Detiene el cronómetro y deja de escuchar el teclado (al salir de la pantalla). */
    void desconectar() {
        reloj.stop();
        if (getScene() != null) getScene().removeEventFilter(KeyEvent.KEY_PRESSED, manejadorTeclas);
    }
}
