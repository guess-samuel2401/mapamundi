package com.mapamundi.i18n;

import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.juego.Modo;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.juego.Pista;
import com.mapamundi.juego.Resultado;
import com.mapamundi.juego.Velocidad;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextosTest {

    @AfterEach
    void volverAlEspanol() {
        Textos.usar(Idioma.ES);
    }

    @Test
    void losDosDiccionariosTienenLasMismasClaves() {
        Set<String> soloEs = new TreeSet<>(Textos.claves(Idioma.ES));
        soloEs.removeAll(Textos.claves(Idioma.EN));
        Set<String> soloEn = new TreeSet<>(Textos.claves(Idioma.EN));
        soloEn.removeAll(Textos.claves(Idioma.ES));
        assertTrue(soloEs.isEmpty(), "faltan en inglés: " + soloEs);
        assertTrue(soloEn.isEmpty(), "faltan en español: " + soloEn);
    }

    /** Cada t("clave") escrito en el código, y cada clave que se arma con el nombre de un enum, existe. */
    @Test
    void todasLasClavesUsadasExisten() throws IOException {
        List<String> claves = new ArrayList<>();
        // Solo claves completas: t("clave") o t("clave", …). Las que se arman con un enum se añaden abajo.
        Pattern llamada = Pattern.compile("\\bt\\(\"([A-Za-z0-9_.]+)\"\\s*[,)]");
        try (Stream<Path> archivos = Files.walk(Path.of("src/main/java"))) {
            for (Path a : archivos.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = llamada.matcher(Files.readString(a));
                while (m.find()) claves.add(m.group(1));
            }
        }
        for (Continente c : Continente.values()) claves.add("continente." + c.name());
        for (Modo m : Modo.values()) claves.addAll(List.of("modo." + m.name() + ".nombre", "modo." + m.name() + ".ayuda"));
        for (Velocidad v : Velocidad.values()) claves.add("velocidad." + v.name());
        for (Resultado.Motivo m : Resultado.Motivo.values()) claves.add("motivo." + m.name());
        for (Pista p : Pista.values()) claves.addAll(List.of("pista." + p.name() + ".nombre", "pista." + p.name() + ".descripcion"));
        for (Logro l : Logro.values()) claves.addAll(List.of("logro." + l.name() + ".nombre", "logro." + l.name() + ".descripcion"));
        for (String fase : List.of("continente", "mapa", "resultado", "clasico.caida", "clasico.mapa")) claves.add("consejo." + fase);
        for (PartidaClasica.Motivo m : PartidaClasica.Motivo.values()) claves.add("clasico.motivo." + m.name());
        assertTrue(claves.size() > 150, "se encontraron " + claves.size() + " claves");
        for (Idioma i : Idioma.values()) {
            List<String> faltan = claves.stream().filter(c -> !Textos.claves(i).contains(c)).distinct().toList();
            assertTrue(faltan.isEmpty(), "faltan en " + i + ": " + faltan);
        }
    }

    @Test
    void losHuecosSeRellenan() {
        Textos.usar(Idioma.EN);
        assertEquals("Tries: 2/5", Textos.t("continente.intentos", 2, 5));
        assertEquals("Time's up!", Textos.t("motivo.TIEMPO"));
        assertEquals("12,345", Textos.entero(12345));
        Textos.usar(Idioma.ES);
        assertEquals("Intentos: 2/5", Textos.t("continente.intentos", 2, 5));
        assertEquals("12.345", Textos.entero(12345));
    }

    @Test
    void losPaisesEnIngles() {
        Atlas atlas = Atlas.cargar();
        Pais ecuador = atlas.paises().stream().filter(p -> p.iso().equals("EC")).findFirst().orElseThrow();
        Pais vaticano = atlas.paises().stream().filter(p -> p.iso().equals("VA")).findFirst().orElseThrow();
        String poblacionEs = ecuador.poblacion();
        assertTrue(poblacionEs.contains("habs."));
        Textos.usar(Idioma.EN);
        assertEquals("Ecuador", ecuador.nombre());
        assertEquals("United States dollar", ecuador.moneda());
        assertEquals("Oil, bananas, shrimp, cocoa, coffee", ecuador.exporta());
        assertEquals(poblacionEs.replace(".", ",").replace("habs,", "inhabitants"), ecuador.poblacion());
        assertEquals("Vatican City", vaticano.nombre());
        assertEquals("Africa", Continente.AFRICA.nombre());
        // Los 197 países tienen nombre, capital, idioma y moneda en inglés.
        for (Pais p : atlas.paises()) {
            assertTrue(!p.ingles().nombre().isBlank() && !p.ingles().capital().isBlank()
                    && !p.ingles().idioma().isBlank() && !p.ingles().moneda().isBlank(), p.iso());
        }
        Textos.usar(Idioma.ES);
        assertEquals("Ciudad del Vaticano", vaticano.nombre());
    }
}
