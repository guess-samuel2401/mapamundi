package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Pais;
import javafx.scene.canvas.Canvas;
import javafx.scene.shape.Rectangle;

import java.util.function.Predicate;

/**
 * Mapa del mundo en colores que llena su espacio y se vuelve a dibujar al cambiar de tamaño. En la pantalla de
 * inicio de partida, solo los países que se van a jugar van en color y el resto en gris.
 */
public final class MapaDecorativo extends javafx.scene.layout.Region {

    private static final Region MUNDO = Region.deGrados(-168, 192, -50, 76);

    private final Canvas canvas = new Canvas();
    private final Atlas atlas;
    private final Predicate<Pais> resaltar;

    public MapaDecorativo(Atlas atlas) {
        this(atlas, p -> true);
    }

    public MapaDecorativo(Atlas atlas, Predicate<Pais> resaltar) {
        this.atlas = atlas;
        this.resaltar = resaltar;
        getChildren().add(canvas);
        setMinSize(0, 0);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth(), h = getHeight();
        if (w < 2 || h < 2) return;
        if (canvas.getWidth() == w && canvas.getHeight() == h) return;
        canvas.setWidth(w);
        canvas.setHeight(h);
        Pintor.mapa(canvas.getGraphicsContext2D(), Transformacion.cubriendo(MUNDO, w, h), atlas,
                p -> resaltar.test(p) ? Pintor.colorDe(p) : Pintor.RESTO_DEL_MUNDO, 0.6);
        Rectangle recorte = new Rectangle(w, h);
        recorte.setArcWidth(36);
        recorte.setArcHeight(36);
        setClip(recorte);
    }
}
