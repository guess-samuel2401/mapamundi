package com.mapamundi.ui;

import com.mapamundi.juego.Logros.Logro;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de juego: panel superior, la fase actual, la pausa, el cartel de nivel superado,
 * los avisos de insignias y los consejos de la primera partida.
 */
final class PantallaJuego extends StackPane {

    interface AlTerminar {
        void fin(Partida partida, List<Logro> insigniasGanadas);
    }

    private final Partida partida;
    private final Contexto ctx;
    private final Atlas atlas;
    private final Sonidos sonidos;
    private final Hud hud = new Hud();
    private final BorderPane cuerpo = new BorderPane();
    private final StackPane capaPausa;
    private final AvisoLogros avisos;
    private final Timeline reloj;
    private FaseContinente faseContinente;
    private FaseMapa faseMapa;
    private Partida.Fase faseAnterior;
    private String ultimoCartel;
    private StackPane dialogo;
    private Consejo consejo;
    private final List<Logro> ganadas = new ArrayList<>();
    // Una sola instancia, para poder quitarla después con removeEventFilter.
    private final EventHandler<KeyEvent> manejadorTeclas = this::tecla;

    PantallaJuego(Partida partida, Contexto ctx, Runnable salir, AlTerminar alTerminar) {
        this.partida = partida;
        this.ctx = ctx;
        this.atlas = ctx.atlas();
        this.sonidos = ctx.sonidos();
        cuerpo.setTop(hud);
        capaPausa = construirPausa(salir);
        avisos = new AvisoLogros(sonidos);
        getChildren().addAll(cuerpo, avisos, capaPausa);

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
            List<Logro> nuevos = ctx.logros().revisar(partida);
            if (!nuevos.isEmpty()) {
                avisos.mostrar(nuevos);
                ganadas.addAll(nuevos);
            }
            if (partida.fase() == Partida.Fase.FIN) {
                reloj.stop();
                alTerminar.fin(partida, ganadas);
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
        Button ajustes = new Button("⚙  Ajustes");
        ajustes.setMaxWidth(Double.MAX_VALUE);
        ajustes.setOnAction(e -> abrirAjustes());
        Button menu = new Button("Salir al menú");
        menu.setMaxWidth(Double.MAX_VALUE);
        menu.setOnAction(e -> {
            reloj.stop();
            salir.run();
        });
        VBox caja = new VBox(12, FichaPais.etiqueta("Pausa", "titulo-mapa-grande"),
                FichaPais.etiqueta("El cronómetro está detenido.", ""), seguir, ajustes, menu);
        caja.getStyleClass().add("panel");
        caja.setAlignment(Pos.CENTER);
        caja.setMaxSize(320, USE_PREF_SIZE);
        StackPane capa = new StackPane(caja);
        capa.getStyleClass().add("capa-pausa");
        capa.setVisible(false);
        return capa;
    }

    private void abrirAjustes() {
        dialogo = Dialogo.mostrar(this, "Ajustes", new PanelAjustes(ctx, this::actualizar, null), 520, () -> dialogo = null);
    }

    private void tecla(KeyEvent e) {
        if (dialogo != null) return; // Esc y Enter son para la ventana de Ajustes
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
        if (fase != faseAnterior) consejoDeLaFase(fase);
        faseAnterior = fase;

        String cartel = partida.nivelSuperado();
        if (cartel != null && !cartel.equals(ultimoCartel)) mostrarCartel(cartel);
        ultimoCartel = cartel;
    }

    /** En la primera partida, un consejo breve al entrar en cada fase (solo una vez por fase). */
    private void consejoDeLaFase(Partida.Fase fase) {
        Consejo.quitar(this, consejo);
        consejo = null;
        String clave = switch (fase) {
            case CONTINENTE -> "continente";
            case UBICACION -> "mapa";
            case RESULTADO -> "resultado";
            default -> null;
        };
        if (clave == null || !ctx.preferencias().primeraVez(clave)) return;
        String texto = switch (clave) {
            case "continente" -> "Elige el continente del país con un clic o con las teclas 1 a 5. Si te equivocas, puedes volver a intentarlo.";
            case "mapa" -> "Arrastra el país hasta su lugar y pulsa Enter. La rueda del ratón hace zoom y arrastrando el mar mueves el mapa.";
            default -> "Aquí ves la distancia, los puntos y los datos del país. Pulsa Enter para seguir.";
        };
        consejo = Consejo.mostrar(this, texto);
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
