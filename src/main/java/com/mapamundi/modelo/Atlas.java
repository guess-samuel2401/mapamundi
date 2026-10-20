package com.mapamundi.modelo;

import com.google.gson.Gson;
import com.mapamundi.geo.Forma;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Todos los países y territorios del mapa, cargados desde datos/paises.json. */
public final class Atlas {

    private static final String RECURSO = "/com/mapamundi/datos/paises.json";

    private final List<Pais> paises;
    private final List<Forma> otrasTierras;
    private final String datosActualizados;

    private Atlas(List<Pais> paises, List<Forma> otrasTierras, String datosActualizados) {
        this.paises = Collections.unmodifiableList(paises);
        this.otrasTierras = Collections.unmodifiableList(otrasTierras);
        this.datosActualizados = datosActualizados;
    }

    public static Atlas cargar() {
        try (InputStream in = Atlas.class.getResourceAsStream(RECURSO)) {
            if (in == null) throw new IllegalStateException("No se encontró " + RECURSO);
            Archivo archivo = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Archivo.class);
            List<Pais> paises = new ArrayList<>();
            for (PaisJson p : archivo.paises) {
                paises.add(new Pais(p.iso, p.nombre, Continente.valueOf(p.continente), p.sudamerica, p.capital,
                        p.poblacion, p.superficie, p.idioma, p.moneda, p.pibPerCapita, p.religion, p.exporta,
                        p.datosOriginales, p.color, new Forma(p.anillos)));
            }
            List<Forma> otras = new ArrayList<>();
            for (List<double[]> anillos : archivo.otrasTierras) otras.add(new Forma(anillos));
            return new Atlas(paises, otras, archivo.datosActualizados);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Pais> paises() {
        return paises;
    }

    public List<Forma> otrasTierras() {
        return otrasTierras;
    }

    /** Fecha (AAAA-MM-DD) de la consulta a Wikidata de la que salen los datos actuales, o null. */
    public String datosActualizados() {
        return datosActualizados;
    }

    public List<Pais> de(Continente c) {
        return paises.stream().filter(p -> p.continente() == c).toList();
    }

    // Estructura del JSON generado por herramientas/construir_datos.py
    private static final class Archivo {
        String datosActualizados;
        List<PaisJson> paises;
        List<List<double[]>> otrasTierras;
    }

    private static final class PaisJson {
        String iso, nombre, continente, capital, poblacion, superficie, idioma, moneda, pibPerCapita, religion, exporta;
        boolean sudamerica, datosOriginales;
        int color;
        List<double[]> anillos;
    }
}
