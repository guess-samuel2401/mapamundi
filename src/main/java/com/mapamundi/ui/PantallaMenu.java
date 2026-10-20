package com.mapamundi.ui;

import com.mapamundi.juego.Ajustes;
import com.mapamundi.juego.Modo;
import com.mapamundi.juego.Nivel;
import com.mapamundi.juego.Puntajes;
import com.mapamundi.juego.Velocidad;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.mapa.MapaDecorativo;
import com.mapamundi.ui.mapa.Miniatura;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.Locale;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Portada del juego: a la izquierda el mapa del mundo con el botón Jugar; a la derecha la configuración
 * de la partida (nivel, velocidad, modo). Récords y "Cómo se juega" se abren en ventanas superpuestas.
 */
final class PantallaMenu extends StackPane {

    private static final double ANCHO_CONFIG = 500;
    private static final double ANCHO_MINI = 70, ALTO_MINI = 46;

    private final List<Nivel> niveles;
    private final ToggleGroup grupoNivel = new ToggleGroup();
    private final ToggleGroup grupoVelocidad = new ToggleGroup();
    private final ComboBox<Modo> modo = new ComboBox<>();
    private final CheckBox mostrarDatos = new CheckBox("Mostrar los datos del país después de cada ronda");
    private final Map<Continente, CheckBox> casillas = new EnumMap<>(Continente.class);
    private final Label resumen = FichaPais.etiqueta("", "resumen-partida");
    private final Consumer<Ajustes> jugar;
    private final Puntajes puntajes;

    PantallaMenu(Atlas atlas, Ajustes inicial, Puntajes puntajes, Consumer<Ajustes> jugar) {
        this.niveles = Nivel.progresion(atlas);
        this.jugar = jugar;
        this.puntajes = puntajes;

        VBox portada = portada(atlas);
        HBox.setHgrow(portada, Priority.ALWAYS);
        VBox configuracion = configuracion(atlas, inicial);

        HBox columnas = new HBox(24, portada, configuracion);
        columnas.setPadding(new Insets(16, 26, 18, 26));
        ScrollPane desplazable = new ScrollPane(columnas);
        desplazable.setFitToWidth(true);
        desplazable.setFitToHeight(true);
        getChildren().add(desplazable);

        grupoNivel.selectedToggleProperty().addListener(o -> actualizarResumen());
        grupoVelocidad.selectedToggleProperty().addListener(o -> actualizarResumen());
        modo.valueProperty().addListener(o -> actualizarResumen());
        actualizarResumen();
    }

    // ------------------------------------------------------------------ portada

    private VBox portada(Atlas atlas) {
        Label titulo = Logotipo.crear("Mapamundi");
        Label edicion = FichaPais.etiqueta("2.0", "insignia");
        HBox cabecera = new HBox(10, titulo, edicion);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        Label lema = FichaPais.etiqueta("Adivina el continente y coloca cada país en su lugar antes de que se acabe el tiempo.", "lema");
        lema.setWrapText(true);

        MapaDecorativo mapa = new MapaDecorativo(atlas);
        StackPane marco = new StackPane(mapa);
        marco.getStyleClass().add("marco-mapa");
        VBox.setVgrow(marco, Priority.ALWAYS);
        marco.setMinHeight(160);

        Button botonJugar = new Button("▶  JUGAR");
        botonJugar.getStyleClass().addAll("boton-principal", "boton-jugar");
        botonJugar.setDefaultButton(true);
        botonJugar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(botonJugar, Priority.ALWAYS);
        botonJugar.setOnAction(e -> empezar());
        Animaciones.alPasar(botonJugar, 1.06, 3, latir(botonJugar));

        Button records = new Button("🏆  Récords");
        records.getStyleClass().add("boton-fantasma");
        records.setOnAction(e -> mostrarDialogo("Puntuaciones más altas", tablaPuntajes()));
        Animaciones.alPasar(records, 1.08, 3);
        Button ayuda = new Button("?  Cómo se juega");
        ayuda.getStyleClass().add("boton-fantasma");
        ayuda.setOnAction(e -> mostrarDialogo("Cómo se juega", comoSeJuega()));
        Animaciones.alPasar(ayuda, 1.08, 3);

        HBox acciones = new HBox(12, botonJugar, records, ayuda);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox v = new VBox(12, cabecera, lema, marco, acciones, resumen);
        v.setMinWidth(420);
        return v;
    }

    /** Un latido suave en el botón Jugar para que sea lo primero que se mira. */
    private static ScaleTransition latir(Node nodo) {
        ScaleTransition t = new ScaleTransition(Duration.millis(900), nodo);
        t.setFromX(1);
        t.setFromY(1);
        t.setToX(1.025);
        t.setToY(1.025);
        t.setAutoReverse(true);
        t.setCycleCount(ScaleTransition.INDEFINITE);
        t.play();
        return t;
    }

    // ------------------------------------------------------------------ configuración

    private VBox configuracion(Atlas atlas, Ajustes inicial) {
        Label titulo = FichaPais.etiqueta("Configura tu partida", "titulo-config");

        // Nivel: fichas con mini-mapa
        GridPane fichas = new GridPane();
        fichas.setHgap(10);
        fichas.setVgap(6);
        for (int i = 0; i < niveles.size(); i++) {
            Nivel n = niveles.get(i);
            Set<Pais> deNivel = new HashSet<>(n.paises());
            ToggleButton ficha = fichaNivel(Miniatura.region(atlas, n.vista(), deNivel::contains, ANCHO_MINI, ALTO_MINI),
                    (i + 1) + ". " + n.nombre(), n.paises().size() + " países", i);
            ficha.setSelected(i == inicial.nivelInicial());
            fichas.add(ficha, i % 2, i / 2);
        }
        ToggleButton personal = fichaNivel(Miniatura.region(atlas, Nivel.VISTA_MUNDO, p -> true, ANCHO_MINI, ALTO_MINI),
                "Personalizado", "Elige continentes", -1);
        personal.setSelected(inicial.personalizado());
        fichas.add(personal, 1, 3);
        // Siempre hay un nivel elegido.
        grupoNivel.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) antes.setSelected(true);
        });

        FlowPane continentes = new FlowPane(12, 6);
        continentes.getStyleClass().add("caja-continentes");
        for (Continente c : Continente.values()) {
            CheckBox cb = new CheckBox(c.nombre());
            cb.setSelected(inicial.continentes().contains(c));
            casillas.put(c, cb);
            continentes.getChildren().add(cb);
        }
        continentes.visibleProperty().bind(personal.selectedProperty());
        continentes.managedProperty().bind(personal.selectedProperty());

        // Velocidad: control segmentado
        HBox segmentos = new HBox();
        Velocidad[] velocidades = Velocidad.values();
        for (int i = 0; i < velocidades.length; i++) {
            Velocidad v = velocidades[i];
            ToggleButton b = new ToggleButton();
            b.setGraphic(dosLineas(v.nombre(), v.segundos() + " s · ±" + v.toleranciaKm() + " km", "segmento-titulo", "segmento-detalle"));
            b.getStyleClass().setAll("segmento", i == 0 ? "segmento-izq" : i == velocidades.length - 1 ? "segmento-der" : "segmento-centro");
            b.setToggleGroup(grupoVelocidad);
            b.setUserData(v);
            b.setSelected(v == inicial.velocidad());
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            // Los segmentos están pegados entre sí: crecen poco para no invadir al vecino.
            Animaciones.alPasar(b, 1.04, 2);
            segmentos.getChildren().add(b);
        }
        grupoVelocidad.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) antes.setSelected(true);
        });

        // Modo: lista desplegable con una línea de ayuda
        modo.getItems().setAll(Modo.values());
        modo.setValue(inicial.modo());
        modo.setMaxWidth(Double.MAX_VALUE);
        modo.setCellFactory(l -> celdaModo());
        modo.setButtonCell(celdaModo());
        Label ayudaModo = FichaPais.etiqueta("", "texto-suave");
        ayudaModo.textProperty().bind(modo.valueProperty().map(Modo::ayuda));

        mostrarDatos.setSelected(inicial.mostrarDatos());

        // El récord al pie, como reto antes de empezar. Con las casillas de continentes a la vista no cabe en 768 px.
        Region empuje = new Region();
        VBox.setVgrow(empuje, Priority.ALWAYS);
        Node record = recordActual();
        record.visibleProperty().bind(personal.selectedProperty().not());
        record.managedProperty().bind(personal.selectedProperty().not());

        VBox v = new VBox(7,
                titulo,
                seccion("Nivel", "Cada 6 países bien ubicados subes al siguiente."), fichas, continentes,
                seccion("Velocidad", null), segmentos,
                seccion("Modo de juego", null), modo, ayudaModo,
                mostrarDatos, empuje, record);
        v.getStyleClass().add("panel");
        v.setPrefWidth(ANCHO_CONFIG);
        v.setMinWidth(ANCHO_CONFIG);
        v.setMaxWidth(ANCHO_CONFIG);
        return v;
    }

    private Node recordActual() {
        List<Puntajes.Entrada> lista = puntajes.leer();
        Label trofeo = FichaPais.etiqueta("🏆", "trofeo");
        Node texto = lista.isEmpty()
                ? dosLineas("Aún no hay récords", "¡Juega una partida y sé el primero de la tabla!", "negrita", "texto-suave")
                : dosLineas(NumberFormat.getIntegerInstance(Locale.of("es")).format(lista.getFirst().puntaje()) + " puntos",
                "Récord de " + lista.getFirst().nombre() + " · " + lista.getFirst().nivel(), "record-puntos", "texto-suave");
        HBox h = new HBox(12, trofeo, texto);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("caja-record");
        return h;
    }

    private ToggleButton fichaNivel(Node miniatura, String nombre, String detalle, int indice) {
        ToggleButton b = new ToggleButton();
        HBox contenido = new HBox(10, miniatura, dosLineas(nombre, detalle, "ficha-nivel-nombre", "ficha-nivel-detalle"));
        contenido.setAlignment(Pos.CENTER_LEFT);
        b.setGraphic(contenido);
        b.getStyleClass().setAll("ficha-nivel");
        b.setToggleGroup(grupoNivel);
        b.setUserData(indice);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefWidth((ANCHO_CONFIG - 40) / 2);
        Animaciones.alPasar(b, 1.05, 3);
        return b;
    }

    private static VBox dosLineas(String arriba, String abajo, String claseArriba, String claseAbajo) {
        VBox v = new VBox(0, FichaPais.etiqueta(arriba, claseArriba), FichaPais.etiqueta(abajo, claseAbajo));
        v.setAlignment(Pos.CENTER_LEFT);
        return v;
    }

    private static Node seccion(String nombre, String nota) {
        HBox h = new HBox(8, FichaPais.etiqueta(nombre.toUpperCase(), "titulo-seccion"));
        h.setAlignment(Pos.BASELINE_LEFT);
        if (nota != null) h.getChildren().add(FichaPais.etiqueta(nota, "texto-suave"));
        h.setPadding(new Insets(2, 0, 0, 0));
        return h;
    }

    private static ListCell<Modo> celdaModo() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Modo m, boolean vacio) {
                super.updateItem(m, vacio);
                setText(vacio || m == null ? null : m.nombre());
            }
        };
    }

    // ------------------------------------------------------------------ acciones

    private void actualizarResumen() {
        if (grupoNivel.getSelectedToggle() == null || grupoVelocidad.getSelectedToggle() == null || modo.getValue() == null) return;
        int i = (Integer) grupoNivel.getSelectedToggle().getUserData();
        Velocidad v = (Velocidad) grupoVelocidad.getSelectedToggle().getUserData();
        String nivel = i < 0 ? "Continentes a elegir" : "Nivel " + (i + 1) + " · " + niveles.get(i).nombre();
        resumen.setText(nivel + "   ·   " + v.nombre() + " (" + v.segundos() + " s)   ·   " + modo.getValue().nombre());
    }

    private void empezar() {
        if (getChildren().size() > 1) return; // hay una ventana abierta
        EnumSet<Continente> elegidos = EnumSet.noneOf(Continente.class);
        casillas.forEach((c, cb) -> {
            if (cb.isSelected()) elegidos.add(c);
        });
        jugar.accept(new Ajustes(
                (Velocidad) grupoVelocidad.getSelectedToggle().getUserData(),
                modo.getValue(),
                (Integer) grupoNivel.getSelectedToggle().getUserData(),
                elegidos.isEmpty() ? EnumSet.allOf(Continente.class) : elegidos,
                mostrarDatos.isSelected()));
    }

    // ------------------------------------------------------------------ ventanas superpuestas

    private Node tablaPuntajes() {
        TablaPuntajes tabla = new TablaPuntajes(puntajes.leer(), null);
        tabla.getStyleClass().remove("panel");
        tabla.getChildren().removeFirst(); // el título ya lo pone la ventana
        return tabla;
    }

    private static Node comoSeJuega() {
        VBox pasos = new VBox(12,
                paso("1", "Elige el continente", "Aparece un país. Elige su continente con las flechas + Enter, las teclas 1–5 o un clic. Tienes 5 intentos: +100 puntos."),
                paso("2", "Coloca el país", "Arrástralo con el ratón (o usa las flechas; con Shift va más rápido) y pulsa Enter o Confirmar. La rueda del ratón hace zoom."),
                paso("3", "Suma puntos", "Si queda dentro del margen (en km reales): +500 y un bonus por los segundos que sobran. Si fallas o se acaba el tiempo, pierdes un corazón ♥ (tienes 5)."),
                paso("4", "Pausa", "F5, P o Esc detienen el cronómetro."));
        Label creditos = FichaPais.etiqueta("Fronteras: Natural Earth (dominio público). Datos: Mapamundi original (2003) y Natural Earth.", "texto-suave");
        creditos.setWrapText(true);
        pasos.getChildren().add(creditos);
        return pasos;
    }

    private static Node paso(String numero, String titulo, String texto) {
        Label circulo = FichaPais.etiqueta(numero, "numero-paso");
        circulo.setMinSize(30, 30);
        circulo.setAlignment(Pos.CENTER);
        Label detalle = FichaPais.etiqueta(texto, "");
        detalle.setWrapText(true);
        VBox v = new VBox(2, FichaPais.etiqueta(titulo, "negrita"), detalle);
        HBox.setHgrow(v, Priority.ALWAYS);
        HBox h = new HBox(12, circulo, v);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    private void mostrarDialogo(String titulo, Node contenido) {
        Button cerrar = new Button("Cerrar");
        cerrar.getStyleClass().add("boton-principal");
        cerrar.setCancelButton(true);
        HBox pie = new HBox(cerrar);
        pie.setAlignment(Pos.CENTER_RIGHT);
        VBox caja = new VBox(14, FichaPais.etiqueta(titulo, "titulo-config"), contenido, pie);
        caja.getStyleClass().add("panel");
        caja.setMaxSize(620, USE_PREF_SIZE);
        caja.setOnMouseClicked(e -> e.consume());

        StackPane capa = new StackPane(caja);
        capa.getStyleClass().add("capa-dialogo");
        Runnable quitar = () -> getChildren().remove(capa);
        cerrar.setOnAction(e -> quitar.run());
        capa.setOnMouseClicked(e -> quitar.run());
        capa.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) quitar.run();
        });
        getChildren().add(capa);
        FadeTransition entrada = new FadeTransition(Duration.millis(160), capa);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.play();
        cerrar.requestFocus();
    }
}
