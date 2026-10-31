package com.cineclick.exception;

public class ReservaNoActivaException extends ReglaNegocioException {
    public ReservaNoActivaException(String mensaje) {
        super(mensaje);
    }
}
