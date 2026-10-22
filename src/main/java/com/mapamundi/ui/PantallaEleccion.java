package com.mapamundi.ui;

import com.mapamundi.geo.Region;
import com.mapamundi.juego.Modo;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.mapa.MapaDecorativo;
import com.mapamundi.ui.mapa.Miniatura;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mapamundi.i18n.Textos.t;

/**
 * Las dos preguntas antes de jugar, cada una en su pantalla y con tarjetas grandes: con qué continente se
 * juega y qué reto (modo de juego) se quiere. Se elige con un clic, con ← → y Enter o con las teclas de número;
 * Esc vuelve atrás. A la derecha siempre está la pestaña «Aprende a jugar».
 */
final class PantallaEleccion extends StackPane {

    /** Una tarjeta: el dibujo, el nombre y una línea de detalle. */
    record Opcion(Node dibujo, String nombre, String detalle) {
    }

    private final List<Button> tarjetas = new ArrayList<>();
    private final Consumer<Integer> alElegir;
    private int marcada;
    private boolean elegida;

    /**
     * @param atras      vuelve a la pantalla anterior (null si no hay)
     * @param refrescar  rehace la pantalla (tras cambiar el modo daltónico en Ajustes)
     * @param aprender   abre el tutorial
     */
    private PantallaEleccion(Contexto ctx, String titulo, String ayuda, List<Opcion> opciones, int columnas, double ancho, int inicial,
                             Consumer<Integer> alElegir, Runnable atras, Runnable refrescar, Runnable aprender) {
        this.alElegir = alElegir;
        this.marcada = Math.max(0, Math.min(inicial, opciones.size() - 1));

        MapaDecorativo mapa = new MapaDecorativo(ctx.atlas());
        mapa.setOpacity(0.14);
        mapa.setMouseTransparent(true);

        // Arriba: el logotipo (y «Volver»), y a la derecha récords, música y ajustes.
        Label logo = Logotipo.crear("Mapamundi");
        logo.setStyle("-fx-font-size: 34px;");
        HBox izquierda = new HBox(18, logo);
        izquierda.setAlignment(Pos.CENTER_LEFT);
        if (atras != null) {
            Button volver = new Button("‹  " + t("eleccion.volver"));
            volver.getStyleClass().add("boton-fantasma");
            volver.setFocusTraversable(false);
            volver.setOnAction(e -> atras.run());
            izquierda.getChildren().add(volver);
        }
        BorderPane cabecera = new BorderPane();
        cabecera.setLeft(izquierda);
        cabecera.setRight(MenuComun.botones(this, ctx, refrescar, aprender, true));

        Label pregunta = FichaPais.etiqueta(titulo, "eleccion-titulo");
        pregunta.setWrapText(true);
        GridPane rejilla = new GridPane();
        rejilla.setHgap(18);
        rejilla.setVgap(18);
        rejilla.setAlignment(Pos.CENTER);
        for (int i = 0; i < opciones.size(); i++) {
            rejilla.add(tarjeta(opciones.get(i), i, ancho), i % columnas, i / columnas);
        }
        Label pie = FichaPais.etiqueta(ayuda, "idioma-ayuda");
        VBox centro = new VBox(22, pregunta, rejilla, pie);
        centro.setAlignment(Pos.CENTER);
        VBox.setVgrow(centro, Priority.ALWAYS);

        VBox todo = new VBox(10, cabecera, centro);
        todo.setPadding(new Insets(16, 90, 20, 26));

        // La pestaña «Aprende a jugar», pegada al borde derecho y bien a la vista.
        Label texto = FichaPais.etiqueta("🎓  " + t("eleccion.aprende"), "pestana-aprender-texto");
        texto.setRotate(-90);
        Button pestana = new Button(null, new Group(texto));
        pestana.getStyleClass().setAll("pestana-aprender");
        pestana.setFocusTraversable(false);
        pestana.setOnAction(e -> aprender.run());
        StackPane.setAlignment(pestana, Pos.CENTER_RIGHT);
        MenuComun.latir(pestana);

        getChildren().addAll(mapa, todo, pestana);
        getStyleClass().add("pantalla-idioma");

        setOnKeyPressed(e -> {
            if (getChildren().size() > 3) return; // hay una ventana encima (tutorial, ajustes…)
            KeyCode k = e.getCode();
            int n = tarjetas.size();
            if (k == KeyCode.RIGHT || k == KeyCode.TAB) marcar((marcada + 1) % n);
            else if (k == KeyCode.LEFT) marcar((marcada + n - 1) % n);
            else if (k == KeyCode.DOWN) marcar(Math.min(n - 1, marcada + columnas));
            else if (k == KeyCode.UP) marcar(Math.max(0, marcada - columnas));
            else if (k == KeyCode.ENTER || k == KeyCode.SPACE) elegir(marcada);
            else if (k == KeyCode.ESCAPE && atras != null) atras.run();
            else if (k.isDigitKey() && !k.getName().isEmpty()) {
                int numero = Character.getNumericValue(k.getName().charAt(k.getName().length() - 1));
                if (numero < 1 || numero > n) return;
                elegir(numero - 1);
            } else return;
            e.consume();
        });
        setFocusTraversable(true);
        Platform.runLater(this::requestFocus);
        marcar(marcada);

        FadeTransition entrada = new FadeTransition(Duration.millis(300), centro);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.play();
    }

    // ------------------------------------------------------------------ las dos pantallas

    /** «¿Con qué continente quieres jugar?»: todo el mundo y los cinco continentes. */
    static PantallaEleccion continentes(Contexto ctx, Set<Continente> inicial, Consumer<Set<Continente>> alElegir,
                                        Runnable refrescar, Runnable aprender) {
        Atlas atlas = ctx.atlas();
        List<Set<Continente>> zonas = new ArrayList<>();
        List<Opcion> opciones = new ArrayList<>();
        zonas.add(EnumSet.allOf(Continente.class));
        opciones.add(new Opcion(Miniatura.region(atlas, Region.deGrados(-170, 190, -56, 78), p -> true, 230, 130),
                t("eleccion.mundo"), t("menu.paises", atlas.paises().size())));
        for (Continente c : Continente.values()) {
            zonas.add(EnumSet.of(c));
            long n = atlas.paises().stream().filter(p -> p.continente() == c).count();
            opciones.add(new Opcion(Miniatura.continente(atlas, c, 230, 130), c.nombre(), t("menu.paises", n)));
        }
        return new PantallaEleccion(ctx, t("eleccion.continente.titulo"), t("eleccion.continente.ayuda"), opciones, 3, 250,
                Math.max(0, zonas.indexOf(inicial)), i -> alElegir.accept(zonas.get(i)), null, refrescar, aprender);
    }

    /** «¿Qué reto quieres en …?»: los cinco modos, de más fácil a más difícil, con un ejemplo de la pieza. */
    static PantallaEleccion retos(Contexto ctx, Set<Continente> zona, Modo inicial, Consumer<Modo> alElegir,
                                  Runnable atras, Runnable refrescar, Runnable aprender) {
        // El ejemplo es un país conocido del continente elegido (con todo el mundo, Perú).
        String iso = zona.size() != 1 ? "PE" : switch (zona.iterator().next()) {
            case AFRICA -> "EG";
            case AMERICA -> "PE";
            case EUROPA -> "FR";
            case ASIA -> "JP";
            case OCEANIA -> "AU";
        };
        Pais ejemplo = ctx.atlas().paises().stream().filter(p -> p.iso().equals(iso)).findFirst()
                .orElse(ctx.atlas().paises().getFirst());
        List<Opcion> opciones = new ArrayList<>();
        for (Modo m : Modo.values()) {
            String dificultad = "●".repeat(m.ordinal() + 1) + "○".repeat(4 - m.ordinal());
            opciones.add(new Opcion(muestra(ejemplo, m), m.nombre(),
                    dificultad + "   +" + PartidaClasica.puntosPorPais(m) + " " + t("eleccion.porPais")));
        }
        return new PantallaEleccion(ctx, t("eleccion.reto.titulo", zona(zona)), t("eleccion.reto.ayuda"), opciones, 5, 212,
                inicial.ordinal(), i -> alElegir.accept(Modo.values()[i]), atras, refrescar, aprender);
    }

    /** "Todo el mundo" o los nombres de los continentes elegidos. */
    static String zona(Set<Continente> continentes) {
        if (continentes.isEmpty() || continentes.size() == Continente.values().length) return t("eleccion.mundo");
        return continentes.stream().map(Continente::nombre).collect(Collectors.joining(", "));
    }

    /** Cómo se ve la pieza en cada reto: la silueta o un círculo de color, con el nombre, la capital o una interrogación. */
    private static Node muestra(Pais p, Modo m) {
        String rotulo = switch (m) {
            case DATOS -> "?";
            case MAPA_CAPITAL, CAPITAL -> p.capital();
            default -> p.nombre();
        };
        Node pieza = m.muestraForma() ? Miniatura.silueta(p, 120, 90) : Miniatura.figura(p, 40);
        VBox v = new VBox(4, FichaPais.etiqueta(rotulo, "rotulo-caida"), pieza);
        v.setAlignment(Pos.CENTER);
        v.setMinHeight(130);
        return v;
    }

    // ------------------------------------------------------------------ tarjetas

    private Button tarjeta(Opcion o, int i, double ancho) {
        Label nombre = FichaPais.etiqueta(o.nombre(), "eleccion-nombre");
        nombre.setWrapText(true);
        Label detalle = FichaPais.etiqueta(o.detalle(), "idioma-detalle");
        Label numero = FichaPais.etiqueta(String.valueOf(i + 1), "numero-tecla");
        VBox contenido = new VBox(8, o.dibujo(), nombre, detalle);
        contenido.setAlignment(Pos.CENTER);
        StackPane capa = new StackPane(contenido, numero);
        StackPane.setAlignment(numero, Pos.TOP_LEFT);

        Button b = new Button(null, capa);
        b.getStyleClass().setAll("tarjeta-idioma", "tarjeta-eleccion");
        b.setPrefWidth(ancho);
        b.setMinWidth(ancho);
        // Todas las tarjetas de una fila, del mismo alto.
        b.setMaxHeight(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.setOnAction(e -> elegir(i));
        b.setOnMouseEntered(e -> marcar(i));
        Animaciones.alPasar(b, 1.04, 3);
        tarjetas.add(b);
        return b;
    }

    private void marcar(int i) {
        marcada = i;
        for (int j = 0; j < tarjetas.size(); j++) {
            tarjetas.get(j).getStyleClass().remove("seleccionada");
            if (j == i) tarjetas.get(j).getStyleClass().add("seleccionada");
        }
    }

    private void elegir(int i) {
        if (elegida) return;
        elegida = true;
        marcar(i);
        ScaleTransition pulso = new ScaleTransition(Duration.millis(130), tarjetas.get(i));
        pulso.setToX(0.95);
        pulso.setToY(0.95);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(2);
        pulso.setOnFinished(e -> alElegir.accept(i));
        pulso.play();
    }
}
