package com.mapamundi.ui;

import com.mapamundi.i18n.Textos;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.juego.PartidaClasica.Direccion;
import com.mapamundi.juego.PartidaClasica.Fase;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.mapa.MapaClasico;
import com.mapamundi.ui.mapa.Miniatura;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.mapamundi.i18n.Textos.t;

/**
 * Pantalla del modo clásico (el juego de 2000): arriba el panel con puntaje, errores y la barra de tiempo;
 * en el centro, el país que cae sobre los cinco continentes o el mapa del continente; a la derecha, la
 * ficha del país. Los tres relojes del original (caída, movimiento y tiempo) son tres Timeline.
 */
final class PantallaClasica extends StackPane {

    /**
     * Velocidad de la pieza en el mapa respecto al ritmo del original: a la deriva (sin tocar nada) va más
     * despacio, para dar tiempo a pensar, y con una flecha pulsada, algo más rápido.
     */
    private static final double VELOCIDAD_DERIVA = 0.6;
    private static final double VELOCIDAD_CON_FLECHA = 1.6;

    private final PartidaClasica partida;
    private final Contexto ctx;
    private final Sonidos sonidos;
    private final PartidaClasica.Ritmo ritmo;
    private final Timeline caida, movimiento, reloj;

    private final Label puntaje = FichaPais.etiqueta("", "hud-puntaje");
    private final Label errores = FichaPais.etiqueta("", "hud-valor");
    private final Label avance = FichaPais.etiqueta("", "hud-valor");
    private final ProgressBar tiempo = new ProgressBar(1);
    private final Label tiempoTexto = FichaPais.etiqueta("", "hud-valor");
    private final Button sonido = new Button("🔊");

    private final BorderPane cuerpo = new BorderPane();
    private final StackPane centro = new StackPane();
    private final VBox lateral = new VBox(10);
    private final MapaClasico mapa;

    // Vista de la caída
    private final Pane cielo = new Pane();
    private final VBox pieza = new VBox(4);
    private final Map<Continente, Button> tarjetas = new EnumMap<>(Continente.class);
    private final VBox vistaCaida;
    private int seleccion;

    private final StackPane capaPausa;
    private StackPane mensaje;
    private PartidaClasica.Resultado mostrado;
    private Pais paisMostrado;
    private Fase faseAnterior;
    /** Partida de práctica: una guía a la derecha dice qué hacer y una flecha señala el continente. */
    private final boolean practica;
    private VBox guia;
    private Label guiaTexto, guiaPaso, mano;
    private final EventHandler<KeyEvent> pulsar = this::tecla;
    private final EventHandler<KeyEvent> soltar = this::soltar;

    PantallaClasica(PartidaClasica partida, Contexto ctx, Runnable salir, Consumer<PartidaClasica> alTerminar) {
        this(partida, ctx, salir, alTerminar, null);
    }

    /** @param saltarPractica si no es null, es una partida de práctica guiada y esto la abandona */
    PantallaClasica(PartidaClasica partida, Contexto ctx, Runnable salir, Consumer<PartidaClasica> alTerminar,
                    Runnable saltarPractica) {
        this.partida = partida;
        this.practica = saltarPractica != null;
        this.ctx = ctx;
        this.sonidos = ctx.sonidos();
        this.ritmo = PartidaClasica.RITMO;
        this.mapa = new MapaClasico(ctx.atlas());

        cuerpo.setTop(cabecera());
        vistaCaida = construirCaida();
        HBox.setHgrow(centro, Priority.ALWAYS);
        lateral.getStyleClass().add("panel");
        lateral.setPrefWidth(320);
        lateral.setMinWidth(280);
        lateral.setMaxHeight(Region.USE_PREF_SIZE);
        HBox fila = new HBox(18, centro, lateral);
        BorderPane.setMargin(fila, new Insets(16));
        cuerpo.setCenter(fila);
        capaPausa = construirPausa(salir);
        getChildren().addAll(cuerpo, capaPausa);
        if (practica) construirGuia(saltarPractica);

        caida = reloj(ritmo.caida(), partida::ticCaida);
        movimiento = reloj(ritmo.movimiento(), partida::ticMovimiento);
        movimiento.setRate(VELOCIDAD_DERIVA);
        reloj = reloj(ritmo.reloj(), partida::ticReloj);

        partida.alSonar(sonidos::reproducir);
        partida.alCambiar(() -> {
            if (partida.fase() == Fase.FIN) {
                desconectar();
                alTerminar.accept(partida);
            } else {
                actualizar();
            }
        });
        sceneProperty().addListener((o, antes, escena) -> {
            if (antes != null) {
                antes.removeEventFilter(KeyEvent.KEY_PRESSED, pulsar);
                antes.removeEventFilter(KeyEvent.KEY_RELEASED, soltar);
            }
            if (escena != null) {
                escena.addEventFilter(KeyEvent.KEY_PRESSED, pulsar);
                escena.addEventFilter(KeyEvent.KEY_RELEASED, soltar);
            }
        });
        actualizar();
    }

    private static Timeline reloj(int ms, Runnable accion) {
        Timeline t = new Timeline(new KeyFrame(Duration.millis(ms), e -> accion.run()));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
        return t;
    }

    // ------------------------------------------------------------------ construcción

    private VBox cabecera() {
        Label titulo = FichaPais.etiqueta("Mapamundi", "titulo-hud");
        Label estilo = FichaPais.etiqueta(PantallaEleccion.zona(partida.enJuego()), "insignia-clasico");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Label modo = FichaPais.etiqueta(partida.ajustes().modo().nombre(), "texto-suave");
        Button pausa = new Button("⏸ F5");
        for (Button b : new Button[]{sonido, pausa}) b.setFocusTraversable(false);
        sonido.setOnAction(e -> {
            sonidos.alternarSilencio();
            actualizar();
        });
        pausa.setOnAction(e -> partida.alternarPausa());
        HBox fila = new HBox(18, titulo, estilo, par(t("hud.puntaje"), puntaje), par(t("hud.errores"), errores),
                par(t("hud.avance"), avance), hueco, modo, sonido, pausa);
        fila.setAlignment(Pos.CENTER_LEFT);
        tiempo.getStyleClass().add("barra-tiempo");
        tiempo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tiempo, Priority.ALWAYS);
        tiempoTexto.setMinWidth(130);
        HBox barra = new HBox(10, tiempo, tiempoTexto);
        barra.setAlignment(Pos.CENTER);
        VBox v = new VBox(6, fila, barra);
        v.getStyleClass().add("hud");
        v.setPadding(new Insets(8, 16, 8, 16));
        return v;
    }

    private static HBox par(String clave, Label valor) {
        HBox h = new HBox(5, FichaPais.etiqueta(clave.toUpperCase(Textos.idioma().locale()), "hud-clave"), valor);
        h.setAlignment(Pos.BASELINE_LEFT);
        return h;
    }

    /** El "cielo" por donde cae el país y, debajo, los cinco perfiles de continente del original. */
    private VBox construirCaida() {
        cielo.getStyleClass().add("cielo-clasico");
        cielo.getChildren().add(pieza);
        pieza.setAlignment(Pos.CENTER);
        VBox.setVgrow(cielo, Priority.ALWAYS);
        cielo.setMinHeight(200);
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(cielo.widthProperty());
        recorte.heightProperty().bind(cielo.heightProperty());
        recorte.setArcWidth(28);
        recorte.setArcHeight(28);
        cielo.setClip(recorte);
        cielo.heightProperty().addListener(o -> colocarPieza());
        cielo.widthProperty().addListener(o -> colocarPieza());

        HBox fila = new HBox(12);
        fila.setAlignment(Pos.CENTER);
        for (Continente c : Continente.values()) fila.getChildren().add(tarjeta(c));
        VBox v = new VBox(14, cielo, fila);
        return v;
    }

    private Button tarjeta(Continente c) {
        Canvas mini = Miniatura.continente(ctx.atlas(), c, 150, 104);
        Label nombre = FichaPais.etiqueta(c.nombre(), "tutorial-continente");
        Label tecla = FichaPais.etiqueta(String.valueOf(c.tecla()), "numero-tecla");
        Label completo = FichaPais.etiqueta(t("clasico.mapaCompleto"), "mapa-completo");
        completo.setVisible(false);
        StackPane contenido = new StackPane(mini, nombre, tecla, completo);
        StackPane.setAlignment(nombre, Pos.TOP_CENTER);
        StackPane.setAlignment(tecla, Pos.BOTTOM_LEFT);
        Button b = new Button(null, contenido);
        b.getStyleClass().setAll("tarjeta-continente");
        b.setFocusTraversable(false);
        b.setUserData(completo);
        // Como en el original: un clic marca el continente y doble clic lo elige.
        b.setOnMouseClicked(e -> {
            if (!partida.elegible(c)) return;
            seleccion = c.ordinal();
            if (e.getClickCount() >= 2) partida.elegirContinente(c);
            else actualizarTarjetas();
        });
        tarjetas.put(c, b);
        return b;
    }

    private StackPane construirPausa(Runnable salir) {
        Button seguir = new Button(t("pausa.continuar") + " (F5)");
        seguir.getStyleClass().add("boton-principal");
        seguir.setMaxWidth(Double.MAX_VALUE);
        seguir.setOnAction(e -> partida.alternarPausa());
        Button menu = new Button(t("pausa.salir"));
        menu.setMaxWidth(Double.MAX_VALUE);
        menu.setOnAction(e -> {
            desconectar();
            salir.run();
        });
        VBox caja = new VBox(12, FichaPais.etiqueta(t("pausa.titulo"), "titulo-mapa-grande"),
                FichaPais.etiqueta(t("pausa.detenido"), ""), seguir, menu);
        caja.getStyleClass().add("panel");
        caja.setAlignment(Pos.CENTER);
        caja.setMaxSize(320, USE_PREF_SIZE);
        StackPane capa = new StackPane(caja);
        capa.getStyleClass().add("capa-pausa");
        capa.setVisible(false);
        return capa;
    }

    // ------------------------------------------------------------------ actualización

    private void actualizar() {
        puntaje.setText(String.format("%04d", partida.puntaje()));
        errores.setText(partida.errores() + "/" + PartidaClasica.ERRORES_MAX);
        errores.setStyle(partida.errores() >= PartidaClasica.ERRORES_MAX - 1 ? "-fx-text-fill: -rojo;" : "");
        avance.setText(partida.ubicados().size() + "/" + partida.totalPaises());
        sonido.setText(sonidos.enSilencio() ? "🔇" : "🔊");
        capaPausa.setVisible(partida.enPausa());

        Fase fase = partida.fase();
        boolean enMapa = fase == Fase.UBICACION || fase == Fase.RESULTADO && faseAnterior == Fase.UBICACION;
        double restante = enMapa ? 1 - partida.reloj() : 1 - partida.caida();
        tiempo.setProgress(restante);
        tiempo.getStyleClass().remove("poco");
        if (restante <= 0.25) tiempo.getStyleClass().add("poco");
        tiempoTexto.setText(t(enMapa ? "clasico.tiempoMapa" : "clasico.tiempoCaida"));

        Node vista = enMapa ? mapa : vistaCaida;
        if (centro.getChildren().isEmpty() || centro.getChildren().getFirst() != vista) {
            centro.getChildren().setAll(vista);
            FadeTransition f = new FadeTransition(Duration.millis(200), vista);
            f.setFromValue(0.3);
            f.setToValue(1);
            f.play();
        }
        if (partida.actual() != paisMostrado || fase != faseAnterior) {
            paisMostrado = partida.actual();
            armarPieza();
            armarLateral(enMapa);
            if (fase == Fase.CAIDA) seleccion = primeraElegible(seleccion, 0);
        }
        if (enMapa) mapa.mostrar(partida);
        else {
            colocarPieza();
            actualizarTarjetas();
        }
        if (practica) actualizarGuia(fase);
        faseAnterior = fase == Fase.RESULTADO ? faseAnterior : fase;

        PartidaClasica.Resultado r = fase == Fase.RESULTADO ? partida.resultado() : null;
        if (r != null && r != mostrado) mostrarMensaje(r);
        if (r == null && mensaje != null) {
            getChildren().remove(mensaje);
            mensaje = null;
        }
        mostrado = r;
    }

    /** La pieza que cae: la silueta del país (o un círculo de color) con su nombre o su capital encima. */
    private void armarPieza() {
        Pais p = partida.actual();
        pieza.getChildren().clear();
        String rotulo = switch (partida.ajustes().modo()) {
            case DATOS -> null;
            case MAPA_CAPITAL, CAPITAL -> p.capital();
            default -> p.nombre();
        };
        if (rotulo != null) pieza.getChildren().add(FichaPais.etiqueta(rotulo, "rotulo-caida"));
        if (partida.ajustes().modo().muestraForma()) {
            pieza.getChildren().add(Miniatura.silueta(p, 150, 115));
        } else {
            pieza.getChildren().add(Miniatura.figura(p, 34));
        }
    }

    private void colocarPieza() {
        double alto = pieza.prefHeight(-1), ancho = pieza.prefWidth(-1);
        pieza.resizeRelocate((cielo.getWidth() - ancho) / 2, 8 + partida.caida() * Math.max(0, cielo.getHeight() - alto - 16), ancho, alto);
    }

    private void actualizarTarjetas() {
        for (Continente c : Continente.values()) {
            Button b = tarjetas.get(c);
            boolean elegible = partida.elegible(c);
            b.setOpacity(elegible ? 1 : 0.45);
            ((Label) b.getUserData()).setVisible(partida.completos().contains(c));
            b.getStyleClass().remove("seleccionada");
            if (elegible && c.ordinal() == seleccion) b.getStyleClass().add("seleccionada");
        }
    }

    /** A la derecha: qué hay que hacer en esta fase y, si se eligió (o en "Solo datos"), la ficha del país. */
    private void armarLateral(boolean enMapa) {
        Pais p = partida.actual();
        lateral.getChildren().setAll(FichaPais.etiqueta(t(enMapa ? "clasico.lateral.mapa" : "clasico.lateral.caida"), "subtitulo-rojo"));
        if (partida.ajustes().modo() == com.mapamundi.juego.Modo.DATOS) {
            lateral.getChildren().add(FichaPais.pista(p, partida.ajustes().modo(), false));
        } else if (partida.ajustes().mostrarDatos()) {
            ScrollPane datos = new ScrollPane(FichaPais.datos(p, true));
            datos.setFitToWidth(true);
            datos.setPrefViewportHeight(300);
            lateral.getChildren().add(datos);
        } else {
            lateral.getChildren().add(FichaPais.pista(p, partida.ajustes().modo(), false));
        }
    }

    /** El mensaje de cada intento (FrmGano, FrmPerdio, FrmEscogeMal… del original), encima de todo. */
    private void mostrarMensaje(PartidaClasica.Resultado r) {
        if (mensaje != null) getChildren().remove(mensaje);
        Pais p = partida.actual();
        VBox caja = new VBox(8);
        caja.getStyleClass().add("panel");
        caja.setAlignment(Pos.CENTER);
        caja.setPrefWidth(330);
        caja.setMaxSize(330, USE_PREF_SIZE);
        Label titulo = FichaPais.etiqueta((r.acierto() ? "😃 " : "😞 ") + r.motivo().titulo(), r.acierto() ? "acierto" : "fallo");
        titulo.setWrapText(true);
        caja.getChildren().addAll(titulo, FichaPais.etiqueta(p.nombre(), "titulo-mapa-mediano"));
        if (r.tieneDistancia()) caja.getChildren().add(FichaPais.etiqueta(t("resultado.distancia", Textos.entero(Math.round(r.distanciaKm()))), "negrita"));
        if (r.acierto()) caja.getChildren().add(FichaPais.etiqueta("+" + r.puntos(), "tutorial-puntos"));
        if (r.completado() != null && !practica) caja.getChildren().add(FichaPais.etiqueta(t("clasico.completado", r.completado().nombre()), "acierto"));
        if (!r.acierto()) {
            String quedan = partida.terminada() ? t("clasico.sinErrores") : t("clasico.quedanErrores", PartidaClasica.ERRORES_MAX - partida.errores());
            caja.getChildren().add(FichaPais.etiqueta(quedan, "negrita"));
        }
        boolean fin = partida.terminada();
        String clave = fin ? (practica ? "practica.terminar" : "resultado.verFinal") : r.acierto() ? "resultado.siguiente" : "clasico.otraVez";
        Button seguir = new Button(t(clave) + " (Enter)");
        seguir.getStyleClass().add("boton-principal");
        seguir.setFocusTraversable(false);
        seguir.setOnAction(e -> partida.siguiente());
        caja.getChildren().add(seguir);
        mensaje = new StackPane(caja);
        mensaje.setPickOnBounds(false);
        // En medio de la pantalla, tanto el acierto como el fallo.
        StackPane.setAlignment(caja, Pos.CENTER);
        getChildren().add(getChildren().indexOf(capaPausa), mensaje);
        TranslateTransition entra = new TranslateTransition(Duration.millis(220), caja);
        entra.setFromY(-30);
        entra.setToY(0);
        entra.play();
        if (!fin) {
            PauseTransition espera = new PauseTransition(Duration.millis(2600));
            espera.setOnFinished(e -> {
                if (partida.resultado() == r && partida.fase() == Fase.RESULTADO) partida.siguiente();
            });
            espera.play();
        }
    }

    // ------------------------------------------------------------------ práctica guiada

    /** A la derecha, un globo con el paso y qué hacer; encima de todo, una flecha que señala el continente correcto. */
    private void construirGuia(Runnable saltar) {
        guiaPaso = FichaPais.etiqueta("", "practica-paso");
        guiaTexto = FichaPais.etiqueta("", "practica-texto");
        guiaTexto.setWrapText(true);
        Button saltarBoton = new Button(t("practica.saltar"));
        saltarBoton.getStyleClass().add("boton-discreto");
        saltarBoton.setFocusTraversable(false);
        saltarBoton.setOnAction(e -> {
            desconectar();
            saltar.run();
        });
        guia = new VBox(4, guiaPaso, guiaTexto, saltarBoton);
        guia.getStyleClass().addAll("panel", "practica-globo");
        guia.setAlignment(Pos.CENTER);
        guia.setMaxSize(320, USE_PREF_SIZE);
        // En la columna de la derecha, debajo de la ficha: no tapa ni el país que cae ni el mapa.
        StackPane.setAlignment(guia, Pos.TOP_RIGHT);
        StackPane.setMargin(guia, new Insets(250, 16, 0, 0));

        mano = FichaPais.etiqueta("▼", "practica-mano");
        mano.setMouseTransparent(true);
        mano.setManaged(false);
        Pane capaMano = new Pane(mano);
        capaMano.setMouseTransparent(true);
        TranslateTransition rebote = new TranslateTransition(Duration.millis(450), mano);
        rebote.setByY(14);
        rebote.setAutoReverse(true);
        rebote.setCycleCount(TranslateTransition.INDEFINITE);
        rebote.play();
        // Debajo de los mensajes y de la pausa.
        getChildren().add(1, guia);
        getChildren().add(2, capaMano);
        mapa.guia(true);
    }

    private void actualizarGuia(Fase fase) {
        Pais p = partida.actual();
        guiaPaso.setText(t("practica.paso", partida.ubicados().size() + 1, partida.totalPaises()));
        boolean cae = fase == Fase.CAIDA;
        guia.setVisible(fase != Fase.RESULTADO);
        guiaTexto.setText(cae ? t("practica.caida", p.nombre(), p.continente().nombre()) : t("practica.mapa"));
        mano.setVisible(cae);
        if (cae) {
            // Sobre la tarjeta del continente correcto (cuando ya está colocada en pantalla).
            Button tarjeta = tarjetas.get(p.continente());
            javafx.geometry.Bounds b = tarjeta.localToScene(tarjeta.getBoundsInLocal());
            javafx.geometry.Point2D punto = mano.getParent().sceneToLocal(b.getCenterX(), b.getMinY());
            mano.resizeRelocate(punto.getX() - 28, punto.getY() - 62, 56, 56);
        }
    }

    // ------------------------------------------------------------------ teclado

    private void tecla(KeyEvent e) {
        KeyCode k = e.getCode();
        if (k == KeyCode.F5 || k == KeyCode.P || k == KeyCode.ESCAPE) {
            partida.alternarPausa();
            e.consume();
            return;
        }
        if (partida.enPausa()) return;
        switch (partida.fase()) {
            case CAIDA -> teclaCaida(e);
            case UBICACION -> teclaMapa(e);
            case RESULTADO -> {
                if (k == KeyCode.ENTER || k == KeyCode.SPACE) {
                    partida.siguiente();
                    e.consume();
                }
            }
            default -> { }
        }
    }

    private void teclaCaida(KeyEvent e) {
        KeyCode k = e.getCode();
        if (k == KeyCode.RIGHT || k == KeyCode.DOWN) seleccion = primeraElegible(seleccion + 1, 1);
        else if (k == KeyCode.LEFT || k == KeyCode.UP) seleccion = primeraElegible(seleccion - 1, -1);
        else if (k == KeyCode.ENTER || k == KeyCode.SPACE) partida.elegirContinente(Continente.values()[seleccion]);
        else if (k.isDigitKey() || k.isKeypadKey()) {
            int n = k.getName().charAt(k.getName().length() - 1) - '1';
            if (n < 0 || n >= Continente.values().length) return;
            partida.elegirContinente(Continente.values()[n]);
        } else return;
        e.consume();
        actualizarTarjetas();
    }

    /**
     * Las flechas cambian la dirección. Mientras una flecha está pulsada la pieza va algo más rápido que a la
     * deriva, y con Ctrl, muy rápido (como en el original).
     */
    private void teclaMapa(KeyEvent e) {
        Direccion d = direccionDe(e.getCode());
        if (d != null) {
            movimiento.setRate(e.isControlDown() ? (double) ritmo.movimiento() / PartidaClasica.Ritmo.MOVIMIENTO_RAPIDO
                    : VELOCIDAD_CON_FLECHA);
            partida.cambiarDireccion(d);
            e.consume();
        } else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
            partida.confirmar();
            e.consume();
        }
    }

    /** Al soltar la flecha (o Ctrl), la pieza sigue en esa dirección, pero otra vez despacio. */
    private void soltar(KeyEvent e) {
        if (e.getCode() == KeyCode.CONTROL || direccionDe(e.getCode()) != null) movimiento.setRate(VELOCIDAD_DERIVA);
    }

    private static Direccion direccionDe(KeyCode k) {
        return switch (k) {
            case UP -> Direccion.ARRIBA;
            case DOWN -> Direccion.ABAJO;
            case LEFT -> Direccion.IZQUIERDA;
            case RIGHT -> Direccion.DERECHA;
            default -> null;
        };
    }

    /** El siguiente continente que se puede elegir a partir de {@code desde}, avanzando en {@code paso}. */
    private int primeraElegible(int desde, int paso) {
        int n = Continente.values().length;
        for (int i = 0; i < n; i++) {
            int c = Math.floorMod(desde + (paso == 0 ? i : i * paso), n);
            if (partida.elegible(Continente.values()[c])) return c;
        }
        return Math.floorMod(desde, n);
    }

    /** Detiene los relojes y deja de escuchar el teclado (al salir de la pantalla). */
    void desconectar() {
        caida.stop();
        movimiento.stop();
        reloj.stop();
        if (getScene() != null) {
            getScene().removeEventFilter(KeyEvent.KEY_PRESSED, pulsar);
            getScene().removeEventFilter(KeyEvent.KEY_RELEASED, soltar);
        }
    }
}
