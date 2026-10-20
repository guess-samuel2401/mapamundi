package com.mapamundi;

import com.mapamundi.ui.Ventana;
import javafx.application.Application;
import javafx.stage.Stage;

/** Mapamundi 2.0: el juego de geografía del año 2000, ahora en Java con fronteras reales. */
public class App extends Application {

    @Override
    public void start(Stage escenario) {
        new Ventana(escenario).mostrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
