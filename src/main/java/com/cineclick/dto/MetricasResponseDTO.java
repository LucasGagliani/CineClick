package com.cineclick.dto;

import com.cineclick.model.TipoEntrada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MetricasResponseDTO(
    LocalDate desde,
    LocalDate hasta,
    LocalDate desdeAnterior,
    LocalDate hastaAnterior,
    Resumen resumen,
    List<ProximaFuncion> proximasFunciones,
    List<PeliculaRanking> rankingPeliculas,
    Conversion conversion,
    List<CineMetricas> porCine,
    List<Franja> franjas,
    List<PromocionUso> promociones,
    List<TipoEntradaVenta> tiposEntrada,
    Asistencia asistencia
) {

    public record Indicador(BigDecimal valor, BigDecimal valorAnterior) {
    }

    public record Resumen(Indicador ingresos, Indicador entradasVendidas, Indicador ocupacion, Indicador ticketPromedio) {
    }

    public record ProximaFuncion(
        Long funcionId,
        String pelicula,
        String cine,
        String sala,
        LocalDateTime inicio,
        long vendidas,
        long capacidad
    ) {
    }

    public record PeliculaRanking(Long peliculaId, String titulo, long entradas, BigDecimal ingresos) {
    }

    public record Conversion(long creadas, long pagadas, long vencidas, long canceladas, long pendientes) {
    }

    public record CineMetricas(Long cineId, String nombre, BigDecimal ingresos, long entradas, BigDecimal ocupacion) {
    }

    public record Franja(int dia, int hora, long entradas) {
    }

    public record PromocionUso(String codigo, long usos, BigDecimal descuentoTotal) {
    }

    public record TipoEntradaVenta(TipoEntrada tipo, String nombre, long cantidad, BigDecimal ingresos) {
    }

    public record Asistencia(long emitidas, long usadas) {
    }
}
