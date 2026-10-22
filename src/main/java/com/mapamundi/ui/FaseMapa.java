package com.mapamundi.ui;

import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Pista;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.ui.mapa.MapaLienzo;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import static com.mapamundi.i18n.Textos.t;

/** Fase 2: mover la pieza sobre el mapa y confirmar antes de que acabe el tiempo. */
final class FaseMapa extends HBox {

    /** Pasos de las flechas, en píxeles de pantalla. */
    private static final double PASO = 1, PASO_RAPIDO = 8;

    private final Partida partida;
    private final MapaLienzo mapa;
    private final VBox lateral = new VBox(12);
    private final VBox controles;
    private final BotonesPista pistas;
    private Partida.Fase faseMostrada;

    FaseMapa(Partida partida, Atlas atlas) {
        this.partida = partida;
        setSpacing(18);
        mapa = new MapaLienzo(atlas);
        HBox.setHgrow(mapa, Priority.ALWAYS);

        lateral.getStyleClass().add("panel");
        lateral.setPrefWidth(320);
        lateral.setMinWidth(290);
        lateral.setMaxHeight(USE_PREF_SIZE);
        pistas = new BotonesPista(partida, Pista.ZONA, Pista.VECINOS);
        controles = construirControles();
        getChildren().addAll(lateral, mapa);
        actualizar();
    }

    private VBox construirControles() {
        var p = partida.actual();
        Label continente = FichaPais.etiqueta(t("mapa.continente", p.continente().nombre()), "negrita");
        Label margen = FichaPais.etiqueta(t("mapa.margen", partida.ajustes().velocidad().toleranciaKm()), "");

        Button confirmar = new Button(t("mapa.confirmar") + "\n(Enter)");
        confirmar.getStyleClass().add("boton-principal");
        confirmar.setStyle("-fx-font-size: 16px; -fx-padding: 10 14 10 14;");
        confirmar.setMinWidth(USE_PREF_SIZE);
        confirmar.setFocusTraversable(false);
        confirmar.setOnAction(e -> partida.confirmar());
        HBox acciones = new HBox(16, cruceta(), confirmar);
        acciones.setAlignment(Pos.CENTER);

        Button centrar = new Button("⟲ " + t("mapa.verTodo"));
        centrar.setFocusTraversable(false);
        centrar.setOnAction(e -> mapa.restablecerZoom());

        boolean girada = partida.ajustes().piezaGirada();
        Label ayuda = FichaPais.etiqueta(t("mapa.ayuda") + (girada ? " " + t("mapa.ayudaGiro") : ""), "texto-suave");
        ayuda.setWrapText(true);
        VBox v = new VBox(9, FichaPais.pista(p, partida.ajustes().modo(), false), continente, margen, acciones);
        if (girada) v.getChildren().add(controlesGiro());
        v.getChildren().addAll(centrar, pistas, ayuda);
        return v;
    }

    /** Modo experto: botones para enderezar la pieza, de 15° en 15°. */
    private HBox controlesGiro() {
        Button izquierda = new Button("⟲  Q");
        Button derecha = new Button("E  ⟳");
        for (Button b : new Button[]{izquierda, derecha}) {
            b.setFocusTraversable(false);
            b.getStyleClass().add("boton-pista");
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
        }
        izquierda.setOnAction(e -> partida.girar(-1));
        derecha.setOnAction(e -> partida.girar(1));
        HBox h = new HBox(6, FichaPais.etiqueta(t("mapa.girar"), "negrita"), izquierda, derecha);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Cruceta en pantalla: mantener pulsado repite el movimiento. */
    private GridPane cruceta() {
        GridPane g = new GridPane();
        g.getStyleClass().add("cruceta");
        g.setHgap(4);
        g.setVgap(4);
        g.add(flecha("▲", 0, -1), 1, 0);
        g.add(flecha("◀", -1, 0), 0, 1);
        g.add(flecha("▶", 1, 0), 2, 1);
        g.add(flecha("▼", 0, 1), 1, 2);
        return g;
    }

    private Button flecha(String texto, int dx, int dy) {
        Button b = new Button(texto);
        b.setFocusTraversable(false);
        int[] pulsos = {0};
        Timeline repetir = new Timeline(new KeyFrame(Duration.millis(70), e -> {
            pulsos[0]++;
            mover(dx, dy, pulsos[0] > 8 ? 3 : 1);
        }));
        repetir.setCycleCount(Timeline.INDEFINITE);
        b.setOnMousePressed(e -> {
            pulsos[0] = 0;
            mover(dx, dy, 1);
            repetir.playFromStart();
        });
        b.setOnMouseReleased(e -> repetir.stop());
        b.setOnMouseExited(e -> repetir.stop());
        return b;
    }

    private void mover(int dx, int dy, double pasos) {
        double k = mapa.unidadesPorPixel() * pasos;
        partida.mover(dx * k, dy * k);
    }

    /** Lo llama la pantalla de juego con cada tecla pulsada. */
    void tecla(KeyEvent e) {
        if (partida.enPausa()) return;
        KeyCode k = e.getCode();
        if (partida.fase() == Partida.Fase.RESULTADO) {
            if (k == KeyCode.ENTER || k == KeyCode.SPACE) {
                partida.siguiente();
                e.consume();
            }
            return;
        }
        double paso = e.isShiftDown() ? PASO_RAPIDO : PASO;
        switch (k) {
            case UP -> mover(0, -1, paso);
            case DOWN -> mover(0, 1, paso);
            case LEFT -> mover(-1, 0, paso);
            case RIGHT -> mover(1, 0, paso);
            case Q -> partida.girar(-1);
            case E -> partida.girar(1);
            case ENTER, SPACE -> partida.confirmar();
            default -> {
                return;
            }
        }
        e.consume();
    }

    void actualizar() {
        if (partida.fase() != faseMostrada) {
            faseMostrada = partida.fase();
            lateral.getChildren().setAll(faseMostrada == Partida.Fase.RESULTADO
                    ? new PanelResultado(partida)
                    : controles);
        }
        pistas.actualizar();
        mapa.mostrar(partida);
    }
}
