package com.mapamundi.juego;

import com.mapamundi.geo.Forma;
import com.mapamundi.geo.Mercator;
import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Estado y reglas de una partida. No sabe nada de la interfaz: la pantalla llama a sus métodos
 * y se entera de los cambios con {@link #alCambiar} y de los sonidos con {@link #alSonar}.
 *
 * <p>La pieza se describe por su desplazamiento (en unidades Mercator) respecto a su lugar real:
 * (0, 0) es la posición correcta.
 */
public final class Partida {

    public static final int VIDAS = 5;
    public static final int INTENTOS_CONTINENTE = 5;
    public static final int PAISES_POR_NIVEL = 6;
    public static final int PUNTOS_CONTINENTE = 100;
    public static final int PUNTOS_UBICACION = 500;

    public enum Fase { CONTINENTE, UBICACION, RESULTADO, FIN }

    private final Ajustes ajustes;
    private final List<Nivel> progresion;
    private final Random azar;

    private Nivel nivel;
    private int indiceNivel;
    private final Deque<Pais> cola = new ArrayDeque<>();
    private final Set<Pais> ubicados = new LinkedHashSet<>();
    private int aciertosNivel;

    private Pais actual;
    private Fase fase = Fase.CONTINENTE;
    private int erroresContinente;
    private final Set<Continente> descartados = EnumSet.noneOf(Continente.class);
    private int vidas = VIDAS;
    private int puntaje;
    private int segundos;
    private boolean pausa;
    private Region vista;
    private int vueltas;
    private double despU;
    private double despV;
    private Resultado resultado;
    private String nivelSuperado;

    private Runnable alCambiar = () -> { };
    private Consumer<Sonido> alSonar = s -> { };

    public Partida(Ajustes ajustes, List<Nivel> progresion, Nivel personalizado, Random azar) {
        this.ajustes = ajustes;
        this.progresion = progresion;
        this.azar = azar;
        if (ajustes.personalizado()) {
            indiceNivel = -1;
            empezarNivel(personalizado);
        } else {
            indiceNivel = ajustes.nivelInicial();
            empezarNivel(progresion.get(indiceNivel));
        }
    }

    public void alCambiar(Runnable r) {
        alCambiar = r;
    }

    public void alSonar(Consumer<Sonido> c) {
        alSonar = c;
    }

    // ------------------------------------------------------------------ acciones

    /**
     * Fase 1: el jugador elige un continente. Repetir un continente ya descartado también cuenta
     * como error; si no, con 4 continentes equivocados nunca se llegaría a los 5 intentos.
     */
    public void elegirContinente(Continente c) {
        if (fase != Fase.CONTINENTE || pausa) return;
        if (c == actual.continente()) {
            puntaje += PUNTOS_CONTINENTE;
            segundos = ajustes.velocidad().segundos();
            fase = Fase.UBICACION;
            sonar(Sonido.CONTINENTE_CORRECTO);
        } else {
            descartados.add(c);
            erroresContinente++;
            if (erroresContinente >= INTENTOS_CONTINENTE) {
                terminarRonda(new Resultado(false, Resultado.Motivo.CONTINENTE, -1, 0, 0));
                return;
            }
            sonar(Sonido.CONTINENTE_EQUIVOCADO);
        }
        cambio();
    }

    /** Mueve la pieza (en unidades Mercator). */
    public void mover(double du, double dv) {
        colocar(despU + du, despV + dv);
    }

    /** Coloca la pieza con un desplazamiento absoluto respecto a su lugar real. */
    public void colocar(double du, double dv) {
        if (fase != Fase.UBICACION || pausa) return;
        // El centro de la pieza no puede salir del mapa visible.
        Forma f = actual.forma();
        double u = f.anclaU() + vueltas + du, v = f.anclaV() + dv;
        u = Math.max(vista.uMin(), Math.min(vista.uMax(), u));
        v = Math.max(vista.vMin(), Math.min(vista.vMax(), v));
        despU = u - f.anclaU() - vueltas;
        despV = v - f.anclaV();
        cambio();
    }

    public void confirmar() {
        if (fase != Fase.UBICACION || pausa) return;
        double km = distanciaKm();
        boolean acierto = km <= ajustes.velocidad().toleranciaKm();
        int bonus = acierto ? segundos * ajustes.velocidad().bonusPorSegundo() : 0;
        terminarRonda(new Resultado(acierto, acierto ? Resultado.Motivo.UBICADO : Resultado.Motivo.LEJOS, km,
                acierto ? PUNTOS_UBICACION : 0, bonus));
    }

    /** Un segundo del cronómetro. */
    public void tic() {
        if (fase != Fase.UBICACION || pausa) return;
        if (segundos > 1) {
            segundos--;
            cambio();
        } else {
            segundos = 0;
            terminarRonda(new Resultado(false, Resultado.Motivo.TIEMPO, distanciaKm(), 0, 0));
        }
    }

    public void alternarPausa() {
        if (fase == Fase.RESULTADO || fase == Fase.FIN) return;
        pausa = !pausa;
        cambio();
    }

    /** Tras ver el resultado: siguiente país, siguiente nivel o fin de la partida. */
    public void siguiente() {
        if (fase != Fase.RESULTADO) return;
        if (vidas <= 0) {
            fase = Fase.FIN;
        } else if (indiceNivel >= 0 && indiceNivel < progresion.size() - 1 && aciertosNivel >= PAISES_POR_NIVEL) {
            indiceNivel++;
            empezarNivel(progresion.get(indiceNivel));
            nivelSuperado = nivel.nombre();
            sonar(Sonido.NIVEL_SUPERADO);
        } else {
            nivelSuperado = null;
            siguientePais();
        }
        cambio();
    }

    // ------------------------------------------------------------------ internos

    private void empezarNivel(Nivel n) {
        nivel = n;
        aciertosNivel = 0;
        cola.clear();
        siguientePais();
    }

    private void siguientePais() {
        if (cola.isEmpty()) {
            List<Pais> pendientes = new ArrayList<>(nivel.paises().stream().filter(p -> !ubicados.contains(p)).toList());
            if (pendientes.isEmpty()) pendientes = new ArrayList<>(nivel.paises());
            Collections.shuffle(pendientes, azar);
            cola.addAll(pendientes);
        }
        actual = cola.poll();
        fase = Fase.CONTINENTE;
        erroresContinente = 0;
        descartados.clear();
        resultado = null;
        prepararMapa();
    }

    /** Elige el trozo de mapa de la ronda y una posición inicial al azar, lejos del lugar correcto. */
    private void prepararMapa() {
        Forma f = actual.forma();
        Region base = nivel.vistaPara(actual);
        vueltas = base.vueltasPara(f.anclaU());
        vista = base.union(f.cajaNucleo().desplazada(vueltas).ampliada(0.08));

        double minimo = Math.min(vista.ancho(), vista.alto()) * 0.3;
        double mejorU = 0, mejorV = 0, mejorD = -1;
        for (int i = 0; i < 60; i++) {
            double u = vista.uMin() + vista.ancho() * (0.1 + 0.8 * azar.nextDouble());
            double v = vista.vMin() + vista.alto() * (0.1 + 0.8 * azar.nextDouble());
            double du = u - f.anclaU() - vueltas, dv = v - f.anclaV();
            double d = Math.hypot(du, dv);
            if (d > mejorD) {
                mejorD = d;
                mejorU = du;
                mejorV = dv;
            }
            if (d >= minimo) break;
        }
        despU = mejorU;
        despV = mejorV;
    }

    private double distanciaKm() {
        Forma f = actual.forma();
        return Mercator.distanciaKmUV(f.anclaU(), f.anclaV(), f.anclaU() + despU, f.anclaV() + despV);
    }

    private void terminarRonda(Resultado r) {
        resultado = r;
        fase = Fase.RESULTADO;
        puntaje += r.puntos() + r.bonus();
        if (r.acierto()) {
            ubicados.add(actual);
            aciertosNivel++;
            despU = 0;
            despV = 0;
        } else {
            vidas--;
        }
        if (vidas <= 0) sonar(Sonido.FIN_PARTIDA);
        else if (r.acierto()) sonar(Sonido.UBICADO);
        else sonar(r.motivo() == Resultado.Motivo.TIEMPO ? Sonido.FIN_TIEMPO : Sonido.MAL_UBICADO);
        cambio();
    }

    private void sonar(Sonido s) {
        alSonar.accept(s);
    }

    private void cambio() {
        alCambiar.run();
    }

    // ------------------------------------------------------------------ consultas

    public Ajustes ajustes() { return ajustes; }
    public Nivel nivel() { return nivel; }
    /** Número de nivel en la progresión (desde 1), o 0 si los continentes son personalizados. */
    public int numeroNivel() { return indiceNivel + 1; }
    public boolean hayNivelSiguiente() { return indiceNivel >= 0 && indiceNivel < progresion.size() - 1; }
    public int aciertosNivel() { return aciertosNivel; }
    public Pais actual() { return actual; }
    public Fase fase() { return fase; }
    public int erroresContinente() { return erroresContinente; }
    /** Continentes ya elegidos por error con el país actual (se marcan con ✗). */
    public Set<Continente> descartados() { return Collections.unmodifiableSet(descartados); }
    public int vidas() { return vidas; }
    public int puntaje() { return puntaje; }
    public int segundos() { return segundos; }
    public boolean enPausa() { return pausa; }
    public Region vista() { return vista; }
    /** Vueltas al mundo (±1 en u) que se desplaza el país actual para caer dentro de la vista. */
    public int vueltas() { return vueltas; }
    public double despU() { return despU; }
    public double despV() { return despV; }
    public Resultado resultado() { return resultado; }
    public Set<Pais> ubicados() { return Collections.unmodifiableSet(ubicados); }
    public String nivelSuperado() { return nivelSuperado; }
}
