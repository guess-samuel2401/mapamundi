package com.mapamundi.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Los textos del juego en el idioma elegido, leídos de i18n/textos_es.properties y textos_en.properties.
 * Los huecos {0}, {1}… se rellenan con los argumentos (sin MessageFormat, para que los apóstrofos del
 * inglés no den problemas).
 */
public final class Textos {

    private static final Map<Idioma, Properties> DICCIONARIOS = new EnumMap<>(Idioma.class);
    private static Idioma idioma = Idioma.ES;

    static {
        for (Idioma i : Idioma.values()) DICCIONARIOS.put(i, cargar(i));
    }

    private Textos() {
    }

    private static Properties cargar(Idioma i) {
        String ruta = "/com/mapamundi/i18n/textos_" + i.codigo() + ".properties";
        Properties p = new Properties();
        try (InputStream in = Textos.class.getResourceAsStream(ruta)) {
            if (in == null) throw new IllegalStateException("No se encontró " + ruta);
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }

    public static void usar(Idioma nuevo) {
        idioma = nuevo;
    }

    public static Idioma idioma() {
        return idioma;
    }

    public static boolean ingles() {
        return idioma == Idioma.EN;
    }

    /** El texto de {@code clave} en el idioma actual, con {0}, {1}… sustituidos por los argumentos. */
    public static String t(String clave, Object... argumentos) {
        String texto = DICCIONARIOS.get(idioma).getProperty(clave);
        if (texto == null) texto = DICCIONARIOS.get(Idioma.ES).getProperty(clave, "¡" + clave + "!");
        for (int i = 0; i < argumentos.length; i++) texto = texto.replace("{" + i + "}", String.valueOf(argumentos[i]));
        return texto;
    }

    /** Un entero con el separador de miles del idioma (12.345 / 12,345). */
    public static String entero(long n) {
        return NumberFormat.getIntegerInstance(idioma.locale()).format(n);
    }

    /** Las claves de un idioma (para comprobar en las pruebas que ambos diccionarios están completos). */
    public static Set<String> claves(Idioma i) {
        return DICCIONARIOS.get(i).stringPropertyNames();
    }
}
