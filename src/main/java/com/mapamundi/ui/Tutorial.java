package com.mapamundi.ui;

import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.juego.Partida;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.mapa.MapaDecorativo;
import com.mapamundi.ui.mapa.Miniatura;
import com.mapamundi.ui.mapa.Pintor;
import com.mapamundi.ui.mapa.Transformacion;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.List;

/** Guía de bienvenida en 5 pasos ilustrados, que se muestra la primera vez que se abre el juego. */
final class Tutorial {

    private record Paso(String titulo, String texto, Node ilustracion) {
    }

    private Tutorial() {
    }

    static void mostrar(StackPane contenedor, Atlas atlas, Runnable alTerminar) {
        List<Paso> pasos = List.of(
                new Paso("¡Bienvenido a Mapamundi!",
                        "Aprende dónde está cada uno de los 197 países del mundo jugando. Te lo explicamos en 4 pasos.",
                        mundo(atlas)),
                new Paso("1. Elige el continente",
                        "Aparece un país con su silueta. Elige a qué continente pertenece con un clic o con las teclas 1 a 5. "
                                + "Tienes 5 intentos.",
                        continentes(atlas)),
                new Paso("2. Colócalo en el mapa",
                        "Arrastra el país con el ratón (o muévelo con las flechas) hasta su lugar y pulsa Confirmar o Enter "
                                + "antes de que se acabe el tiempo. La rueda del ratón hace zoom.",
                        colocar(atlas)),
                new Paso("3. Suma puntos y cuida tus corazones",
                        "Cuanto más cerca y más rápido, más puntos. Si fallas o se acaba el tiempo pierdes un corazón; "
                                + "sin corazones, termina la partida.",
                        puntos()),
                new Paso("4. Gana insignias",
                        "Consigue insignias por rachas de aciertos, puntería, rapidez y por completar continentes. "
                                + "Las verás en el botón «Logros» del menú. ¡A jugar!",
                        insignias()));

        int[] actual = {0};
        Label titulo = FichaPais.etiqueta("", "titulo-config");
        Label texto = FichaPais.etiqueta("", "tutorial-texto");
        texto.setWrapText(true);
        texto.setTextAlignment(TextAlignment.CENTER);
        texto.setMinHeight(66);
        StackPane lamina = new StackPane();
        lamina.getStyleClass().add("tutorial-lamina");
        lamina.setPrefSize(520, 250);
        lamina.setMinSize(520, 250);
        lamina.setMaxSize(520, 250);
        HBox puntitos = new HBox(8);
        puntitos.setAlignment(Pos.CENTER);

        Button saltar = new Button("Saltar");
        saltar.getStyleClass().add("boton-discreto");
        Button atras = new Button("‹ Atrás");
        Button siguiente = new Button("Siguiente ›");
        siguiente.getStyleClass().add("boton-principal");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox botones = new HBox(10, saltar, hueco, atras, siguiente);
        botones.setAlignment(Pos.CENTER);

        VBox caja = new VBox(14, titulo, lamina, texto, puntitos, botones);
        caja.setAlignment(Pos.TOP_CENTER);
        caja.getStyleClass().addAll("panel", "tutorial");
        caja.setMaxSize(600, Region.USE_PREF_SIZE);
        caja.setPadding(new Insets(20, 24, 18, 24));
        StackPane capa = new StackPane(caja);
        capa.getStyleClass().add("capa-dialogo");

        Runnable cerrar = () -> {
            contenedor.getChildren().remove(capa);
            alTerminar.run();
        };
        Runnable[] ir = new Runnable[1];
        ir[0] = () -> {
            Paso p = pasos.get(actual[0]);
            titulo.setText(p.titulo());
            texto.setText(p.texto());
            lamina.getChildren().setAll(p.ilustracion());
            FadeTransition f = new FadeTransition(Duration.millis(220), lamina);
            f.setFromValue(0.2);
            f.setToValue(1);
            f.play();
            puntitos.getChildren().clear();
            for (int i = 0; i < pasos.size(); i++) {
                Circle c = new Circle(5, i == actual[0] ? Pintor.CORAL : Color.web("#d9d2c5"));
                puntitos.getChildren().add(c);
            }
            atras.setDisable(actual[0] == 0);
            boolean ultimo = actual[0] == pasos.size() - 1;
            siguiente.setText(ultimo ? "¡A jugar!" : "Siguiente ›");
            saltar.setVisible(!ultimo);
        };
        siguiente.setOnAction(e -> {
            if (actual[0] == pasos.size() - 1) cerrar.run();
            else {
                actual[0]++;
                ir[0].run();
            }
        });
        atras.setOnAction(e -> {
            if (actual[0] > 0) {
                actual[0]--;
                ir[0].run();
            }
        });
        saltar.setOnAction(e -> cerrar.run());
        // Enter se maneja aquí: el botón Jugar del menú ya es el botón por defecto de la escena.
        capa.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> cerrar.run();
                case ENTER, RIGHT -> siguiente.fire();
                case LEFT -> atras.fire();
                default -> {
                    return;
                }
            }
            e.consume();
        });
        ir[0].run();
        contenedor.getChildren().add(capa);
        siguiente.requestFocus();
    }

    // ------------------------------------------------------------------ ilustraciones

    private static Node mundo(Atlas atlas) {
        MapaDecorativo m = new MapaDecorativo(atlas);
        m.setPrefSize(500, 230);
        m.setMaxSize(500, 230);
        return m;
    }

    private static Node continentes(Atlas atlas) {
        HBox fila = new HBox(8);
        fila.setAlignment(Pos.CENTER);
        for (Continente c : Continente.values()) {
            Canvas mini = Miniatura.continente(atlas, c, 84, 72);
            StackPane t = new StackPane(mini, FichaPais.etiqueta(c.nombre(), "tutorial-continente"));
            StackPane.setAlignment(t.getChildren().get(1), Pos.TOP_CENTER);
            t.getStyleClass().add("tutorial-tarjeta");
            if (c == Continente.AMERICA) t.getStyleClass().add("seleccionada");
            fila.getChildren().add(t);
        }
        Pais ecuador = atlas.paises().stream().filter(p -> p.iso().equals("EC")).findFirst().orElse(atlas.paises().getFirst());
        VBox v = new VBox(12, new HBox(10, Miniatura.silueta(ecuador, 90, 70), FichaPais.etiqueta(ecuador.nombre(), "titulo-mapa-mediano")), fila);
        ((HBox) v.getChildren().getFirst()).setAlignment(Pos.CENTER);
        v.setAlignment(Pos.CENTER);
        return v;
    }

    /** Sudamérica en blanco, con Perú desplazado y una flecha hacia su lugar. */
    private static Node colocar(Atlas atlas) {
        double ancho = 500, alto = 230;
        Canvas c = new Canvas(ancho, alto);
        GraphicsContext gc = c.getGraphicsContext2D();
        com.mapamundi.geo.Region vista = com.mapamundi.geo.Region.deGrados(-95, -28, -57, 14);
        Transformacion t = Transformacion.ajustada(vista, ancho, alto);
        Pintor.mapa(gc, t, atlas, p -> null, 0.6);
        Pais peru = atlas.paises().stream().filter(p -> p.iso().equals("PE")).findFirst().orElse(atlas.paises().getFirst());
        double du = -0.07, dv = 0.05;
        Pintor.forma(gc, peru.forma().nucleo(), du, dv, t, Pintor.colorDe(peru), Pintor.BORDE, 1.4);
        double x0 = t.x(peru.forma().anclaU() + du), y0 = t.y(peru.forma().anclaV() + dv);
        double x1 = t.x(peru.forma().anclaU()), y1 = t.y(peru.forma().anclaV());
        gc.setStroke(Pintor.ROJO);
        gc.setLineWidth(3);
        gc.setLineDashes(8, 6);
        gc.strokeLine(x0, y0, x1, y1);
        gc.setLineDashes();
        double ang = Math.atan2(y1 - y0, x1 - x0);
        gc.setFill(Pintor.ROJO);
        gc.fillPolygon(new double[]{x1, x1 - 14 * Math.cos(ang - 0.45), x1 - 14 * Math.cos(ang + 0.45)},
                new double[]{y1, y1 - 14 * Math.sin(ang - 0.45), y1 - 14 * Math.sin(ang + 0.45)}, 3);
        // La manita que arrastra
        Label mano = new Label("✋");
        mano.setStyle("-fx-font-size: 30px;");
        StackPane p = new StackPane(c, mano);
        StackPane.setAlignment(mano, Pos.TOP_LEFT);
        mano.setTranslateX(x0 - 10);
        mano.setTranslateY(y0 - 10);
        TranslateTransition mover = new TranslateTransition(Duration.millis(1400), mano);
        mover.setFromX(x0 - 10);
        mover.setFromY(y0 - 10);
        mover.setToX(x1 - 10);
        mover.setToY(y1 - 10);
        mover.setCycleCount(TranslateTransition.INDEFINITE);
        mover.setAutoReverse(true);
        mover.play();
        return p;
    }

    private static Node puntos() {
        Corazones corazones = new Corazones(Partida.VIDAS);
        corazones.mostrar(4);
        corazones.setScaleX(1.6);
        corazones.setScaleY(1.6);
        corazones.setAlignment(Pos.CENTER);
        Label puntos = FichaPais.etiqueta("+860", "tutorial-puntos");
        Label ejemplo = FichaPais.etiqueta("Distancia: 26 km  ·  +500 ubicación  ·  +360 bonus de tiempo", "negrita");
        VBox v = new VBox(18, puntos, ejemplo, corazones);
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private static Node insignias() {
        HBox fila = new HBox(18,
                new Insignia(Logro.PRIMER_PAIS, true, 34),
                new Insignia(Logro.PUNTERIA, true, 34),
                new Insignia(Logro.RACHA_10, true, 34),
                new Insignia(Logro.DUENO_DEL_MUNDO, false, 34));
        fila.setAlignment(Pos.CENTER);
        VBox v = new VBox(14, fila, FichaPais.etiqueta("Bronce · Plata · Oro", "texto-suave"));
        v.setAlignment(Pos.CENTER);
        return v;
    }
}
