package com.mapamundi.modelo;

import com.mapamundi.geo.Forma;
import com.mapamundi.i18n.Textos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un país jugable. Las fronteras vienen de Natural Earth; capital, población, superficie, idioma,
 * moneda y PIB per cápita, de Wikidata; religión y exportaciones, del Mapamundi original (2003).
 *
 * <p>Los textos se guardan en español y en inglés ({@link Ingles}); cada consulta devuelve el del idioma
 * del juego. Las cifras se guardan una sola vez, en formato español, y se convierten al vuelo.
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
        Ingles ingles,
        boolean datosOriginales,
        int color,
        Forma forma) {

    /** Los textos del país en inglés (las cifras no: esas se convierten). */
    public record Ingles(String nombre, String capital, String idioma, String moneda, String religion, String exporta) {
    }

    private static final Pattern NUMERO = Pattern.compile("\\d[\\d.,]*\\d|\\d");

    private boolean enIngles() {
        return Textos.ingles() && ingles != null;
    }

    private static String elegir(String espanol, String ingles, boolean enIngles) {
        return enIngles && ingles != null && !ingles.isBlank() ? ingles : espanol;
    }

    @Override public String nombre() { return elegir(nombre, enIngles() ? ingles.nombre() : null, enIngles()); }
    @Override public String capital() { return elegir(capital, enIngles() ? ingles.capital() : null, enIngles()); }
    @Override public String idioma() { return elegir(idioma, enIngles() ? ingles.idioma() : null, enIngles()); }
    @Override public String moneda() { return elegir(moneda, enIngles() ? ingles.moneda() : null, enIngles()); }
    @Override public String religion() { return elegir(religion, enIngles() ? ingles.religion() : null, enIngles()); }
    @Override public String exporta() { return elegir(exporta, enIngles() ? ingles.exporta() : null, enIngles()); }
    @Override public String poblacion() { return Textos.ingles() ? cifraEnIngles(poblacion) : poblacion; }
    @Override public String superficie() { return Textos.ingles() ? cifraEnIngles(superficie) : superficie; }
    @Override public String pibPerCapita() { return Textos.ingles() ? cifraEnIngles(pibPerCapita) : pibPerCapita; }

    /** "41.454.761 habs. (2023)" → "41,454,761 inhabitants (2023)"; "0,49 km²" → "0.49 km²". */
    static String cifraEnIngles(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        Matcher m = NUMERO.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String n = m.group().replace('.', '\u0001').replace(',', '.').replace('\u0001', ',');
            m.appendReplacement(sb, Matcher.quoteReplacement(n));
        }
        m.appendTail(sb);
        return sb.toString()
                .replace("habs.", "inhabitants")
                .replace("aprox.", "approx.")
                .replace("No Disponible", "Not available");
    }

    @Override
    public String toString() {
        return nombre();
    }
}
