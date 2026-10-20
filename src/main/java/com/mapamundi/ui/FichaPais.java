package com.mapamundi.ui;

import com.mapamundi.juego.Modo;
import com.mapamundi.modelo.Pais;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** La pista del país según el modo, y la ficha "Datos del País" del original. */
public final class FichaPais {

    private FichaPais() {
    }

    /** Lo que el jugador sabe: nombre, capital o solo datos. */
    public static Node pista(Pais p, Modo modo, boolean grande) {
        VBox caja = new VBox(2);
        caja.getStyleClass().add("pista");
        if (modo == Modo.DATOS) {
            caja.getChildren().addAll(etiqueta("¿Qué país es?", "subtitulo-rojo"), datos(p, false, datosMinimos(p)));
            return caja;
        }
        Label tipo = etiqueta(modo.porCapital() ? "País cuya capital es" : "País", "pista-tipo");
        Label nombre = etiqueta(modo.porCapital() ? p.capital() : p.nombre(), grande ? "titulo-mapa-grande" : "titulo-mapa-mediano");
        nombre.setWrapText(true);
        caja.getChildren().addAll(tipo, nombre);
        return caja;
    }

    /** En modo datos, si el país no venía en el juego original, se añade la capital para que haya pistas suficientes. */
    private static boolean datosMinimos(Pais p) {
        return !p.datosOriginales();
    }

    public static GridPane datos(Pais p, boolean conNombre, boolean conCapital) {
        List<String[]> filas = new ArrayList<>();
        if (conNombre) filas.add(new String[]{"País", p.nombre()});
        if (conNombre || conCapital) filas.add(new String[]{"Capital", p.capital()});
        filas.add(new String[]{"Superficie", p.superficie()});
        filas.add(new String[]{"Población", p.poblacion()});
        filas.add(new String[]{"Idioma", p.idioma()});
        filas.add(new String[]{"Religión", p.religion()});
        filas.add(new String[]{"Moneda", p.moneda()});
        filas.add(new String[]{"Exporta", p.exporta()});
        GridPane g = new GridPane();
        g.getStyleClass().add("ficha");
        g.setHgap(6);
        g.setVgap(2);
        int fila = 0;
        for (String[] f : filas) {
            if (f[1] == null || f[1].isBlank()) continue;
            Label clave = etiqueta(f[0] + ":", "ficha-clave");
            clave.setMinWidth(Region.USE_PREF_SIZE);
            clave.setAlignment(Pos.TOP_LEFT);
            Label valor = etiqueta(f[1], "ficha-valor");
            valor.setWrapText(true);
            GridPane.setValignment(clave, VPos.TOP);
            g.addRow(fila++, clave, valor);
        }
        return g;
    }

    static Label etiqueta(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
        return l;
    }
}
