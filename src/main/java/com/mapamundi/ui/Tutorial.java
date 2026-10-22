package com.mapamundi.ui;

import com.mapamundi.geo.Forma;
import com.mapamundi.geo.Mercator;
import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.juego.Modo;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.juego.Pista;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.mapa.Miniatura;
import com.mapamundi.ui.mapa.Pintor;
import com.mapamundi.ui.mapa.Transformacion;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import static com.mapamundi.i18n.Textos.t;

/**
 * Tutorial animado de 14 pasos en cuatro capítulos (introducción, estilo moderno, estilo clásico y controles).
 * Se muestra cada vez que se abre el programa, tras elegir el idioma, y desde el botón "?" o Ajustes.
 *
 * <p>Cada paso es una pequeña escena con sus animaciones; al cambiar de paso, las de la escena anterior se
 * detienen.
 */
final class Tutorial {

    /** Tamaño de la lámina donde se dibuja cada escena. */
    private static final double W = 720, H = 330;
    private static final Color[] TEMPERATURA = {
            Color.web("#e3263b"), Color.web("#ff8a3d"), Color.web("#f2c230"), Color.web("#6fb3e8"), Color.web("#2f6fd6"),
    };

    private record Paso(int capitulo, String clave, Supplier<Escena> escena) {
        String titulo() {
            return t("tut." + clave + ".titulo");
        }

        String texto() {
            return t("tut." + clave + ".texto");
        }
    }

    /** Una escena: sus nodos y las animaciones que hay que arrancar y parar. */
    private static final class Escena {
        final Pane raiz = new Pane();
        final List<Animation> animaciones = new ArrayList<>();
        final List<AnimationTimer> relojes = new ArrayList<>();

        Escena() {
            raiz.setPrefSize(W, H);
            raiz.setMinSize(W, H);
            raiz.setMaxSize(W, H);
            Rectangle recorte = new Rectangle(W, H);
            recorte.setArcWidth(22);
            recorte.setArcHeight(22);
            raiz.setClip(recorte);
        }

        <T extends Node> T poner(T n, double x, double y) {
            n.relocate(x, y);
            raiz.getChildren().add(n);
            return n;
        }

        Timeline bucle(KeyFrame... cuadros) {
            Timeline tl = new Timeline(cuadros);
            tl.setCycleCount(Animation.INDEFINITE);
            animaciones.add(tl);
            return tl;
        }

        void empezar() {
            animaciones.forEach(Animation::play);
            relojes.forEach(AnimationTimer::start);
        }

        void detener() {
            animaciones.forEach(Animation::stop);
            relojes.forEach(AnimationTimer::stop);
        }
    }

    private Tutorial() {
    }

    /** El tutorial completo, con los dos estilos de juego. */
    static void mostrar(StackPane contenedor, Atlas atlas, Runnable alTerminar) {
        mostrar(contenedor, alTerminar, true, List.of(
                new Paso(0, "bienvenida", () -> bienvenida(atlas)),
                new Paso(0, "pasos", () -> dosPasos(atlas)),
                new Paso(0, "modos", () -> cincoModos(atlas)),
                new Paso(1, "m1", () -> continenteModerno(atlas)),
                new Paso(1, "m2", () -> ubicacionModerna(atlas)),
                new Paso(1, "m3", Tutorial::corazonesYRachas),
                new Paso(1, "m4", () -> pistas(atlas)),
                new Paso(1, "m5", () -> reglasExtra(atlas)),
                new Paso(1, "m6", Tutorial::insignias),
                new Paso(2, "c1", Tutorial::clasicoIntro),
                new Paso(2, "c2", () -> caidaClasica(atlas)),
                new Paso(2, "c3", () -> mapaClasico(atlas)),
                new Paso(2, "c4", () -> puntosClasicos(atlas)),
                new Paso(3, "controles", Tutorial::controles)));
    }

    /** La versión sencilla: cinco pasos sobre su única forma de jugar, sin pestañas de capítulo. */
    static void mostrarSencillo(StackPane contenedor, Atlas atlas, Runnable alTerminar) {
        mostrar(contenedor, alTerminar, false, List.of(
                new Paso(0, "s.bienvenida", () -> bienvenida(atlas)),
                new Paso(0, "s.c1", Tutorial::clasicoIntro),
                new Paso(0, "s.c2", () -> caidaClasica(atlas)),
                new Paso(0, "s.c3", () -> mapaClasico(atlas)),
                new Paso(0, "s.c4", () -> puntosClasicos(atlas))));
    }

    private static void mostrar(StackPane contenedor, Runnable alTerminar, boolean conCapitulos, List<Paso> pasos) {
        int[] actual = {0};
        Escena[] escena = {null};

        // Pestañas de capítulo: llevan al primer paso de cada uno.
        String[] capitulos = {t("tut.cap.intro"), t("tut.cap.moderno"), t("tut.cap.clasico"), t("tut.cap.controles")};
        List<Button> pestanas = new ArrayList<>();
        HBox barra = new HBox(8);
        barra.setAlignment(Pos.CENTER);
        barra.setVisible(conCapitulos);
        barra.setManaged(conCapitulos);
        Runnable[] ir = new Runnable[1];
        for (int c = 0; c < capitulos.length; c++) {
            int cap = c;
            Button b = new Button(capitulos[c]);
            b.getStyleClass().setAll("tutorial-capitulo");
            b.setFocusTraversable(false);
            b.setOnAction(e -> {
                for (int i = 0; i < pasos.size(); i++) {
                    if (pasos.get(i).capitulo() == cap) {
                        actual[0] = i;
                        ir[0].run();
                        return;
                    }
                }
            });
            pestanas.add(b);
            barra.getChildren().add(b);
        }

        Label titulo = FichaPais.etiqueta("", "titulo-config");
        Label texto = FichaPais.etiqueta("", "tutorial-texto");
        texto.setWrapText(true);
        texto.setTextAlignment(TextAlignment.CENTER);
        texto.setAlignment(Pos.CENTER);
        texto.setMinHeight(72);
        texto.setMaxWidth(W);
        StackPane lamina = new StackPane();
        lamina.getStyleClass().add("tutorial-lamina");
        lamina.setPrefSize(W + 16, H + 16);
        lamina.setMinSize(W + 16, H + 16);
        lamina.setMaxSize(W + 16, H + 16);
        HBox puntitos = new HBox(7);
        puntitos.setAlignment(Pos.CENTER);
        Label numero = FichaPais.etiqueta("", "texto-suave");

        Button saltar = new Button(t("tutorial.saltar"));
        saltar.getStyleClass().add("boton-discreto");
        Button atras = new Button("‹ " + t("tutorial.atras"));
        Button siguiente = new Button(t("tutorial.siguiente") + " ›");
        siguiente.getStyleClass().add("boton-principal");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox botones = new HBox(10, saltar, hueco, numero, atras, siguiente);
        botones.setAlignment(Pos.CENTER);

        VBox caja = new VBox(12, barra, titulo, lamina, texto, puntitos, botones);
        caja.setAlignment(Pos.TOP_CENTER);
        caja.getStyleClass().addAll("panel", "tutorial");
        caja.setMaxSize(W + 70, Region.USE_PREF_SIZE);
        caja.setPadding(new Insets(16, 24, 16, 24));
        StackPane capa = new StackPane(caja);
        capa.getStyleClass().add("capa-dialogo");

        Runnable cerrar = () -> {
            if (escena[0] != null) escena[0].detener();
            contenedor.getChildren().remove(capa);
            alTerminar.run();
        };
        ir[0] = () -> {
            Paso p = pasos.get(actual[0]);
            if (escena[0] != null) escena[0].detener();
            escena[0] = p.escena().get();
            titulo.setText(p.titulo());
            texto.setText(p.texto());
            lamina.getChildren().setAll(escena[0].raiz);
            escena[0].empezar();
            FadeTransition f = new FadeTransition(Duration.millis(220), lamina);
            f.setFromValue(0.2);
            f.setToValue(1);
            f.play();
            for (int c = 0; c < pestanas.size(); c++) {
                pestanas.get(c).getStyleClass().remove("activo");
                if (c == p.capitulo()) pestanas.get(c).getStyleClass().add("activo");
            }
            puntitos.getChildren().clear();
            for (int i = 0; i < pasos.size(); i++) {
                Color color = i == actual[0] ? Pintor.CORAL : pasos.get(i).capitulo() == p.capitulo() ? Color.web("#c9bfae") : Color.web("#e6dfd2");
                puntitos.getChildren().add(new Circle(i == actual[0] ? 6 : 4.5, color));
            }
            numero.setText((actual[0] + 1) + " / " + pasos.size());
            atras.setDisable(actual[0] == 0);
            boolean ultimo = actual[0] == pasos.size() - 1;
            siguiente.setText(ultimo ? t("tutorial.aJugar") : t("tutorial.siguiente") + " ›");
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

    // ================================================================== ayudas

    private static Label lbl(String texto, String clase) {
        return FichaPais.etiqueta(texto, clase);
    }

    private static KeyFrame en(double s, Runnable accion) {
        return new KeyFrame(Duration.seconds(s), e -> accion.run());
    }

    private static KeyFrame valores(double s, KeyValue... kv) {
        return new KeyFrame(Duration.seconds(s), kv);
    }

    private static KeyValue kv(javafx.beans.value.WritableValue<Number> p, double v) {
        return new KeyValue(p, v, Interpolator.EASE_BOTH);
    }

    /** Se pasa un poco del valor final y vuelve, como un muelle ("back out"). */
    private static final Interpolator REBOTE = new Interpolator() {
        @Override
        protected double curve(double t) {
            double s = 1.7, u = t - 1;
            return u * u * ((s + 1) * u + s) + 1;
        }
    };

    /** La flecha del ratón, para mostrar dónde se hace clic. */
    private static Polygon cursor() {
        Polygon p = new Polygon(0, 0, 0, 22, 6, 17, 10, 26, 14, 24, 10, 15, 17, 15);
        p.setFill(Color.WHITE);
        p.setStroke(Pintor.TINTA);
        p.setStrokeWidth(1.6);
        p.setEffect(new DropShadow(4, 1, 2, Color.rgb(0, 0, 0, 0.4)));
        p.setMouseTransparent(true);
        return p;
    }

    private static void dibujarCursor(GraphicsContext gc, double x, double y) {
        double[] xs = {0, 0, 6, 10, 14, 10, 17}, ys = {0, 22, 17, 26, 24, 15, 15};
        for (int i = 0; i < xs.length; i++) {
            xs[i] += x;
            ys[i] += y;
        }
        gc.setFill(Color.WHITE);
        gc.setStroke(Pintor.TINTA);
        gc.setLineWidth(1.6);
        gc.fillPolygon(xs, ys, xs.length);
        gc.strokePolygon(xs, ys, xs.length);
    }

    private static Pais pais(Atlas atlas, String iso) {
        return atlas.paises().stream().filter(p -> p.iso().equals(iso)).findFirst().orElse(atlas.paises().getFirst());
    }

    /** Una tecla dibujada, que se puede resaltar. */
    private static Label tecla(String texto) {
        Label l = lbl(texto, "tecla");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private static void resaltar(Node n, boolean si, String clase) {
        n.getStyleClass().remove(clase);
        if (si) n.getStyleClass().add(clase);
    }

    private static StackPane tarjetaContinente(Atlas atlas, Continente c, double ancho, double alto) {
        StackPane t = new StackPane(Miniatura.continente(atlas, c, ancho, alto), lbl(c.nombre(), "tutorial-continente"));
        StackPane.setAlignment(t.getChildren().get(1), Pos.TOP_CENTER);
        t.getStyleClass().add("tutorial-tarjeta");
        return t;
    }

    private static Color colorTemperatura(double margenes) {
        double s = margenes <= 1 ? 0 : Math.min(1, Math.log(margenes) / Math.log(12));
        double pos = s * (TEMPERATURA.length - 1);
        int i = Math.min(TEMPERATURA.length - 2, (int) pos);
        return TEMPERATURA[i].interpolate(TEMPERATURA[i + 1], pos - i);
    }

    private static String textoTemperatura(double margenes) {
        if (margenes <= 1) return t("temperatura.quemas");
        if (margenes <= 2.5) return t("temperatura.caliente");
        if (margenes <= 5) return t("temperatura.templado");
        if (margenes <= 10) return t("temperatura.frio");
        return t("temperatura.helado");
    }

    // ================================================================== introducción

    /** El mundo se va coloreando país a país. */
    private static Escena bienvenida(Atlas atlas) {
        Escena e = new Escena();
        Canvas c = e.poner(new Canvas(W, H), 0, 0);
        Label contador = e.poner(lbl("", "rotulo-caida"), 14, H - 40);
        List<Pais> orden = new ArrayList<>(atlas.paises());
        Collections.shuffle(orden, new Random(3));
        Set<Pais> pintados = new HashSet<>();
        Transformacion tr = Transformacion.cubriendo(com.mapamundi.geo.Region.deGrados(-168, 192, -56, 76), W, H);
        Runnable dibujar = () -> {
            Pintor.mapa(c.getGraphicsContext2D(), tr, atlas, p -> pintados.contains(p) ? Pintor.colorDe(p) : null, 0.5);
            contador.setText(t("tut.l.paises", pintados.size(), atlas.paises().size()));
        };
        dibujar.run();
        long[] ultimo = {0};
        int[] i = {0};
        e.relojes.add(new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (ahora - ultimo[0] < 45_000_000L) return;
                ultimo[0] = ahora;
                if (i[0] < orden.size()) {
                    for (int k = 0; k < 3 && i[0] < orden.size(); k++) pintados.add(orden.get(i[0]++));
                    dibujar.run();
                } else if (++i[0] > orden.size() + 50) {
                    pintados.clear();
                    i[0] = 0;
                    dibujar.run();
                }
            }
        });
        return e;
    }

    /** Paso 1 (configurar) → paso 2 (¡todo listo!) → Jugar, que se van iluminando. */
    private static Escena dosPasos(Atlas atlas) {
        Escena e = new Escena();
        VBox paso1 = new VBox(8, lbl(t("pasos.paso", 1, 2), "paso-indicador"), lbl(t("menu.configura"), "negrita"));
        HBox estilos = new HBox(6, bloque(70, 26, "#fff1b8"), bloque(70, 26, "#ffffff"));
        GridPane niveles = new GridPane();
        niveles.setHgap(5);
        niveles.setVgap(5);
        for (int i = 0; i < 4; i++) niveles.add(bloque(32, 22, i == 0 ? "#fff1b8" : "#ffffff"), i % 4, 0);
        VBox modos = new VBox(4);
        for (int i = 0; i < 4; i++) modos.getChildren().add(bloque(146, 9, i == 0 ? "#fff1b8" : "#ffffff"));
        paso1.getChildren().addAll(estilos, niveles, modos, lbl(t("config.continuar") + " ›", "mini-boton"));

        VBox paso2 = new VBox(8, lbl(t("pasos.paso", 2, 2), "paso-indicador"), lbl(t("inicio.titulo"), "negrita"),
                Miniatura.region(atlas, com.mapamundi.geo.Region.deGrados(-168, 192, -56, 76),
                        p -> p.continente() == Continente.EUROPA, 150, 70));
        for (int i = 0; i < 3; i++) paso2.getChildren().add(bloque(150, 9, "#ffffff"));

        Label jugar = lbl("▶  " + t("menu.jugar"), "mini-jugar");
        List<Node> tarjetas = List.of(paso1, paso2, jugar);
        for (Node n : List.of(paso1, paso2)) {
            n.getStyleClass().add("tut-tarjeta");
            ((VBox) n).setPrefSize(175, 230);
        }
        HBox fila = new HBox(14, paso1, lbl("➜", "tut-flecha"), paso2, lbl("➜", "tut-flecha"), jugar);
        fila.setAlignment(Pos.CENTER);
        fila.setPrefSize(W, H);
        e.poner(fila, 0, 0);
        List<KeyFrame> cuadros = new ArrayList<>();
        for (int i = 0; i < tarjetas.size(); i++) {
            int k = i;
            cuadros.add(en(i * 1.4, () -> {
                for (int j = 0; j < tarjetas.size(); j++) resaltar(tarjetas.get(j), j == k, "resaltada");
            }));
        }
        cuadros.add(en(4.6, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    private static Region bloque(double ancho, double alto, String color) {
        Region r = new Region();
        r.setPrefSize(ancho, alto);
        r.setMinSize(ancho, alto);
        r.setStyle("-fx-background-color: rgba(29,43,79,0.35), " + color + "; -fx-background-insets: 0, 1.5; -fx-background-radius: 5;");
        return r;
    }

    /** Los cinco modos: la pieza cambia según el modo (forma, círculo de color o solo datos). */
    private static Escena cincoModos(Atlas atlas) {
        Escena e = new Escena();
        Pais ecuador = pais(atlas, "EC");
        StackPane escenario = new StackPane();
        escenario.getStyleClass().add("tut-escenario");
        escenario.setPrefSize(300, 290);
        e.poner(escenario, 18, 20);

        VBox lista = new VBox(8);
        List<HBox> filas = new ArrayList<>();
        for (Modo m : Modo.values()) {
            int n = m.ordinal() + 1;
            HBox fila = new HBox(10, lbl("●".repeat(n) + "○".repeat(5 - n), "dificultad-modo"), lbl(m.nombre(), "opcion-modo-nombre"));
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("tut-fila");
            fila.setPrefWidth(360);
            filas.add(fila);
            lista.getChildren().add(fila);
        }
        e.poner(lista, 340, 40);
        Label ayuda = e.poner(lbl("", "texto-suave"), 340, 270);
        ayuda.setMaxWidth(360);
        ayuda.setWrapText(true);

        List<KeyFrame> cuadros = new ArrayList<>();
        for (Modo m : Modo.values()) {
            cuadros.add(en(m.ordinal() * 2.4, () -> {
                for (int i = 0; i < filas.size(); i++) resaltar(filas.get(i), i == m.ordinal(), "activa");
                ayuda.setText(m.ayuda());
                VBox v = new VBox(8);
                v.setAlignment(Pos.CENTER);
                if (m == Modo.DATOS) {
                    v.getChildren().addAll(lbl(t("ficha.quePais"), "subtitulo-rojo"),
                            dato(t("ficha.moneda"), ecuador.moneda()), dato(t("ficha.idioma"), ecuador.idioma()),
                            dato(t("ficha.poblacion"), ecuador.poblacion()));
                } else {
                    v.getChildren().add(lbl(m.porCapital() ? ecuador.capital() : ecuador.nombre(), "rotulo-caida"));
                    if (m.muestraForma()) {
                        v.getChildren().add(Miniatura.silueta(ecuador, 190, 150));
                    } else {
                        v.getChildren().add(Miniatura.figura(ecuador, 46));
                    }
                }
                escenario.getChildren().setAll(v);
            }));
        }
        cuadros.add(en(12, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    private static HBox dato(String clave, String valor) {
        Label v = lbl(valor, "ficha-valor");
        v.setWrapText(true);
        v.setMaxWidth(170);
        HBox h = new HBox(6, lbl(clave + ":", "ficha-clave"), v);
        h.setMaxWidth(270);
        return h;
    }

    // ================================================================== estilo moderno

    /** El cursor se equivoca de continente (✗, intento 1/5) y después acierta (+100). */
    private static Escena continenteModerno(Atlas atlas) {
        Escena e = new Escena();
        Pais ecuador = pais(atlas, "EC");
        Label intentos = lbl("", "negrita");
        VBox izquierda = new VBox(6, lbl(t("continente.pregunta"), "subtitulo-rojo"), lbl(ecuador.nombre(), "titulo-mapa-mediano"),
                Miniatura.silueta(ecuador, 190, 150), intentos);
        izquierda.setAlignment(Pos.TOP_CENTER);
        izquierda.setPrefWidth(230);
        e.poner(izquierda, 16, 16);

        double[][] pos = {{262, 34}, {412, 34}, {562, 34}, {337, 174}, {487, 174}};
        List<StackPane> cartas = new ArrayList<>();
        List<Label> cruces = new ArrayList<>();
        for (Continente c : Continente.values()) {
            StackPane carta = tarjetaContinente(atlas, c, 128, 104);
            Label cruz = lbl("✗", "marca-error-mini");
            cruz.setVisible(false);
            carta.getChildren().add(cruz);
            cartas.add(e.poner(carta, pos[c.ordinal()][0], pos[c.ordinal()][1]));
            cruces.add(cruz);
        }
        Label mas = e.poner(lbl("+100", "tutorial-puntos-mini"), pos[1][0] + 30, pos[1][1] + 20);
        Polygon cursor = e.poner(cursor(), 0, 0);
        StackPane europa = cartas.get(Continente.EUROPA.ordinal()), america = cartas.get(Continente.AMERICA.ordinal());
        double ex = pos[2][0] + 70, ey = pos[2][1] + 60, ax = pos[1][0] + 70, ay = pos[1][1] + 60;

        e.bucle(
                en(0, () -> {
                    cruces.forEach(c -> c.setVisible(false));
                    resaltar(america, false, "seleccionada");
                    intentos.setText(t("continente.intentos", 0, Partida.INTENTOS_CONTINENTE));
                }),
                valores(0, kv(cursor.translateXProperty(), 640), kv(cursor.translateYProperty(), 300),
                        kv(mas.opacityProperty(), 0), kv(mas.translateYProperty(), 0)),
                valores(0.4, kv(cursor.translateXProperty(), 640), kv(cursor.translateYProperty(), 300)),
                valores(1.2, kv(cursor.translateXProperty(), ex), kv(cursor.translateYProperty(), ey),
                        kv(europa.translateXProperty(), 0)),
                en(1.25, () -> {
                    cruces.get(Continente.EUROPA.ordinal()).setVisible(true);
                    intentos.setText(t("continente.intentos", 1, Partida.INTENTOS_CONTINENTE));
                }),
                valores(1.33, kv(europa.translateXProperty(), -9)),
                valores(1.43, kv(europa.translateXProperty(), 9)),
                valores(1.53, kv(europa.translateXProperty(), -5)),
                valores(1.63, kv(europa.translateXProperty(), 0)),
                valores(2.2, kv(cursor.translateXProperty(), ex), kv(cursor.translateYProperty(), ey)),
                valores(3.0, kv(cursor.translateXProperty(), ax), kv(cursor.translateYProperty(), ay),
                        kv(mas.opacityProperty(), 0), kv(mas.translateYProperty(), 0)),
                en(3.05, () -> resaltar(america, true, "seleccionada")),
                valores(3.15, kv(mas.opacityProperty(), 1)),
                valores(4.2, kv(mas.translateYProperty(), -36), kv(mas.opacityProperty(), 0),
                        kv(cursor.translateXProperty(), ax), kv(cursor.translateYProperty(), ay)),
                en(5.2, () -> { }));
        return e;
    }

    /** La pieza se arrastra hasta su lugar mientras corre el reloj, y se celebra el acierto. */
    private static Escena ubicacionModerna(Atlas atlas) {
        Escena e = new Escena();
        double ancho = 470;
        Canvas base = e.poner(new Canvas(ancho, H), 0, 0);
        Canvas capa = e.poner(new Canvas(ancho, H), 0, 0);
        com.mapamundi.geo.Region vista = com.mapamundi.geo.Region.deGrados(-100, -20, -57, 14);
        Transformacion tr = Transformacion.ajustada(vista, ancho, H);
        Pintor.mapa(base.getGraphicsContext2D(), tr, atlas, p -> null, 0.6);
        Pais peru = pais(atlas, "PE");
        Forma f = peru.forma();
        double du0 = 0.075, dv0 = 0.045;

        Label bonus = lbl(t("tut.l.puntosUbicacion", Partida.PUNTOS_UBICACION, 20), "texto-suave");
        bonus.setWrapText(true);
        VBox lado = new VBox(10,
                fila(cursor(), t("tut.l.arrastra")),
                fila(teclasFlechas(), t("tut.l.mover")),
                fila(tecla("Shift"), t("tut.l.rapido")),
                fila(tecla("Enter"), t("tut.l.confirmar")),
                lbl(t("tut.l.margen", 150), "negrita"),
                bonus);
        lado.setPrefWidth(230);
        lado.setMaxWidth(230);
        e.poner(lado, ancho + 14, 24);

        long[] inicio = {-1};
        e.relojes.add(new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (inicio[0] < 0) inicio[0] = ahora;
                double t = ((ahora - inicio[0]) / 1e9) % 5.6;
                GraphicsContext gc = capa.getGraphicsContext2D();
                gc.clearRect(0, 0, ancho, H);
                double p = Math.max(0, Math.min(1, (t - 0.5) / 2.0));
                p = p * p * (3 - 2 * p);
                boolean puesta = t > 2.6;
                double du = du0 * (1 - p), dv = dv0 * (1 - p);
                // Reloj
                double resto = 1 - 0.38 * Math.min(1, t / 2.6);
                gc.setFill(Color.web("#eadfce"));
                gc.fillRoundRect(16, H - 20, ancho - 32, 10, 8, 8);
                gc.setFill(Color.web("#2e9e5b"));
                gc.fillRoundRect(16, H - 20, (ancho - 32) * resto, 10, 8, 8);
                if (!puesta) {
                    gc.setEffect(new DropShadow(4, 2, 3, Pintor.TINTA.deriveColor(0, 1, 1, 0.45)));
                    Pintor.forma(gc, f.nucleo(), du, dv, tr, Pintor.colorDe(peru), Pintor.BORDE, 1.3);
                    gc.setEffect(null);
                    dibujarCursor(gc, tr.x(f.anclaU() + du), tr.y(f.anclaV() + dv));
                } else {
                    double q = Math.min(1, (t - 2.6) / 0.5);
                    gc.setEffect(new DropShadow(18 * (1 - q) + 4, Pintor.SOL));
                    Pintor.forma(gc, f.nucleo(), 0, 0, tr, Pintor.colorDe(peru), Pintor.BORDE, 1.4);
                    gc.setEffect(null);
                    double cx = tr.x(f.anclaU()), cy = tr.y(f.anclaV());
                    gc.setStroke(Pintor.SOL);
                    gc.setGlobalAlpha(1 - q);
                    gc.setLineWidth(4);
                    double r = 20 + 70 * q;
                    gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
                    gc.setGlobalAlpha(1);
                    gc.setFont(Fuentes.titulo(28));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.setTextBaseline(VPos.CENTER);
                    gc.setLineWidth(5);
                    gc.setStroke(Pintor.TINTA);
                    String s = "+" + Partida.PUNTOS_UBICACION + "  +360";
                    double sx = cx + 150, sy = cy + 20 - 20 * q;
                    gc.strokeText(s, sx, sy);
                    gc.setFill(Pintor.SOL);
                    gc.fillText(s, sx, sy);
                    gc.setFont(Fuentes.texto(FontWeight.EXTRA_BOLD, 15));
                    gc.setFill(Color.web("#1b7a44"));
                    gc.fillText(t("motivo.UBICADO"), ancho / 2, 24);
                }
            }
        });
        return e;
    }

    private static HBox fila(Node izquierda, String texto) {
        Label l = lbl(texto, "");
        l.setWrapText(true);
        HBox h = new HBox(8, izquierda, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static HBox teclasFlechas() {
        return new HBox(2, tecla("←"), tecla("↑"), tecla("→"), tecla("↓"));
    }

    /** Aciertos seguidos: la racha sube, llega el ×2, se sube de nivel y un fallo rompe un corazón. */
    private static Escena corazonesYRachas() {
        Escena e = new Escena();
        Corazones corazones = new Corazones(Partida.VIDAS);
        corazones.setScaleX(1.5);
        corazones.setScaleY(1.5);
        Label puntos = lbl("", "titulo-mapa");
        HBox vidas = new HBox(14, lbl(t("hud.vidas").toUpperCase(), "hud-clave"), new javafx.scene.Group(corazones));
        vidas.setAlignment(Pos.CENTER);
        HBox arriba = new HBox(40, vidas,
                new HBox(10, lbl(t("hud.puntaje").toUpperCase(), "hud-clave"), puntos));
        arriba.setAlignment(Pos.CENTER);

        List<Label> marcas = new ArrayList<>();
        HBox racha = new HBox(10);
        racha.setAlignment(Pos.CENTER);
        for (int i = 0; i < 9; i++) {
            Label m = lbl("", "tut-racha");
            m.setMinSize(46, 46);
            m.setAlignment(Pos.CENTER);
            marcas.add(m);
            racha.getChildren().add(m);
        }
        Label cartel = lbl("", "titulo-mapa-grande");
        Label avance = lbl("", "negrita");
        VBox v = new VBox(18, arriba, new VBox(6, lbl(t("hud.racha").toUpperCase(), "hud-clave"), racha), avance, cartel);
        ((VBox) v.getChildren().get(1)).setAlignment(Pos.CENTER);
        v.setAlignment(Pos.CENTER);
        v.setPrefSize(W, H);
        e.poner(v, 0, 0);

        int[] total = {0};
        List<KeyFrame> cuadros = new ArrayList<>();
        cuadros.add(en(0, () -> {
            corazones.mostrar(Partida.VIDAS);
            marcas.forEach(m -> {
                m.setText("");
                m.getStyleClass().removeAll("ok", "mal");
            });
            total[0] = 0;
            puntos.setText("0000");
            cartel.setText("");
            avance.setText(t("tut.l.avance", 0, Partida.PAISES_POR_NIVEL));
        }));
        for (int i = 1; i <= 8; i++) {
            int n = i;
            cuadros.add(en(0.2 + 0.65 * i, () -> {
                Label m = marcas.get(n - 1);
                m.setText("✔");
                m.getStyleClass().add("ok");
                int mult = Partida.multiplicador(n);
                total[0] += (Partida.PUNTOS_UBICACION + 300) * mult;
                puntos.setText(String.format("%04d", total[0]));
                avance.setText(n >= Partida.PAISES_POR_NIVEL ? t("cartel.nivel") : t("tut.l.avance", n, Partida.PAISES_POR_NIVEL));
                if (n == 4 || n == 8) cartel.setText(t("cartel.racha", n) + "  " + t("cartel.puntos", mult));
                else if (n == 5 || n == 1) cartel.setText("");
            }));
        }
        cuadros.add(en(6.4, () -> {
            Label m = marcas.get(8);
            m.setText("✗");
            m.getStyleClass().add("mal");
            corazones.mostrar(Partida.VIDAS - 1);
            cartel.setText(t("tut.l.rachaPerdida"));
        }));
        cuadros.add(en(8.6, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    /** Las tres pistas, una tras otra: descartar continentes, la zona y los vecinos. */
    private static Escena pistas(Atlas atlas) {
        Escena e = new Escena();
        double ancho = 430;
        Canvas base = e.poner(new Canvas(ancho, H), 0, 0);
        Canvas capa = e.poner(new Canvas(ancho, H), 0, 0);
        com.mapamundi.geo.Region vista = com.mapamundi.geo.Region.deGrados(-95, -25, -57, 14);
        Transformacion tr = Transformacion.ajustada(vista, ancho, H);
        Pintor.mapa(base.getGraphicsContext2D(), tr, atlas, p -> null, 0.6);
        Pais paraguay = pais(atlas, "PY");
        Forma f = paraguay.forma();
        List<Pais> vecinos = atlas.vecinos(paraguay);
        double km = 2 * Math.PI * Mercator.RADIO_TIERRA_KM * Math.cos(Math.toRadians(Mercator.lat(f.anclaV())));
        double radio = Math.max(4 * 150 / km, Math.hypot(f.cajaNucleo().ancho(), f.cajaNucleo().alto()) * 0.7);

        List<Label> botones = new ArrayList<>();
        VBox lado = new VBox(10, lbl(t("pista.titulo"), "titulo-seccion"));
        for (Pista p : Pista.values()) {
            Label b = lbl(p.nombre() + "  −" + p.costo(), "boton-pista-tut");
            b.setMaxWidth(Double.MAX_VALUE);
            botones.add(b);
            lado.getChildren().add(b);
        }
        Label explicacion = lbl("", "");
        explicacion.setWrapText(true);
        HBox descartados = new HBox(6);
        for (Continente c : Continente.values()) {
            boolean tachado = c == Continente.AFRICA || c == Continente.OCEANIA;
            Label l = lbl((tachado ? "✗ " : "") + c.nombre(), tachado ? "tut-tachado" : "tut-continente-chip");
            descartados.getChildren().add(l);
        }
        FlowWrap fw = new FlowWrap(descartados);
        lado.getChildren().addAll(explicacion, fw.nodo);
        lado.setPrefWidth(270);
        e.poner(lado, ancho + 12, 20);

        int[] estado = {0};
        Runnable dibujar = () -> {
            GraphicsContext gc = capa.getGraphicsContext2D();
            gc.clearRect(0, 0, ancho, H);
            if (estado[0] == 3) {
                for (Pais v : vecinos) Pintor.forma(gc, v.forma().anillos(), 0, 0, tr, Pintor.colorDe(v), Pintor.BORDE, 0.8);
                gc.setFont(Fuentes.texto(FontWeight.EXTRA_BOLD, 11));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                for (Pais v : vecinos) {
                    double x = tr.x(v.forma().anclaU()), y = tr.y(v.forma().anclaV());
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(3);
                    gc.strokeText(v.nombre(), x, y);
                    gc.setFill(Pintor.TINTA);
                    gc.fillText(v.nombre(), x, y);
                }
            }
            if (estado[0] == 2) {
                double cx = tr.x(f.anclaU() + radio * 0.3), cy = tr.y(f.anclaV() - radio * 0.25), r = radio * tr.escala();
                gc.setFill(Pintor.SOL.deriveColor(0, 1, 1, 0.25));
                gc.fillOval(cx - r, cy - r, r * 2, r * 2);
                gc.setStroke(Pintor.TINTA);
                gc.setLineWidth(4);
                gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
                gc.setStroke(Pintor.SOL);
                gc.setLineWidth(2.5);
                gc.setLineDashes(10, 7);
                gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
                gc.setLineDashes();
            }
            // La pieza, todavía sin colocar
            gc.setEffect(new DropShadow(4, 2, 3, Pintor.TINTA.deriveColor(0, 1, 1, 0.45)));
            Pintor.forma(gc, f.nucleo(), 0.05, -0.05, tr, Pintor.colorDe(paraguay), Pintor.BORDE, 1.2);
            gc.setEffect(null);
        };
        List<KeyFrame> cuadros = new ArrayList<>();
        cuadros.add(en(0, () -> {
            estado[0] = 0;
            botones.forEach(b -> resaltar(b, false, "activa"));
            explicacion.setText("");
            fw.nodo.setVisible(false);
            dibujar.run();
        }));
        for (int i = 0; i < 3; i++) {
            int k = i;
            cuadros.add(en(0.6 + i * 2.3, () -> {
                estado[0] = k + 1;
                for (int j = 0; j < botones.size(); j++) resaltar(botones.get(j), j == k, "activa");
                Pista p = Pista.values()[k];
                explicacion.setText(p.descripcion() + " " + t("pista.cuesta", p.costo()));
                fw.nodo.setVisible(k == 0);
                dibujar.run();
            }));
        }
        cuadros.add(en(8.2, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    /** Contenedor que reparte en dos filas una lista corta de chips. */
    private static final class FlowWrap {
        final javafx.scene.layout.FlowPane nodo;

        FlowWrap(HBox origen) {
            nodo = new javafx.scene.layout.FlowPane(6, 6);
            nodo.getChildren().addAll(new ArrayList<>(origen.getChildren()));
            nodo.setPrefWrapLength(260);
        }
    }

    /** Las tres reglas extra a la vez: el reloj, el frío o caliente y la pieza que se endereza. */
    private static Escena reglasExtra(Atlas atlas) {
        Escena e = new Escena();
        double col = 228;

        // Contrarreloj
        Label reloj = lbl("", "titulo-mapa-grande");
        Label mas = lbl("+5 s", "tutorial-puntos-mini");
        StackPane relojCaja = new StackPane(reloj, mas);
        StackPane.setAlignment(mas, Pos.TOP_RIGHT);
        relojCaja.setPrefSize(col, 170);
        relojCaja.getStyleClass().add("tut-escenario");
        VBox a = columna("◷  " + t("regla.contrarreloj"), relojCaja,
                t("tut.l.contrarreloj", Partida.SEGUNDOS_CONTRARRELOJ, Partida.SEGUNDOS_POR_ACIERTO), col);
        e.poner(a, 8, 10);
        int[] segundos = {Partida.SEGUNDOS_CONTRARRELOJ};
        int[] tics = {0};
        Timeline cuenta = new Timeline(new KeyFrame(Duration.millis(110), ev -> {
            tics[0]++;
            segundos[0] = segundos[0] <= 0 ? Partida.SEGUNDOS_CONTRARRELOJ : segundos[0] - 1;
            if (tics[0] % 16 == 0) {
                segundos[0] += Partida.SEGUNDOS_POR_ACIERTO;
                mas.setOpacity(1);
            } else {
                mas.setOpacity(Math.max(0, mas.getOpacity() - 0.12));
            }
            reloj.setText(String.format("%d:%02d", segundos[0] / 60, segundos[0] % 60));
        }));
        cuenta.setCycleCount(Animation.INDEFINITE);
        e.animaciones.add(cuenta);
        mas.setOpacity(0);
        reloj.setText("1:30");

        // Frío o caliente
        double fw = col - 20, fh = 150;
        Canvas frio = new Canvas(fw, fh);
        Label temperatura = lbl("", "negrita");
        StackPane frioCaja = new StackPane(frio);
        frioCaja.getStyleClass().add("tut-escenario");
        frioCaja.setPrefSize(col, 170);
        VBox b = columna("♨  " + t("regla.frioCaliente"), frioCaja, "", col);
        b.getChildren().set(2, temperatura);
        e.poner(b, 8 + col + 12, 10);
        long[] inicio = {-1};
        e.relojes.add(new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (inicio[0] < 0) inicio[0] = ahora;
                double s = ((ahora - inicio[0]) / 1e9) % 4;
                double p = s < 2 ? s / 2 : 1 - (s - 2) / 2;
                p = p * p * (3 - 2 * p);
                double tx = fw / 2, ty = fh / 2, x = 24 + (tx - 24) * p, y = 26 + (ty - 26) * p;
                double margenes = 1 + Math.hypot(x - tx, y - ty) / 9;
                if (p > 0.97) margenes = 1;
                Color c = colorTemperatura(margenes);
                GraphicsContext gc = frio.getGraphicsContext2D();
                gc.clearRect(0, 0, fw, fh);
                gc.setFont(Fuentes.texto(FontWeight.BOLD, 26));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.setFill(Pintor.SOL);
                gc.fillText("★", tx, ty);
                gc.setEffect(new DropShadow(BlurType.GAUSSIAN, c, 22, 0.6, 0, 0));
                gc.setFill(Color.web("#f3d27a"));
                gc.fillRoundRect(x - 20, y - 14, 40, 28, 12, 12);
                gc.setEffect(null);
                gc.setStroke(Pintor.BORDE);
                gc.setLineWidth(1.4);
                gc.strokeRoundRect(x - 20, y - 14, 40, 28, 12, 12);
                temperatura.setText("♨ " + textoTemperatura(margenes));
                temperatura.setStyle("-fx-text-fill: " + web(c.darker()) + ";");
            }
        });

        // Pieza girada
        Pais italia = pais(atlas, "IT");
        Canvas silueta = Miniatura.silueta(italia, 140, 130);
        Label teclaE = tecla("E");
        Label derecha = lbl("", "acierto");
        StackPane giroCaja = new StackPane(silueta, derecha);
        StackPane.setAlignment(derecha, Pos.BOTTOM_CENTER);
        giroCaja.getStyleClass().add("tut-escenario");
        giroCaja.setPrefSize(col, 170);
        VBox c = columna("⟳  " + t("regla.piezaGirada"), giroCaja, "", col);
        HBox teclas = new HBox(6, tecla("Q"), teclaE, lbl(t("tut.l.girar"), "texto-suave"));
        teclas.setAlignment(Pos.CENTER);
        c.getChildren().set(2, teclas);
        e.poner(c, 8 + 2 * (col + 12), 10);
        List<KeyFrame> cuadros = new ArrayList<>();
        cuadros.add(en(0, () -> {
            silueta.setRotate(135);
            derecha.setText("");
        }));
        for (int i = 1; i <= 9; i++) {
            int k = i;
            cuadros.add(en(0.5 + 0.35 * i, () -> {
                silueta.setRotate(135 - 15 * k);
                resaltar(teclaE, true, "activa");
            }));
            cuadros.add(en(0.5 + 0.35 * i + 0.15, () -> resaltar(teclaE, false, "activa")));
        }
        cuadros.add(en(4.0, () -> derecha.setText(t("tut.l.derecha", Partida.PUNTOS_GIRO))));
        cuadros.add(en(5.6, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    private static String web(Color c) {
        return String.format("#%02x%02x%02x", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private static VBox columna(String titulo, Node contenido, String texto, double ancho) {
        Label t = lbl(texto, "texto-suave");
        t.setWrapText(true);
        t.setTextAlignment(TextAlignment.CENTER);
        VBox v = new VBox(10, lbl(titulo, "negrita"), contenido, t);
        v.setAlignment(Pos.TOP_CENTER);
        v.setPrefWidth(ancho);
        v.setMaxWidth(ancho);
        return v;
    }

    /** Las insignias aparecen una a una. */
    private static Escena insignias() {
        Escena e = new Escena();
        Logro[] lista = {Logro.PRIMER_PAIS, Logro.RACHA_5, Logro.PUNTERIA, Logro.VELOCISTA, Logro.EUROPA, Logro.DUENO_DEL_MUNDO};
        HBox fila = new HBox(14);
        fila.setAlignment(Pos.CENTER);
        List<VBox> items = new ArrayList<>();
        for (Logro l : lista) {
            Label nombre = lbl(l.nombre(), "logro-nombre");
            nombre.setWrapText(true);
            nombre.setTextAlignment(TextAlignment.CENTER);
            nombre.setMaxWidth(104);
            VBox item = new VBox(8, new Insignia(l, l != Logro.DUENO_DEL_MUNDO, 32), nombre);
            item.setAlignment(Pos.TOP_CENTER);
            item.setPrefWidth(106);
            items.add(item);
            fila.getChildren().add(item);
        }
        VBox v = new VBox(26, fila, lbl(t("tutorial.categorias") + "   ·   " + t("tut.l.insignias", Logro.values().length), "texto-suave"));
        v.setAlignment(Pos.CENTER);
        v.setPrefSize(W, H);
        e.poner(v, 0, 0);
        List<KeyFrame> cuadros = new ArrayList<>();
        List<KeyValue> ceros = new ArrayList<>();
        for (VBox item : items) {
            ceros.add(new KeyValue(item.scaleXProperty(), 0));
            ceros.add(new KeyValue(item.scaleYProperty(), 0));
        }
        cuadros.add(new KeyFrame(Duration.ZERO, ceros.toArray(KeyValue[]::new)));
        for (int i = 0; i < items.size(); i++) {
            VBox item = items.get(i);
            double t0 = 0.3 + 0.45 * i;
            cuadros.add(valores(t0, new KeyValue(item.scaleXProperty(), 0), new KeyValue(item.scaleYProperty(), 0)));
            cuadros.add(valores(t0 + 0.4, new KeyValue(item.scaleXProperty(), 1, REBOTE), new KeyValue(item.scaleYProperty(), 1, REBOTE)));
        }
        cuadros.add(en(6.0, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    // ================================================================== estilo clásico

    /** Las tres diferencias del clásico, que se van iluminando. */
    private static Escena clasicoIntro() {
        Escena e = new Escena();
        String[][] datos = {
                {"⬇", t("tut.l.cae"), t("tut.l.cae.d")},
                {"➜", t("tut.l.noSeDetiene"), t("tut.l.noSeDetiene.d")},
                {"✖", t("tut.l.cincoErrores", PartidaClasica.ERRORES_MAX), t("tut.l.cincoErrores.d")}};
        HBox fila = new HBox(18);
        fila.setAlignment(Pos.CENTER);
        List<VBox> cartas = new ArrayList<>();
        List<Label> iconos = new ArrayList<>();
        for (String[] d : datos) {
            Label icono = lbl(d[0], "estilo-icono-grande");
            icono.setMinSize(64, 64);
            icono.setAlignment(Pos.CENTER);
            Label desc = lbl(d[2], "texto-suave");
            desc.setWrapText(true);
            desc.setTextAlignment(TextAlignment.CENTER);
            VBox carta = new VBox(12, icono, lbl(d[1], "estilo-nombre"), desc);
            carta.setAlignment(Pos.TOP_CENTER);
            carta.getStyleClass().add("tut-tarjeta");
            carta.setPrefSize(210, 250);
            cartas.add(carta);
            iconos.add(icono);
            fila.getChildren().add(carta);
        }
        VBox v = new VBox(12, lbl(t("tut.l.insigniaClasico"), "insignia-clasico"), fila);
        v.setAlignment(Pos.CENTER);
        v.setPrefSize(W, H);
        e.poner(v, 0, 0);
        Label cae = iconos.get(0), avanza = iconos.get(1), error = iconos.get(2);
        e.bucle(
                en(0, () -> cartas.forEach(c -> resaltar(c, false, "resaltada"))),
                en(0.1, () -> resaltar(cartas.get(0), true, "resaltada")),
                valores(0.1, kv(cae.translateYProperty(), -8)),
                valores(0.6, kv(cae.translateYProperty(), 10)),
                valores(1.1, kv(cae.translateYProperty(), -8)),
                valores(1.6, kv(cae.translateYProperty(), 0), kv(avanza.translateXProperty(), 0)),
                en(1.6, () -> {
                    resaltar(cartas.get(0), false, "resaltada");
                    resaltar(cartas.get(1), true, "resaltada");
                }),
                valores(2.1, kv(avanza.translateXProperty(), 22)),
                valores(2.6, kv(avanza.translateXProperty(), -22)),
                valores(3.1, kv(avanza.translateXProperty(), 0), kv(error.scaleXProperty(), 1), kv(error.scaleYProperty(), 1)),
                en(3.1, () -> {
                    resaltar(cartas.get(1), false, "resaltada");
                    resaltar(cartas.get(2), true, "resaltada");
                }),
                valores(3.4, kv(error.scaleXProperty(), 1.3), kv(error.scaleYProperty(), 1.3)),
                valores(3.7, kv(error.scaleXProperty(), 1), kv(error.scaleYProperty(), 1)),
                valores(4.0, kv(error.scaleXProperty(), 1.3), kv(error.scaleYProperty(), 1.3)),
                valores(4.3, kv(error.scaleXProperty(), 1), kv(error.scaleYProperty(), 1)),
                en(5.0, () -> { }));
        return e;
    }

    /** El país cae paso a paso y el cursor elige su continente con doble clic. */
    private static Escena caidaClasica(Atlas atlas) {
        Escena e = new Escena();
        Rectangle cielo = new Rectangle(W - 40, 200);
        cielo.setArcWidth(24);
        cielo.setArcHeight(24);
        cielo.setFill(Pintor.AGUA);
        cielo.setStroke(Pintor.TINTA);
        cielo.setStrokeWidth(3);
        e.poner(cielo, 20, 8);
        Pais japon = pais(atlas, "JP");
        VBox pieza = new VBox(4, lbl(japon.nombre(), "rotulo-caida"), Miniatura.silueta(japon, 100, 70));
        pieza.setAlignment(Pos.CENTER);
        e.poner(pieza, W / 2 - 55, 12);
        Rectangle barraFondo = new Rectangle(W - 40, 8, Color.web("#eadfce"));
        e.poner(barraFondo, 20, 214);
        Rectangle barra = new Rectangle(W - 40, 8, Color.web("#2e9e5b"));
        e.poner(barra, 20, 214);

        List<StackPane> cartas = new ArrayList<>();
        for (Continente c : Continente.values()) {
            StackPane carta = tarjetaContinente(atlas, c, 112, 72);
            carta.getChildren().add(lbl(String.valueOf(c.tecla()), "numero-tecla"));
            StackPane.setAlignment(carta.getChildren().get(2), Pos.BOTTOM_LEFT);
            cartas.add(e.poner(carta, 22 + c.ordinal() * 136, 232));
        }
        Label bien = e.poner(lbl("", "acierto"), 20, 22);
        bien.setPrefWidth(W - 40);
        bien.setAlignment(Pos.CENTER);
        Polygon cursor = e.poner(cursor(), 0, 0);
        StackPane asia = cartas.get(Continente.ASIA.ordinal());
        double ax = 22 + 3 * 136 + 64, ay = 232 + 46;

        List<KeyFrame> cuadros = new ArrayList<>();
        cuadros.add(en(0, () -> {
            cartas.forEach(c -> resaltar(c, false, "seleccionada"));
            bien.setText("");
            pieza.setOpacity(1);
        }));
        // Caída a saltos, como el Timer del original
        for (int k = 0; k <= 16; k++) {
            cuadros.add(new KeyFrame(Duration.seconds(0.2 * k),
                    new KeyValue(pieza.translateYProperty(), k * 4.5, Interpolator.DISCRETE),
                    new KeyValue(barra.widthProperty(), (W - 40) * (1 - k / 30.0), Interpolator.DISCRETE)));
        }
        cuadros.add(valores(0, kv(cursor.translateXProperty(), 660), kv(cursor.translateYProperty(), 120)));
        cuadros.add(valores(1.2, kv(cursor.translateXProperty(), 660), kv(cursor.translateYProperty(), 120)));
        cuadros.add(valores(2.4, kv(cursor.translateXProperty(), ax), kv(cursor.translateYProperty(), ay)));
        cuadros.add(en(2.5, () -> resaltar(asia, true, "seleccionada")));
        cuadros.add(en(2.75, () -> resaltar(asia, false, "seleccionada")));
        cuadros.add(en(2.9, () -> resaltar(asia, true, "seleccionada")));
        cuadros.add(en(3.3, () -> {
            bien.setText("✔ " + t("tut.l.dobleClic"));
            pieza.setOpacity(0.25);
        }));
        cuadros.add(valores(4.8, kv(cursor.translateXProperty(), ax), kv(cursor.translateYProperty(), ay)));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    /** En el mapa la pieza avanza sola y despacio; con la flecha pulsada, más rápido; Enter la suelta. */
    private static Escena mapaClasico(Atlas atlas) {
        Escena e = new Escena();
        double ancho = 430;
        Canvas base = e.poner(new Canvas(ancho, H), 0, 0);
        Canvas capa = e.poner(new Canvas(ancho, H), 0, 0);
        com.mapamundi.geo.Region vista = Continente.AFRICA.vista();
        Transformacion tr = Transformacion.ajustada(vista, ancho, H);
        Pintor.mapa(base.getGraphicsContext2D(), tr, atlas, p -> null, 0.6);
        Pais kenia = pais(atlas, "KE");
        Forma f = kenia.forma();
        double su = vista.centroU() - f.anclaU(), sv = vista.vMin() + vista.alto() * 0.06 - f.anclaV();

        Label arriba = tecla("↑"), abajo = tecla("↓"), izq = tecla("←"), der = tecla("→"), enter = tecla("Enter");
        GridPane flechas = new GridPane();
        flechas.setHgap(3);
        flechas.setVgap(3);
        flechas.add(arriba, 1, 0);
        flechas.add(izq, 0, 1);
        flechas.add(abajo, 1, 1);
        flechas.add(der, 2, 1);
        Label estado = lbl("", "negrita");
        estado.setWrapText(true);
        VBox lado = new VBox(12, flechas, enter, estado,
                lbl(t("tut.l.sola"), "texto-suave"), lbl(t("tut.l.pulsada"), "texto-suave"), lbl(t("tut.l.ctrl"), "texto-suave"));
        lado.setPrefWidth(270);
        e.poner(lado, ancho + 16, 24);

        long[] inicio = {-1};
        String[] ultimo = {""};
        e.relojes.add(new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (inicio[0] < 0) inicio[0] = ahora;
                double t = ((ahora - inicio[0]) / 1e9) % 7.0;
                double du, dv;
                String fase;
                double sv1 = sv * 0.82;
                if (t < 1.6) {
                    du = su;
                    dv = sv + (sv1 - sv) * (t / 1.6);
                    fase = "sola";
                } else if (t < 3.4) {
                    du = su;
                    dv = sv1 * (1 - (t - 1.6) / 1.8);
                    fase = "abajo";
                } else if (t < 4.6) {
                    du = su * (1 - (t - 3.4) / 1.2);
                    dv = 0;
                    fase = su < 0 ? "der" : "izq";
                } else {
                    du = 0;
                    dv = 0;
                    fase = t < 4.9 ? "enter" : "bien";
                }
                if (!fase.equals(ultimo[0])) {
                    ultimo[0] = fase;
                    resaltar(abajo, fase.equals("abajo"), "activa");
                    resaltar(der, fase.equals("der"), "activa");
                    resaltar(izq, fase.equals("izq"), "activa");
                    resaltar(enter, fase.equals("enter") || fase.equals("bien"), "activa");
                    estado.setText(switch (fase) {
                        case "sola" -> t("tut.l.estadoSola");
                        case "abajo", "der", "izq" -> t("tut.l.estadoPulsada");
                        default -> t("clasico.motivo.UBICADO") + "  +" + PartidaClasica.puntosPorPais(Modo.MAPA_PAIS);
                    });
                }
                GraphicsContext gc = capa.getGraphicsContext2D();
                gc.clearRect(0, 0, ancho, H);
                boolean bien = fase.equals("bien");
                gc.setEffect(bien ? new DropShadow(14, Pintor.SOL) : new DropShadow(4, 2, 3, Pintor.TINTA.deriveColor(0, 1, 1, 0.45)));
                Pintor.forma(gc, f.nucleo(), du, dv, tr, Pintor.colorDe(kenia), Pintor.BORDE, 1.3);
                gc.setEffect(null);
                if (!bien) {
                    double cx = tr.x(f.anclaU() + du), cy = tr.y(f.anclaV() + dv);
                    double ang = switch (fase) {
                        case "der" -> 0;
                        case "izq" -> Math.PI;
                        default -> Math.PI / 2;
                    };
                    double x = cx + Math.cos(ang) * 24, y = cy + Math.sin(ang) * 24;
                    double[] xs = {x + Math.cos(ang) * 8, x + Math.cos(ang + 2.3) * 8, x + Math.cos(ang - 2.3) * 8};
                    double[] ys = {y + Math.sin(ang) * 8, y + Math.sin(ang + 2.3) * 8, y + Math.sin(ang - 2.3) * 8};
                    gc.setFill(Pintor.SOL);
                    gc.setStroke(Pintor.TINTA);
                    gc.setLineWidth(1.6);
                    gc.fillPolygon(xs, ys, 3);
                    gc.strokePolygon(xs, ys, 3);
                }
            }
        });
        return e;
    }

    /** Los errores se acumulan (5 para toda la partida), los puntos por modo y el "¡Mapa completo!". */
    private static Escena puntosClasicos(Atlas atlas) {
        Escena e = new Escena();
        List<Label> cajas = new ArrayList<>();
        HBox errores = new HBox(6);
        for (int i = 0; i < PartidaClasica.ERRORES_MAX; i++) {
            Label c = lbl("", "tut-error");
            c.setMinSize(34, 34);
            c.setAlignment(Pos.CENTER);
            cajas.add(c);
            errores.getChildren().add(c);
        }
        Label fin = lbl("", "fallo");
        VBox a = new VBox(10, lbl(t("hud.errores").toUpperCase(), "hud-clave"), errores, fin);
        a.setAlignment(Pos.TOP_CENTER);
        a.setPrefWidth(210);

        VBox tabla = new VBox(5, lbl(t("inicio.puntos").toUpperCase(), "hud-clave"));
        List<HBox> filas = new ArrayList<>();
        for (Modo m : Modo.values()) {
            Label nombre = lbl(m.nombre(), "");
            HBox.setHgrow(nombre, Priority.ALWAYS);
            nombre.setMaxWidth(Double.MAX_VALUE);
            HBox f = new HBox(8, nombre, lbl("+" + PartidaClasica.puntosPorPais(m), "negrita"));
            f.getStyleClass().add("tut-fila");
            f.setPrefWidth(230);
            filas.add(f);
            tabla.getChildren().add(f);
        }
        tabla.setPrefWidth(240);

        StackPane africa = tarjetaContinente(atlas, Continente.AFRICA, 170, 130);
        Label sello = lbl(t("clasico.mapaCompleto"), "mapa-completo");
        africa.getChildren().add(sello);
        VBox c = new VBox(10, lbl(t("menu.continentes").toUpperCase(), "hud-clave"), africa);
        c.setAlignment(Pos.TOP_CENTER);

        HBox fila = new HBox(24, a, tabla, c);
        fila.setAlignment(Pos.CENTER);
        fila.setFillHeight(false);
        fila.setPrefSize(W, H);
        e.poner(fila, 0, 0);

        List<KeyFrame> cuadros = new ArrayList<>();
        cuadros.add(en(0, () -> {
            cajas.forEach(x -> {
                x.setText("");
                x.getStyleClass().remove("mal");
            });
            fin.setText("");
        }));
        for (int i = 0; i < cajas.size(); i++) {
            int k = i;
            cuadros.add(en(0.5 + 0.7 * i, () -> {
                cajas.get(k).setText("✖");
                cajas.get(k).getStyleClass().add("mal");
                if (k == cajas.size() - 1) fin.setText(t("fin.titulo"));
            }));
        }
        for (int i = 0; i < filas.size(); i++) {
            int k = i;
            cuadros.add(en(0.5 + 1.1 * i, () -> {
                for (int j = 0; j < filas.size(); j++) resaltar(filas.get(j), j == k, "activa");
            }));
        }
        cuadros.add(valores(0, kv(sello.scaleXProperty(), 0), kv(sello.scaleYProperty(), 0)));
        cuadros.add(valores(1.8, kv(sello.scaleXProperty(), 0), kv(sello.scaleYProperty(), 0)));
        cuadros.add(valores(2.2, new KeyValue(sello.scaleXProperty(), 1.15, REBOTE),
                new KeyValue(sello.scaleYProperty(), 1.15, REBOTE)));
        cuadros.add(en(6.4, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }

    // ================================================================== controles

    /** El teclado, fila a fila. */
    private static Escena controles() {
        Escena e = new Escena();
        Object[][] filas = {
                {new String[]{"←", "↑", "→", "↓"}, t("tut.k.mover")},
                {new String[]{"1", "2", "3", "4", "5"}, t("tut.k.numeros")},
                {new String[]{"Enter"}, t("tut.k.confirmar")},
                {new String[]{"Shift", "+", "↑"}, t("tut.k.shift")},
                {new String[]{"Q", "E"}, t("tut.k.girar")},
                {new String[]{"Ctrl", "+", "→"}, t("tut.k.ctrl")},
                {new String[]{"F5", "P", "Esc"}, t("tut.k.pausa")},
                {new String[]{"F11"}, t("tut.k.f11")}};
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(7);
        List<List<Label>> teclas = new ArrayList<>();
        for (int i = 0; i < filas.length; i++) {
            HBox caja = new HBox(4);
            caja.setAlignment(Pos.CENTER_RIGHT);
            List<Label> propias = new ArrayList<>();
            for (String s : (String[]) filas[i][0]) {
                Label l = s.equals("+") ? lbl("+", "negrita") : tecla(s);
                if (!s.equals("+")) propias.add(l);
                caja.getChildren().add(l);
            }
            teclas.add(propias);
            caja.setPrefWidth(250);
            g.add(caja, 0, i);
            g.add(lbl((String) filas[i][1], ""), 1, i);
        }
        g.setAlignment(Pos.CENTER);
        g.setPrefSize(W, H);
        e.poner(g, 0, 0);
        List<KeyFrame> cuadros = new ArrayList<>();
        for (int i = 0; i < teclas.size(); i++) {
            int k = i;
            cuadros.add(en(i * 0.9, () -> {
                for (int j = 0; j < teclas.size(); j++) for (Label l : teclas.get(j)) resaltar(l, j == k, "activa");
            }));
        }
        cuadros.add(en(teclas.size() * 0.9, () -> { }));
        e.bucle(cuadros.toArray(KeyFrame[]::new));
        return e;
    }
}
