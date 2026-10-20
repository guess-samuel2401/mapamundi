package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Atlas;
import javafx.scene.canvas.Canvas;
import javafx.scene.shape.Rectangle;

/** Mapa del mundo en colores que llena su espacio y se vuelve a dibujar al cambiar de tamaño (portada del menú). */
public final class MapaDecorativo extends javafx.scene.layout.Region {

    private static final Region MUNDO = Region.deGrados(-168, 192, -50, 76);

    private final Canvas canvas = new Canvas();
    private final Atlas atlas;

    public MapaDecorativo(Atlas atlas) {
        this.atlas = atlas;
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
        Pintor.mapa(canvas.getGraphicsContext2D(), Transformacion.cubriendo(MUNDO, w, h), atlas, Pintor::colorDe, 0.6);
        Rectangle recorte = new Rectangle(w, h);
        recorte.setArcWidth(36);
        recorte.setArcHeight(36);
        setClip(recorte);
    }
}
