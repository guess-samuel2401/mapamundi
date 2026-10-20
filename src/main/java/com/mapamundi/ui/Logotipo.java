package com.mapamundi.ui;

import com.mapamundi.ui.mapa.Pintor;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;

/** El título "Mapamundi" de la portada: letras crema, contorno azul marino y sombra coral, como una pegatina. */
final class Logotipo {

    private Logotipo() {
    }

    static Label crear(String texto) {
        Label l = FichaPais.etiqueta(texto, "titulo-portada");
        // CSS solo admite un efecto por nodo: se encadenan aquí (contorno y, debajo, la sombra desplazada).
        DropShadow contorno = new DropShadow(BlurType.ONE_PASS_BOX, Pintor.TINTA, 4, 1, 0, 0);
        DropShadow sombra = new DropShadow(BlurType.ONE_PASS_BOX, Pintor.CORAL, 0, 0, 4, 5);
        sombra.setInput(contorno);
        l.setEffect(sombra);
        return l;
    }
}
