package com.mapamundi.ui;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tipografías del juego (licencia OFL, incluidas en el programa):
 * Baloo 2 para títulos y números, Nunito para el texto.
 */
public final class Fuentes {

    public static final String TITULOS = "Baloo 2";
    public static final String TEXTO = "Nunito";

    private static final String[] ARCHIVOS = {
            "Nunito-Regular.ttf", "Nunito-SemiBold.ttf", "Nunito-Bold.ttf", "Nunito-ExtraBold.ttf",
            "Baloo2-Bold.ttf", "Baloo2-ExtraBold.ttf",
    };

    private Fuentes() {
    }

    /** Registra las fuentes en JavaFX; hay que llamarlo antes de aplicar la hoja de estilos. */
    public static void cargar() {
        for (String archivo : ARCHIVOS) {
            try (InputStream in = Fuentes.class.getResourceAsStream("/com/mapamundi/fuentes/" + archivo)) {
                if (in == null || Font.loadFont(in, 12) == null) System.err.println("No se pudo cargar la fuente " + archivo);
            } catch (IOException e) {
                System.err.println("No se pudo cargar la fuente " + archivo + ": " + e.getMessage());
            }
        }
    }

    /**
     * Para dibujar texto en el Canvas del mapa. JavaFX solo combina Regular y Bold dentro de una familia,
     * así que SemiBold y ExtraBold son familias aparte (ver herramientas/renombrar_fuentes.py).
     */
    public static Font texto(FontWeight peso, double tamano) {
        return switch (peso) {
            case SEMI_BOLD, MEDIUM -> Font.font(TEXTO + " SemiBold", tamano);
            case BOLD -> Font.font(TEXTO, FontWeight.BOLD, tamano);
            case EXTRA_BOLD, BLACK -> Font.font(TEXTO + " ExtraBold", tamano);
            default -> Font.font(TEXTO, tamano);
        };
    }

    public static Font titulo(double tamano) {
        return Font.font(TITULOS + " ExtraBold", tamano);
    }
}
