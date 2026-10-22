package com.mapamundi.ui;

import com.mapamundi.i18n.Textos;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Puntajes;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

import static com.mapamundi.i18n.Textos.t;

/**
 * Lo que comparten la pantalla de configuración y la de inicio de partida: los botones de Récords, Logros,
 * Cómo se juega y Ajustes (que abren ventanas superpuestas), el récord a batir y algunos rótulos.
 */
final class MenuComun {

    private MenuComun() {
    }

    /**
     * @param contenedor  donde se abren las ventanas superpuestas
     * @param refrescar   vuelve a construir la pantalla (tras el modo daltónico, para redibujar los mapas)
     * @param verTutorial abre la guía de bienvenida
     */
    static HBox botones(StackPane contenedor, Contexto ctx, Runnable refrescar, Runnable verTutorial) {
        Button records = new Button("🏆  " + t("menu.records"));
        records.getStyleClass().add("boton-fantasma");
        records.setOnAction(e -> Dialogo.mostrar(contenedor, t("menu.records.titulo"), tablaPuntajes(ctx.puntajes())));
        Animaciones.alPasar(records, 1.08, 3);
        Button logros = new Button("★  " + t("menu.logros"));
        logros.getStyleClass().add("boton-fantasma");
        logros.setOnAction(e -> Dialogo.mostrar(contenedor, t("menu.logros.titulo"), new PanelLogros(ctx.logros()), 720, null));
        Animaciones.alPasar(logros, 1.08, 3);
        Button ayuda = new Button("?");
        ayuda.getStyleClass().addAll("boton-fantasma", "boton-icono");
        ayuda.setTooltip(new Tooltip(t("menu.comoSeJuega")));
        ayuda.setOnAction(e -> Dialogo.mostrar(contenedor, t("menu.comoSeJuega"), comoSeJuega(verTutorial)));
        Animaciones.alPasar(ayuda, 1.1, 3);
        Button ajustes = new Button("⚙");
        ajustes.getStyleClass().addAll("boton-fantasma", "boton-icono");
        ajustes.setTooltip(new Tooltip(t("menu.ajustes.ayuda")));
        boolean[] cambioAspecto = {false};
        ajustes.setOnAction(e -> Dialogo.mostrar(contenedor, t("ajustes.titulo"),
                new PanelAjustes(ctx, () -> cambioAspecto[0] = true, verTutorial), 520,
                () -> {
                    // Los mapas están dibujados de antemano: se rehace la pantalla para el modo daltónico.
                    if (cambioAspecto[0]) refrescar.run();
                }));
        Animaciones.alPasar(ajustes, 1.1, 3);
        // Sin foco de teclado: si no, el primero sale resaltado nada más abrir la pantalla.
        for (Button b : new Button[]{records, logros, ayuda, ajustes}) b.setFocusTraversable(false);
        HBox h = new HBox(10, records, logros, ayuda, ajustes);
        h.setAlignment(Pos.CENTER_RIGHT);
        return h;
    }

    /** El récord a batir (o una invitación a ser el primero). */
    static Node recordActual(Puntajes puntajes) {
        List<Puntajes.Entrada> lista = puntajes.leer();
        Label trofeo = FichaPais.etiqueta("🏆", "trofeo");
        Node texto = lista.isEmpty()
                ? dosLineas(t("menu.sinRecords"), t("menu.sinRecords.detalle"), "negrita", "texto-suave")
                : dosLineas(t("menu.recordPuntos", Textos.entero(lista.getFirst().puntaje())),
                t("menu.recordDe", lista.getFirst().nombre(), lista.getFirst().nivel()), "record-puntos", "texto-suave");
        HBox h = new HBox(12, trofeo, texto);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("caja-record");
        return h;
    }

    /** Un latido suave en el botón principal para que sea lo primero que se mira. */
    static ScaleTransition latir(Node nodo) {
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

    static VBox dosLineas(String arriba, String abajo, String claseArriba, String claseAbajo) {
        VBox v = new VBox(0, FichaPais.etiqueta(arriba, claseArriba), FichaPais.etiqueta(abajo, claseAbajo));
        v.setAlignment(Pos.CENTER_LEFT);
        return v;
    }

    static Node seccion(String nombre, String nota) {
        HBox h = new HBox(8, FichaPais.etiqueta(nombre.toUpperCase(Textos.idioma().locale()), "titulo-seccion"));
        h.setAlignment(Pos.BASELINE_LEFT);
        if (nota != null) h.getChildren().add(FichaPais.etiqueta(nota, "texto-suave"));
        h.setPadding(new Insets(2, 0, 0, 0));
        return h;
    }

    /** "Paso 1 de 2 · Configura tu partida": en qué punto del camino al juego se está. */
    static HBox indicadorPaso(int paso, String titulo) {
        Label numero = FichaPais.etiqueta(t("pasos.paso", paso, 2), "paso-indicador");
        Label texto = FichaPais.etiqueta(titulo, "titulo-paso");
        HBox h = new HBox(12, numero, texto);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Node tablaPuntajes(Puntajes puntajes) {
        TablaPuntajes tabla = new TablaPuntajes(puntajes.leer(), null);
        tabla.getStyleClass().remove("panel");
        tabla.getChildren().removeFirst(); // el título ya lo pone la ventana
        return tabla;
    }

    private static Node comoSeJuega(Runnable verTutorial) {
        VBox pasos = new VBox(12,
                paso("1", t("ayuda.1.titulo"), t("ayuda.1.texto")),
                paso("2", t("ayuda.2.titulo"), t("ayuda.2.texto")),
                paso("3", t("ayuda.3.titulo"), t("ayuda.3.texto")),
                paso("4", t("ayuda.4.titulo"), t("ayuda.4.texto")),
                paso("5", t("ayuda.5.titulo"), t("ayuda.5.texto", Partida.SEGUNDOS_CONTRARRELOJ)),
                paso("6", t("ayuda.6.titulo"), t("ayuda.6.texto")),
                paso("7", t("ayuda.7.titulo"), t("ayuda.7.texto")));
        Button tutorial = new Button("🎓  " + t("ayuda.verTutorial"));
        tutorial.setOnAction(e -> verTutorial.run());
        Label creditos = FichaPais.etiqueta(t("ayuda.creditos"), "texto-suave");
        creditos.setWrapText(true);
        pasos.getChildren().addAll(tutorial, creditos);
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
}
