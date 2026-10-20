package com.mapamundi.modelo;

import com.mapamundi.geo.Forma;

/**
 * Un país jugable. Las fronteras vienen de Natural Earth; capital, población, superficie, idioma,
 * moneda y PIB per cápita, de Wikidata; religión y exportaciones, del Mapamundi original (2003).
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
        String moneda,
        String pibPerCapita,
        String religion,
        String exporta,
        boolean datosOriginales,
        int color,
        Forma forma) {

    @Override
    public String toString() {
        return nombre;
    }
}
