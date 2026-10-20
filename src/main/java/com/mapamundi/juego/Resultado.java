package com.mapamundi.juego;

/**
 * Cómo terminó una ronda.
 *
 * @param distanciaKm distancia entre la pieza y su lugar real, o -1 si no se llegó a ubicar
 */
public record Resultado(boolean acierto, Motivo motivo, double distanciaKm, int puntos, int bonus) {

    public enum Motivo {
        UBICADO("¡Ubicación correcta!"),
        LEJOS("Ubicación incorrecta"),
        TIEMPO("¡Se terminó el tiempo!"),
        CONTINENTE("Agotaste los intentos del continente");

        private final String titulo;

        Motivo(String titulo) {
            this.titulo = titulo;
        }

        public String titulo() {
            return titulo;
        }
    }

    public boolean tieneDistancia() {
        return distanciaKm >= 0;
    }
}
