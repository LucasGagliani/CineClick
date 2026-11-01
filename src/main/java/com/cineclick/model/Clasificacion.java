package com.cineclick.model;

import com.cineclick.service.Normalizador;

import java.util.Arrays;
import java.util.Optional;

public enum Clasificacion {
    ATP("ATP"),
    MAS_13("+13"),
    MAS_16("+16"),
    MAS_18("+18");

    private final String etiqueta;

    Clasificacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static Optional<Clasificacion> desdeTexto(String texto) {
        String clave = Normalizador.clave(texto).replace(" ", "");
        return Arrays.stream(values())
            .filter(c -> c.name().equalsIgnoreCase(clave) || Normalizador.clave(c.etiqueta).equals(clave))
            .findFirst();
    }
}
