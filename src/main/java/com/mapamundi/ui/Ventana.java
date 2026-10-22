package com.mapamundi.ui;

import com.mapamundi.i18n.Idioma;
import com.mapamundi.i18n.Textos;
import com.mapamundi.juego.Ajustes;
import com.mapamundi.juego.Logros;
import com.mapamundi.juego.Modo;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.juego.Puntajes;
import com.mapamundi.juego.Velocidad;
import com.mapamundi.modelo.Atlas;
import com.mapamundi.modelo.Continente;
import com.mapamundi.ui.mapa.Pintor;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** La ventana del juego: carga el atlas y las preferencias, y cambia entre menú, partida y fin. */
public final class Ventana {

    private final Stage escenario;
    private final Contexto ctx;
    private final StackPane raiz = new StackPane();
    private Set<Continente> zona = EnumSet.allOf(Continente.class);
    private Modo modo = Modo.MAPA_PAIS;
    private PantallaClasica clasica;

    public Ventana(Stage escenario) {
        this.escenario = escenario;
        Atlas atlas = Atlas.cargar();
        Preferencias preferencias = Preferencias.cargar();
        ctx = new Contexto(atlas, new Sonidos(preferencias), Puntajes.delUsuario(), Logros.delUsuario(atlas), preferencias, escenario);
        FichaPais.fechaDatos(atlas.datosActualizados());
        Pintor.daltonico(preferencias.daltonico);
    }

    public void mostrar() {
        // En pantallas pequeñas (p. ej. 1366×768) la ventana se abre maximizada para que quepa el mapa.
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        boolean cabe = pantalla.getWidth() >= 1300 && pantalla.getHeight() >= 840;
        Fuentes.cargar();
        Scene escena = new Scene(raiz, Math.min(1280, pantalla.getWidth()), Math.min(800, pantalla.getHeight()));
        escenario.setMaximized(!cabe);
        escena.getStylesheets().add(Objects.requireNonNull(Ventana.class.getResource("/com/mapamundi/estilos.css")).toExternalForm());
        if (ctx.preferencias().daltonico) raiz.getStyleClass().add("daltonico");
        escenario.setTitle("Mapamundi");
        for (String icono : new String[]{"icono-32.png", "icono.png", "icono-256.png"}) {
            escenario.getIcons().add(new Image(Objects.requireNonNull(Ventana.class.getResourceAsStream("/com/mapamundi/" + icono))));
        }
        escenario.setMinWidth(Math.min(980, pantalla.getWidth()));
        escenario.setMinHeight(Math.min(660, pantalla.getHeight()));
        escenario.setFullScreenExitHint(Textos.t("ventana.salirPantallaCompleta"));
        escena.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F11) {
                escenario.setFullScreen(!escenario.isFullScreen());
                ctx.preferencias().pantallaCompleta = escenario.isFullScreen();
                ctx.preferencias().guardar();
                e.consume();
            }
        });
        escenario.setScene(escena);
        elegirIdioma();
        escenario.show();
        if (ctx.preferencias().pantallaCompleta) escenario.setFullScreen(true);
    }

    /**
     * Versión sencilla: cada vez que se abre el programa, el idioma; después, con qué continente se juega y
     * qué reto se quiere, y a jugar. El tutorial no sale solo: está siempre a mano en la pestaña
     * «Aprende a jugar».
     */
    private void elegirIdioma() {
        ctx.sonidos().musica(Sonidos.Musica.MENU);
        Idioma ultimo = Idioma.deCodigo(ctx.preferencias().idioma);
        cambiarA(new PantallaIdioma(ctx, ultimo, idioma -> {
            Textos.usar(idioma);
            ctx.preferencias().idioma = idioma.codigo();
            ctx.preferencias().guardar();
            escenario.setFullScreenExitHint(Textos.t("ventana.salirPantallaCompleta"));
            continentes();
        }));
    }

    /** Pregunta 1: ¿con qué continente quieres jugar? (también a donde se vuelve al salir de una partida) */
    private void continentes() {
        ctx.sonidos().musica(Sonidos.Musica.MENU);
        PantallaEleccion[] p = {null};
        p[0] = PantallaEleccion.continentes(ctx, zona, elegida -> {
            zona = elegida;
            retos();
        }, this::continentes, () -> aprender(p[0]));
        cambiarA(p[0]);
    }

    /** Pregunta 2: ¿qué reto quieres? Al elegirlo empieza la partida. */
    private void retos() {
        PantallaEleccion[] p = {null};
        p[0] = PantallaEleccion.retos(ctx, zona, modo, elegido -> {
            modo = elegido;
            jugar();
        }, this::continentes, this::retos, () -> aprender(p[0]));
        cambiarA(p[0]);
    }

    private void aprender(StackPane sobre) {
        Tutorial.mostrarSencillo(sobre, ctx.atlas(), sobre::requestFocus);
    }

    /** El juego tal como era en 2000: el país cae, la pieza no se detiene y hay 5 errores. */
    private void jugar() {
        ctx.sonidos().musica(Sonidos.Musica.JUEGO);
        Ajustes ajustes = new Ajustes(Velocidad.NORMAL, modo, -1, zona, false, false, false, false, true);
        PartidaClasica partida = new PartidaClasica(ajustes, ctx.atlas().paises(), new Random());
        clasica = new PantallaClasica(partida, ctx, this::continentes, pc -> fin(PantallaFin.Resumen.de(pc)));
        cambiarA(clasica);
    }

    private void fin(PantallaFin.Resumen resumen) {
        ctx.sonidos().musica(Sonidos.Musica.MENU);
        // Con la tabla de récords llena no cabe en pantallas bajas: se puede desplazar.
        StackPane centrada = new StackPane(new PantallaFin(resumen, ctx.puntajes(), List.of(), this::continentes, this::jugar));
        centrada.setAlignment(Pos.TOP_CENTER);
        ScrollPane desplazable = new ScrollPane(centrada);
        desplazable.setFitToWidth(true);
        cambiarA(desplazable);
    }

    private void cambiarA(Parent pantalla) {
        if (clasica != null && pantalla != clasica) {
            clasica.desconectar();
            clasica = null;
        }
        raiz.getChildren().setAll(pantalla);
    }
}
