package com.mapamundi.juego;

import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Un conjunto de países con el mapa donde se juegan.
 *
 * @param vista región del mapa en la fase de ubicación; null = el continente de cada país
 */
public record Nivel(String nombre, List<Pais> paises, Region vista) {

    public static final Region VISTA_SUDAMERICA = Region.deGrados(-95, -28, -57, 14);
    public static final Region VISTA_MUNDO = Region.deGrados(-170, 190, -57, 78);

    /** La progresión del diseño: Sudamérica → América → Europa → África → Asia → Oceanía → Mundo. */
    public static List<Nivel> progresion(Atlas atlas) {
        return List.of(
                new Nivel("Sudamérica", atlas.paises().stream().filter(Pais::sudamerica).toList(), VISTA_SUDAMERICA),
                continente(atlas, Continente.AMERICA),
                continente(atlas, Continente.EUROPA),
                continente(atlas, Continente.AFRICA),
                continente(atlas, Continente.ASIA),
                continente(atlas, Continente.OCEANIA),
                new Nivel("Todo el mundo", atlas.paises(), VISTA_MUNDO));
    }

    /** Cuántos niveles tiene la progresión (el último es "Todo el mundo"). */
    public static int progresionTotal() {
        return 7;
    }

    private static Nivel continente(Atlas atlas, Continente c) {
        return new Nivel(c.nombre(), atlas.de(c), c.vista());
    }

    /** "Escoja Ud el o los continentes" del original: cada país se juega en el mapa de su continente. */
    public static Nivel personalizado(Atlas atlas, Set<Continente> continentes) {
        Set<Continente> elegidos = continentes.isEmpty() ? EnumSet.allOf(Continente.class) : continentes;
        String nombre = elegidos.stream().map(Continente::nombre).collect(Collectors.joining(" · "));
        return new Nivel(nombre, atlas.paises().stream().filter(p -> elegidos.contains(p.continente())).toList(), null);
    }

    public Region vistaPara(Pais pais) {
        return vista != null ? vista : pais.continente().vista();
    }
}
