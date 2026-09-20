package com.cineclick.dto;

import com.cineclick.model.Butaca;
import com.cineclick.model.Cine;
import com.cineclick.model.Cliente;
import com.cineclick.model.Compra;
import com.cineclick.model.Entrada;
import com.cineclick.model.Funcion;
import com.cineclick.model.Pago;
import com.cineclick.model.Pelicula;
import com.cineclick.model.Promocion;
import com.cineclick.model.Sala;

import java.util.Comparator;
import java.util.Set;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static PeliculaResponseDTO toPeliculaResponse(Pelicula pelicula) {
        return new PeliculaResponseDTO(
            pelicula.getId(),
            pelicula.getTitulo(),
            pelicula.getSinopsis(),
            pelicula.getDuracionMinutos(),
            pelicula.getClasificacion(),
            pelicula.getGenero(),
            pelicula.getIdioma(),
            Set.copyOf(pelicula.getFormatosDisponibles()),
            pelicula.getUrlPoster(),
            pelicula.getFechaEstreno(),
            pelicula.isActiva()
        );
    }

    public static CineResponseDTO toCineResponse(Cine cine) {
        return new CineResponseDTO(
            cine.getId(),
            cine.getNombre(),
            cine.getDireccion(),
            cine.getCiudad(),
            cine.getTelefono(),
            cine.isActivo(),
            cine.getSalas().stream()
                .sorted(Comparator.comparing(Sala::getNumero))
                .map(DtoMapper::toSalaResponse)
                .toList()
        );
    }

    public static SalaResponseDTO toSalaResponse(Sala sala) {
        return new SalaResponseDTO(
            sala.getId(),
            sala.getNombre(),
            sala.getNumero(),
            sala.getTipo(),
            sala.isActiva(),
            sala.capacidad()
        );
    }

    public static ButacaResponseDTO toButacaResponse(Butaca butaca, boolean disponible) {
        return new ButacaResponseDTO(
            butaca.getId(),
            butaca.getFila(),
            butaca.getNumero(),
            butaca.codigo(),
            butaca.getTipo(),
            disponible
        );
    }

    public static FuncionResponseDTO toFuncionResponse(Funcion funcion) {
        return new FuncionResponseDTO(
            funcion.getId(),
            funcion.getPelicula().getId(),
            funcion.getPelicula().getTitulo(),
            funcion.getSala().getCine().getId(),
            funcion.getSala().getCine().getNombre(),
            funcion.getSala().getId(),
            funcion.getSala().getNombre(),
            funcion.getFechaHoraInicio(),
            funcion.getFechaHoraFin(),
            funcion.getPrecioBase(),
            funcion.getFormato(),
            funcion.getIdioma(),
            funcion.getEstado()
        );
    }

    public static ClienteResponseDTO toClienteResponse(Cliente cliente) {
        return new ClienteResponseDTO(
            cliente.getId(),
            cliente.getUsuario().getId(),
            cliente.getUsuario().getNombre(),
            cliente.getUsuario().getApellido(),
            cliente.getUsuario().getEmail(),
            cliente.getTelefono(),
            cliente.getFechaRegistro(),
            cliente.getUsuario().isActivo()
        );
    }

    public static CompraResponseDTO toCompraResponse(Compra compra) {
        return new CompraResponseDTO(
            compra.getId(),
            compra.getCliente().getId(),
            compra.getCliente().nombreCompleto(),
            compra.getFuncion().getId(),
            compra.getFuncion().getPelicula().getTitulo(),
            compra.getFechaCompra(),
            compra.getTotal(),
            compra.getEstado(),
            compra.getEntradas().stream().map(DtoMapper::toEntradaResponse).toList(),
            compra.getPago() == null ? null : toPagoResponse(compra.getPago())
        );
    }

    public static EntradaResponseDTO toEntradaResponse(Entrada entrada) {
        return new EntradaResponseDTO(
            entrada.getId(),
            entrada.getCodigoQr(),
            entrada.getButaca().getFila(),
            entrada.getButaca().getNumero(),
            entrada.getButaca().codigo(),
            entrada.getPrecioUnitario(),
            entrada.getEstado()
        );
    }

    public static PagoResponseDTO toPagoResponse(Pago pago) {
        return new PagoResponseDTO(
            pago.getId(),
            pago.getMonto(),
            pago.getMetodoPago(),
            pago.getEstado(),
            pago.getFechaPago(),
            pago.getCodigoTransaccion()
        );
    }

    public static PromocionResponseDTO toPromocionResponse(Promocion promocion) {
        return new PromocionResponseDTO(
            promocion.getId(),
            promocion.getCodigo(),
            promocion.getDescripcion(),
            promocion.getPorcentajeDescuento(),
            promocion.getFechaInicio(),
            promocion.getFechaFin(),
            promocion.isActiva()
        );
    }
}
