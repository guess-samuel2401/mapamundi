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
import java.util.TreeSet;
import java.util.function.Consumer;

import static com.mapamundi.i18n.Textos.t;

/**
 * Las dos preguntas antes de jugar, cada una en su pantalla y con tarjetas grandes: con qué continentes se
 * juega y cómo (el reto, es decir, el modo de juego). A la derecha siempre está la pestaña «Aprende a jugar».
 *
 * <ul>
 *   <li><b>Continentes:</b> las tarjetas son casillas: un clic (o su número, o Espacio) marca o desmarca cada
 *       continente, y la última, «Todo el mundo», los marca o desmarca todos. «Continuar» (o Enter) pasa a los
 *       retos cuando hay al menos uno marcado.</li>
 *   <li><b>Retos:</b> se elige uno con un clic, con ← → y Enter o con su número, y empieza la partida.
 *       Esc vuelve a los continentes.</li>
 * </ul>
 */
final class PantallaEleccion extends StackPane {

    /** Una tarjeta: el dibujo, el nombre y una línea de detalle. */
    record Opcion(Node dibujo, String nombre, String detalle) {
    }

    private final List<Button> tarjetas = new ArrayList<>();
    private final List<Label> palomitas = new ArrayList<>();
    /** Una sola elección (retos); null si las tarjetas son casillas. */
    private final Consumer<Integer> alElegir;
    /** Casillas marcadas (continentes); la última tarjeta no se guarda aquí: marca o desmarca todas. */
    private final Set<Integer> marcadas = new TreeSet<>();
    private Button continuar;
    /** La tarjeta resaltada con el ratón o las flechas. */
    private int cursor;
    private boolean elegida;

    /**
     * @param alElegir    recibe la tarjeta elegida (una sola elección), o null para casillas
     * @param alConfirmar con casillas, recibe las marcadas al pulsar «Continuar»
     * @param atras       vuelve a la pantalla anterior (null si no hay)
     * @param refrescar   rehace la pantalla (tras cambiar el modo daltónico en Ajustes)
     * @param aprender    abre el tutorial
     */
    private PantallaEleccion(Contexto ctx, String titulo, String ayuda, List<Opcion> opciones, int columnas, double ancho,
                             int inicial, Set<Integer> marcadasIniciales, Consumer<Integer> alElegir,
                             Consumer<Set<Integer>> alConfirmar, Runnable atras, Runnable refrescar, Runnable aprender) {
        this.alElegir = alElegir;
        this.cursor = Math.max(0, Math.min(inicial, opciones.size() - 1));
        boolean casillas = alConfirmar != null;

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
            rejilla.add(tarjeta(opciones.get(i), i, ancho, casillas), i % columnas, i / columnas);
        }
        Label pie = FichaPais.etiqueta(ayuda, "idioma-ayuda");
        VBox centro = new VBox(casillas ? 16 : 22, pregunta, rejilla);
        if (casillas) {
            continuar = new Button(t("eleccion.continuar") + "  ›");
            continuar.getStyleClass().addAll("boton-principal", "boton-continuar-eleccion");
            continuar.setFocusTraversable(false);
            continuar.setOnAction(e -> {
                if (!marcadas.isEmpty()) alConfirmar.accept(Set.copyOf(marcadas));
            });
            centro.getChildren().add(continuar);
        }
        centro.getChildren().add(pie);
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
            if (k == KeyCode.RIGHT || k == KeyCode.TAB) resaltar((cursor + 1) % n);
            else if (k == KeyCode.LEFT) resaltar((cursor + n - 1) % n);
            else if (k == KeyCode.DOWN) resaltar(Math.min(n - 1, cursor + columnas));
            else if (k == KeyCode.UP) resaltar(Math.max(0, cursor - columnas));
            else if (k == KeyCode.SPACE) pulsar(cursor);
            else if (k == KeyCode.ENTER) {
                if (casillas) continuar.fire();
                else pulsar(cursor);
            } else if (k == KeyCode.ESCAPE && atras != null) atras.run();
            else if (k.isDigitKey() && !k.getName().isEmpty()) {
                int numero = Character.getNumericValue(k.getName().charAt(k.getName().length() - 1));
                if (numero < 1 || numero > n) return;
                resaltar(numero - 1);
                pulsar(numero - 1);
            } else return;
            e.consume();
        });
        setFocusTraversable(true);
        Platform.runLater(this::requestFocus);
        if (casillas) {
            marcadas.addAll(marcadasIniciales);
            pintarCasillas();
        }
        resaltar(cursor);

        FadeTransition entrada = new FadeTransition(Duration.millis(300), centro);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.play();
    }

    // ------------------------------------------------------------------ las dos pantallas

    /**
     * «¿Con qué continentes quieres jugar?»: los cinco continentes como casillas y, al final, «Todo el mundo»,
     * que los marca todos. Vienen marcados los de la última partida.
     */
    static PantallaEleccion continentes(Contexto ctx, Set<Continente> inicial, Consumer<Set<Continente>> alElegir,
                                        Runnable refrescar, Runnable aprender) {
        Atlas atlas = ctx.atlas();
        List<Opcion> opciones = new ArrayList<>();
        Set<Integer> marcadas = new TreeSet<>();
        for (Continente c : Continente.values()) {
            long n = atlas.paises().stream().filter(p -> p.continente() == c).count();
            opciones.add(new Opcion(Miniatura.continente(atlas, c, 230, 112), c.nombre(), t("menu.paises", n)));
            if (inicial.contains(c)) marcadas.add(c.ordinal());
        }
        opciones.add(new Opcion(Miniatura.region(atlas, Region.deGrados(-170, 190, -56, 78), p -> true, 230, 112),
                t("eleccion.mundo"), t("menu.paises", atlas.paises().size())));
        return new PantallaEleccion(ctx, t("eleccion.continente.titulo"), t("eleccion.continente.ayuda"), opciones, 3, 250,
                0, marcadas, null, elegidas -> {
                    Set<Continente> zona = EnumSet.noneOf(Continente.class);
                    elegidas.forEach(i -> zona.add(Continente.values()[i]));
                    alElegir.accept(zona);
                }, null, refrescar, aprender);
    }

    /** «¿Cómo te gustaría jugar en …?»: los cinco modos, de más fácil a más difícil, con un ejemplo de la pieza. */
    static PantallaEleccion retos(Contexto ctx, Set<Continente> zona, Modo inicial, Consumer<Modo> alElegir,
                                  Runnable atras, Runnable refrescar, Runnable aprender) {
        boolean mundo = zona.isEmpty() || zona.size() == Continente.values().length;
        // El ejemplo es un país conocido del (primer) continente elegido; con todo el mundo, Perú.
        String iso = mundo ? "PE" : switch (zona.iterator().next()) {
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
        String titulo = mundo ? t("eleccion.reto.tituloMundo") : t("eleccion.reto.titulo", zona(zona));
        return new PantallaEleccion(ctx, titulo, t("eleccion.reto.ayuda"), opciones, 5, 212,
                inicial.ordinal(), Set.of(), i -> alElegir.accept(Modo.values()[i]), null, atras, refrescar, aprender);
    }

    /** "Todo el mundo" o los continentes elegidos: "Europa", "África y Asia", "América, Europa y Asia". */
    static String zona(Set<Continente> continentes) {
        if (continentes.isEmpty() || continentes.size() == Continente.values().length) return t("eleccion.mundo");
        List<String> nombres = continentes.stream().map(Continente::nombre).toList();
        if (nombres.size() == 1) return nombres.getFirst();
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " " + t("eleccion.y") + " " + nombres.getLast();
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

    private Button tarjeta(Opcion o, int i, double ancho, boolean casilla) {
        Label nombre = FichaPais.etiqueta(o.nombre(), "eleccion-nombre");
        nombre.setWrapText(true);
        Label detalle = FichaPais.etiqueta(o.detalle(), "idioma-detalle");
        Label numero = FichaPais.etiqueta(String.valueOf(i + 1), "numero-tecla");
        VBox contenido = new VBox(8, o.dibujo(), nombre, detalle);
        contenido.setAlignment(Pos.CENTER);
        StackPane capa = new StackPane(contenido, numero);
        StackPane.setAlignment(numero, Pos.TOP_LEFT);
        if (casilla) {
            // La casilla: vacía o con su ✓, arriba a la derecha.
            Label palomita = FichaPais.etiqueta("✓", "casilla-eleccion");
            palomita.setMinSize(34, 34);
            palomita.setMaxSize(34, 34);
            palomita.setAlignment(Pos.CENTER);
            StackPane.setAlignment(palomita, Pos.TOP_RIGHT);
            capa.getChildren().add(palomita);
            palomitas.add(palomita);
        }

        Button b = new Button(null, capa);
        b.getStyleClass().setAll("tarjeta-idioma", "tarjeta-eleccion");
        b.setPrefWidth(ancho);
        b.setMinWidth(ancho);
        // Todas las tarjetas de una fila, del mismo alto.
        b.setMaxHeight(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.setOnAction(e -> pulsar(i));
        b.setOnMouseEntered(e -> resaltar(i));
        Animaciones.alPasar(b, 1.04, 3);
        tarjetas.add(b);
        return b;
    }

    /** El ratón o las flechas: con casillas, un borde; con una sola elección, la tarjeta marcada. */
    private void resaltar(int i) {
        cursor = i;
        String clase = alElegir == null ? "enfocada" : "seleccionada";
        for (int j = 0; j < tarjetas.size(); j++) {
            tarjetas.get(j).getStyleClass().remove(clase);
            if (j == i) tarjetas.get(j).getStyleClass().add(clase);
        }
    }

    /** Clic, número o Espacio sobre una tarjeta: la elige o marca/desmarca su casilla. */
    private void pulsar(int i) {
        if (alElegir == null) alternar(i);
        else elegir(i);
    }

    private void alternar(int i) {
        int continentes = tarjetas.size() - 1;
        if (i == continentes) {
            // «Todo el mundo»: si ya estaban todos, se desmarcan; si no, se marcan todos.
            if (marcadas.size() == continentes) marcadas.clear();
            else for (int j = 0; j < continentes; j++) marcadas.add(j);
        } else if (!marcadas.remove(i)) {
            marcadas.add(i);
        }
        pintarCasillas();
        ScaleTransition pulso = new ScaleTransition(Duration.millis(90), tarjetas.get(i));
        pulso.setToX(0.97);
        pulso.setToY(0.97);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(2);
        pulso.play();
    }

    private void pintarCasillas() {
        int continentes = tarjetas.size() - 1;
        for (int j = 0; j < tarjetas.size(); j++) {
            boolean marcada = j == continentes ? marcadas.size() == continentes : marcadas.contains(j);
            tarjetas.get(j).getStyleClass().remove("seleccionada");
            if (marcada) tarjetas.get(j).getStyleClass().add("seleccionada");
            palomitas.get(j).getStyleClass().remove("marcada");
            if (marcada) palomitas.get(j).getStyleClass().add("marcada");
        }
        continuar.setDisable(marcadas.isEmpty());
    }

    private void elegir(int i) {
        if (elegida) return;
        elegida = true;
        resaltar(i);
        ScaleTransition pulso = new ScaleTransition(Duration.millis(130), tarjetas.get(i));
        pulso.setToX(0.95);
        pulso.setToY(0.95);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(2);
        pulso.setOnFinished(e -> alElegir.accept(i));
        pulso.play();
    }
}
