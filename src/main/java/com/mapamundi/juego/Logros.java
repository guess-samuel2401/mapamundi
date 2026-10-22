package com.mapamundi.juego;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapamundi.i18n.Textos;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.modelo.Pais;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Logros e insignias. Algunos se ganan en una sola jugada (puntería, rapidez, racha) y otros
 * se acumulan entre partidas (países distintos ubicados, continentes completos). Se guardan en
 * ~/.mapamundi/logros.json; la variable de entorno MAPAMUNDI_LOGROS permite usar otro archivo.
 */
public final class Logros {

    public enum Categoria { BRONCE, PLATA, ORO }

    public enum Logro {
        PRIMER_PAIS("✔", Categoria.BRONCE, 1),
        RACHA_5("5×", Categoria.BRONCE, 5),
        RACHA_10("10×", Categoria.PLATA, 10),
        RACHA_20("20×", Categoria.ORO, 20),
        PUNTERIA("◎", Categoria.PLATA, 1),
        RELAMPAGO("⚡", Categoria.BRONCE, 1),
        VELOCISTA("»", Categoria.PLATA, 1),
        CAPITALES("♛", Categoria.PLATA, 10),
        DETECTIVE("?", Categoria.ORO, 10),
        SIN_RASGUNO("♥", Categoria.PLATA, 1),
        CONTRARRELOJ("◷", Categoria.PLATA, 10),
        GIROS("⟳", Categoria.ORO, 10),
        NIVEL_3("3", Categoria.BRONCE, 1),
        NIVEL_MUNDO("7", Categoria.ORO, 1),
        PUNTOS_5000("5K", Categoria.PLATA, 5000),
        PUNTOS_15000("15K", Categoria.ORO, 15000),
        VIAJERO("25", Categoria.BRONCE, 25),
        TROTAMUNDOS("100", Categoria.PLATA, 100),
        DUENO_DEL_MUNDO("197", Categoria.ORO, 197),
        SUDAMERICA("SA", Categoria.PLATA, 0),
        AMERICA("AM", Categoria.ORO, 0),
        EUROPA("EU", Categoria.ORO, 0),
        AFRICA("AF", Categoria.ORO, 0),
        ASIA("AS", Categoria.ORO, 0),
        OCEANIA("OC", Categoria.PLATA, 0);

        private final String icono;
        private final Categoria categoria;
        private final int objetivo;

        Logro(String icono, Categoria categoria, int objetivo) {
            this.icono = icono;
            this.categoria = categoria;
            this.objetivo = objetivo;
        }

        public String nombre() { return Textos.t("logro." + name() + ".nombre"); }
        public String descripcion() { return Textos.t("logro." + name() + ".descripcion"); }
        public String icono() { return icono; }
        public Categoria categoria() { return categoria; }
    }

    /** Lo que se guarda en disco. */
    private static final class Estado {
        Map<String, String> desbloqueados = new LinkedHashMap<>();
        Set<String> ubicados = new HashSet<>();
        int aciertos;
        int aciertosCapital;
        int aciertosDatos;
        int aciertosGirados;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path archivo;
    private final Map<Logro, Set<String>> paisesDeColeccion = new LinkedHashMap<>();
    private Estado estado;
    private Resultado revisado;
    private String nivelRevisado;

    public Logros(Path archivo, Atlas atlas) {
        this.archivo = archivo;
        Predicate<Pais> sudamerica = Pais::sudamerica;
        coleccion(Logro.SUDAMERICA, atlas, sudamerica);
        coleccion(Logro.AMERICA, atlas, p -> p.continente() == Continente.AMERICA);
        coleccion(Logro.EUROPA, atlas, p -> p.continente() == Continente.EUROPA);
        coleccion(Logro.AFRICA, atlas, p -> p.continente() == Continente.AFRICA);
        coleccion(Logro.ASIA, atlas, p -> p.continente() == Continente.ASIA);
        coleccion(Logro.OCEANIA, atlas, p -> p.continente() == Continente.OCEANIA);
        estado = leer();
    }

    public static Logros delUsuario(Atlas atlas) {
        String otro = System.getenv("MAPAMUNDI_LOGROS");
        Path archivo = otro != null && !otro.isBlank() ? Path.of(otro)
                : Path.of(System.getProperty("user.home"), ".mapamundi", "logros.json");
        return new Logros(archivo, atlas);
    }

    private void coleccion(Logro logro, Atlas atlas, Predicate<Pais> filtro) {
        Set<String> isos = new HashSet<>();
        for (Pais p : atlas.paises()) if (filtro.test(p)) isos.add(p.iso());
        paisesDeColeccion.put(logro, isos);
    }

    // ------------------------------------------------------------------ evaluación

    /**
     * Revisa la partida tras cada cambio (la llama la pantalla de juego). Devuelve los logros que se
     * acaban de desbloquear, en orden, para mostrarlos. Cada ronda y cada nivel se cuentan una sola vez.
     */
    public List<Logro> revisar(Partida p) {
        List<Logro> nuevos = new ArrayList<>();
        Resultado r = p.fase() == Partida.Fase.RESULTADO ? p.resultado() : null;
        if (r != null && r != revisado) {
            revisado = r;
            if (r.acierto()) acierto(p, r, nuevos);
            comprobar(Logro.PUNTOS_5000, p.puntaje() >= 5000, nuevos);
            comprobar(Logro.PUNTOS_15000, p.puntaje() >= 15000, nuevos);
        }
        String nivel = p.nivelSuperado();
        if (nivel != null && !nivel.equals(nivelRevisado)) {
            nivelRevisado = nivel;
            comprobar(Logro.SIN_RASGUNO, p.nivelSinPerderVidas(), nuevos);
            comprobar(Logro.NIVEL_3, p.numeroNivel() >= 3, nuevos);
            comprobar(Logro.NIVEL_MUNDO, p.numeroNivel() >= Nivel.progresionTotal(), nuevos);
        }
        if (!nuevos.isEmpty() || r != null) guardar();
        return nuevos;
    }

    private void acierto(Partida p, Resultado r, List<Logro> nuevos) {
        estado.aciertos++;
        estado.ubicados.add(p.actual().iso());
        Modo modo = p.ajustes().modo();
        if (modo.porCapital()) estado.aciertosCapital++;
        if (modo == Modo.DATOS) estado.aciertosDatos++;
        if (p.ajustes().piezaGirada()) estado.aciertosGirados++;
        Velocidad v = p.ajustes().velocidad();
        double tiempoRestante = r.bonus() / (double) (v.segundos() * v.bonusPorSegundo());

        comprobar(Logro.PRIMER_PAIS, estado.aciertos >= 1, nuevos);
        comprobar(Logro.RACHA_5, p.racha() >= 5, nuevos);
        comprobar(Logro.RACHA_10, p.racha() >= 10, nuevos);
        comprobar(Logro.RACHA_20, p.racha() >= 20, nuevos);
        comprobar(Logro.PUNTERIA, r.distanciaKm() < 25, nuevos);
        comprobar(Logro.RELAMPAGO, tiempoRestante >= 0.8, nuevos);
        // Antes era "acertar en velocidad Rápida"; ya no se elige la velocidad, así que ahora es acertar en menos
        // de 3 segundos (el cronómetro baja de segundo en segundo: como mucho 2 tics). En contrarreloj no hay bonus.
        int segundosUsados = v.segundos() - r.bonus() / v.bonusPorSegundo();
        comprobar(Logro.VELOCISTA, r.bonus() > 0 && segundosUsados <= 2, nuevos);
        comprobar(Logro.CAPITALES, estado.aciertosCapital >= Logro.CAPITALES.objetivo, nuevos);
        comprobar(Logro.DETECTIVE, estado.aciertosDatos >= Logro.DETECTIVE.objetivo, nuevos);
        comprobar(Logro.CONTRARRELOJ, p.ajustes().contrarreloj() && p.aciertosPartida() >= Logro.CONTRARRELOJ.objetivo, nuevos);
        comprobar(Logro.GIROS, estado.aciertosGirados >= Logro.GIROS.objetivo, nuevos);
        comprobar(Logro.VIAJERO, estado.ubicados.size() >= Logro.VIAJERO.objetivo, nuevos);
        comprobar(Logro.TROTAMUNDOS, estado.ubicados.size() >= Logro.TROTAMUNDOS.objetivo, nuevos);
        comprobar(Logro.DUENO_DEL_MUNDO, estado.ubicados.size() >= Logro.DUENO_DEL_MUNDO.objetivo, nuevos);
        paisesDeColeccion.forEach((logro, isos) -> comprobar(logro, estado.ubicados.containsAll(isos), nuevos));
    }

    private void comprobar(Logro l, boolean conseguido, List<Logro> nuevos) {
        if (conseguido && !estado.desbloqueados.containsKey(l.name())) {
            estado.desbloqueados.put(l.name(), LocalDate.now().toString());
            nuevos.add(l);
        }
    }

    // ------------------------------------------------------------------ consultas

    public boolean desbloqueado(Logro l) {
        return estado.desbloqueados.containsKey(l.name());
    }

    /** Fecha (AAAA-MM-DD) en que se consiguió, o null. */
    public String fecha(Logro l) {
        return estado.desbloqueados.get(l.name());
    }

    public int cantidadDesbloqueados() {
        return (int) java.util.Arrays.stream(Logro.values()).filter(this::desbloqueado).count();
    }

    /** Avance hacia un logro acumulativo, como {actual, objetivo}; null si se gana en una sola jugada. */
    public int[] progreso(Logro l) {
        return switch (l) {
            case CAPITALES -> new int[]{estado.aciertosCapital, l.objetivo};
            case DETECTIVE -> new int[]{estado.aciertosDatos, l.objetivo};
            case GIROS -> new int[]{estado.aciertosGirados, l.objetivo};
            case VIAJERO, TROTAMUNDOS, DUENO_DEL_MUNDO -> new int[]{estado.ubicados.size(), l.objetivo};
            case SUDAMERICA, AMERICA, EUROPA, AFRICA, ASIA, OCEANIA -> {
                Set<String> isos = paisesDeColeccion.get(l);
                int hechos = (int) isos.stream().filter(estado.ubicados::contains).count();
                yield new int[]{hechos, isos.size()};
            }
            default -> null;
        };
    }

    // ------------------------------------------------------------------ disco

    private Estado leer() {
        try {
            if (Files.exists(archivo)) {
                Estado e = GSON.fromJson(Files.readString(archivo, StandardCharsets.UTF_8), Estado.class);
                if (e != null) {
                    if (e.desbloqueados == null) e.desbloqueados = new LinkedHashMap<>();
                    if (e.ubicados == null) e.ubicados = new HashSet<>();
                    return e;
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Logros ilegibles, se empieza de cero: " + e.getMessage());
        }
        return new Estado();
    }

    private void guardar() {
        try {
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, GSON.toJson(estado), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudieron guardar los logros: " + e.getMessage());
        }
    }
}
