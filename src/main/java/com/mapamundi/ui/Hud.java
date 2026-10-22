package com.mapamundi.ui;

import com.mapamundi.juego.Partida;

import static com.mapamundi.i18n.Textos.t;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
    private final Corazones vidas = new Corazones(Partida.VIDAS);
    private final Label racha = valor("hud-valor");
    private final Label modo = valor("texto-suave");
    private final Label reloj = valor("hud-puntaje");
    private final ProgressBar tiempo = new ProgressBar(1);
    private final HBox cajaAvance;
    private final HBox cajaVidas;
    final Button sonido = new Button("🔊");
    final Button pausa = new Button("⏸ F5");

    Hud() {
        getStyleClass().add("hud");
        setSpacing(6);
        Label titulo = FichaPais.etiqueta("Mapamundi", "titulo-hud");
        cajaAvance = par(t("hud.avance"), avance);
        cajaVidas = par(t("hud.vidas"), vidas);
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        sonido.setFocusTraversable(false);
        pausa.setFocusTraversable(false);
        HBox fila = new HBox(18, titulo, par(t("hud.nivel"), nivel), cajaAvance, par(t("hud.puntaje"), puntaje),
                par(t("hud.errores"), errores), cajaVidas, par(t("hud.racha"), racha), hueco, modo, sonido, pausa);
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
        boolean contrarreloj = p.ajustes().contrarreloj();
        vidas.mostrar(p.vidas());
        cajaVidas.setVisible(!contrarreloj);
        cajaVidas.setManaged(!contrarreloj);
        // El combo que valdrá el próximo acierto.
        int combo = Partida.multiplicador(p.racha() + 1);
        racha.setText(p.racha() + (combo > 1 ? "  ×" + combo : ""));
        racha.getStyleClass().remove("hud-combo");
        if (combo > 1) racha.getStyleClass().add("hud-combo");
        String extras = (contrarreloj ? " · " + t("regla.contrarreloj") : "") + (p.ajustes().piezaGirada() ? " · " + t("hud.experto") : "");
        modo.setText(p.ajustes().modo().nombre() + extras);
        sonido.setText(silencio ? "🔇" : "🔊");

        double fraccion;
        if (contrarreloj) {
            int s = p.segundosPartida();
            fraccion = Math.min(1, (double) s / Partida.SEGUNDOS_CONTRARRELOJ);
            reloj.setText(String.format("%d:%02d", s / 60, s % 60));
        } else {
            boolean enMapa = p.fase() != Partida.Fase.CONTINENTE;
            fraccion = enMapa ? (double) p.segundos() / p.ajustes().velocidad().segundos() : 1;
            reloj.setText(enMapa ? String.format("00:%02d", p.segundos()) : "--:--");
        }
        tiempo.setProgress(fraccion);
        tiempo.getStyleClass().remove("poco");
        if (fraccion <= 0.33) tiempo.getStyleClass().add("poco");
    }

    private static HBox par(String clave, Node valor) {
        HBox h = new HBox(5, FichaPais.etiqueta(clave.toUpperCase(), "hud-clave"), valor);
        // Los corazones no tienen línea base de texto: se centran con su etiqueta.
        h.setAlignment(valor instanceof Label ? Pos.BASELINE_LEFT : Pos.CENTER_LEFT);
        return h;
    }

    private static Label valor(String clase) {
        return FichaPais.etiqueta("", clase);
    }
}
