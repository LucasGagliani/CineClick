package com.cineclick.model;

import java.util.ArrayList;
import java.util.List;

public enum PlanoSala {
    GRANDE(
        "DDooo.oooooooooooo.oooDD",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo",
        "ooooo.oooooooooooo.ooooo"
    ),
    CLASICA(
        "DD..oooooooooo..DD",
        "ooo.oooooooooo.ooo",
        "ooo.oooooooooo.ooo",
        "ooo.oooooooooo.ooo",
        "ooo.oooooooooo.ooo",
        "ooo.oooooooooo.ooo",
        "ooo.oooooooooo.ooo",
        "ooo.oooooooooo.ooo",
        "ooo.oooooooooo.ooo"
    ),
    ESTADIO(
        "..oooooo.oooooo..",
        ".ooooooo.ooooooo.",
        "oooooooo.oooooooo",
        "oooooooo.oooooooo",
        "oooooooo.oooooooo",
        "oooooooo.oooooooo",
        "oooooooo.oooooooo",
        "oooooooo.oooooooo",
        "DDoooooo.ooooooDD"
    ),
    IMAX(
        "......oooooooooo......",
        "...oo.oooooooooo.oo...",
        ".oooo.oooooooooo.oooo.",
        "ooooo.oooooooooo.ooooo",
        "ooooo.oooooooooo.ooooo",
        "ooooo.oooooooooo.ooooo",
        "ooooo.oooooooooo.ooooo",
        "ooooo.oooooooooo.ooooo",
        "ooooo.oooooooooo.ooooo",
        "DDooo.oooooooooo.oooDD"
    ),
    CHICA(
        "DDo.oooooo.oDD",
        "ooo.oooooo.ooo",
        "ooo.oooooo.ooo",
        "ooo.oooooo.ooo",
        "ooo.oooooo.ooo",
        "ooo.oooooo.ooo",
        "ooo.oooooo.ooo"
    ),
    PREMIUM(
        "DD.oooooo.DD",
        "oo.oooooo.oo",
        "oo.oooooo.oo",
        "oo.oooooo.oo",
        "oo.oooooo.oo",
        "oo.oooooo.oo"
    ),
    CUATRO_D(
        "DD...oooo.oooo...DD",
        "oooo.oooo.oooo.oooo",
        "oooo.oooo.oooo.oooo",
        "oooo.oooo.oooo.oooo",
        "oooo.oooo.oooo.oooo",
        "oooo.oooo.oooo.oooo"
    );

    private static final char BUTACA = 'o';
    private static final char DISCAPACIDAD = 'D';

    private final String[] filas;

    PlanoSala(String... filas) {
        this.filas = filas;
    }

    public List<Butaca> crearButacas(Sala sala) {
        List<Butaca> butacas = new ArrayList<>();
        for (int i = 0; i < filas.length; i++) {
            String fila = String.valueOf((char) ('A' + i));
            int numero = 0;
            for (int columna = 0; columna < filas[i].length(); columna++) {
                char lugar = filas[i].charAt(columna);
                if (lugar != BUTACA && lugar != DISCAPACIDAD) {
                    continue;
                }
                Butaca butaca = new Butaca();
                butaca.setSala(sala);
                butaca.setFila(fila);
                butaca.setNumero(++numero);
                butaca.setColumna(columna + 1);
                butaca.setTipo(lugar == DISCAPACIDAD ? TipoButaca.DISCAPACIDAD : TipoButaca.NORMAL);
                butacas.add(butaca);
            }
        }
        return butacas;
    }

    public static PlanoSala sugeridoPara(Sala sala) {
        return switch (sala.getTipo()) {
            case IMAX -> IMAX;
            case PREMIUM -> PREMIUM;
            case CUATRO_D -> CUATRO_D;
            case TRES_D -> ESTADIO;
            case TRADICIONAL -> (sala.getCine().getId() + sala.getNumero()) % 2 == 0 ? CLASICA : ESTADIO;
        };
    }
}
