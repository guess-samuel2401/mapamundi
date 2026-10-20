package com.mapamundi.modelo;

import com.mapamundi.geo.Forma;

/**
 * Un país jugable. Las fronteras vienen de Natural Earth; idioma, religión, moneda y
 * exportaciones vienen del Mapamundi original (datos de 2003) cuando el país existía allí.
 *
 * @param color índice 1-7 para colorear el mapa sin que dos vecinos coincidan (MAPCOLOR7)
 */
public record Pais(
        String iso,
        String nombre,
        Continente continente,
        boolean sudamerica,
        String capital,
        String poblacion,
        String superficie,
        String idioma,
        String religion,
        String moneda,
        String exporta,
        boolean datosOriginales,
        int color,
        Forma forma) {

    @Override
    public String toString() {
        return nombre;
    }
}
