package com.cineclick.exception;

public class CredencialesInvalidasException extends ReglaNegocioException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
