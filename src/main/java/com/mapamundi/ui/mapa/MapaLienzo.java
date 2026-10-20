package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Forma;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Resultado;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.Fuentes;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Fase 2: el mapa en blanco del continente con la pieza que mueve el jugador.
 * Arrastrar la pieza la mueve; arrastrar el fondo desplaza el mapa; la rueda hace zoom.
 */
public final class MapaLienzo extends Region {

    /** Área mínima (px) para poder agarrar países diminutos como Andorra o Tuvalu. */
    private static final double AGARRE_MIN = 34;
    private static final double ZOOM_MAX = 12;

    private final Canvas canvas = new Canvas();
    /** Capa encima del mapa para la animación de acierto. */
    private final Canvas efectos = new Canvas();
    private final Celebracion celebracion = new Celebracion(efectos);
    private final Atlas atlas;
    private Partida partida;
    private Resultado celebrado;

    private com.mapamundi.geo.Region vistaActual;
    private double zoom = 1;
    private double centroU;
    private double centroV;

    private boolean arrastrandoPieza;
    private double inicioX, inicioY, inicioDespU, inicioDespV, inicioCentroU, inicioCentroV;

    public MapaLienzo(Atlas atlas) {
        this.atlas = atlas;
        getChildren().addAll(canvas, efectos);
        // Si la ronda termina antes de que acabe la animación, no seguir dibujando en una capa invisible.
        sceneProperty().addListener((o, antes, escena) -> {
            if (escena == null) celebracion.detener();
        });
        getStyleClass().add("mapa");
        setMinSize(200, 200);
        widthProperty().addListener(o -> dibujar());
        heightProperty().addListener(o -> dibujar());
        configurarRaton();
    }

    public void mostrar(Partida p) {
        this.partida = p;
        if (p.vista() != vistaActual) {
            vistaActual = p.vista();
            restablecerZoom();
        }
        dibujar();
        Resultado r = p.fase() == Partida.Fase.RESULTADO ? p.resultado() : null;
        if (r != null && r.acierto() && r != celebrado) {
            celebrado = r;
            celebracion.iniciar(p.actual(), p.vueltas(), r.puntos() + r.bonus(), this::transformacion);
        }
    }

    public void restablecerZoom() {
        if (vistaActual == null) return;
        zoom = 1;
        centroU = vistaActual.centroU();
        centroV = vistaActual.centroV();
        dibujar();
    }

    /** Cuántas unidades Mercator equivale un píxel de pantalla con el zoom actual. */
    public double unidadesPorPixel() {
        return 1 / transformacion().escala();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        efectos.setWidth(getWidth());
        efectos.setHeight(getHeight());
        // Esquinas redondeadas, como los paneles.
        Rectangle recorte = new Rectangle(getWidth(), getHeight());
        recorte.setArcWidth(28);
        recorte.setArcHeight(28);
        setClip(recorte);
    }

    private Transformacion transformacion() {
        Transformacion base = Transformacion.ajustada(vistaActual, Math.max(1, getWidth()), Math.max(1, getHeight()));
        return base.con(centroU, centroV, base.escala() * zoom);
    }

    // ------------------------------------------------------------------ dibujo

    public void dibujar() {
        if (partida == null || vistaActual == null || getWidth() < 2 || getHeight() < 2) return;
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Transformacion t = transformacion();
        double grosor = Math.min(1.4, 0.6 + zoom * 0.15);

        Pais actual = partida.actual();
        boolean terminada = partida.fase() == Partida.Fase.RESULTADO;
        // Mapa en blanco; los países ya ubicados en esta partida quedan pintados.
        Pintor.mapa(gc, t, atlas, p -> partida.ubicados().contains(p) && !(p == actual && !terminada) ? Pintor.colorDe(p) : null, grosor);

        Forma f = actual.forma();
        int vueltas = partida.vueltas();
        boolean acierto = terminada && partida.resultado().acierto();

        if (terminada && !acierto) {
            // Ubicación real del país, resaltada.
            gc.setEffect(new DropShadow(10, Pintor.SOL));
            Pintor.forma(gc, f.anillos(), vueltas, 0, t, Pintor.colorDe(actual), Pintor.BORDE, grosor + 0.6);
            gc.setEffect(null);
        }

        if (!acierto) dibujarPieza(gc, t, actual, f, vueltas + partida.despU(), partida.despV(), terminada);

        if (terminada && !acierto && partida.resultado().tieneDistancia()) dibujarMarcas(gc, t, f, vueltas);
        if (partida.enPausa()) {
            gc.setFill(Pintor.FONDO_OSCURO.deriveColor(0, 1, 1, 0.92));
            gc.fillRect(0, 0, t.ancho(), t.alto());
        }
    }

    private void dibujarPieza(GraphicsContext gc, Transformacion t, Pais pais, Forma f, double du, double dv, boolean terminada) {
        boolean forma = partida.ajustes().modo().muestraForma();
        gc.setGlobalAlpha(terminada ? 0.55 : 1);
        if (forma) {
            gc.setEffect(new DropShadow(4, 2, 3, Pintor.TINTA.deriveColor(0, 1, 1, 0.45)));
            Pintor.forma(gc, f.nucleo(), du, dv, t, Pintor.colorDe(pais), Pintor.BORDE, 1.2);
        } else {
            gc.setEffect(new DropShadow(6, Pintor.SOL));
            Pintor.forma(gc, f.nucleo(), du, dv, t, Pintor.TINTA.deriveColor(0, 1, 1, 0.85), null, 0);
        }
        gc.setEffect(null);
        gc.setGlobalAlpha(1);
        if (terminada) return;

        // Anillo amarillo para encontrar la pieza aunque sea diminuta, y el rótulo con su nombre.
        double[] caja = cajaPantalla(t, f, du, dv);
        double cx = (caja[0] + caja[2]) / 2, cy = (caja[1] + caja[3]) / 2;
        double radio = Math.max(AGARRE_MIN / 2, Math.hypot(caja[2] - caja[0], caja[3] - caja[1]) / 2) + 6;
        if (radio < 80) {
            gc.setStroke(Pintor.SOL);
            gc.setLineWidth(2.5);
            gc.setLineDashes(6, 4);
            gc.strokeOval(cx - radio, cy - radio, radio * 2, radio * 2);
            gc.setLineDashes();
        }
        String rotulo = rotulo(pais);
        if (rotulo != null) {
            Font fuente = Fuentes.texto(FontWeight.EXTRA_BOLD, 14);
            gc.setFont(fuente);
            Text medida = new Text(rotulo);
            medida.setFont(fuente);
            double ancho = medida.getLayoutBounds().getWidth() + 16;
            double y = Math.max(4, Math.min(caja[1], cy - radio) - 28);
            gc.setFill(Pintor.TINTA.deriveColor(0, 1, 1, 0.9));
            gc.fillRoundRect(cx - ancho / 2, y, ancho, 23, 10, 10);
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(rotulo, cx, y + 11.5);
        }
    }

    private String rotulo(Pais p) {
        return switch (partida.ajustes().modo()) {
            case DATOS -> null;
            case MAPA_CAPITAL, CAPITAL -> p.capital();
            default -> p.nombre();
        };
    }

    /** ● donde quedó la pieza, ★ donde iba, y una línea entre ambos. */
    private void dibujarMarcas(GraphicsContext gc, Transformacion t, Forma f, int vueltas) {
        double rx = t.x(f.anclaU() + vueltas), ry = t.y(f.anclaV());
        double gx = t.x(f.anclaU() + vueltas + partida.despU()), gy = t.y(f.anclaV() + partida.despV());
        gc.setStroke(Pintor.ROJO);
        gc.setLineWidth(2.5);
        gc.setLineDashes(8, 6);
        gc.strokeLine(gx, gy, rx, ry);
        gc.setLineDashes();
        gc.setFill(Pintor.TINTA);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.fillOval(gx - 7, gy - 7, 14, 14);
        gc.strokeOval(gx - 7, gy - 7, 14, 14);
        gc.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 30));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setStroke(Pintor.TINTA);
        gc.setLineWidth(1.5);
        gc.setFill(Pintor.SOL);
        gc.fillText("★", rx, ry);
        gc.strokeText("★", rx, ry);
    }

    private double[] cajaPantalla(Transformacion t, Forma f, double du, double dv) {
        com.mapamundi.geo.Region c = f.cajaNucleo();
        return new double[]{t.x(c.uMin() + du), t.y(c.vMin() + dv), t.x(c.uMax() + du), t.y(c.vMax() + dv)};
    }

    // ------------------------------------------------------------------ ratón

    private boolean sobrePieza(double x, double y) {
        if (partida == null || partida.fase() != Partida.Fase.UBICACION) return false;
        Transformacion t = transformacion();
        Forma f = partida.actual().forma();
        double[] c = cajaPantalla(t, f, partida.vueltas() + partida.despU(), partida.despV());
        double cx = (c[0] + c[2]) / 2, cy = (c[1] + c[3]) / 2;
        double mx = Math.max(AGARRE_MIN, c[2] - c[0]) / 2, my = Math.max(AGARRE_MIN, c[3] - c[1]) / 2;
        return Math.abs(x - cx) <= mx && Math.abs(y - cy) <= my;
    }

    private void configurarRaton() {
        setOnMouseMoved(e -> setCursor(sobrePieza(e.getX(), e.getY()) ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.DEFAULT));
        setOnMousePressed(e -> {
            if (partida == null) return;
            inicioX = e.getX();
            inicioY = e.getY();
            arrastrandoPieza = e.getButton() == MouseButton.PRIMARY && !partida.enPausa() && sobrePieza(e.getX(), e.getY());
            inicioDespU = partida.despU();
            inicioDespV = partida.despV();
            inicioCentroU = centroU;
            inicioCentroV = centroV;
            setCursor(arrastrandoPieza ? javafx.scene.Cursor.CLOSED_HAND : javafx.scene.Cursor.MOVE);
            requestFocus();
        });
        setOnMouseDragged(e -> {
            if (partida == null) return;
            double k = unidadesPorPixel();
            double dx = (e.getX() - inicioX) * k, dy = (e.getY() - inicioY) * k;
            if (arrastrandoPieza) {
                partida.colocar(inicioDespU + dx, inicioDespV + dy);
            } else {
                centroU = inicioCentroU - dx;
                centroV = inicioCentroV - dy;
                dibujar();
            }
        });
        setOnMouseReleased(e -> {
            arrastrandoPieza = false;
            setCursor(sobrePieza(e.getX(), e.getY()) ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.DEFAULT);
        });
        setOnScroll(e -> {
            if (vistaActual == null || e.getDeltaY() == 0) return;
            Transformacion antes = transformacion();
            double u = antes.u(e.getX()), v = antes.v(e.getY());
            zoom = Math.max(1, Math.min(ZOOM_MAX, zoom * (e.getDeltaY() > 0 ? 1.2 : 1 / 1.2)));
            // Mantener fijo el punto bajo el cursor.
            Transformacion despues = transformacion();
            centroU += u - despues.u(e.getX());
            centroV += v - despues.v(e.getY());
            if (zoom == 1) {
                centroU = vistaActual.centroU();
                centroV = vistaActual.centroV();
            }
            dibujar();
        });
    }
}
