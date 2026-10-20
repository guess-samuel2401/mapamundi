package com.mapamundi.ui;

import com.mapamundi.juego.Ajustes;
import com.mapamundi.juego.Nivel;
import com.mapamundi.juego.Partida;
import com.mapamundi.juego.Puntajes;
import com.mapamundi.modelo.Atlas;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.Random;

/** La ventana del juego: carga el atlas y cambia entre menú, partida y fin. */
public final class Ventana {

    private final Stage escenario;
    private final Atlas atlas = Atlas.cargar();
    private final Sonidos sonidos = new Sonidos();
    private final Puntajes puntajes = Puntajes.delUsuario();
    private final StackPane raiz = new StackPane();
    private Ajustes ajustes = Ajustes.porDefecto();
    private PantallaJuego juego;

    public Ventana(Stage escenario) {
        this.escenario = escenario;
    }

    public void mostrar() {
        // En pantallas pequeñas (p. ej. 1366×768) la ventana se abre maximizada para que quepa el mapa.
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        boolean cabe = pantalla.getWidth() >= 1300 && pantalla.getHeight() >= 840;
        Fuentes.cargar();
        Scene escena = new Scene(raiz, Math.min(1280, pantalla.getWidth()), Math.min(800, pantalla.getHeight()));
        escenario.setMaximized(!cabe);
        escena.getStylesheets().add(Objects.requireNonNull(Ventana.class.getResource("/com/mapamundi/estilos.css")).toExternalForm());
        escenario.setTitle("Mapamundi");
        escenario.getIcons().add(new Image(Objects.requireNonNull(Ventana.class.getResourceAsStream("/com/mapamundi/icono.png"))));
        escenario.setMinWidth(Math.min(980, pantalla.getWidth()));
        escenario.setMinHeight(Math.min(660, pantalla.getHeight()));
        escenario.setScene(escena);
        menu();
        escenario.show();
    }

    private void menu() {
        cambiarA(new PantallaMenu(atlas, ajustes, puntajes, this::jugar));
    }

    private void jugar(Ajustes elegidos) {
        ajustes = elegidos;
        Partida partida = new Partida(elegidos, Nivel.progresion(atlas), Nivel.personalizado(atlas, elegidos.continentes()), new Random());
        juego = new PantallaJuego(partida, atlas, sonidos, this::menu, this::fin);
        cambiarA(juego);
    }

    private void fin(Partida partida) {
        cambiarA(new PantallaFin(partida, puntajes, this::menu, () -> jugar(ajustes)));
    }

    private void cambiarA(Parent pantalla) {
        if (juego != null && pantalla != juego) {
            juego.desconectar();
            juego = null;
        }
        raiz.getChildren().setAll(pantalla);
    }
}
