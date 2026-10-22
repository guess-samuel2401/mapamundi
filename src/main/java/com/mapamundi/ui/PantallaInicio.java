package com.mapamundi.ui;

import com.mapamundi.juego.Ajustes;
import com.mapamundi.juego.Nivel;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.mapa.MapaDecorativo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.mapamundi.i18n.Textos.t;

/**
 * Paso 2 de 2: la pantalla de inicio de partida. Muestra lo que se eligió en la configuración (y en el mapa,
 * en color, los países que se van a jugar), el récord a batir y el botón Jugar. Enter juega; Esc vuelve.
 */
final class PantallaInicio extends StackPane {

    /**
     * @param jugar     empieza la partida con estos ajustes
     * @param volver    vuelve a la configuración
     * @param refrescar vuelve a construir la pantalla (tras cambiar el modo daltónico)
     */
    PantallaInicio(Contexto ctx, Ajustes ajustes, Consumer<Ajustes> jugar, Runnable volver, Runnable refrescar) {
        Atlas atlas = ctx.atlas();
        Runnable verTutorial = () -> Tutorial.mostrar(this, atlas, () -> { });

        Label logo = Logotipo.crear("Mapamundi");
        logo.setStyle("-fx-font-size: 40px;");
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        HBox cabecera = new HBox(22, logo, MenuComun.indicadorPaso(2, t("inicio.titulo")), hueco,
                MenuComun.botones(this, ctx, refrescar, verTutorial));
        cabecera.setAlignment(Pos.CENTER_LEFT);

        // Izquierda: el lema y el mapa con los países de la partida en color.
        Label lema = FichaPais.etiqueta(t("menu.lema"), "lema");
        lema.setWrapText(true);
        Set<Pais> enJuego = new HashSet<>(atlas.paises().stream().filter(paisesEnJuego(atlas, ajustes)).toList());
        StackPane marco = new StackPane(new MapaDecorativo(atlas, enJuego::contains));
        marco.getStyleClass().add("marco-mapa");
        VBox.setVgrow(marco, Priority.ALWAYS);
        marco.setMinHeight(200);
        Label leyenda = FichaPais.etiqueta(t("inicio.leyenda", enJuego.size()), "resumen-partida");
        VBox izquierda = new VBox(12, lema, marco, leyenda);
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        // Derecha: la ficha de la partida, el récord y los botones.
        GridPane ficha = new GridPane();
        ficha.getStyleClass().add("ficha-partida");
        ficha.setHgap(14);
        ficha.setVgap(9);
        int fila = 0;
        for (String[] f : filas(atlas, ajustes, enJuego.size())) {
            Label clave = FichaPais.etiqueta(f[0], "ficha-clave");
            clave.setMinWidth(Region.USE_PREF_SIZE);
            Label valor = FichaPais.etiqueta(f[1], "ficha-partida-valor");
            valor.setWrapText(true);
            GridPane.setValignment(clave, VPos.TOP);
            ficha.addRow(fila++, clave, valor);
        }

        Button botonJugar = new Button("▶  " + t("menu.jugar"));
        botonJugar.getStyleClass().addAll("boton-principal", "boton-jugar");
        botonJugar.setDefaultButton(true);
        botonJugar.setMaxWidth(Double.MAX_VALUE);
        botonJugar.setOnAction(e -> {
            if (getChildren().size() == 1) jugar.accept(ajustes); // no, si hay una ventana abierta
        });
        Animaciones.alPasar(botonJugar, 1.05, 3, MenuComun.latir(botonJugar));
        Button cambiar = new Button("‹  " + t("inicio.cambiar"));
        cambiar.setCancelButton(true);
        cambiar.setMaxWidth(Double.MAX_VALUE);
        cambiar.setOnAction(e -> {
            if (getChildren().size() == 1) volver.run();
        });

        Region empuje = new Region();
        VBox.setVgrow(empuje, Priority.ALWAYS);
        VBox derecha = new VBox(12, FichaPais.etiqueta(t("inicio.tuPartida"), "titulo-config"), ficha, empuje,
                MenuComun.recordActual(ctx.puntajes()), botonJugar, cambiar);
        derecha.getStyleClass().add("panel");
        derecha.setPrefWidth(440);
        derecha.setMinWidth(400);

        HBox columnas = new HBox(22, izquierda, derecha);
        VBox.setVgrow(columnas, Priority.ALWAYS);
        VBox todo = new VBox(14, cabecera, columnas);
        todo.setPadding(new Insets(14, 26, 18, 26));
        ScrollPane desplazable = new ScrollPane(todo);
        desplazable.setFitToWidth(true);
        desplazable.setFitToHeight(true);
        getChildren().add(desplazable);

        // Enter juega y Esc vuelve a la configuración (si no hay una ventana abierta encima).
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (getChildren().size() != 1) return;
            if (e.getCode() == KeyCode.ENTER) botonJugar.fire();
            else if (e.getCode() == KeyCode.ESCAPE) cambiar.fire();
            else return;
            e.consume();
        });
    }

    /** Los países que salen en la partida: los del nivel inicial o los de los continentes elegidos. */
    private static Predicate<Pais> paisesEnJuego(Atlas atlas, Ajustes a) {
        if (a.clasico() || a.personalizado()) return p -> a.continentes().contains(p.continente());
        Set<Pais> nivel = new HashSet<>(Nivel.progresion(atlas).get(a.nivelInicial()).paises());
        return nivel::contains;
    }

    /** Las filas de la ficha "Tu partida": {clave, valor}. */
    private static List<String[]> filas(Atlas atlas, Ajustes a, int paises) {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{t("inicio.estilo"), a.clasico() ? t("clasico.nombre") : t("estilo.moderno")});
        String continentes = a.continentes().size() == Continente.values().length ? t("inicio.todos")
                : a.continentes().stream().map(Continente::nombre).collect(Collectors.joining(", "));
        if (a.clasico() || a.personalizado()) {
            filas.add(new String[]{t("menu.continentes"), continentes});
        } else {
            Nivel n = Nivel.progresion(atlas).get(a.nivelInicial());
            filas.add(new String[]{t("menu.nivel"), t("menu.resumen.nivel", a.nivelInicial() + 1, n.nombre())});
        }
        filas.add(new String[]{t("inicio.paises"), String.valueOf(paises)});
        filas.add(new String[]{t("menu.modo"), a.modo().nombre()});
        if (a.clasico()) {
            filas.add(new String[]{t("inicio.reglas"), t("inicio.reglasClasico", PartidaClasica.ERRORES_MAX)});
            filas.add(new String[]{t("inicio.puntos"), t("clasico.puntosPorPais", PartidaClasica.puntosPorPais(a.modo()))});
        } else {
            filas.add(new String[]{t("inicio.tiempo"), a.contrarreloj()
                    ? t("inicio.tiempoContrarreloj", Partida.SEGUNDOS_CONTRARRELOJ, a.velocidad().toleranciaKm())
                    : t("inicio.tiempoNormal", a.velocidad().segundos(), a.velocidad().toleranciaKm())});
            List<String> extras = new ArrayList<>();
            if (a.contrarreloj()) extras.add(t("regla.contrarreloj"));
            if (a.frioCaliente()) extras.add(t("regla.frioCaliente"));
            if (a.piezaGirada()) extras.add(t("regla.piezaGirada"));
            filas.add(new String[]{t("menu.reglas"), extras.isEmpty() ? t("inicio.ninguna") : String.join(", ", extras)});
            filas.add(new String[]{t("inicio.vidas"), a.contrarreloj() ? t("inicio.sinVidas") : "♥ × " + Partida.VIDAS});
        }
        filas.add(new String[]{t("inicio.ficha"), t(a.mostrarDatos() ? "inicio.si" : "inicio.no")});
        return filas;
    }
}
