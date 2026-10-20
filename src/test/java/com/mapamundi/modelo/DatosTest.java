package com.mapamundi.modelo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatosTest {

    private static final Atlas ATLAS = Atlas.cargar();

    @Test
    void todosLosPaisesTienenLosDatosBasicos() {
        List<Function<Pais, String>> campos = List.of(Pais::capital, Pais::poblacion, Pais::superficie, Pais::idioma, Pais::moneda);
        for (Pais p : ATLAS.paises()) {
            for (Function<Pais, String> campo : campos) {
                String v = campo.apply(p);
                assertTrue(v != null && !v.isBlank(), "Falta un dato de " + p.nombre());
            }
        }
    }

    @Test
    void losDatosSonActuales() {
        assertNotNull(ATLAS.datosActualizados(), "El JSON debe decir cuándo se consultó Wikidata");
        Pais ecuador = ATLAS.paises().stream().filter(p -> p.iso().equals("EC")).findFirst().orElseThrow();
        assertEquals("Quito", ecuador.capital());
        assertEquals("Dólar estadounidense", ecuador.moneda());
        Pais croacia = ATLAS.paises().stream().filter(p -> p.iso().equals("HR")).findFirst().orElseThrow();
        assertEquals("Euro", croacia.moneda(), "Croacia usa el euro desde 2023");
    }
}
