package com.mapamundi.juego;

import com.mapamundi.geo.Forma;
import com.mapamundi.geo.Mercator;
import com.mapamundi.geo.Region;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
 * (0, 0) es la posición correcta. En el modo experto, además, por su giro en grados (0 = derecha).
 */
public final class Partida {

    public static final int VIDAS = 5;
    public static final int INTENTOS_CONTINENTE = 5;
    public static final int PAISES_POR_NIVEL = 6;
    public static final int PUNTOS_CONTINENTE = 100;
    public static final int PUNTOS_UBICACION = 500;
    /** Premio extra por acertar con la pieza girada (modo experto). */
    public static final int PUNTOS_GIRO = 250;
    /** Cada pulsación de girar mueve la pieza este ángulo. */
    public static final int PASO_GIRO = 15;
    /** Duración de una partida contrarreloj, y segundos que se ganan con cada acierto. */
    public static final int SEGUNDOS_CONTRARRELOJ = 90;
    public static final int SEGUNDOS_POR_ACIERTO = 5;
    /** El círculo de la pista "zona" mide este múltiplo del margen de acierto. */
    static final double RADIO_ZONA_EN_MARGENES = 4;

    public enum Fase { CONTINENTE, UBICACION, RESULTADO, FIN }

    private final Ajustes ajustes;
    private final List<Nivel> progresion;
    private final Random azar;

    private Nivel nivel;
    private int indiceNivel;
    private final Deque<Pais> cola = new ArrayDeque<>();
    private final Set<Pais> ubicados = new LinkedHashSet<>();
    private int aciertosNivel;
    private int fallosNivel;
    private int aciertosPartida;

    private Pais actual;
    private Fase fase = Fase.CONTINENTE;
    private int erroresContinente;
    private final Set<Continente> descartados = EnumSet.noneOf(Continente.class);
    private int vidas = VIDAS;
    private int puntaje;
    private int segundos;
    private int segundosPartida = SEGUNDOS_CONTRARRELOJ;
    private boolean tiempoAgotado;
    private boolean pausa;
    private Region vista;
    private int vueltas;
    private double despU;
    private double despV;
    private int giro;
    private Resultado resultado;
    private String nivelSuperado;
    private int racha;
    private int mejorRacha;
    private boolean nivelSinPerderVidas;

    private final Set<Pista> pistas = EnumSet.noneOf(Pista.class);
    private int penalizacion;
    private double zonaU;
    private double zonaV;
    private double zonaRadio;

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

    /** Combo según los aciertos seguidos: ×2 desde el 4.º y ×3 desde el 8.º. */
    public static int multiplicador(int racha) {
        return racha >= 8 ? 3 : racha >= 4 ? 2 : 1;
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
                terminarRonda(new Resultado(false, Resultado.Motivo.CONTINENTE, -1, 0, 0, 1, penalizacion));
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

    /** Modo experto: gira la pieza {@code pasos} veces {@link #PASO_GIRO} grados (positivo = sentido horario). */
    public void girar(int pasos) {
        if (fase != Fase.UBICACION || pausa || !ajustes.piezaGirada()) return;
        giro = normalizar(giro + pasos * PASO_GIRO);
        cambio();
    }

    public void confirmar() {
        if (fase != Fase.UBICACION || pausa) return;
        double km = distanciaKm();
        boolean cerca = km <= ajustes.velocidad().toleranciaKm();
        boolean acierto = cerca && giro == 0;
        // En contrarreloj no hay cronómetro por ronda, así que tampoco bonus de tiempo.
        int bonus = acierto && !ajustes.contrarreloj() ? segundos * ajustes.velocidad().bonusPorSegundo() : 0;
        int puntos = acierto ? PUNTOS_UBICACION + (ajustes.piezaGirada() ? PUNTOS_GIRO : 0) : 0;
        Resultado.Motivo motivo = acierto ? Resultado.Motivo.UBICADO : cerca ? Resultado.Motivo.GIRADA : Resultado.Motivo.LEJOS;
        terminarRonda(new Resultado(acierto, motivo, km, puntos, bonus, acierto ? multiplicador(racha + 1) : 1, penalizacion));
    }

    /** Un segundo del cronómetro: el de la ronda o, en contrarreloj, el de toda la partida. */
    public void tic() {
        if (pausa) return;
        if (ajustes.contrarreloj()) {
            if (fase != Fase.CONTINENTE && fase != Fase.UBICACION) return;
            if (segundosPartida > 1) {
                segundosPartida--;
                cambio();
            } else {
                segundosPartida = 0;
                tiempoAgotado = true;
                double km = fase == Fase.UBICACION ? distanciaKm() : -1;
                terminarRonda(new Resultado(false, Resultado.Motivo.FIN_CONTRARRELOJ, km, 0, 0, 1, penalizacion));
            }
            return;
        }
        if (fase != Fase.UBICACION) return;
        if (segundos > 1) {
            segundos--;
            cambio();
        } else {
            segundos = 0;
            terminarRonda(new Resultado(false, Resultado.Motivo.TIEMPO, distanciaKm(), 0, 0, 1, penalizacion));
        }
    }

    public void alternarPausa() {
        if (fase == Fase.RESULTADO || fase == Fase.FIN) return;
        pausa = !pausa;
        cambio();
    }

    /** Si ahora se puede pedir esa pista: en su fase, una vez por ronda y (para descartar) con algo que tachar. */
    public boolean puedePedir(Pista p) {
        if (pausa || pistas.contains(p) || fase != p.fase()) return false;
        return p != Pista.DESCARTAR || !continentesDescartables().isEmpty();
    }

    /** Pide una pista: su costo se descontará de los puntos de esta ronda. */
    public void pedirPista(Pista p) {
        if (!puedePedir(p)) return;
        pistas.add(p);
        penalizacion += p.costo();
        switch (p) {
            case DESCARTAR -> {
                List<Continente> opciones = continentesDescartables();
                Collections.shuffle(opciones, azar);
                descartados.addAll(opciones.subList(0, Math.min(2, opciones.size())));
            }
            case ZONA -> prepararZona();
            case VECINOS -> { } // los vecinos los calcula y pinta la interfaz con el atlas
        }
        cambio();
    }

    /** Tras ver el resultado: siguiente país, siguiente nivel o fin de la partida. */
    public void siguiente() {
        if (fase != Fase.RESULTADO) return;
        if (terminada()) {
            fase = Fase.FIN;
        } else if (indiceNivel >= 0 && indiceNivel < progresion.size() - 1 && aciertosNivel >= PAISES_POR_NIVEL) {
            nivelSinPerderVidas = fallosNivel == 0;
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
        fallosNivel = 0;
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
        pistas.clear();
        penalizacion = 0;
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
        // Modo experto: entre 45° y 180° hacia cualquier lado, siempre en pasos de 15°.
        giro = ajustes.piezaGirada() ? normalizar((3 + azar.nextInt(10)) * PASO_GIRO * (azar.nextBoolean() ? 1 : -1)) : 0;
    }

    /**
     * Círculo con el país dentro, pero no en el centro (si no, la pista daría la respuesta). Mide varios
     * márgenes de acierto, y al menos un poco más que el propio país (si no, en Brasil o Rusia no se vería).
     */
    private void prepararZona() {
        Forma f = actual.forma();
        double kmPorUnidad = 2 * Math.PI * Mercator.RADIO_TIERRA_KM * Math.cos(Math.toRadians(Mercator.lat(f.anclaV())));
        double tamano = Math.hypot(f.cajaNucleo().ancho(), f.cajaNucleo().alto()) / 2;
        zonaRadio = Math.max(RADIO_ZONA_EN_MARGENES * ajustes.velocidad().toleranciaKm() / kmPorUnidad, tamano * 1.4);
        double angulo = azar.nextDouble() * 2 * Math.PI;
        double d = zonaRadio * 0.6 * Math.sqrt(azar.nextDouble());
        zonaU = f.anclaU() + Math.cos(angulo) * d;
        zonaV = f.anclaV() + Math.sin(angulo) * d;
    }

    private List<Continente> continentesDescartables() {
        return new ArrayList<>(Arrays.stream(Continente.values())
                .filter(c -> c != actual.continente() && !descartados.contains(c)).toList());
    }

    private static int normalizar(int grados) {
        int g = Math.floorMod(grados, 360);
        return g > 180 ? g - 360 : g;
    }

    private double distanciaKm() {
        Forma f = actual.forma();
        return Mercator.distanciaKmUV(f.anclaU(), f.anclaV(), f.anclaU() + despU, f.anclaV() + despV);
    }

    private void terminarRonda(Resultado r) {
        resultado = r;
        fase = Fase.RESULTADO;
        puntaje = Math.max(0, puntaje + r.total());
        if (r.acierto()) {
            ubicados.add(actual);
            aciertosNivel++;
            aciertosPartida++;
            racha++;
            mejorRacha = Math.max(mejorRacha, racha);
            despU = 0;
            despV = 0;
            if (ajustes.contrarreloj()) segundosPartida += SEGUNDOS_POR_ACIERTO;
        } else {
            if (!ajustes.contrarreloj()) vidas--;
            fallosNivel++;
            racha = 0;
        }
        if (terminada()) sonar(Sonido.FIN_PARTIDA);
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
    /** Países bien ubicados en esta partida (contando repeticiones). */
    public int aciertosPartida() { return aciertosPartida; }
    public Pais actual() { return actual; }
    public Fase fase() { return fase; }
    public int erroresContinente() { return erroresContinente; }
    /** Continentes ya elegidos por error, o tachados con la pista, con el país actual (se marcan con ✗). */
    public Set<Continente> descartados() { return Collections.unmodifiableSet(descartados); }
    public int vidas() { return vidas; }
    public int puntaje() { return puntaje; }
    /** Segundos que quedan en la ronda (en contrarreloj no se usa: ver {@link #segundosPartida}). */
    public int segundos() { return segundos; }
    /** Contrarreloj: segundos que quedan de partida. */
    public int segundosPartida() { return segundosPartida; }
    /** Sin corazones o, en contrarreloj, sin tiempo: tras este resultado viene el final. */
    public boolean terminada() { return vidas <= 0 || tiempoAgotado; }
    public boolean enPausa() { return pausa; }
    public Region vista() { return vista; }
    /** Vueltas al mundo (±1 en u) que se desplaza el país actual para caer dentro de la vista. */
    public int vueltas() { return vueltas; }
    public double despU() { return despU; }
    public double despV() { return despV; }
    /** Giro de la pieza en grados, entre -165 y 180 (0 = derecha). Siempre 0 fuera del modo experto. */
    public int giro() { return giro; }
    /** Distancia en km de la pieza a su lugar (para el "frío o caliente"). */
    public double distanciaActualKm() { return distanciaKm(); }
    public Resultado resultado() { return resultado; }
    public Set<Pais> ubicados() { return Collections.unmodifiableSet(ubicados); }
    public String nivelSuperado() { return nivelSuperado; }
    /** Aciertos seguidos en esta partida (se reinicia al fallar). */
    public int racha() { return racha; }
    public int mejorRacha() { return mejorRacha; }
    /** Si el último nivel superado se completó sin fallar ningún país. */
    public boolean nivelSinPerderVidas() { return nivelSinPerderVidas; }
    /** Pistas pedidas en la ronda actual y lo que costarán. */
    public Set<Pista> pistas() { return Collections.unmodifiableSet(pistas); }
    public int penalizacion() { return penalizacion; }
    /** Pista "zona": centro (sin las vueltas al mundo) y radio del círculo, en unidades Mercator. */
    public double zonaU() { return zonaU; }
    public double zonaV() { return zonaV; }
    public double zonaRadio() { return zonaRadio; }
}
