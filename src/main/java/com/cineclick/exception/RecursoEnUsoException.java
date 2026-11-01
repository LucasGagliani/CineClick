package com.cineclick.exception;

import java.util.List;

public class RecursoEnUsoException extends RuntimeException {

    private final List<String> asociados;

    public RecursoEnUsoException(String mensaje, List<String> asociados) {
        super(mensaje);
        this.asociados = asociados;
    }

    public List<String> getAsociados() {
        return asociados;
    }
}
