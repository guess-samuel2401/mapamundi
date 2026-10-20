package com.mapamundi.ui;

import com.mapamundi.juego.Sonido;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

/**
 * Efectos (los .wav originales) y música de fondo (composiciones propias del juego).
 * Si el equipo no tiene audio, el juego sigue en silencio.
 */
public final class Sonidos {

    /** Las dos piezas de música: la del menú y la de la partida. */
    public enum Musica {
        MENU("menu"), JUEGO("juego");

        private final String archivo;

        Musica(String archivo) {
            this.archivo = archivo;
        }
    }

    private final Map<Sonido, AudioClip> clips = new EnumMap<>(Sonido.class);
    private final Map<Musica, MediaPlayer> reproductores = new EnumMap<>(Musica.class);
    private final Preferencias preferencias;
    private Musica sonando;
    private boolean silencio;
    private Timeline fundido;

    public Sonidos(Preferencias preferencias) {
        this.preferencias = preferencias;
        for (Sonido s : Sonido.values()) {
            URL url = Sonidos.class.getResource("/com/mapamundi/sonidos/" + s.archivo() + ".wav");
            if (url == null) continue;
            try {
                clips.put(s, new AudioClip(url.toExternalForm()));
            } catch (RuntimeException e) {
                System.err.println("Sonido no disponible: " + s.archivo());
            }
        }
        for (Musica m : Musica.values()) {
            URL url = Sonidos.class.getResource("/com/mapamundi/musica/" + m.archivo + ".wav");
            if (url == null) continue;
            try {
                MediaPlayer r = new MediaPlayer(new Media(url.toExternalForm()));
                r.setCycleCount(MediaPlayer.INDEFINITE);
                r.setVolume(0);
                reproductores.put(m, r);
            } catch (RuntimeException e) {
                System.err.println("Música no disponible: " + m.archivo);
            }
        }
    }

    public void reproducir(Sonido s) {
        AudioClip clip = clips.get(s);
        double volumen = silencio ? 0 : preferencias.volumenEfectos;
        if (clip != null && volumen > 0) clip.play(volumen);
    }

    /** Cambia la música de fondo con un fundido corto. */
    public void musica(Musica m) {
        if (m == sonando) return;
        MediaPlayer anterior = sonando == null ? null : reproductores.get(sonando);
        if (anterior != null) anterior.stop();
        sonando = m;
        MediaPlayer r = reproductores.get(m);
        if (r == null) return;
        r.setVolume(0);
        r.seek(Duration.ZERO);
        r.play();
        aplicarVolumenMusica(Duration.millis(900));
    }

    /** Aplica el volumen de música de las preferencias (tras cambiarlo en Ajustes). */
    public void aplicarVolumenMusica() {
        aplicarVolumenMusica(Duration.millis(150));
    }

    private void aplicarVolumenMusica(Duration duracion) {
        MediaPlayer r = sonando == null ? null : reproductores.get(sonando);
        if (r == null) return;
        double destino = silencio || !preferencias.musica ? 0 : preferencias.volumenMusica;
        if (fundido != null) fundido.stop();
        fundido = new Timeline(new KeyFrame(duracion, new KeyValue(r.volumeProperty(), destino)));
        fundido.play();
    }

    public boolean enSilencio() {
        return silencio;
    }

    /** El botón 🔊 del panel superior: silencia efectos y música a la vez. */
    public void alternarSilencio() {
        silencio = !silencio;
        aplicarVolumenMusica();
    }
}
