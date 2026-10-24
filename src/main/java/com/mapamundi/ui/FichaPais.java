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

import static com.mapamundi.i18n.Textos.t;

/** La pista del país según el modo, y la ficha "Datos del País" del original. */
public final class FichaPais {

    private FichaPais() {
    }

    /** Lo que el jugador sabe: nombre, capital o solo datos. */
    public static Node pista(Pais p, Modo modo, boolean grande) {
        VBox caja = new VBox(2);
        caja.getStyleClass().add("pista");
        if (modo == Modo.DATOS) {
            // Como en el juego original: solo cifras. La moneda o el idioma delatarían el país ("Won surcoreano").
            List<String[]> cifras = new ArrayList<>();
            cifras.add(new String[]{t("ficha.superficie"), p.superficie()});
            cifras.add(new String[]{t("ficha.poblacion"), p.poblacion()});
            cifras.add(new String[]{t("ficha.pib"), p.pibPerCapita()});
            caja.getChildren().addAll(etiqueta(t("ficha.quePais"), "subtitulo-rojo"), ficha(cifras));
            return caja;
        }
        Label tipo = etiqueta(t(modo.porCapital() ? "ficha.capitalDe" : "ficha.pais"), "pista-tipo");
        Label nombre = etiqueta(modo.porCapital() ? p.capital() : p.nombre(), grande ? "titulo-mapa-grande" : "titulo-mapa-mediano");
        nombre.setWrapText(true);
        caja.getChildren().addAll(tipo, nombre);
        return caja;
    }

    /** Fecha de la consulta a Wikidata (la pone la ventana al cargar el atlas), para citar la fuente. */
    private static String fechaDatos;

    static void fechaDatos(String fecha) {
        fechaDatos = fecha;
    }

    /**
     * Ficha del país. Capital, superficie, población, idioma, moneda y PIB son actuales (Wikidata);
     * religión y exportaciones vienen del juego original y se marcan con su año.
     */
    public static GridPane datos(Pais p, boolean conNombreYCapital) {
        List<String[]> filas = new ArrayList<>();
        if (conNombreYCapital) {
            filas.add(new String[]{t("ficha.pais"), p.nombre()});
            filas.add(new String[]{t("ficha.capital"), p.capital()});
        }
        filas.add(new String[]{t("ficha.superficie"), p.superficie()});
        filas.add(new String[]{t("ficha.poblacion"), p.poblacion()});
        filas.add(new String[]{t("ficha.idioma"), p.idioma()});
        filas.add(new String[]{t("ficha.moneda"), p.moneda()});
        filas.add(new String[]{t("ficha.pib"), p.pibPerCapita()});
        filas.add(new String[]{t("ficha.religion"), p.religion()});
        filas.add(new String[]{t("ficha.exporta"), p.exporta()});
        return ficha(filas);
    }

    /** Las filas {clave, valor} en dos columnas, con la fuente de los datos al pie. */
    private static GridPane ficha(List<String[]> filas) {
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
        if (fechaDatos != null) {
            Label fuente = etiqueta(t("ficha.fuente", fechaDatos), "ficha-fuente");
            fuente.setWrapText(true);
            g.add(fuente, 0, fila, 2, 1);
            GridPane.setMargin(fuente, new javafx.geometry.Insets(4, 0, 0, 0));
        }
        return g;
    }

    static Label etiqueta(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
        return l;
    }
}
