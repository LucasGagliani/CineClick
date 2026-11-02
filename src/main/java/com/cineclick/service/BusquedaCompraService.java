package com.cineclick.service;

import com.cineclick.dto.ResumenCompraDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.EntradaRepository;
import com.cineclick.repository.SolicitudArrepentimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BusquedaCompraService {

    public static final String NO_ENCONTRADA = "No encontramos una compra con esos datos. Revisá el email con el que compraste";
    private static final int DIAS_PARA_ARREPENTIRSE = 10;
    private static final int MINUTOS_ANTES_DE_LA_FUNCION = 60;

    private final CompraRepository compraRepository;
    private final EntradaRepository entradaRepository;
    private final SolicitudArrepentimientoRepository solicitudRepository;

    public record Elegibilidad(boolean elegible, String motivo) {
    }

    @Transactional(readOnly = true)
    public ResumenCompraDTO resumen(String email, String codigo) {
        Compra compra = buscar(email, codigo);
        Elegibilidad elegibilidad = elegibilidad(compra, LocalDateTime.now());
        return new ResumenCompraDTO(
            compra.getId(),
            compra.getFuncion().getPelicula().getTitulo(),
            compra.getFuncion().getFechaHoraInicio(),
            compra.getFuncion().getSala().getCine().getNombre(),
            compra.getFuncion().getSala().getNombre(),
            compra.getEntradas().size(),
            compra.getTotal(),
            compra.getEstado(),
            compra.getFechaCompra(),
            elegibilidad.elegible(),
            elegibilidad.motivo(),
            solicitudRepository.findByCompraId(compra.getId()).map(solicitud -> solicitud.getCodigo()).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public Compra buscar(String email, String codigo) {
        String limpio = codigo == null ? "" : codigo.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        Optional<Compra> porEntrada = limpio.length() == GeneradorCodigos.LARGO
            ? entradaRepository.findByCodigoQrIgnoreCase(limpio).map(entrada -> entrada.getCompra())
            : Optional.empty();
        Optional<Compra> porNumero = limpio.matches("\\d{1,12}") ? compraRepository.findById(Long.valueOf(limpio)) : Optional.empty();
        String mail = email == null ? "" : email.trim();
        return Stream.of(porEntrada, porNumero)
            .flatMap(Optional::stream)
            .filter(c -> c.getCliente().getUsuario().getEmail().equalsIgnoreCase(mail))
            .findFirst()
            .orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
    }

    public Elegibilidad elegibilidad(Compra compra, LocalDateTime ahora) {
        if (compra.getEstado() == EstadoCompra.ARREPENTIDA) {
            return new Elegibilidad(false, "Ya te arrepentiste de esta compra");
        }
        if (compra.getEstado() != EstadoCompra.CONFIRMADA) {
            return new Elegibilidad(false, "La compra no está confirmada");
        }
        LocalDateTime inicio = compra.getFuncion().getFechaHoraInicio();
        if (!inicio.isAfter(ahora)) {
            return new Elegibilidad(false, "La función ya pasó");
        }
        if (inicio.minusMinutes(MINUTOS_ANTES_DE_LA_FUNCION).isBefore(ahora)) {
            return new Elegibilidad(false, "La función empieza en menos de 60 minutos");
        }
        if (ahora.isAfter(compra.getFechaCompra().plusDays(DIAS_PARA_ARREPENTIRSE))) {
            return new Elegibilidad(false, "Pasaron más de 10 días corridos desde la compra");
        }
        return new Elegibilidad(true, null);
    }
}
