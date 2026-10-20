package com.mapamundi.ui;

import com.mapamundi.juego.Partida;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Panel salmón superior: nivel, avance, puntaje, errores, vidas y cronómetro. */
final class Hud extends VBox {

    private final Label nivel = valor("hud-valor");
    private final Label avance = valor("hud-valor");
    private final Label puntaje = valor("hud-puntaje");
    private final Label errores = valor("hud-valor");
    private final Label vidas = valor("estrellas");
    private final Label modo = valor("texto-suave");
    private final Label reloj = valor("hud-puntaje");
    private final ProgressBar tiempo = new ProgressBar(1);
    private final HBox cajaAvance;
    final Button sonido = new Button("🔊");
    final Button pausa = new Button("⏸ F5");

    Hud() {
        getStyleClass().add("hud");
        setSpacing(6);
        Label titulo = FichaPais.etiqueta("Mapamundi", "titulo-mapa");
        cajaAvance = par("Avance", avance);
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        sonido.setFocusTraversable(false);
        pausa.setFocusTraversable(false);
        HBox fila = new HBox(18, titulo, par("Nivel", nivel), cajaAvance, par("Puntaje", puntaje), par("Errores", errores),
                par("Vidas", vidas), hueco, modo, sonido, pausa);
        fila.setAlignment(Pos.CENTER_LEFT);

        tiempo.getStyleClass().add("barra-tiempo");
        tiempo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tiempo, Priority.ALWAYS);
        HBox barra = new HBox(10, tiempo, reloj);
        barra.setAlignment(Pos.CENTER);
        reloj.setMinWidth(60);
        getChildren().addAll(fila, barra);
        setPadding(new Insets(8, 16, 8, 16));
    }

    void actualizar(Partida p, boolean silencio) {
        nivel.setText(p.numeroNivel() > 0 ? p.numeroNivel() + ". " + p.nivel().nombre() : p.nivel().nombre());
        cajaAvance.setVisible(p.hayNivelSiguiente());
        cajaAvance.setManaged(p.hayNivelSiguiente());
        avance.setText(Math.min(p.aciertosNivel(), Partida.PAISES_POR_NIVEL) + "/" + Partida.PAISES_POR_NIVEL);
        puntaje.setText(String.format("%04d", p.puntaje()));
        errores.setText(p.erroresContinente() + "/" + Partida.INTENTOS_CONTINENTE);
        errores.setStyle(p.erroresContinente() >= Partida.INTENTOS_CONTINENTE - 1 ? "-fx-text-fill: -rojo;" : "");
        vidas.setText("★".repeat(Math.max(0, p.vidas())) + "☆".repeat(Partida.VIDAS - Math.max(0, p.vidas())));
        modo.setText(p.ajustes().modo().nombre() + " · " + p.ajustes().velocidad().nombre());
        sonido.setText(silencio ? "🔇" : "🔊");

        int total = p.ajustes().velocidad().segundos();
        boolean enMapa = p.fase() != Partida.Fase.CONTINENTE;
        double fraccion = enMapa ? (double) p.segundos() / total : 1;
        tiempo.setProgress(fraccion);
        tiempo.getStyleClass().remove("poco");
        if (fraccion <= 0.33) tiempo.getStyleClass().add("poco");
        reloj.setText(enMapa ? String.format("00:%02d", p.segundos()) : "--:--");
    }

    private static HBox par(String clave, Label valor) {
        HBox h = new HBox(5, FichaPais.etiqueta(clave.toUpperCase(), "hud-clave"), valor);
        h.setAlignment(Pos.BASELINE_LEFT);
        return h;
    }

    private static Label valor(String clase) {
        return FichaPais.etiqueta("", clase);
    }
}
