package com.mapamundi.ui.mapa;

import com.mapamundi.geo.Forma;
import com.mapamundi.modelo.Pais;
import com.mapamundi.ui.Fuentes;
import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Animación al acertar la ubicación de un país: el país da un salto con brillo, salen dos ondas,
 * una lluvia de confeti y estrellas, y los puntos ganados suben flotando. Se dibuja en su propio
 * Canvas, encima del mapa, para no redibujar el mapa en cada fotograma.
 */
final class Celebracion {

    private static final double DURACION = 1.8;        // segundos
    private static final double DURACION_SALTO = 0.45;
    private static final double GRAVEDAD = 700;         // px/s²
    private static final int PARTICULAS = 46;
    private static final double SUBIDA = 60;            // px que suben los puntos
    private static final double MARGEN_TEXTO = 34;      // distancia mínima del texto al borde del mapa
    private static final Color[] CONFETI = {
            Pintor.SOL, Pintor.CORAL, Color.web("#f4a99b"), Color.web("#a9d38c"),
            Color.web("#eda7c9"), Color.web("#c4b0e6"), Color.WHITE,
    };

    private record Particula(double angulo, double velocidad, Color color, double tamano, double giro, boolean estrella) {
    }

    private final Canvas capa;
    private final Random azar = new Random();
    private AnimationTimer reloj;

    Celebracion(Canvas capa) {
        this.capa = capa;
        capa.setMouseTransparent(true);
    }

    /**
     * @param transformacion la transformación actual del mapa (se consulta en cada fotograma, por si hay zoom)
     * @param vueltas        desplazamiento del país para caer dentro de la vista (antimeridiano)
     */
    void iniciar(Pais pais, int vueltas, String puntos, Supplier<Transformacion> transformacion) {
        detener();
        List<Particula> particulas = new ArrayList<>();
        for (int i = 0; i < PARTICULAS; i++) {
            // Más hacia arriba que hacia abajo, como un estallido.
            double angulo = -Math.PI / 2 + (azar.nextDouble() - 0.5) * Math.PI * 1.7;
            particulas.add(new Particula(angulo, 220 + azar.nextDouble() * 260, CONFETI[azar.nextInt(CONFETI.length)],
                    5 + azar.nextDouble() * 5, (azar.nextDouble() - 0.5) * 14, azar.nextDouble() < 0.25));
        }
        long[] inicio = {-1};
        reloj = new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (inicio[0] < 0) inicio[0] = ahora;
                double t = (ahora - inicio[0]) / 1e9;
                if (t > DURACION) {
                    detener();
                    return;
                }
                dibujar(t, pais, vueltas, puntos, particulas, transformacion.get());
            }
        };
        reloj.start();
    }

    void detener() {
        if (reloj != null) reloj.stop();
        reloj = null;
        capa.getGraphicsContext2D().clearRect(0, 0, capa.getWidth(), capa.getHeight());
    }

    /** @param puntos el texto que sube flotando ("+860", "+1720 ×2"), o null si la ronda no sumó */
    private void dibujar(double t, Pais pais, int vueltas, String puntos, List<Particula> particulas, Transformacion tr) {
        GraphicsContext gc = capa.getGraphicsContext2D();
        gc.clearRect(0, 0, capa.getWidth(), capa.getHeight());
        Forma f = pais.forma();
        double cx = tr.x(f.anclaU() + vueltas), cy = tr.y(f.anclaV());
        com.mapamundi.geo.Region c = f.cajaNucleo();
        double radio = Math.max(18, Math.hypot(c.ancho(), c.alto()) * tr.escala() / 2);

        // 1. El país salta (crece y vuelve) con un brillo amarillo.
        if (t < DURACION_SALTO) {
            double fase = t / DURACION_SALTO;
            double escala = 1 + 0.22 * Math.sin(Math.PI * fase);
            gc.save();
            gc.translate(cx, cy);
            gc.scale(escala, escala);
            gc.translate(-cx, -cy);
            gc.setEffect(new DropShadow(18 * (1 - fase) + 4, Pintor.SOL));
            Pintor.forma(gc, f.nucleo(), vueltas, 0, tr, Pintor.colorDe(pais), Pintor.BORDE, 1.4);
            gc.setEffect(null);
            gc.restore();
        }

        // 2. Dos ondas que se expanden desde el país.
        for (int i = 0; i < 2; i++) {
            double ti = (t - i * 0.18) / 0.75;
            if (ti <= 0 || ti >= 1) continue;
            double r = radio + 90 * easeOut(ti);
            gc.setGlobalAlpha(1 - ti);
            gc.setStroke(i == 0 ? Pintor.SOL : Color.WHITE);
            gc.setLineWidth(5 * (1 - ti) + 1);
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }
        gc.setGlobalAlpha(1);

        // 3. Confeti y estrellas: salen disparados y caen con la gravedad.
        double ts = Math.max(0, t - 0.05);
        for (Particula p : particulas) {
            double x = cx + Math.cos(p.angulo()) * p.velocidad() * ts;
            double y = cy + Math.sin(p.angulo()) * p.velocidad() * ts + GRAVEDAD * ts * ts / 2;
            double vida = ts / (DURACION - 0.05);
            gc.setGlobalAlpha(vida < 0.6 ? 1 : Math.max(0, 1 - (vida - 0.6) / 0.4));
            gc.save();
            gc.translate(x, y);
            gc.rotate(Math.toDegrees(p.giro() * ts));
            gc.setFill(p.color());
            if (p.estrella()) {
                estrella(gc, p.tamano() * 1.3);
            } else {
                gc.fillRect(-p.tamano() / 2, -p.tamano() / 4, p.tamano(), p.tamano() / 2);
            }
            gc.restore();
        }
        gc.setGlobalAlpha(1);

        // 4. Los puntos ganados suben flotando (encima del país; si no caben, debajo).
        if (puntos != null) {
            double pop = t < 0.3 ? 0.6 + 0.55 * easeOut(t / 0.3) : 1.15 - 0.15 * Math.min(1, (t - 0.3) / 0.2);
            double arriba = cy - radio - 18;
            double inicioY = arriba - SUBIDA >= MARGEN_TEXTO ? arriba
                    : Math.min(capa.getHeight() - MARGEN_TEXTO, cy + radio + SUBIDA + 24);
            double y = inicioY - SUBIDA * easeOut(Math.min(1, t / DURACION));
            double x = Math.max(110, Math.min(capa.getWidth() - 110, cx));
            gc.setGlobalAlpha(t < 1.1 ? 1 : Math.max(0, 1 - (t - 1.1) / (DURACION - 1.1)));
            gc.save();
            gc.translate(x, y);
            gc.scale(pop, pop);
            gc.setFont(Fuentes.titulo(36));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setLineWidth(6);
            gc.setStroke(Pintor.TINTA);
            gc.strokeText(puntos, 0, 0);
            gc.setFill(Pintor.SOL);
            gc.fillText(puntos, 0, 0);
            gc.restore();
            gc.setGlobalAlpha(1);
        }
    }

    private static void estrella(GraphicsContext gc, double r) {
        double[] xs = new double[10], ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double radio = i % 2 == 0 ? r : r * 0.45;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            xs[i] = Math.cos(a) * radio;
            ys[i] = Math.sin(a) * radio;
        }
        gc.fillPolygon(xs, ys, 10);
    }

    private static double easeOut(double x) {
        return 1 - Math.pow(1 - x, 3);
    }
}
