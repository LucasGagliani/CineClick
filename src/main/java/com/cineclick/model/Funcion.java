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
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "funciones")
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormatoFuncion formato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Idioma idioma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFuncion estado = EstadoFuncion.PROGRAMADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pelicula_id", nullable = false)
    private Pelicula pelicula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    public void calcularHorarioFin(int minutosLimpieza) {
        if (pelicula != null && fechaHoraInicio != null && pelicula.getDuracionMinutos() != null) {
            fechaHoraFin = fechaHoraInicio.plusMinutes(pelicula.getDuracionMinutos() + (long) minutosLimpieza);
        }
    }

    public boolean estaDisponibleParaVenta() {
        return estado == EstadoFuncion.PROGRAMADA && fechaHoraInicio.isAfter(LocalDateTime.now());
    }

    public void cambiarEstado(EstadoFuncion estado) {
        this.estado = estado;
    }
}
