package com.mapamundi.juego;

import com.mapamundi.geo.Forma;
import com.mapamundi.geo.Mercator;
import com.mapamundi.geo.Region;
import com.mapamundi.i18n.Textos;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * El juego tal como era en el Mapamundi original (Visual FoxPro, 2000), recreado a partir de su código
 * (frmprincipal, frmafrica… y errores.prg):
 *
 * <ol>
 *   <li><b>Caída:</b> el país cae desde arriba un paso por tic; hay que elegir su continente antes de que
 *       llegue abajo. Equivocarse o dejarlo caer es un error, y el mismo país vuelve a caer.</li>
 *   <li><b>Mapa:</b> la pieza nunca se para: avanza sola (al principio hacia abajo), las flechas solo cambian
 *       su dirección y rebota en los bordes. Enter la suelta. Mientras, una barra de tiempo se llena.</li>
 * </ol>
 *
 * Hay 5 errores para toda la partida. Cada país bien ubicado suma 10, 20, 30, 40 o 50 puntos según el modo.
 * Cuando todos los países de un continente están ubicados, ese continente se completa ("Mapa completo") y
 * ya no salen más países suyos; completar todos los elegidos gana la partida.
 *
 * <p>El reloj lo pone la interfaz: llama a {@link #ticCaida}, {@link #ticMovimiento} y {@link #ticReloj}
 * con los intervalos de {@link #ritmo}.
 */
public final class PartidaClasica {

    public static final int ERRORES_MAX = 5;
    /** Pasos que da el país al caer: en el original, de 17 a 330 píxeles de 7 en 7. */
    public static final int PASOS_CAIDA = 45;
    /** Tics de la barra de tiempo del mapa: crecía de 2 en 2 píxeles hasta 504. */
    public static final int PASOS_RELOJ = 252;
    /** Paso de la pieza en el mapa, como fracción del ancho de la vista (5 px de un mapa de 740). */
    static final double PASO = 5.0 / 740;

    public enum Fase { CAIDA, UBICACION, RESULTADO, FIN }

    public enum Direccion {
        ARRIBA(0, -1), IZQUIERDA(-1, 0), ABAJO(0, 1), DERECHA(1, 0);

        final int du, dv;

        Direccion(int du, int dv) {
            this.du = du;
            this.dv = dv;
        }

        Direccion opuesta() {
            return values()[(ordinal() + 2) % 4];
        }
    }

    /** Milisegundos entre tics de cada reloj, según la velocidad (los Timer del original). */
    public record Ritmo(int caida, int movimiento, int reloj) {
        /** Ctrl + flecha: la pieza va rápido. */
        public static final int MOVIMIENTO_RAPIDO = 30;
    }

    public enum Motivo {
        UBICADO, LEJOS, TIEMPO_CAIDA, CONTINENTE_EQUIVOCADO, TIEMPO_MAPA;

        public String titulo() {
            return Textos.t("clasico.motivo." + name());
        }
    }

    /** Cómo terminó un intento. */
    public record Resultado(boolean acierto, Motivo motivo, double distanciaKm, int puntos, Continente completado) {
        public boolean tieneDistancia() {
            return distanciaKm >= 0;
        }
    }

    private final Ajustes ajustes;
    private final List<Pais> todos;
    private final Random azar;
    private final Set<Continente> enJuego;
    private final Set<Continente> completos = EnumSet.noneOf(Continente.class);
    private final Set<Pais> ubicados = new LinkedHashSet<>();

    private Pais actual;
    private Fase fase = Fase.CAIDA;
    private int errores;
    private int puntaje;
    private int caida;
    private int reloj;
    private boolean pausa;
    private Region vista;
    private int vueltas;
    private double despU;
    private double despV;
    private Direccion direccion = Direccion.ABAJO;
    private Resultado resultado;
    private boolean ganada;

    private Runnable alCambiar = () -> { };
    private Consumer<Sonido> alSonar = s -> { };

    /**
     * @param paises los países del atlas; se juega con los de los continentes elegidos en {@code ajustes}
     */
    public PartidaClasica(Ajustes ajustes, List<Pais> paises, Random azar) {
        this.ajustes = ajustes;
        this.azar = azar;
        this.enJuego = ajustes.continentes().isEmpty() ? EnumSet.allOf(Continente.class) : EnumSet.copyOf(ajustes.continentes());
        this.todos = paises.stream().filter(p -> enJuego.contains(p.continente())).toList();
        nuevoPais();
    }

    public void alCambiar(Runnable r) {
        alCambiar = r;
    }

    public void alSonar(Consumer<Sonido> c) {
        alSonar = c;
    }

    public static Ritmo ritmo(Velocidad v) {
        return switch (v) {
            case LENTA -> new Ritmo(700, 200, 400);
            case NORMAL -> new Ritmo(450, 130, 150);
            case RAPIDA -> new Ritmo(200, 100, 90);
        };
    }

    /** Puntos por país: 10 en "Mapa y nombre del país" … 50 en "Solo datos del país". */
    public static int puntosPorPais(Modo m) {
        return (m.ordinal() + 1) * 10;
    }

    // ------------------------------------------------------------------ fase 1: la caída

    public void ticCaida() {
        if (fase != Fase.CAIDA || pausa) return;
        caida++;
        if (caida >= PASOS_CAIDA) {
            caida = PASOS_CAIDA;
            fallar(new Resultado(false, Motivo.TIEMPO_CAIDA, -1, 0, null), Sonido.FIN_TIEMPO);
        } else {
            sonar(Sonido.TIC);
            cambio();
        }
    }

    /** Los continentes ya completos (o no elegidos) no se pueden elegir: en el original salían en gris. */
    public boolean elegible(Continente c) {
        return enJuego.contains(c) && !completos.contains(c);
    }

    public void elegirContinente(Continente c) {
        if (fase != Fase.CAIDA || pausa || !elegible(c)) return;
        if (c == actual.continente()) {
            sonar(Sonido.CONTINENTE_CORRECTO);
            fase = Fase.UBICACION;
            prepararMapa();
            cambio();
        } else {
            fallar(new Resultado(false, Motivo.CONTINENTE_EQUIVOCADO, -1, 0, null), Sonido.CONTINENTE_EQUIVOCADO);
        }
    }

    // ------------------------------------------------------------------ fase 2: el mapa

    /** Las flechas: la pieza sigue moviéndose, pero hacia otro lado. */
    public void cambiarDireccion(Direccion d) {
        if (fase != Fase.UBICACION || pausa) return;
        direccion = d;
        cambio();
    }

    /** Un paso de la pieza en su dirección; si se saldría del mapa, rebota. */
    public void ticMovimiento() {
        if (fase != Fase.UBICACION || pausa) return;
        double paso = vista.ancho() * PASO;
        Forma f = actual.forma();
        double u = f.anclaU() + vueltas + despU + direccion.du * paso;
        double v = f.anclaV() + despV + direccion.dv * paso;
        if (u < vista.uMin() || u > vista.uMax() || v < vista.vMin() || v > vista.vMax()) {
            direccion = direccion.opuesta();
        } else {
            despU += direccion.du * paso;
            despV += direccion.dv * paso;
        }
        cambio();
    }

    /** La barra de tiempo del mapa: cuando se llena, se acabó el tiempo. */
    public void ticReloj() {
        if (fase != Fase.UBICACION || pausa) return;
        reloj++;
        if (reloj >= PASOS_RELOJ) {
            reloj = PASOS_RELOJ;
            fallar(new Resultado(false, Motivo.TIEMPO_MAPA, distanciaKm(), 0, null), Sonido.FIN_TIEMPO);
        } else {
            if (reloj % Math.max(1, 400 / ritmo(ajustes.velocidad()).reloj()) == 0) sonar(Sonido.TIC);
            cambio();
        }
    }

    /**
     * Enter: se suelta la pieza. Vale si su centro cae dentro del país (como el recuadro del original)
     * o a menos del margen de la velocidad elegida (para los países diminutos).
     */
    public void confirmar() {
        if (fase != Fase.UBICACION || pausa) return;
        Forma f = actual.forma();
        double km = distanciaKm();
        boolean acierto = f.contiene(f.anclaU() + despU, f.anclaV() + despV) || km <= ajustes.velocidad().toleranciaKm();
        if (!acierto) {
            fallar(new Resultado(false, Motivo.LEJOS, km, 0, null), Sonido.MAL_UBICADO);
            return;
        }
        int puntos = puntosPorPais(ajustes.modo());
        puntaje += puntos;
        ubicados.add(actual);
        Continente c = actual.continente();
        Continente completado = null;
        if (todos.stream().filter(p -> p.continente() == c).allMatch(ubicados::contains)) {
            completos.add(c);
            completado = c;
        }
        ganada = completos.containsAll(enJuego);
        resultado = new Resultado(true, Motivo.UBICADO, km, puntos, completado);
        fase = Fase.RESULTADO;
        sonar(ganada || completado != null ? Sonido.NIVEL_SUPERADO : Sonido.UBICADO);
        cambio();
    }

    public void alternarPausa() {
        if (fase == Fase.RESULTADO || fase == Fase.FIN) return;
        pausa = !pausa;
        cambio();
    }

    /** Tras el mensaje: otro país (si acertó), el mismo otra vez (si falló) o el final. */
    public void siguiente() {
        if (fase != Fase.RESULTADO) return;
        if (terminada()) {
            fase = Fase.FIN;
        } else if (resultado.acierto()) {
            nuevoPais();
        } else {
            reiniciarCaida();
        }
        cambio();
    }

    // ------------------------------------------------------------------ internos

    private void fallar(Resultado r, Sonido sonido) {
        errores++;
        resultado = r;
        fase = Fase.RESULTADO;
        sonar(errores >= ERRORES_MAX ? Sonido.FIN_PARTIDA : sonido);
        cambio();
    }

    /** Un país al azar de los continentes que faltan, que todavía no se haya ubicado. */
    private void nuevoPais() {
        List<Pais> pendientes = new ArrayList<>(todos.stream()
                .filter(p -> !ubicados.contains(p) && !completos.contains(p.continente())).toList());
        Collections.shuffle(pendientes, azar);
        actual = pendientes.isEmpty() ? todos.getFirst() : pendientes.getFirst();
        reiniciarCaida();
    }

    private void reiniciarCaida() {
        fase = Fase.CAIDA;
        caida = 0;
        resultado = null;
    }

    /** El mapa del continente; la pieza empieza arriba y en el centro, bajando (como Image3 del original). */
    private void prepararMapa() {
        Forma f = actual.forma();
        Region base = actual.continente().vista();
        vueltas = base.vueltasPara(f.anclaU());
        vista = base.union(f.cajaNucleo().desplazada(vueltas).ampliada(0.05));
        double u = vista.centroU();
        double v = vista.vMin() + vista.alto() * 0.06;
        despU = u - f.anclaU() - vueltas;
        despV = v - f.anclaV();
        direccion = Direccion.ABAJO;
        reloj = 0;
    }

    private double distanciaKm() {
        Forma f = actual.forma();
        return Mercator.distanciaKmUV(f.anclaU(), f.anclaV(), f.anclaU() + despU, f.anclaV() + despV);
    }

    private void sonar(Sonido s) {
        alSonar.accept(s);
    }

    private void cambio() {
        alCambiar.run();
    }

    // ------------------------------------------------------------------ consultas

    public Ajustes ajustes() { return ajustes; }
    public Pais actual() { return actual; }
    public Fase fase() { return fase; }
    public int errores() { return errores; }
    public int puntaje() { return puntaje; }
    /** Cuánto ha caído el país, de 0 (arriba) a 1 (abajo). */
    public double caida() { return (double) caida / PASOS_CAIDA; }
    /** Cuánto se ha llenado la barra de tiempo del mapa, de 0 a 1. */
    public double reloj() { return (double) reloj / PASOS_RELOJ; }
    public boolean enPausa() { return pausa; }
    public Region vista() { return vista; }
    public int vueltas() { return vueltas; }
    public double despU() { return despU; }
    public double despV() { return despV; }
    public Direccion direccion() { return direccion; }
    public Resultado resultado() { return resultado; }
    public Set<Pais> ubicados() { return Collections.unmodifiableSet(ubicados); }
    public Set<Continente> enJuego() { return Collections.unmodifiableSet(enJuego); }
    public Set<Continente> completos() { return Collections.unmodifiableSet(completos); }
    /** Países de los continentes elegidos (para el avance). */
    public int totalPaises() { return todos.size(); }
    /** Sin errores disponibles o con todos los continentes completos. */
    public boolean terminada() { return errores >= ERRORES_MAX || ganada; }
    public boolean ganada() { return ganada; }
}
