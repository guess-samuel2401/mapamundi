package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Forma;
import com.mapamundi.i18n.Textos;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Pista;
import com.mapamundi.juego.Resultado;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.Fuentes;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fase 2: el mapa en blanco del continente con la pieza que mueve el jugador.
 * Arrastrar la pieza la mueve; arrastrar el fondo desplaza el mapa; la rueda hace zoom.
 */
public final class MapaLienzo extends Region {

    /** Área mínima (px) para poder agarrar países diminutos como Andorra o Tuvalu. */
    private static final double AGARRE_MIN = 34;
    /** Tamaño en pantalla de la figura que hace de pieza en los modos sin forma. */
    private static final double LADO_FIGURA = 26;
    private static final double ZOOM_MAX = 12;
    /** "Frío o caliente": de rojo (en el margen de acierto) a azul (a 12 márgenes o más). */
    private static final Color[] TEMPERATURA = {
            Color.web("#e3263b"), Color.web("#ff8a3d"), Color.web("#f2c230"), Color.web("#6fb3e8"), Color.web("#2f6fd6"),
    };
    private static final double MARGENES_HELADO = 12;

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
            String texto = r.total() > 0 ? "+" + r.total() + (r.multiplicador() > 1 ? "  ×" + r.multiplicador() : "") : null;
            celebracion.iniciar(p.actual(), p.vueltas(), texto, this::transformacion);
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
        // Mapa en blanco; los países ya ubicados en esta partida quedan pintados, y los vecinos si se pidió esa pista.
        Set<Pais> vecinos = partida.pistas().contains(Pista.VECINOS) ? new HashSet<>(atlas.vecinos(actual)) : Set.of();
        Pintor.mapa(gc, t, atlas, p -> vecinos.contains(p) || partida.ubicados().contains(p) && !(p == actual && !terminada)
                ? Pintor.colorDe(p) : null, grosor);

        Forma f = actual.forma();
        int vueltas = partida.vueltas();
        boolean acierto = terminada && partida.resultado().acierto();
        boolean zona = !terminada && partida.pistas().contains(Pista.ZONA);

        if (zona) dibujarZona(gc, t, vueltas, true);

        if (terminada && !acierto) {
            // Ubicación real del país, resaltada.
            gc.setEffect(new DropShadow(10, Pintor.SOL));
            Pintor.forma(gc, f.anillos(), vueltas, 0, t, Pintor.colorDe(actual), Pintor.BORDE, grosor + 0.6);
            gc.setEffect(null);
        }

        if (!acierto) dibujarPieza(gc, t, actual, f, vueltas + partida.despU(), partida.despV(), terminada);
        // El borde de la zona y los nombres de los vecinos van encima de la pieza, para que no los tape.
        if (zona) dibujarZona(gc, t, vueltas, false);
        if (!vecinos.isEmpty()) nombrarVecinos(gc, t, vecinos);
        if (!terminada) dibujarRotulos(gc, t, actual, f, vueltas + partida.despU(), partida.despV());

        if (terminada && !acierto && partida.resultado().tieneDistancia()) dibujarMarcas(gc, t, f, vueltas);
        if (partida.enPausa()) {
            gc.setFill(Pintor.FONDO_OSCURO.deriveColor(0, 1, 1, 0.92));
            gc.fillRect(0, 0, t.ancho(), t.alto());
        }
    }

    private void dibujarPieza(GraphicsContext gc, Transformacion t, Pais pais, Forma f, double du, double dv, boolean terminada) {
        boolean forma = partida.ajustes().modo().muestraForma();
        Color calor = !terminada && partida.ajustes().frioCaliente() ? colorTemperatura(margenes()) : null;
        double px = t.x(f.anclaU() + du), py = t.y(f.anclaV() + dv);
        gc.save();
        // Modo experto: la pieza se dibuja girada sobre su punto de referencia.
        if (partida.giro() != 0) {
            gc.translate(px, py);
            gc.rotate(partida.giro());
            gc.translate(-px, -py);
        }
        gc.setGlobalAlpha(terminada ? 0.55 : 1);
        if (calor != null) gc.setEffect(new DropShadow(BlurType.GAUSSIAN, calor, 20, 0.55, 0, 0));
        if (forma) {
            if (calor == null) gc.setEffect(new DropShadow(4, 2, 3, Pintor.TINTA.deriveColor(0, 1, 1, 0.45)));
            Pintor.forma(gc, f.nucleo(), du, dv, t, Pintor.colorDe(pais), Pintor.BORDE, 1.2);
        } else {
            if (calor == null) gc.setEffect(new DropShadow(6, Pintor.SOL));
            Pintor.figura(gc, pais, px, py, LADO_FIGURA, Pintor.TINTA.deriveColor(0, 1, 1, 0.9), Pintor.SOL, 2);
        }
        gc.restore();
        gc.setEffect(null);
        gc.setGlobalAlpha(1);
    }

    /**
     * Anillo para encontrar la pieza aunque sea diminuta, el rótulo con su nombre y, con "frío o caliente",
     * la temperatura justo debajo del nombre.
     */
    private void dibujarRotulos(GraphicsContext gc, Transformacion t, Pais pais, Forma f, double du, double dv) {
        Color calor = partida.ajustes().frioCaliente() ? colorTemperatura(margenes()) : null;
        double[] caja = cajaPantalla(t, f, du, dv);
        double cx = (caja[0] + caja[2]) / 2, cy = (caja[1] + caja[3]) / 2;
        double radio = Math.max(AGARRE_MIN / 2, Math.hypot(caja[2] - caja[0], caja[3] - caja[1]) / 2) + 6;
        if (radio < 80) {
            gc.setStroke(calor != null ? calor : Pintor.SOL);
            gc.setLineWidth(2.5);
            gc.setLineDashes(6, 4);
            gc.strokeOval(cx - radio, cy - radio, radio * 2, radio * 2);
            gc.setLineDashes();
        }
        String rotulo = rotulo(pais);
        double y = Math.max(4, Math.min(caja[1], cy - radio) - (calor != null ? 54 : 28));
        if (rotulo != null) {
            etiqueta(gc, rotulo, cx, y, Pintor.TINTA.deriveColor(0, 1, 1, 0.9), Color.WHITE, false);
            y += 26;
        }
        if (calor != null) etiqueta(gc, "♨ " + textoTemperatura(margenes()), cx, y, calor, Pintor.TINTA, true);
    }

    /** Un rótulo redondeado centrado en x, con su borde superior en y. */
    private static void etiqueta(GraphicsContext gc, String texto, double x, double y, Color fondo, Color tinta, boolean contorno) {
        Font fuente = Fuentes.texto(FontWeight.EXTRA_BOLD, 14);
        gc.setFont(fuente);
        Text medida = new Text(texto);
        medida.setFont(fuente);
        double ancho = medida.getLayoutBounds().getWidth() + 16;
        gc.setFill(fondo);
        gc.fillRoundRect(x - ancho / 2, y, ancho, 23, 10, 10);
        if (contorno) {
            gc.setStroke(Pintor.TINTA);
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(x - ancho / 2, y, ancho, 23, 10, 10);
        }
        gc.setFill(tinta);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(texto, x, y + 11.5);
    }

    /** Distancia actual de la pieza, medida en márgenes de acierto (1 = justo en el límite). */
    private double margenes() {
        return partida.distanciaActualKm() / partida.ajustes().velocidad().toleranciaKm();
    }

    static Color colorTemperatura(double margenes) {
        double s = margenes <= 1 ? 0 : Math.min(1, Math.log(margenes) / Math.log(MARGENES_HELADO));
        double pos = s * (TEMPERATURA.length - 1);
        int i = Math.min(TEMPERATURA.length - 2, (int) pos);
        return TEMPERATURA[i].interpolate(TEMPERATURA[i + 1], pos - i);
    }

    static String textoTemperatura(double margenes) {
        if (margenes <= 1) return Textos.t("temperatura.quemas");
        if (margenes <= 2.5) return Textos.t("temperatura.caliente");
        if (margenes <= 5) return Textos.t("temperatura.templado");
        if (margenes <= 10) return Textos.t("temperatura.frio");
        return Textos.t("temperatura.helado");
    }

    /** Pista "zona": un círculo amarillo translúcido que contiene el lugar del país (relleno o borde). */
    private void dibujarZona(GraphicsContext gc, Transformacion t, int vueltas, boolean relleno) {
        double x = t.x(partida.zonaU() + vueltas), y = t.y(partida.zonaV()), r = partida.zonaRadio() * t.escala();
        if (relleno) {
            gc.setFill(Pintor.SOL.deriveColor(0, 1, 1, 0.22));
            gc.fillOval(x - r, y - r, r * 2, r * 2);
            return;
        }
        gc.setStroke(Pintor.TINTA);
        gc.setLineWidth(4.5);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);
        gc.setStroke(Pintor.SOL);
        gc.setLineWidth(2.5);
        gc.setLineDashes(10, 7);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);
        gc.setLineDashes();
    }

    /** Pista "vecinos": el nombre de cada país vecino sobre su centro. */
    private void nombrarVecinos(GraphicsContext gc, Transformacion t, Set<Pais> vecinos) {
        gc.setFont(Fuentes.texto(FontWeight.EXTRA_BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setLineWidth(3.5);
        com.mapamundi.geo.Region visible = t.visible();
        for (Pais p : vecinos) {
            Forma f = p.forma();
            double x = t.x(f.anclaU() + Pintor.vueltas(visible, f)), y = t.y(f.anclaV());
            gc.setStroke(Color.WHITE);
            gc.strokeText(p.nombre(), x, y);
            gc.setFill(Pintor.TINTA);
            gc.fillText(p.nombre(), x, y);
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

    /** Caja de la pieza en pantalla {xMin, yMin, xMax, yMax}, contando su giro. */
    private double[] cajaPantalla(Transformacion t, Forma f, double du, double dv) {
        if (!partida.ajustes().modo().muestraForma()) {
            double px = t.x(f.anclaU() + du), py = t.y(f.anclaV() + dv), m = LADO_FIGURA * 0.65;
            return new double[]{px - m, py - m, px + m, py + m};
        }
        com.mapamundi.geo.Region c = f.cajaNucleo();
        double x0 = t.x(c.uMin() + du), y0 = t.y(c.vMin() + dv), x1 = t.x(c.uMax() + du), y1 = t.y(c.vMax() + dv);
        int giro = partida.giro();
        if (giro == 0) return new double[]{x0, y0, x1, y1};
        double px = t.x(f.anclaU() + du), py = t.y(f.anclaV() + dv);
        double cos = Math.cos(Math.toRadians(giro)), sin = Math.sin(Math.toRadians(giro));
        double[] r = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] esquina : List.of(new double[]{x0, y0}, new double[]{x1, y0}, new double[]{x0, y1}, new double[]{x1, y1})) {
            double dx = esquina[0] - px, dy = esquina[1] - py;
            double x = px + dx * cos - dy * sin, y = py + dx * sin + dy * cos;
            r[0] = Math.min(r[0], x);
            r[1] = Math.min(r[1], y);
            r[2] = Math.max(r[2], x);
            r[3] = Math.max(r[3], y);
        }
        return r;
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
        // Clic derecho sobre la pieza: girarla (modo experto).
        setOnMouseClicked(e -> {
            if (partida != null && e.getButton() == MouseButton.SECONDARY && sobrePieza(e.getX(), e.getY())) {
                partida.girar(e.isShiftDown() ? -1 : 1);
            }
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
