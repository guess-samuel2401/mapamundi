package com.mapamundi.juego;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Las 10 puntuaciones más altas (como Puntajes.DBF del original), en ~/.mapamundi/puntajes.json. */
public final class Puntajes {

    public record Entrada(String nombre, int puntaje, String nivel, String fecha) {
        public static Entrada nueva(String nombre, int puntaje, String nivel) {
            return new Entrada(nombre, puntaje, nivel, LocalDate.now().toString());
        }
    }

    private static final int MAXIMO = 10;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path archivo;

    public Puntajes(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * La tabla del usuario. La variable de entorno MAPAMUNDI_PUNTAJES permite usar otro archivo
     * (por ejemplo, para probar con datos ficticios sin tocar los récords reales).
     */
    public static Puntajes delUsuario() {
        String otro = System.getenv("MAPAMUNDI_PUNTAJES");
        if (otro != null && !otro.isBlank()) return new Puntajes(Path.of(otro));
        return new Puntajes(Path.of(System.getProperty("user.home"), ".mapamundi", "puntajes.json"));
    }

    public List<Entrada> leer() {
        try {
            if (!Files.exists(archivo)) return List.of();
            List<Entrada> lista = GSON.fromJson(Files.readString(archivo, StandardCharsets.UTF_8), new TypeToken<List<Entrada>>() { }.getType());
            return lista == null ? List.of() : lista;
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    public boolean entraEnLaTabla(int puntaje) {
        if (puntaje <= 0) return false;
        List<Entrada> lista = leer();
        return lista.size() < MAXIMO || puntaje > lista.getLast().puntaje();
    }

    /** Guarda la entrada y devuelve la tabla actualizada (si no se puede escribir, la tabla vive solo en memoria). */
    public List<Entrada> guardar(Entrada nueva) {
        List<Entrada> lista = new ArrayList<>(leer());
        lista.add(nueva);
        lista.sort(Comparator.comparingInt(Entrada::puntaje).reversed());
        if (lista.size() > MAXIMO) lista = new ArrayList<>(lista.subList(0, MAXIMO));
        try {
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, GSON.toJson(lista), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudieron guardar los puntajes: " + e.getMessage());
        }
        return lista;
    }
}
