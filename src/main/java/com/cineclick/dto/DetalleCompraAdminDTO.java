package com.cineclick.dto;

import java.util.List;

public record DetalleCompraAdminDTO(
    CompraResponseDTO compra,
    String clienteEmail,
    PromocionResponseDTO promocion,
    ArrepentimientoResponseDTO arrepentimiento,
    List<SoporteResponseDTO> consultas
) {
}
