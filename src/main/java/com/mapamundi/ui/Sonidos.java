package com.mapamundi.ui;

import com.mapamundi.juego.Sonido;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

/** Reproduce los .wav originales. Si el equipo no tiene audio, el juego sigue en silencio. */
public final class Sonidos {

    private final Map<Sonido, AudioClip> clips = new EnumMap<>(Sonido.class);
    private boolean silencio;

    public Sonidos() {
        for (Sonido s : Sonido.values()) {
            URL url = Sonidos.class.getResource("/com/mapamundi/sonidos/" + s.archivo() + ".wav");
            if (url == null) continue;
            try {
                clips.put(s, new AudioClip(url.toExternalForm()));
            } catch (RuntimeException e) {
                System.err.println("Sonido no disponible: " + s.archivo());
            }
        }
    }

    public void reproducir(Sonido s) {
        AudioClip clip = clips.get(s);
        if (clip != null && !silencio) clip.play();
    }

    public boolean enSilencio() {
        return silencio;
    }

    public void alternarSilencio() {
        silencio = !silencio;
    }
}
