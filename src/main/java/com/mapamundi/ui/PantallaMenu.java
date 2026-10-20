package com.mapamundi.ui;

import com.mapamundi.juego.Ajustes;
import com.mapamundi.juego.Modo;
import com.mapamundi.juego.Nivel;
import com.mapamundi.juego.Puntajes;
import com.mapamundi.juego.Velocidad;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.ui.mapa.Miniatura;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** El menú "Juego" del original (Nuevo, Nivel, Velocidad, Puntuaciones) en una sola pantalla. */
final class PantallaMenu extends ScrollPane {

    PantallaMenu(Atlas atlas, Ajustes inicial, Puntajes puntajes, Consumer<Ajustes> jugar) {
        List<Nivel> niveles = Nivel.progresion(atlas);

        // Portada
        HBox mapas = new HBox(8);
        mapas.setAlignment(Pos.CENTER);
        for (Continente c : Continente.values()) mapas.getChildren().add(Miniatura.continente(atlas, c, 96, 80));
        Label titulo = FichaPais.etiqueta("Mapamundi", "titulo-portada");
        Label lema = FichaPais.etiqueta("Adivina el continente y coloca cada país en su lugar del mapa antes de que se acabe el tiempo.", "negrita");
        VBox portada = new VBox(6, mapas, titulo, lema);
        portada.setAlignment(Pos.CENTER);

        // Velocidad
        ToggleGroup grupoVelocidad = new ToggleGroup();
        HBox velocidades = new HBox(8);
        Map<Velocidad, ToggleButton> botonesVelocidad = new EnumMap<>(Velocidad.class);
        for (Velocidad v : Velocidad.values()) {
            ToggleButton b = new ToggleButton(v.nombre() + "\n" + v.segundos() + " s · ±" + v.toleranciaKm() + " km");
            b.setToggleGroup(grupoVelocidad);
            b.setUserData(v);
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            botonesVelocidad.put(v, b);
            velocidades.getChildren().add(b);
        }
        botonesVelocidad.get(inicial.velocidad()).setSelected(true);
        // Siempre debe haber una velocidad elegida.
        grupoVelocidad.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) antes.setSelected(true);
        });

        // Modo
        ToggleGroup grupoModo = new ToggleGroup();
        VBox modos = new VBox(4);
        for (Modo m : Modo.values()) {
            RadioButton r = new RadioButton(m.nombre());
            r.setToggleGroup(grupoModo);
            r.setUserData(m);
            r.setSelected(m == inicial.modo());
            modos.getChildren().add(new VBox(0, r, conSangria(FichaPais.etiqueta(m.ayuda(), "texto-suave"))));
        }

        VBox izquierda = new VBox(10, FichaPais.etiqueta("VELOCIDAD", "subtitulo-rojo"), velocidades,
                FichaPais.etiqueta("MODO DE JUEGO", "subtitulo-rojo"), modos);
        izquierda.getStyleClass().add("panel");

        // Nivel
        ToggleGroup grupoNivel = new ToggleGroup();
        VBox listaNiveles = new VBox(4);
        for (int i = 0; i < niveles.size(); i++) {
            Nivel n = niveles.get(i);
            RadioButton r = new RadioButton((i + 1) + ". " + n.nombre());
            r.setToggleGroup(grupoNivel);
            r.setUserData(i);
            r.setSelected(i == inicial.nivelInicial());
            Region hueco = new Region();
            HBox.setHgrow(hueco, Priority.ALWAYS);
            listaNiveles.getChildren().add(new HBox(r, hueco, FichaPais.etiqueta(n.paises().size() + " países", "texto-suave")));
        }
        RadioButton personalizado = new RadioButton("Escoger continentes…");
        personalizado.setToggleGroup(grupoNivel);
        personalizado.setUserData(-1);
        personalizado.setSelected(inicial.personalizado());
        FlowPane continentes = new FlowPane(10, 4);
        Map<Continente, CheckBox> casillas = new EnumMap<>(Continente.class);
        for (Continente c : Continente.values()) {
            CheckBox cb = new CheckBox(c.nombre());
            cb.setSelected(inicial.continentes().contains(c));
            casillas.put(c, cb);
            continentes.getChildren().add(cb);
        }
        Region sangria = conSangria(continentes);
        sangria.visibleProperty().bind(personalizado.selectedProperty());
        sangria.managedProperty().bind(personalizado.selectedProperty());
        listaNiveles.getChildren().addAll(personalizado, sangria);
        grupoNivel.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) antes.setSelected(true);
        });

        CheckBox mostrarDatos = new CheckBox("Mostrar datos del país después de cada ronda");
        mostrarDatos.setSelected(inicial.mostrarDatos());

        Button botonJugar = new Button("▶  Jugar");
        botonJugar.getStyleClass().addAll("boton-principal", "boton-grande");
        botonJugar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(botonJugar, Priority.ALWAYS);
        botonJugar.setDefaultButton(true);
        Button verPuntajes = new Button("🏆 Puntuaciones");
        HBox botones = new HBox(10, botonJugar, verPuntajes);
        botones.setAlignment(Pos.CENTER);

        VBox derecha = new VBox(10, FichaPais.etiqueta("NIVEL", "subtitulo-rojo"), listaNiveles,
                FichaPais.etiqueta("Desde un nivel de la lista, cada 6 países bien ubicados pasas al siguiente.", "texto-suave"),
                mostrarDatos, botones);
        derecha.getStyleClass().add("panel");

        HBox.setHgrow(izquierda, Priority.ALWAYS);
        HBox.setHgrow(derecha, Priority.ALWAYS);
        izquierda.setPrefWidth(460);
        derecha.setPrefWidth(460);
        HBox columnas = new HBox(16, izquierda, derecha);

        VBox tablaPuntajes = new VBox();
        verPuntajes.setOnAction(e -> tablaPuntajes.getChildren().setAll(tablaPuntajes.getChildren().isEmpty()
                ? List.of(new TablaPuntajes(puntajes.leer(), null)) : List.of()));

        Label comoSeJuega = FichaPais.etiqueta("""
                1. Aparece un país. Elige su continente (flechas + Enter, teclas 1–5 o clic). Tienes 5 intentos: +100 puntos.
                2. Mueve la pieza sobre el mapa arrastrándola con el ratón, con las flechas (Shift = más rápido) o con la cruceta, y pulsa Enter o Confirmar.
                3. Si queda dentro del margen (en km reales): +500 y un bonus por los segundos sobrantes. Si fallas o se acaba el tiempo, pierdes una vida ★.
                4. F5, P o Esc pausa el juego. La rueda del ratón hace zoom en el mapa.""", "");
        comoSeJuega.setWrapText(true);
        VBox ayuda = new VBox(6, FichaPais.etiqueta("CÓMO SE JUEGA", "subtitulo-rojo"), comoSeJuega,
                FichaPais.etiqueta("Fronteras: Natural Earth (dominio público). Datos de países: Mapamundi original (2003) y Natural Earth.", "texto-suave"));
        ayuda.getStyleClass().add("panel");

        VBox todo = new VBox(18, portada, columnas, tablaPuntajes, ayuda);
        todo.setPadding(new Insets(24));
        todo.setMaxWidth(1000);
        HBox centrado = new HBox(todo);
        centrado.setAlignment(Pos.TOP_CENTER);
        setContent(centrado);
        setFitToWidth(true);

        botonJugar.setOnAction(e -> {
            EnumSet<Continente> elegidos = EnumSet.noneOf(Continente.class);
            casillas.forEach((c, cb) -> {
                if (cb.isSelected()) elegidos.add(c);
            });
            jugar.accept(new Ajustes(
                    (Velocidad) grupoVelocidad.getSelectedToggle().getUserData(),
                    (Modo) grupoModo.getSelectedToggle().getUserData(),
                    (Integer) grupoNivel.getSelectedToggle().getUserData(),
                    elegidos.isEmpty() ? EnumSet.allOf(Continente.class) : elegidos,
                    mostrarDatos.isSelected()));
        });
    }

    private static Region conSangria(Region contenido) {
        VBox v = new VBox(contenido);
        v.setPadding(new Insets(0, 0, 4, 26));
        return v;
    }
}
