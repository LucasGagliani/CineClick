package com.cineclick.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Column(length = 60)
    private String titulo;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    private LocalDateTime vigenciaDesde;

    private LocalDateTime vigenciaHasta;

    @Column(nullable = false)
    private boolean activa = true;

    @ElementCollection(targetClass = DayOfWeek.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "promocion_dias", joinColumns = @JoinColumn(name = "promocion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia", length = 10)
    private Set<DayOfWeek> diasSemana = new HashSet<>();

    private Integer usosMaximosTotales;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int usosActuales = 0;

    private Integer usosMaximosPorUsuario;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean soloPrimeraCompra = false;

    private Integer maxEntradasConDescuento;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoMaximoDescuento;

    public boolean estaVigente(LocalDateTime momento) {
        return !momento.isBefore(vigenciaDesde) && !momento.isAfter(vigenciaHasta);
    }

    public boolean valeElDia(DayOfWeek dia) {
        return diasSemana == null || diasSemana.isEmpty() || diasSemana.contains(dia);
    }

    public boolean estaAgotada() {
        return usosMaximosTotales != null && usosActuales >= usosMaximosTotales;
    }

    public Map<Entrada, BigDecimal> descuentosPorEntrada(List<Entrada> entradas) {
        List<Entrada> porPrecio = entradas.stream()
            .sorted(Comparator.comparing(Entrada::getPrecioUnitario).reversed())
            .toList();
        int conDescuento = maxEntradasConDescuento == null ? porPrecio.size() : Math.min(maxEntradasConDescuento, porPrecio.size());
        BigDecimal disponible = montoMaximoDescuento;
        Map<Entrada, BigDecimal> descuentos = new LinkedHashMap<>();
        for (Entrada entrada : porPrecio.subList(0, conDescuento)) {
            BigDecimal descuento = entrada.getPrecioUnitario()
                .multiply(porcentajeDescuento)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (disponible != null) {
                descuento = descuento.min(disponible);
                disponible = disponible.subtract(descuento);
            }
            if (descuento.signum() > 0) {
                descuentos.put(entrada, descuento);
            }
        }
        return descuentos;
    }
}
