package com.cineclick.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum TipoEntrada {
    GENERAL("General", "Entradas", "Para cualquier persona, sin requisitos.", "1.00"),
    MENOR("Menor", "Con descuento", "Menores de 12 años. Presentar DNI del menor en el ingreso.", "0.70"),
    JUBILADO("Jubilado", "Con descuento", "Jubilados y pensionados. Presentar credencial y DNI en el ingreso.", "0.60");

    private final String nombre;
    private final String seccion;
    private final String condiciones;
    private final BigDecimal factor;

    TipoEntrada(String nombre, String seccion, String condiciones, String factor) {
        this.nombre = nombre;
        this.seccion = seccion;
        this.condiciones = condiciones;
        this.factor = new BigDecimal(factor);
    }

    public String getNombre() {
        return nombre;
    }

    public String getSeccion() {
        return seccion;
    }

    public String getCondiciones() {
        return condiciones;
    }

    public BigDecimal precioPara(BigDecimal precioBase) {
        return precioBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
