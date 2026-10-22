package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Anillo;
import com.mapamundi.geo.Forma;
import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Pais;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineJoin;

import java.util.List;
import java.util.function.Function;

/** Dibuja el mapa en un Canvas con los colores del Mapamundi original. */
public final class Pintor {

    // Los mismos valores que las variables de estilos.css (el Canvas no lee CSS).
    public static final Color AGUA = Color.web("#74bebc");
    public static final Color TIERRA = Color.web("#fffdf8");
    public static final Color OTRA_TIERRA = Color.web("#e8e2d4");
    public static final Color RESTO_DEL_MUNDO = Color.web("#e4e3dc");
    public static final Color BORDE = Color.web("#2a3552");
    public static final Color GRIS_SILUETA = Color.web("#ece8df");
    public static final Color TINTA = Color.web("#1d2b4f");
    public static final Color ROJO = Color.web("#e3263b");
    public static final Color SOL = Color.web("#ffd23f");
    public static final Color CORAL = Color.web("#ff8f6b");
    public static final Color FONDO_OSCURO = Color.web("#2a2150");
    public static final Color CONTORNO_LOGO = Color.web("#1a1433");

    /**
     * Paleta de atlas: siete tonos suaves y cálidos (coral, arena, salvia, rosa, lavanda, albaricoque, caqui).
     * Sin celestes ni verdes azulados, que se confundían con el mar. El índice viene de MAPCOLOR7 de Natural Earth.
     */
    private static final Color[] PALETA = {
            Color.web("#f4a99b"), Color.web("#f3d27a"), Color.web("#a9d38c"), Color.web("#eda7c9"),
            Color.web("#c4b0e6"), Color.web("#f6b878"), Color.web("#d2bf94"),
    };

    /**
     * Modo daltónico: tonos de la paleta Okabe-Ito (distinguibles con daltonismo rojo-verde), aclarados para
     * que se lean los bordes, y además una trama distinta por índice para no depender solo del color.
     */
    private static final Color[] PALETA_DALTONICO = {
            Color.web("#f5b94a"), Color.web("#f2e86b"), Color.web("#d9a0c4"), Color.web("#ee8f5f"),
            Color.web("#7aa6d8"), Color.web("#c9ad84"), Color.web("#b5cf7f"),
    };
    private static final Paint[] TRAMAS = new Paint[PALETA_DALTONICO.length];
    private static boolean daltonico;

    private Pintor() {
    }

    public static void daltonico(boolean activo) {
        daltonico = activo;
    }

    public static boolean daltonico() {
        return daltonico;
    }

    /** Relleno de un país: su color de la paleta o, en modo daltónico, color + trama. */
    public static Paint colorDe(Pais p) {
        int i = Math.floorMod(p.color() - 1, PALETA.length);
        if (!daltonico) return PALETA[i];
        if (TRAMAS[i] == null) TRAMAS[i] = Tramas.crear(i, PALETA_DALTONICO[i]);
        return TRAMAS[i];
    }

    /** Agua, territorios no jugables y países; {@code relleno} decide el color de cada país (null = blanco). */
    public static void mapa(GraphicsContext gc, Transformacion t, Atlas atlas, Function<Pais, Paint> relleno, double grosor) {
        gc.setFill(AGUA);
        gc.fillRect(0, 0, t.ancho(), t.alto());
        Region visible = t.visible();
        for (Forma f : atlas.otrasTierras()) {
            forma(gc, f.anillos(), vueltas(visible, f), 0, t, OTRA_TIERRA, BORDE, grosor * 0.7);
        }
        for (Pais p : atlas.paises()) {
            Paint color = relleno.apply(p);
            forma(gc, p.forma().anillos(), vueltas(visible, p.forma()), 0, t, color != null ? color : TIERRA, BORDE, grosor);
        }
    }

    /** Vueltas al mundo para que la forma caiga cerca de lo que se ve (cruces del antimeridiano). */
    public static int vueltas(Region visible, Forma f) {
        return visible.vueltasPara(f.anclaU());
    }

    /** Rellena y contornea un grupo de anillos desplazados (du, dv). Los huecos se respetan (par-impar). */
    public static void forma(GraphicsContext gc, List<Anillo> anillos, double du, double dv, Transformacion t,
                             Paint relleno, Paint borde, double grosor) {
        Region visible = t.visible();
        gc.beginPath();
        boolean alguno = false;
        for (Anillo a : anillos) {
            Region c = a.caja();
            if (!visible.seCruzaCon(new Region(c.uMin() + du, c.vMin() + dv, c.uMax() + du, c.vMax() + dv))) continue;
            alguno = true;
            gc.moveTo(t.x(a.u(0) + du), t.y(a.v(0) + dv));
            for (int i = 1; i < a.puntos(); i++) gc.lineTo(t.x(a.u(i) + du), t.y(a.v(i) + dv));
            gc.closePath();
        }
        if (!alguno) return;
        gc.setFillRule(FillRule.EVEN_ODD);
        if (relleno != null) {
            gc.setFill(relleno);
            gc.fill();
        }
        if (borde != null && grosor > 0) {
            gc.setStroke(borde);
            gc.setLineWidth(grosor);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.stroke();
        }
    }

    /**
     * En los modos sin forma la pieza es un círculo, para no delatar la silueta del país. Solo cambia su color:
     * el mismo que tiene el país en el mapa (en modo daltónico, con su trama).
     */
    public static void figura(GraphicsContext gc, Pais p, double cx, double cy, double lado, Paint borde, double grosor) {
        double r = lado / 2;
        gc.setFill(colorDe(p));
        gc.fillOval(cx - r, cy - r, lado, lado);
        if (borde != null && grosor > 0) {
            gc.setStroke(borde);
            gc.setLineWidth(grosor);
            gc.strokeOval(cx - r, cy - r, lado, lado);
        }
    }

    /** Dibuja una forma centrada y ajustada a un recuadro (siluetas de la fase 1). */
    public static void ajustada(GraphicsContext gc, List<Anillo> anillos, Region caja, double x, double y, double ancho, double alto,
                                Paint relleno, Paint borde, double grosor) {
        double escala = Math.min(ancho / caja.ancho(), alto / caja.alto());
        Transformacion t = new Transformacion(caja.centroU(), caja.centroV(), escala, ancho, alto);
        gc.save();
        gc.translate(x, y);
        forma(gc, anillos, 0, 0, t, relleno, borde, grosor);
        gc.restore();
    }
}
