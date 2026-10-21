package com.mapamundi.juego;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SonidoTest {

    /** Cada efecto tiene su .wav en PCM de 16 bits (JavaFX no reproduce formatos comprimidos como ADPCM). */
    @Test
    void cadaEfectoTieneSuArchivoEnPcm() throws Exception {
        for (Sonido s : Sonido.values()) {
            String ruta = "/com/mapamundi/sonidos/" + s.archivo() + ".wav";
            InputStream in = Sonido.class.getResourceAsStream(ruta);
            assertNotNull(in, "falta " + ruta);
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(in))) {
                AudioFormat f = audio.getFormat();
                assertEquals(AudioFormat.Encoding.PCM_SIGNED, f.getEncoding(), ruta);
                assertEquals(16, f.getSampleSizeInBits(), ruta);
                double segundos = audio.getFrameLength() / f.getFrameRate();
                assertTrue(segundos > 0.2 && segundos < 3, ruta + " dura " + segundos + " s");
            }
        }
    }
}
