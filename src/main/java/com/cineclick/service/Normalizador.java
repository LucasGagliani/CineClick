package com.cineclick.service;

import com.cineclick.exception.CampoInvalidoException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class Normalizador {

    private static final Set<String> CONECTORES = Set.of("de", "del", "la", "las", "los", "el", "y", "e");

    private Normalizador() {
    }

    public static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("[\\s&&[^\\n]]+", " ").replaceAll(" *\\n *", "\n").trim();
        return limpio.isEmpty() ? null : limpio;
    }

    public static String formatoTitulo(String valor) {
        String limpio = texto(valor);
        if (limpio == null) {
            return null;
        }
        String[] palabras = limpio.split(" ");
        for (int i = 0; i < palabras.length; i++) {
            String palabra = palabras[i];
            boolean todoMinuscula = palabra.equals(palabra.toLowerCase(Locale.ROOT));
            boolean mayuscula = palabra.equals(palabra.toUpperCase(Locale.ROOT));
            boolean sigla = mayuscula && palabra.length() <= 4;
            if (!todoMinuscula && (!mayuscula || sigla)) {
                continue;
            }
            String minuscula = palabra.toLowerCase(Locale.ROOT);
            palabras[i] = i > 0 && CONECTORES.contains(minuscula)
                ? minuscula
                : minuscula.substring(0, 1).toUpperCase(Locale.ROOT) + minuscula.substring(1);
        }
        return String.join(" ", palabras);
    }

    public static String clave(String valor) {
        String limpio = texto(valor);
        if (limpio == null) {
            return "";
        }
        return Normalizer.normalize(limpio, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    }

    public static String telefono(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() == 13 && digitos.startsWith("549")) {
            digitos = digitos.substring(3);
        } else if (digitos.length() == 12 && digitos.startsWith("54")) {
            digitos = digitos.substring(2);
        } else if (digitos.length() == 11 && digitos.startsWith("0")) {
            digitos = digitos.substring(1);
        }
        if (digitos.length() != 10) {
            throw new CampoInvalidoException(campo, "Tiene que tener 10 dígitos con el código de área (por ejemplo, 011-4000-1000)");
        }
        return digitos;
    }
}
