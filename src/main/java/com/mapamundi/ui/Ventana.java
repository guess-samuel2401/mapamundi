package com.mapamundi.ui;

import com.mapamundi.i18n.Idioma;
import com.mapamundi.i18n.Textos;
import com.mapamundi.juego.Ajustes;
import com.mapamundi.juego.Logros;
import com.mapamundi.juego.Logros.Logro;
import com.mapamundi.juego.Nivel;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.PartidaClasica;
import com.mapamundi.juego.Puntajes;
import com.mapamundi.modelo.Atlas;
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

import java.util.List;
import java.util.Objects;
import java.util.Random;

/** La ventana del juego: carga el atlas y las preferencias, y cambia entre menú, partida y fin. */
public final class Ventana {

    private final Stage escenario;
    private final Contexto ctx;
    private final StackPane raiz = new StackPane();
    private Ajustes ajustes = Ajustes.porDefecto();
    private PantallaJuego juego;
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
     * Cada vez que se abre el programa: primero el idioma, después la configuración de la partida con el
     * tutorial encima, luego la pantalla de inicio de partida y, por fin, el juego.
     */
    private void elegirIdioma() {
        ctx.sonidos().musica(Sonidos.Musica.MENU);
        Idioma ultimo = Idioma.deCodigo(ctx.preferencias().idioma);
        cambiarA(new PantallaIdioma(ctx.atlas(), ultimo, idioma -> {
            Textos.usar(idioma);
            ctx.preferencias().idioma = idioma.codigo();
            ctx.preferencias().guardar();
            escenario.setFullScreenExitHint(Textos.t("ventana.salirPantallaCompleta"));
            configurar(true);
        }));
    }

    /** Paso 1: configurar la partida. */
    private void configurar(boolean conTutorial) {
        ctx.sonidos().musica(Sonidos.Musica.MENU);
        cambiarA(new PantallaConfiguracion(ctx, ajustes, elegidos -> {
            ajustes = elegidos;
            menu();
        }, () -> configurar(false), conTutorial));
    }

    /** Paso 2: la pantalla de inicio de partida (también a la que se vuelve al salir de una partida). */
    private void menu() {
        ctx.sonidos().musica(Sonidos.Musica.MENU);
        cambiarA(new PantallaInicio(ctx, ajustes, this::jugar, () -> configurar(false), this::menu));
    }

    private void jugar(Ajustes elegidos) {
        ajustes = elegidos;
        ctx.sonidos().musica(Sonidos.Musica.JUEGO);
        if (elegidos.clasico()) {
            // El juego tal como era en 2000.
            PartidaClasica partida = new PartidaClasica(elegidos, ctx.atlas().paises(), new Random());
            clasica = new PantallaClasica(partida, ctx, this::menu, p -> fin(PantallaFin.Resumen.de(p), List.of()));
            cambiarA(clasica);
            return;
        }
        Partida partida = new Partida(elegidos, Nivel.progresion(ctx.atlas()), Nivel.personalizado(ctx.atlas(), elegidos.continentes()), new Random());
        juego = new PantallaJuego(partida, ctx, this::menu, (p, insignias) -> fin(PantallaFin.Resumen.de(p), insignias));
        cambiarA(juego);
    }

    private void fin(PantallaFin.Resumen resumen, List<Logro> insignias) {
        ctx.sonidos().musica(Sonidos.Musica.MENU);
        // Con la tabla de récords llena no cabe en pantallas bajas: se puede desplazar.
        StackPane centrada = new StackPane(new PantallaFin(resumen, ctx.puntajes(), insignias, this::menu, () -> jugar(ajustes)));
        centrada.setAlignment(Pos.TOP_CENTER);
        ScrollPane desplazable = new ScrollPane(centrada);
        desplazable.setFitToWidth(true);
        cambiarA(desplazable);
    }

    private void cambiarA(Parent pantalla) {
        if (juego != null && pantalla != juego) {
            juego.desconectar();
            juego = null;
        }
        if (clasica != null && pantalla != clasica) {
            clasica.desconectar();
            clasica = null;
        }
        raiz.getChildren().setAll(pantalla);
    }
}
