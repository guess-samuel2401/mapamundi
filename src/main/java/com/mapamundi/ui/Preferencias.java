package com.mapamundi.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Ajustes del jugador que se recuerdan entre partidas, en ~/.mapamundi/preferencias.json.
 * La variable de entorno MAPAMUNDI_PREFERENCIAS permite usar otro archivo (para pruebas).
 */
public final class Preferencias {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Valores por defecto para quien abre el juego por primera vez.
    double volumenMusica = 0.45;
    double volumenEfectos = 0.85;
    boolean musica = true;
    boolean pantallaCompleta = false;
    boolean daltonico = false;
    boolean tutorialVisto = false;
    /** Consejos de la primera partida ya mostrados ("continente", "mapa", "resultado"). */
    Set<String> consejosVistos = new HashSet<>();

    private transient Path archivo;

    static Preferencias cargar() {
        String otro = System.getenv("MAPAMUNDI_PREFERENCIAS");
        Path archivo = otro != null && !otro.isBlank() ? Path.of(otro)
                : Path.of(System.getProperty("user.home"), ".mapamundi", "preferencias.json");
        Preferencias p = null;
        try {
            if (Files.exists(archivo)) p = GSON.fromJson(Files.readString(archivo, StandardCharsets.UTF_8), Preferencias.class);
        } catch (IOException | RuntimeException e) {
            System.err.println("Preferencias ilegibles, se usan las de fábrica: " + e.getMessage());
        }
        if (p == null) p = new Preferencias();
        if (p.consejosVistos == null) p.consejosVistos = new HashSet<>();
        p.archivo = archivo;
        return p;
    }

    void guardar() {
        try {
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudieron guardar las preferencias: " + e.getMessage());
        }
    }

    /** Marca un consejo como visto; devuelve true si era la primera vez. */
    boolean primeraVez(String consejo) {
        if (consejosVistos.contains(consejo)) return false;
        consejosVistos.add(consejo);
        guardar();
        return true;
    }
}
