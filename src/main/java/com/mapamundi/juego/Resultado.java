package com.mapamundi.juego;

import com.mapamundi.i18n.Textos;

/**
 * Cómo terminó una ronda.
 *
 * @param distanciaKm   distancia entre la pieza y su lugar real, o -1 si no se llegó a ubicar
 * @param multiplicador combo por la racha de aciertos (×1, ×2, ×3)
 * @param penalizacion  puntos que costaron las pistas de la ronda
 */
public record Resultado(boolean acierto, Motivo motivo, double distanciaKm, int puntos, int bonus, int multiplicador,
                        int penalizacion) {

    public enum Motivo {
        UBICADO, LEJOS, GIRADA, TIEMPO, CONTINENTE, FIN_CONTRARRELOJ;

        public String titulo() {
            return Textos.t("motivo." + name());
        }
    }

    /** Sin combo ni pistas. */
    public Resultado(boolean acierto, Motivo motivo, double distanciaKm, int puntos, int bonus) {
        this(acierto, motivo, distanciaKm, puntos, bonus, 1, 0);
    }

    public boolean tieneDistancia() {
        return distanciaKm >= 0;
    }

    /** Lo que suma (o resta) la ronda al puntaje. */
    public int total() {
        return (puntos + bonus) * multiplicador - penalizacion;
    }
}
