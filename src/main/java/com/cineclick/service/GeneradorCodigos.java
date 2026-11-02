package com.cineclick.service;

import java.security.SecureRandom;
import java.util.function.Predicate;

public final class GeneradorCodigos {

    public static final String ALFABETO = "0123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
    public static final int LARGO = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorCodigos() {
    }

    public static String nuevo(String prefijo, Predicate<String> yaExiste) {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(prefijo);
            for (int i = 0; i < LARGO; i++) {
                sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
            }
            codigo = sb.toString();
        } while (yaExiste.test(codigo));
        return codigo;
    }
}
