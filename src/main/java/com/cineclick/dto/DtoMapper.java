package com.cineclick.dto;

import com.cineclick.model.Butaca;
import com.cineclick.model.Cine;
import com.cineclick.model.Cliente;
import com.cineclick.model.Compra;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.Funcion;
import com.cineclick.model.Pago;
import com.cineclick.model.Pelicula;
import com.cineclick.model.Promocion;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoEntrada;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
            pelicula.isActiva(),
            pelicula.isDestacada()
        );
    }

    public static CineResponseDTO toCineResponse(Cine cine) {
        return new CineResponseDTO(
            cine.getId(),
            cine.getNombre(),
            cine.getProvincia(),
            cine.getLocalidad(),
            cine.getCalle(),
            cine.getAltura(),
            cine.getTelefono(),
            cine.getUrlImagen(),
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
            sala.getCine().getId(),
            sala.getCine().getNombre(),
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
            butaca.getColumna() == null ? butaca.getNumero() : butaca.getColumna(),
            butaca.codigo(),
            butaca.getTipo(),
            disponible
        );
    }

    public static PrecioEntradaDTO toPrecioEntrada(TipoEntrada tipo, BigDecimal precioBase) {
        return new PrecioEntradaDTO(tipo, tipo.getNombre(), tipo.getSeccion(), tipo.getCondiciones(), tipo.precioPara(precioBase));
    }

    public static FuncionResponseDTO toFuncionResponse(Funcion funcion, Integer butacasLibres, Integer capacidadSala) {
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
            funcion.getEstado(),
            butacasLibres,
            capacidadSala
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
        boolean pendiente = compra.getEstado() == EstadoCompra.PENDIENTE_PAGO;
        LocalDateTime ahora = LocalDateTime.now();
        return new CompraResponseDTO(
            compra.getId(),
            compra.getCliente().getId(),
            compra.getCliente().nombreCompleto(),
            compra.getFuncion().getId(),
            compra.getFuncion().getPelicula().getTitulo(),
            toFuncionCompra(compra.getFuncion()),
            compra.getFechaCompra(),
            compra.calcularSubtotal(),
            compra.getTotal(),
            compra.getPromocion() == null ? null : compra.getPromocion().getCodigo(),
            compra.getEstado(),
            entradasConDescuento(compra),
            compra.getPago() == null ? null : toPagoResponse(compra.getPago()),
            pendiente ? compra.expiraEn() : null,
            pendiente ? compra.segundosRestantes(ahora) : null
        );
    }

    public static FuncionCompraDTO toFuncionCompra(Funcion funcion) {
        Pelicula pelicula = funcion.getPelicula();
        Cine cine = funcion.getSala().getCine();
        return new FuncionCompraDTO(
            funcion.getId(),
            funcion.getFechaHoraInicio(),
            funcion.getFechaHoraFin(),
            funcion.getFormato(),
            funcion.getIdioma(),
            funcion.getEstado(),
            pelicula.getId(),
            pelicula.getTitulo(),
            pelicula.getUrlPoster(),
            pelicula.getClasificacion(),
            cine.getId(),
            cine.getNombre(),
            funcion.getSala().getNombre(),
            cine.getProvincia(),
            cine.getLocalidad(),
            cine.getCalle(),
            cine.getAltura()
        );
    }

    private static List<EntradaResponseDTO> entradasConDescuento(Compra compra) {
        Map<Entrada, BigDecimal> descuentos = compra.descuentosPorEntrada();
        return compra.getEntradas().stream()
            .map(entrada -> toEntradaResponse(entrada, descuentos.getOrDefault(entrada, BigDecimal.ZERO)))
            .toList();
    }

    public static EntradaResponseDTO toEntradaResponse(Entrada entrada, BigDecimal descuento) {
        return new EntradaResponseDTO(
            entrada.getId(),
            entrada.getCodigoQr(),
            entrada.getButaca().getFila(),
            entrada.getButaca().getNumero(),
            entrada.getButaca().codigo(),
            entrada.getTipo() == null ? TipoEntrada.GENERAL : entrada.getTipo(),
            entrada.getPrecioUnitario(),
            descuento,
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
            pago.getCodigoTransaccion(),
            pago.getMarcaTarjeta(),
            pago.getUltimos4()
        );
    }

    public static PromocionResponseDTO toPromocionResponse(Promocion promocion) {
        return toPromocionResponse(promocion, null, null);
    }

    public static PromocionResponseDTO toPromocionResponse(Promocion promocion, Boolean usable, String motivoNoUsable) {
        return new PromocionResponseDTO(
            promocion.getId(),
            promocion.getCodigo(),
            promocion.getTitulo(),
            promocion.getDescripcion(),
            promocion.getPorcentajeDescuento(),
            promocion.getVigenciaDesde(),
            promocion.getVigenciaHasta(),
            promocion.isActiva(),
            promocion.getDiasSemana().stream().sorted().toList(),
            promocion.getUsosMaximosTotales(),
            promocion.getUsosActuales(),
            promocion.getUsosMaximosPorUsuario(),
            promocion.isSoloPrimeraCompra(),
            promocion.getMaxEntradasConDescuento(),
            promocion.getMontoMaximoDescuento(),
            usable,
            motivoNoUsable
        );
    }
}
