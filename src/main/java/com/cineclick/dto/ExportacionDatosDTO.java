package com.cineclick.dto;

import com.cineclick.model.EstadoArrepentimiento;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.EstadoConsulta;
import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.EstadoPago;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.GeneroCliente;
import com.cineclick.model.Idioma;
import com.cineclick.model.MetodoPago;
import com.cineclick.model.MotivoArrepentimiento;
import com.cineclick.model.MotivoSoporte;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.TipoEntrada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExportacionDatosDTO(
    LocalDateTime generadoEn,
    Cuenta cuenta,
    List<CompraExportada> compras,
    List<ArrepentimientoExportado> arrepentimientos,
    List<ConsultaExportada> consultasSoporte
) {

    public record Cuenta(
        String nombre,
        String apellido,
        String email,
        RolUsuario rol,
        LocalDateTime clienteDesde,
        String telefono,
        LocalDate fechaNacimiento,
        GeneroCliente genero,
        String cinePreferido,
        LocalDateTime aceptoTerminosEn,
        LocalDateTime contrasenaModificadaEn,
        boolean recibirNovedades
    ) {
    }

    public record CompraExportada(
        Long numero,
        LocalDateTime fechaCompra,
        EstadoCompra estado,
        BigDecimal total,
        String codigoPromocion,
        FuncionExportada funcion,
        List<EntradaExportada> entradas,
        PagoExportado pago
    ) {
    }

    public record FuncionExportada(
        String pelicula,
        String cine,
        String sala,
        LocalDateTime fechaHoraInicio,
        FormatoFuncion formato,
        Idioma idioma
    ) {
    }

    public record EntradaExportada(
        String codigo,
        String butaca,
        TipoEntrada tipo,
        BigDecimal precio,
        EstadoEntrada estado,
        LocalDateTime usadaEn
    ) {
    }

    public record PagoExportado(
        EstadoPago estado,
        MetodoPago metodo,
        BigDecimal monto,
        LocalDateTime fecha,
        String marcaTarjeta,
        String ultimos4,
        LocalDateTime fechaReembolso
    ) {
    }

    public record ArrepentimientoExportado(
        String codigo,
        Long compra,
        LocalDateTime fecha,
        MotivoArrepentimiento motivo,
        EstadoArrepentimiento estado,
        BigDecimal montoReembolso,
        LocalDateTime fechaReembolso
    ) {
    }

    public record ConsultaExportada(
        String numero,
        LocalDateTime fecha,
        MotivoSoporte motivo,
        Long compra,
        String mensaje,
        EstadoConsulta estado
    ) {
    }
}
