package com.cineclick.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
    name = "entradas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_entrada_funcion_butaca_vigente",
        columnNames = {"funcion_id", "butaca_id", "vigente"}
    )
)
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String codigoQr;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrada estado = EstadoEntrada.EMITIDA;

    // TRUE si la entrada esta activa, NULL si se cancelo.
    // El unique no compara NULLs, asi una entrada cancelada no bloquea la butaca.
    @Column
    private Boolean vigente = Boolean.TRUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcion_id", nullable = false)
    private Funcion funcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "butaca_id", nullable = false)
    private Butaca butaca;

    public void generarCodigoQr() {
        if (codigoQr == null || codigoQr.isBlank()) {
            codigoQr = "CINECLICK-" + UUID.randomUUID();
        }
    }

    public void marcarUsada() {
        estado = EstadoEntrada.USADA;
    }

    public void cancelar() {
        estado = EstadoEntrada.CANCELADA;
        vigente = null;
    }
}
