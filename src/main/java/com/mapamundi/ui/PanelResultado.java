package com.mapamundi.ui;

import com.mapamundi.i18n.Textos;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Resultado;
import com.mapamundi.modelo.Pais;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import static com.mapamundi.i18n.Textos.t;

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
            getChildren().add(FichaPais.etiqueta(t("resultado.distancia", Textos.entero(Math.round(r.distanciaKm()))), "negrita"));
        }
        if (r.motivo() == Resultado.Motivo.GIRADA) {
            Label giro = FichaPais.etiqueta(t("resultado.girada", Math.abs(partida.giro())), "");
            giro.setWrapText(true);
            getChildren().add(giro);
        }
        if (r.acierto()) {
            String desglose = t("resultado.ubicacion", r.puntos()) + (r.bonus() > 0 ? " · " + t("resultado.tiempo", r.bonus()) : "");
            if (r.multiplicador() > 1) desglose += " · " + t("resultado.racha", r.multiplicador());
            if (r.penalizacion() > 0) desglose += " · " + t("resultado.pistas", r.penalizacion());
            Label suma = FichaPais.etiqueta(desglose + " = " + (r.total() >= 0 ? "+" : "") + r.total(), "negrita");
            suma.setWrapText(true);
            getChildren().add(suma);
            if (partida.racha() >= 2) {
                int proximo = Partida.multiplicador(partida.racha() + 1);
                getChildren().add(FichaPais.etiqueta("🔥 " + t("resultado.rachaDe", partida.racha())
                        + (proximo > r.multiplicador() ? " · " + t("resultado.proximo", proximo) : ""), "racha"));
            }
            if (contrarreloj) getChildren().add(FichaPais.etiqueta(t("resultado.segundosExtra", Partida.SEGUNDOS_POR_ACIERTO), "texto-suave"));
        } else {
            String consecuencia;
            if (r.motivo() == Resultado.Motivo.FIN_CONTRARRELOJ) consecuencia = t("resultado.ubicaste", partida.aciertosPartida());
            else if (contrarreloj) consecuencia = t("resultado.sinPuntos");
            else if (finPartida) consecuencia = t("resultado.sinCorazones");
            else consecuencia = t("resultado.pierdesCorazon", partida.vidas());
            if (r.penalizacion() > 0) consecuencia += " " + t("resultado.costoPistas", r.penalizacion());
            Label texto = FichaPais.etiqueta(consecuencia, "");
            texto.setWrapText(true);
            getChildren().add(texto);
            if (r.tieneDistancia()) getChildren().add(FichaPais.etiqueta(t("resultado.leyenda"), "texto-suave"));
        }

        boolean mostrarDatos = partida.ajustes().mostrarDatos();
        if (mostrarDatos) {
            ScrollPane datos = new ScrollPane(FichaPais.datos(p, true));
            datos.setFitToWidth(true);
            datos.setPrefViewportHeight(230);
            getChildren().add(datos);
        }

        Button siguiente = new Button(t(finPartida ? "resultado.verFinal" : "resultado.siguiente") + " (Enter)");
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
