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
        boolean finPartida = partida.terminada();
        boolean contrarreloj = partida.ajustes().contrarreloj();

        Label titulo = FichaPais.etiqueta((r.acierto() ? "😃 " : "😞 ") + r.motivo().titulo(), r.acierto() ? "acierto" : "fallo");
        titulo.setWrapText(true);
        getChildren().addAll(titulo, FichaPais.etiqueta(p.nombre(), "titulo-mapa-mediano"));

        if (r.tieneDistancia()) {
            String km = NumberFormat.getIntegerInstance(Locale.of("es")).format(Math.round(r.distanciaKm()));
            getChildren().add(FichaPais.etiqueta("Distancia al objetivo: " + km + " km", "negrita"));
        }
        if (r.motivo() == Resultado.Motivo.GIRADA) {
            Label giro = FichaPais.etiqueta("Estaba en su sitio, pero girada " + Math.abs(partida.giro()) + "°.", "");
            giro.setWrapText(true);
            getChildren().add(giro);
        }
        if (r.acierto()) {
            String desglose = "+" + r.puntos() + " ubicación" + (r.bonus() > 0 ? " · +" + r.bonus() + " tiempo" : "");
            if (r.multiplicador() > 1) desglose += " · ×" + r.multiplicador() + " racha";
            if (r.penalizacion() > 0) desglose += " · −" + r.penalizacion() + " pistas";
            Label suma = FichaPais.etiqueta(desglose + " = " + (r.total() >= 0 ? "+" : "") + r.total(), "negrita");
            suma.setWrapText(true);
            getChildren().add(suma);
            if (partida.racha() >= 2) {
                getChildren().add(FichaPais.etiqueta("🔥 Racha de " + partida.racha() + (Partida.multiplicador(partida.racha() + 1) > r.multiplicador()
                        ? " · ¡el próximo acierto vale ×" + Partida.multiplicador(partida.racha() + 1) + "!" : ""), "racha"));
            }
            if (contrarreloj) getChildren().add(FichaPais.etiqueta("+" + Partida.SEGUNDOS_POR_ACIERTO + " segundos al reloj", "texto-suave"));
        } else {
            String consecuencia;
            if (r.motivo() == Resultado.Motivo.FIN_CONTRARRELOJ) consecuencia = "Ubicaste " + partida.aciertosPartida() + " países.";
            else if (contrarreloj) consecuencia = "Sin puntos en esta ronda. ¡Sigue, que el reloj corre!";
            else if (finPartida) consecuencia = "¡Sin corazones! Fin de la partida.";
            else consecuencia = "Pierdes un corazón ♥. Te quedan " + partida.vidas() + ".";
            if (r.penalizacion() > 0) consecuencia += " Las pistas costaron −" + r.penalizacion() + ".";
            Label texto = FichaPais.etiqueta(consecuencia, "");
            texto.setWrapText(true);
            getChildren().add(texto);
            if (r.tieneDistancia()) getChildren().add(FichaPais.etiqueta("● tu posición · ★ posición real", "texto-suave"));
        }

        boolean mostrarDatos = partida.ajustes().mostrarDatos();
        if (mostrarDatos) {
            ScrollPane datos = new ScrollPane(FichaPais.datos(p, true));
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
