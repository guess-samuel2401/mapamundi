package com.mapamundi.ui;

import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Resultado;
import com.mapamundi.modelo.Pais;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.Locale;

/** Resultado de la ronda; ocupa el panel lateral para no tapar el mapa. */
final class PanelResultado extends VBox {

    /** Sin la ficha de datos, el resultado se muestra 3,5 s y pasa solo al siguiente país. */
    private static final Duration AUTO_SIGUIENTE = Duration.millis(3500);

    PanelResultado(Partida partida) {
        super(8);
        Resultado r = partida.resultado();
        Pais p = partida.actual();
        boolean finPartida = partida.vidas() <= 0;

        Label titulo = FichaPais.etiqueta((r.acierto() ? "😃 " : "😞 ") + r.motivo().titulo(), r.acierto() ? "acierto" : "fallo");
        titulo.setWrapText(true);
        getChildren().addAll(titulo, FichaPais.etiqueta(p.nombre(), "titulo-mapa-mediano"));

        if (r.tieneDistancia()) {
            String km = NumberFormat.getIntegerInstance(Locale.of("es")).format(Math.round(r.distanciaKm()));
            getChildren().add(FichaPais.etiqueta("Distancia al objetivo: " + km + " km", "negrita"));
        }
        if (r.acierto()) {
            getChildren().add(FichaPais.etiqueta("+" + r.puntos() + " ubicación · +" + r.bonus() + " bonus tiempo = +"
                    + (r.puntos() + r.bonus()), "negrita"));
        } else {
            getChildren().add(FichaPais.etiqueta(finPartida ? "¡Sin vidas! Fin de la partida."
                    : "Pierdes una vida. Te quedan " + partida.vidas() + ".", ""));
            if (r.tieneDistancia()) getChildren().add(FichaPais.etiqueta("● tu posición · ★ posición real", "texto-suave"));
        }

        boolean mostrarDatos = partida.ajustes().mostrarDatos();
        if (mostrarDatos) {
            ScrollPane datos = new ScrollPane(FichaPais.datos(p, true, true));
            datos.setFitToWidth(true);
            datos.setPrefViewportHeight(230);
            getChildren().add(datos);
        }

        Button siguiente = new Button((finPartida ? "Ver puntaje final" : "Siguiente país") + " (Enter)");
        siguiente.getStyleClass().add("boton-principal");
        siguiente.setMaxWidth(Double.MAX_VALUE);
        siguiente.setFocusTraversable(false);
        siguiente.setOnAction(e -> partida.siguiente());
        getChildren().add(siguiente);

        if (!mostrarDatos && !finPartida) {
            PauseTransition espera = new PauseTransition(AUTO_SIGUIENTE);
            // Solo si el jugador no avanzó ya con Enter.
            espera.setOnFinished(e -> {
                if (partida.resultado() == r) partida.siguiente();
            });
            espera.play();
        }
    }
}
