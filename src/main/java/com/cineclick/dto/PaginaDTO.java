package com.cineclick.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PaginaDTO<T>(
    List<T> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas
) {
    public static <E, T> PaginaDTO<T> de(Page<E> pagina, Function<E, T> mapear) {
        return new PaginaDTO<>(
            pagina.getContent().stream().map(mapear).toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages()
        );
    }
}
