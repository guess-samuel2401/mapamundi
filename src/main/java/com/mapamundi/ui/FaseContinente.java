package com.mapamundi.ui;

import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Pista;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.ui.mapa.Miniatura;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/** Fase 1: ¿a qué continente pertenece? Flechas + Enter (el recuadro amarillo del original), teclas 1-5 o clic. */
final class FaseContinente extends HBox {

    private static final double ANCHO_TARJETA = 230, ALTO_TARJETA = 200;

    private final Partida partida;
    private final Map<Continente, Button> botones = new EnumMap<>(Continente.class);
    private final Map<Continente, Label> marcas = new EnumMap<>(Continente.class);
    private final Label intentos = FichaPais.etiqueta("", "negrita");
    private final BotonesPista pistas;
    private int seleccion;

    FaseContinente(Partida partida, Atlas atlas) {
        this.partida = partida;
        pistas = new BotonesPista(partida, Pista.DESCARTAR);
        setSpacing(18);

        VBox izquierda = new VBox(10);
        izquierda.getStyleClass().add("panel");
        izquierda.setAlignment(Pos.TOP_CENTER);
        izquierda.setPrefWidth(320);
        izquierda.setMinWidth(280);
        izquierda.getChildren().addAll(
                FichaPais.etiqueta("¿A qué continente pertenece?", "subtitulo-rojo"),
                FichaPais.pista(partida.actual(), partida.ajustes().modo(), true));
        if (partida.ajustes().modo().muestraForma()) {
            Canvas silueta = Miniatura.silueta(partida.actual(), 220, 170);
            silueta.setTranslateY(-30);
            TranslateTransition caida = new TranslateTransition(Duration.millis(450), silueta);
            caida.setToY(0);
            caida.play();
            izquierda.getChildren().add(silueta);
        }
        Label ayuda = FichaPais.etiqueta("Usa ← → y Enter, las teclas 1–5 o haz clic.", "texto-suave");
        ayuda.setWrapText(true);
        izquierda.getChildren().addAll(intentos, ayuda, pistas);

        GridPane rejilla = new GridPane();
        rejilla.setHgap(14);
        rejilla.setVgap(14);
        rejilla.setAlignment(Pos.CENTER);
        for (Continente c : Continente.values()) {
            Button b = tarjeta(atlas, c);
            rejilla.add(b, c.ordinal() % 3, c.ordinal() / 3);
        }
        HBox.setHgrow(rejilla, Priority.ALWAYS);
        getChildren().addAll(izquierda, rejilla);
        actualizar();
    }

    private Button tarjeta(Atlas atlas, Continente c) {
        Label nombre = FichaPais.etiqueta(c.nombre(), "titulo-tarjeta");
        Label tecla = FichaPais.etiqueta(String.valueOf(c.tecla()), "numero-tecla");
        Label error = FichaPais.etiqueta("✗", "marca-error");
        error.setVisible(false);
        StackPane contenido = new StackPane(Miniatura.continente(atlas, c, ANCHO_TARJETA, ALTO_TARJETA), nombre, tecla, error);
        StackPane.setAlignment(nombre, Pos.TOP_CENTER);
        StackPane.setAlignment(tecla, Pos.BOTTOM_LEFT);
        Button b = new Button(null, contenido);
        b.getStyleClass().setAll("tarjeta-continente");
        b.setFocusTraversable(false);
        b.setOnAction(e -> elegir(c));
        b.setOnMouseEntered(e -> {
            seleccion = c.ordinal();
            actualizar();
        });
        botones.put(c, b);
        marcas.put(c, error);
        return b;
    }

    private void elegir(Continente c) {
        if (c != partida.actual().continente()) temblar(botones.get(c));
        partida.elegirContinente(c);
    }

    private static void temblar(Button b) {
        TranslateTransition t = new TranslateTransition(Duration.millis(60), b);
        t.setFromX(-10);
        t.setToX(10);
        t.setCycleCount(5);
        t.setAutoReverse(true);
        t.setOnFinished(e -> b.setTranslateX(0));
        t.play();
    }

    /** Lo llama la pantalla de juego con cada tecla pulsada. */
    void tecla(KeyEvent e) {
        if (partida.enPausa()) return;
        int n = Continente.values().length;
        KeyCode k = e.getCode();
        if (k == KeyCode.RIGHT || k == KeyCode.DOWN) seleccion = (seleccion + 1) % n;
        else if (k == KeyCode.LEFT || k == KeyCode.UP) seleccion = (seleccion + n - 1) % n;
        else if (k == KeyCode.ENTER || k == KeyCode.SPACE) elegir(Continente.values()[seleccion]);
        else if (k.isDigitKey() && k.getChar().charAt(0) >= '1' && k.getChar().charAt(0) <= '5')
            elegir(Continente.values()[k.getChar().charAt(0) - '1']);
        else if (k.isKeypadKey() && k.getName().matches("Numpad [1-5]"))
            elegir(Continente.values()[k.getName().charAt(7) - '1']);
        else return;
        e.consume();
        actualizar();
    }

    void actualizar() {
        intentos.setText("Intentos: " + partida.erroresContinente() + "/" + Partida.INTENTOS_CONTINENTE);
        pistas.actualizar();
        for (Continente c : Continente.values()) {
            Button b = botones.get(c);
            boolean descartado = partida.descartados().contains(c);
            marcas.get(c).setVisible(descartado);
            b.setOpacity(descartado ? 0.55 : 1);
            b.getStyleClass().remove("seleccionada");
            if (c.ordinal() == seleccion) b.getStyleClass().add("seleccionada");
        }
    }
}
