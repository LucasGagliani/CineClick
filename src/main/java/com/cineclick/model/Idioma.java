package com.cineclick.model;

import com.cineclick.service.Normalizador;

import java.util.Map;
import java.util.Optional;

public enum Idioma {
    CASTELLANO("Castellano"),
    SUBTITULADA("Subtitulada");

    private static final Map<String, Idioma> SINONIMOS = Map.of(
        "castellano", CASTELLANO,
        "espanol", CASTELLANO,
        "es", CASTELLANO,
        "doblada", CASTELLANO,
        "subtitulada", SUBTITULADA,
        "subtitulado", SUBTITULADA,
        "sub", SUBTITULADA,
        "original", SUBTITULADA,
        "ingles", SUBTITULADA
    );

    private final String etiqueta;

    Idioma(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static Optional<Idioma> desdeTexto(String texto) {
        return Optional.ofNullable(SINONIMOS.get(Normalizador.clave(texto)));
    }
}
