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

    public static final Color AGUA = Color.web("#65b1b1");
    public static final Color TIERRA = Color.WHITE;
    public static final Color OTRA_TIERRA = Color.web("#e4e4dc");
    public static final Color BORDE = Color.web("#1b1b1b");
    public static final Color GRIS_SILUETA = Color.web("#e5e5e5");

    /** Colores vivos como los de los mapas del juego original; el índice viene de MAPCOLOR7. */
    private static final Color[] PALETA = {
            Color.web("#ff7f7f"), Color.web("#ffff80"), Color.web("#b7f26a"), Color.web("#7fd0ff"),
            Color.web("#ff99cc"), Color.web("#ffb366"), Color.web("#c7a6ff"),
    };

    private Pintor() {
    }

    public static Color colorDe(Pais p) {
        return PALETA[Math.floorMod(p.color() - 1, PALETA.length)];
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
