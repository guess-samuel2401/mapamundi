package com.mapamundi.ui;

import com.mapamundi.juego.Logros;
import com.mapamundi.juego.Puntajes;
import com.mapamundi.modelo.Atlas;
import javafx.stage.Stage;

/** Lo que comparten todas las pantallas: datos, sonido, récords, logros, preferencias y la ventana. */
record Contexto(Atlas atlas, Sonidos sonidos, Puntajes puntajes, Logros logros, Preferencias preferencias, Stage escenario) {
}
