package com.cineclick.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "promociones")
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activa = true;

    public boolean estaVigente(LocalDate fecha) {
        return activa && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    public BigDecimal calcularDescuento(BigDecimal total) {
        BigDecimal factor = BigDecimal.ONE.subtract(porcentajeDescuento.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return total.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
