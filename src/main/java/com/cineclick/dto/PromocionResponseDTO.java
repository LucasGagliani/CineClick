package com.cineclick.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public record PromocionResponseDTO(
    Long id,
    String codigo,
    String titulo,
    String descripcion,
    BigDecimal porcentajeDescuento,
    LocalDateTime vigenciaDesde,
    LocalDateTime vigenciaHasta,
    boolean activa,
    List<DayOfWeek> diasSemana,
    Integer usosMaximosTotales,
    int usosActuales,
    Integer usosMaximosPorUsuario,
    boolean soloPrimeraCompra,
    Integer maxEntradasConDescuento,
    BigDecimal montoMaximoDescuento,
    Boolean usablePorUsuario,
    String motivoNoUsable
) {
}
