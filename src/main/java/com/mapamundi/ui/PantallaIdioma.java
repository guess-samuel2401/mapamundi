package com.mapamundi.ui;

import com.mapamundi.i18n.Idioma;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.ui.mapa.MapaDecorativo;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lo primero que aparece al abrir el programa: elegir entre español e inglés. Viene marcado el último
 * idioma usado; se elige con un clic, con ← → y Enter o con las teclas 1 y 2. Los textos van en los dos
 * idiomas, porque todavía no se sabe cuál lee el jugador.
 */
final class PantallaIdioma extends StackPane {

    private final Map<Idioma, Button> tarjetas = new EnumMap<>(Idioma.class);
    private final Consumer<Idioma> alElegir;
    private Idioma marcado;
    private boolean elegido;

    PantallaIdioma(Atlas atlas, Idioma inicial, Consumer<Idioma> alElegir) {
        this.alElegir = alElegir;
        this.marcado = inicial;

        // De fondo, el mapa del mundo apagado.
        MapaDecorativo mapa = new MapaDecorativo(atlas);
        mapa.setOpacity(0.18);
        mapa.setMouseTransparent(true);

        Label logo = Logotipo.crear("Mapamundi");
        Label titulo = FichaPais.etiqueta("Elige tu idioma  ·  Choose your language", "idioma-titulo");

        HBox fila = new HBox(28);
        fila.setAlignment(Pos.CENTER);
        fila.getChildren().addAll(
                tarjeta(Idioma.ES, "ES", "Jugar en español", 1),
                tarjeta(Idioma.EN, "EN", "Play in English", 2));

        Label ayuda = FichaPais.etiqueta("← →  ·  Enter  ·  1 / 2", "idioma-ayuda");

        VBox centro = new VBox(26, logo, titulo, fila, ayuda);
        centro.setAlignment(Pos.CENTER);
        centro.setPadding(new Insets(24));
        getChildren().addAll(mapa, centro);
        getStyleClass().add("pantalla-idioma");

        setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.LEFT || k == KeyCode.RIGHT || k == KeyCode.TAB) marcar(marcado == Idioma.ES ? Idioma.EN : Idioma.ES);
            else if (k == KeyCode.ENTER || k == KeyCode.SPACE) elegir(marcado);
            else if (k == KeyCode.DIGIT1 || k == KeyCode.NUMPAD1) elegir(Idioma.ES);
            else if (k == KeyCode.DIGIT2 || k == KeyCode.NUMPAD2) elegir(Idioma.EN);
            else return;
            e.consume();
        });
        setFocusTraversable(true);
        Platform.runLater(this::requestFocus);
        marcar(inicial);

        FadeTransition entrada = new FadeTransition(Duration.millis(350), centro);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.play();
    }

    private Button tarjeta(Idioma idioma, String codigo, String detalle, int tecla) {
        Label circulo = FichaPais.etiqueta(codigo, "idioma-codigo");
        circulo.setMinSize(64, 64);
        circulo.setAlignment(Pos.CENTER);
        Label nombre = FichaPais.etiqueta(idioma.nombre(), "idioma-nombre");
        Label sub = FichaPais.etiqueta(detalle, "idioma-detalle");
        Label numero = FichaPais.etiqueta(String.valueOf(tecla), "numero-tecla");
        VBox contenido = new VBox(8, circulo, nombre, sub);
        contenido.setAlignment(Pos.CENTER);
        StackPane capa = new StackPane(contenido, numero);
        StackPane.setAlignment(numero, Pos.BOTTOM_LEFT);

        Button b = new Button(null, capa);
        b.getStyleClass().setAll("tarjeta-idioma");
        b.setPrefSize(260, 210);
        b.setFocusTraversable(false);
        b.setOnAction(e -> elegir(idioma));
        b.setOnMouseEntered(e -> marcar(idioma));
        Animaciones.alPasar(b, 1.05, 3);
        tarjetas.put(idioma, b);
        return b;
    }

    private void marcar(Idioma idioma) {
        marcado = idioma;
        tarjetas.forEach((i, b) -> {
            b.getStyleClass().remove("seleccionada");
            if (i == idioma) b.getStyleClass().add("seleccionada");
        });
    }

    private void elegir(Idioma idioma) {
        if (elegido) return;
        elegido = true;
        marcar(idioma);
        // Un pequeño "clic" visual antes de pasar al menú.
        ScaleTransition pulso = new ScaleTransition(Duration.millis(140), tarjetas.get(idioma));
        pulso.setToX(0.94);
        pulso.setToY(0.94);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(2);
        pulso.setOnFinished(e -> alElegir.accept(idioma));
        pulso.play();
    }
}
