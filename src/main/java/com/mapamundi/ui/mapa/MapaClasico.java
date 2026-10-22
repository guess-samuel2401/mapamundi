package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Forma;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.Fuentes;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * El mapa del continente en el modo clásico: los países ya ubicados en color y la pieza, que se mueve sola.
 * En los modos sin forma, la pieza es un círculo de color (como la "sombra" ShpPais del original), que no delata
 * la silueta del país.
 */
public final class MapaClasico extends Region {

    private static final double LADO_FIGURA = 22;

    private final Canvas canvas = new Canvas();
    private final Atlas atlas;
    private PartidaClasica partida;

    public MapaClasico(Atlas atlas) {
        this.atlas = atlas;
        getChildren().add(canvas);
        getStyleClass().add("mapa");
        setMinSize(200, 200);
        widthProperty().addListener(o -> dibujar());
        heightProperty().addListener(o -> dibujar());
    }

    public void mostrar(PartidaClasica p) {
        this.partida = p;
        dibujar();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        Rectangle recorte = new Rectangle(getWidth(), getHeight());
        recorte.setArcWidth(28);
        recorte.setArcHeight(28);
        setClip(recorte);
    }

    public void dibujar() {
        if (partida == null || partida.vista() == null || getWidth() < 2 || getHeight() < 2) return;
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Transformacion t = Transformacion.ajustada(partida.vista(), getWidth(), getHeight());
        Pais actual = partida.actual();
        boolean resultado = partida.fase() == PartidaClasica.Fase.RESULTADO;
        boolean acierto = resultado && partida.resultado().acierto();

        Pintor.mapa(gc, t, atlas, p -> partida.ubicados().contains(p) ? Pintor.colorDe(p) : null, 0.9);

        Forma f = actual.forma();
        int vueltas = partida.vueltas();
        if (resultado && !acierto) {
            // Dónde estaba de verdad.
            gc.setEffect(new DropShadow(10, Pintor.SOL));
            Pintor.forma(gc, f.anillos(), vueltas, 0, t, Pintor.colorDe(actual), Pintor.BORDE, 1.6);
            gc.setEffect(null);
        }
        if (!acierto) dibujarPieza(gc, t, actual, f, vueltas + partida.despU(), partida.despV(), resultado);
        if (resultado && !acierto && partida.resultado().tieneDistancia()) {
            double rx = t.x(f.anclaU() + vueltas), ry = t.y(f.anclaV());
            double gx = t.x(f.anclaU() + vueltas + partida.despU()), gy = t.y(f.anclaV() + partida.despV());
            gc.setStroke(Pintor.ROJO);
            gc.setLineWidth(2.5);
            gc.setLineDashes(8, 6);
            gc.strokeLine(gx, gy, rx, ry);
            gc.setLineDashes();
            gc.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 28));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFill(Pintor.SOL);
            gc.setStroke(Pintor.TINTA);
            gc.setLineWidth(1.5);
            gc.fillText("★", rx, ry);
            gc.strokeText("★", rx, ry);
        }
        // En pausa el mapa sigue a la vista: el menú de pausa se muestra encima.
    }

    private void dibujarPieza(GraphicsContext gc, Transformacion t, Pais pais, Forma f, double du, double dv, boolean quieta) {
        double cx = t.x(f.anclaU() + du), cy = t.y(f.anclaV() + dv);
        double arriba;
        gc.setGlobalAlpha(quieta ? 0.6 : 1);
        if (partida.ajustes().modo().muestraForma()) {
            gc.setEffect(new DropShadow(4, 2, 3, Pintor.TINTA.deriveColor(0, 1, 1, 0.45)));
            Pintor.forma(gc, f.nucleo(), du, dv, t, Pintor.colorDe(pais), Pintor.BORDE, 1.3);
            gc.setEffect(null);
            arriba = Math.min(cy - 10, t.y(f.cajaNucleo().vMin() + dv));
        } else {
            Pintor.figura(gc, pais, cx, cy, LADO_FIGURA, Pintor.TINTA, 2);
            arriba = cy - LADO_FIGURA;
        }
        gc.setGlobalAlpha(1);
        if (quieta) return;
        String rotulo = switch (partida.ajustes().modo()) {
            case DATOS -> null;
            case MAPA_CAPITAL, CAPITAL -> pais.capital();
            default -> pais.nombre();
        };
        if (rotulo != null) {
            Font fuente = Fuentes.texto(FontWeight.EXTRA_BOLD, 14);
            gc.setFont(fuente);
            Text medida = new Text(rotulo);
            medida.setFont(fuente);
            double ancho = medida.getLayoutBounds().getWidth() + 16;
            double y = Math.max(4, arriba - 30);
            gc.setFill(Pintor.TINTA.deriveColor(0, 1, 1, 0.9));
            gc.fillRoundRect(cx - ancho / 2, y, ancho, 23, 10, 10);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(rotulo, cx, y + 11.5);
        }
    }
}
