package com.mapamundi.ui;

import com.mapamundi.juego.Ajustes;
import com.mapamundi.juego.Modo;
import com.mapamundi.juego.Nivel;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Velocidad;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.mapa.Miniatura;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.mapamundi.i18n.Textos.t;

/**
 * Paso 1 de 2: se configura la partida (estilo, nivel o continentes, modo, reglas extra y ficha del país).
 * Con «Continuar» se pasa a la pantalla de inicio de partida ({@link PantallaInicio}).
 */
final class PantallaConfiguracion extends StackPane {

    private static final double ANCHO_MINI = 168, ALTO_MINI = 66;
    /** Ya no se elige: todas las partidas tienen 30 s por país y ±150 km de margen. */
    private static final Velocidad VELOCIDAD = Velocidad.NORMAL;

    private final List<Nivel> niveles;
    private final ToggleGroup grupoNivel = new ToggleGroup();
    private final ToggleGroup grupoEstilo = new ToggleGroup();
    private final ComboBox<Modo> modo = new ComboBox<>();
    private final CheckBox mostrarDatos = new CheckBox(t("menu.mostrarDatos"));
    private final ToggleButton contrarreloj = interruptor("◷  " + t("regla.contrarreloj"),
            t("regla.contrarreloj.ayuda", Partida.SEGUNDOS_CONTRARRELOJ, Partida.SEGUNDOS_POR_ACIERTO));
    private final ToggleButton frioCaliente = interruptor("♨  " + t("regla.frioCaliente"), t("regla.frioCaliente.ayuda"));
    private final ToggleButton piezaGirada = interruptor("⟳  " + t("regla.piezaGirada"), t("regla.piezaGirada.ayuda", Partida.PUNTOS_GIRO));
    private final Map<Continente, ToggleButton> casillas = new EnumMap<>(Continente.class);
    private final Label aviso = FichaPais.etiqueta(t("config.faltaContinente"), "aviso-config");
    private final Consumer<Ajustes> continuar;
    private final Contexto ctx;

    /**
     * @param continuar       recibe la configuración elegida y pasa a la pantalla de inicio de partida
     * @param refrescar       vuelve a construir la pantalla (tras cambiar el modo daltónico)
     * @param mostrarTutorial abrir la guía de bienvenida nada más mostrarse (al arrancar el programa)
     */
    PantallaConfiguracion(Contexto ctx, Ajustes inicial, Consumer<Ajustes> continuar, Runnable refrescar, boolean mostrarTutorial) {
        Atlas atlas = ctx.atlas();
        this.ctx = ctx;
        this.niveles = Nivel.progresion(atlas);
        this.continuar = continuar;

        Label logo = Logotipo.crear("Mapamundi");
        logo.setStyle("-fx-font-size: 40px;");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox cabecera = new HBox(22, logo, MenuComun.indicadorPaso(1, t("menu.configura")), hueco,
                MenuComun.botones(this, ctx, refrescar, this::verTutorial));
        cabecera.setAlignment(Pos.CENTER_LEFT);

        BooleanBinding clasico = Bindings.createBooleanBinding(
                () -> grupoEstilo.getSelectedToggle() != null && (Boolean) grupoEstilo.getSelectedToggle().getUserData(),
                grupoEstilo.selectedToggleProperty());
        VBox izquierda = panelIzquierdo(atlas, inicial, clasico);
        HBox.setHgrow(izquierda, Priority.ALWAYS);
        VBox derecha = panelDerecho(inicial, clasico);

        HBox columnas = new HBox(20, izquierda, derecha);
        VBox.setVgrow(columnas, Priority.ALWAYS);
        VBox todo = new VBox(14, cabecera, columnas);
        todo.setPadding(new Insets(14, 26, 18, 26));
        ScrollPane desplazable = new ScrollPane(todo);
        desplazable.setFitToWidth(true);
        desplazable.setFitToHeight(true);
        getChildren().add(desplazable);

        // Enter = Continuar, aunque el foco esté en una tarjeta (que si no se queda la tecla).
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !modo.isShowing() && getChildren().size() == 1) {
                continuar();
                e.consume();
            }
        });

        if (mostrarTutorial) Platform.runLater(this::verTutorial);
    }

    private void verTutorial() {
        getChildren().removeIf(n -> n.getStyleClass().contains("capa-dialogo"));
        Tutorial.mostrar(this, ctx.atlas(), () -> { });
    }

    // ------------------------------------------------------------------ izquierda: estilo y nivel

    private VBox panelIzquierdo(Atlas atlas, Ajustes inicial, BooleanBinding clasico) {
        // Estilo de juego: dos tarjetas grandes.
        HBox estilos = new HBox(14,
                tarjetaEstilo(t("estilo.moderno"), t("estilo.moderno.detalle"), "✦", false, !inicial.clasico()),
                tarjetaEstilo(t("clasico.nombre"), t("estilo.clasico.detalle"), "⌛", true, inicial.clasico()));
        grupoEstilo.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) antes.setSelected(true);
        });
        Label explicacionClasico = FichaPais.etiqueta(t("estilo.clasico.explicacion"), "texto-suave");
        explicacionClasico.setWrapText(true);
        mostrarSi(explicacionClasico, clasico);

        // Nivel: fichas con mini-mapa, en 4 columnas.
        GridPane fichas = new GridPane();
        fichas.setHgap(12);
        fichas.setVgap(12);
        for (int i = 0; i < niveles.size(); i++) {
            Nivel n = niveles.get(i);
            Set<Pais> deNivel = new HashSet<>(n.paises());
            ToggleButton ficha = fichaNivel(Miniatura.region(atlas, n.vista(), deNivel::contains, ANCHO_MINI, ALTO_MINI),
                    (i + 1) + ". " + n.nombre(), t("menu.paises", n.paises().size()), i);
            ficha.setSelected(i == inicial.nivelInicial());
            fichas.add(ficha, i % 4, i / 4);
        }
        ToggleButton personal = fichaNivel(Miniatura.region(atlas, Nivel.VISTA_MUNDO, p -> true, ANCHO_MINI, ALTO_MINI),
                t("menu.personalizado"), t("menu.personalizado.detalle"), -1);
        personal.setSelected(inicial.personalizado());
        fichas.add(personal, 3, 1);
        grupoNivel.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) antes.setSelected(true);
        });

        // Continentes: tarjetas con su mapa que se marcan y desmarcan (como los perfiles del original).
        HBox continentes = new HBox(10);
        for (Continente c : Continente.values()) {
            ToggleButton b = new ToggleButton();
            Label nombre = FichaPais.etiqueta(c.nombre(), "ficha-nivel-nombre");
            Label marca = FichaPais.etiqueta("✔", "marca-continente");
            marca.visibleProperty().bind(b.selectedProperty());
            StackPane mini = new StackPane(Miniatura.continente(atlas, c, 132, 84), marca);
            StackPane.setAlignment(marca, Pos.TOP_RIGHT);
            VBox contenido = new VBox(4, mini, nombre);
            contenido.setAlignment(Pos.CENTER);
            b.setGraphic(contenido);
            b.getStyleClass().setAll("ficha-nivel");
            b.setSelected(inicial.continentes().contains(c));
            b.selectedProperty().addListener(o -> aviso.setVisible(false));
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            Animaciones.alPasar(b, 1.05, 3);
            casillas.put(c, b);
            continentes.getChildren().add(b);
        }
        // En el clásico (y en "Personalizado") se eligen los continentes, como en el formulario del original.
        mostrarSi(continentes, personal.selectedProperty().or(clasico));
        mostrarSi(fichas, clasico.not());

        Node seccionNivel = MenuComun.seccion(t("menu.nivel"), t("menu.nivel.nota", Partida.PAISES_POR_NIVEL));
        Node seccionContinentes = MenuComun.seccion(t("menu.continentes"), t("config.continentes.nota"));
        mostrarSi(seccionNivel, clasico.not());
        mostrarSi(seccionContinentes, personal.selectedProperty().or(clasico));

        VBox v = new VBox(10, MenuComun.seccion(t("menu.estilo"), null), estilos, explicacionClasico,
                seccionNivel, fichas, seccionContinentes, continentes);
        v.getStyleClass().add("panel");
        return v;
    }

    private ToggleButton tarjetaEstilo(String nombre, String detalle, String icono, boolean esClasico, boolean elegida) {
        Label simbolo = FichaPais.etiqueta(icono, "estilo-icono");
        simbolo.setMinSize(46, 46);
        simbolo.setAlignment(Pos.CENTER);
        HBox contenido = new HBox(12, simbolo, MenuComun.dosLineas(nombre, detalle, "estilo-nombre", "texto-suave"));
        contenido.setAlignment(Pos.CENTER_LEFT);
        ToggleButton b = new ToggleButton();
        b.setGraphic(contenido);
        b.getStyleClass().setAll("tarjeta-estilo");
        b.setToggleGroup(grupoEstilo);
        b.setUserData(esClasico);
        b.setSelected(elegida);
        b.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(b, Priority.ALWAYS);
        Animaciones.alPasar(b, 1.03, 3);
        return b;
    }

    private ToggleButton fichaNivel(Node miniatura, String nombre, String detalle, int indice) {
        ToggleButton b = new ToggleButton();
        VBox contenido = new VBox(4, miniatura, MenuComun.dosLineas(nombre, detalle, "ficha-nivel-nombre", "ficha-nivel-detalle"));
        contenido.setAlignment(Pos.TOP_LEFT);
        b.setGraphic(contenido);
        b.getStyleClass().setAll("ficha-nivel");
        b.setToggleGroup(grupoNivel);
        b.setUserData(indice);
        b.setMaxWidth(Double.MAX_VALUE);
        Animaciones.alPasar(b, 1.05, 3);
        return b;
    }

    // ------------------------------------------------------------------ derecha: modo, reglas y continuar

    private VBox panelDerecho(Ajustes inicial, BooleanBinding clasico) {
        modo.getItems().setAll(Modo.values());
        modo.setValue(inicial.modo());
        modo.setMaxWidth(Double.MAX_VALUE);
        modo.setCellFactory(l -> celdaModo());
        modo.setButtonCell(celdaModo());
        Label ayudaModo = FichaPais.etiqueta("", "texto-suave");
        ayudaModo.setWrapText(true);
        ayudaModo.textProperty().bind(modo.valueProperty().map(Modo::ayuda));

        mostrarDatos.setSelected(inicial.mostrarDatos());
        contrarreloj.setSelected(inicial.contrarreloj());
        frioCaliente.setSelected(inicial.frioCaliente());
        piezaGirada.setSelected(inicial.piezaGirada());
        VBox reglas = new VBox(8, contrarreloj, frioCaliente, piezaGirada);
        for (ToggleButton b : List.of(contrarreloj, frioCaliente, piezaGirada)) b.setMaxWidth(Double.MAX_VALUE);
        Node seccionReglas = MenuComun.seccion(t("menu.reglas"), t("menu.reglas.nota"));
        mostrarSi(seccionReglas, clasico.not());
        mostrarSi(reglas, clasico.not());

        Region empuje = new Region();
        VBox.setVgrow(empuje, Priority.ALWAYS);
        aviso.setWrapText(true);
        aviso.setVisible(false);
        aviso.managedProperty().bind(aviso.visibleProperty());

        Button siguiente = new Button(t("config.continuar") + "  ›");
        siguiente.getStyleClass().addAll("boton-principal", "boton-jugar");
        siguiente.setDefaultButton(true);
        siguiente.setMaxWidth(Double.MAX_VALUE);
        siguiente.setOnAction(e -> continuar());
        Animaciones.alPasar(siguiente, 1.04, 3, MenuComun.latir(siguiente));

        VBox v = new VBox(10, MenuComun.seccion(t("menu.modo"), null), modo, ayudaModo,
                seccionReglas, reglas, mostrarDatos, empuje, aviso, siguiente);
        v.getStyleClass().add("panel");
        v.setPrefWidth(430);
        v.setMinWidth(380);
        return v;
    }

    private static ToggleButton interruptor(String texto, String ayuda) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().setAll("interruptor");
        Tooltip t = new Tooltip(ayuda);
        t.setShowDelay(Duration.millis(250));
        t.setWrapText(true);
        t.setMaxWidth(320);
        b.setTooltip(t);
        Animaciones.alPasar(b, 1.03, 2);
        return b;
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

    private static void mostrarSi(Node nodo, ObservableValue<Boolean> condicion) {
        nodo.visibleProperty().bind(condicion);
        nodo.managedProperty().bind(condicion);
    }

    // ------------------------------------------------------------------ continuar

    private void continuar() {
        if (getChildren().size() > 1) return; // hay una ventana abierta
        boolean clasico = grupoEstilo.getSelectedToggle() != null && (Boolean) grupoEstilo.getSelectedToggle().getUserData();
        int nivel = (Integer) grupoNivel.getSelectedToggle().getUserData();
        EnumSet<Continente> elegidos = EnumSet.noneOf(Continente.class);
        casillas.forEach((c, cb) -> {
            if (cb.isSelected()) elegidos.add(c);
        });
        // Con continentes a elegir hace falta al menos uno (como avisaba el original).
        if (elegidos.isEmpty() && (clasico || nivel < 0)) {
            aviso.setVisible(true);
            return;
        }
        // El nivel se guarda aunque el clásico no lo use, para que siga elegido al volver al estilo moderno.
        continuar.accept(new Ajustes(VELOCIDAD, modo.getValue(), nivel,
                elegidos.isEmpty() ? EnumSet.allOf(Continente.class) : elegidos,
                mostrarDatos.isSelected(), contrarreloj.isSelected(), frioCaliente.isSelected(), piezaGirada.isSelected(), clasico));
    }
}
