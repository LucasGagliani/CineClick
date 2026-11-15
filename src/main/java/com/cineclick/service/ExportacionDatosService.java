package com.cineclick.service;

import com.cineclick.dto.CuentaResponseDTO;
import com.cineclick.dto.ExportacionDatosDTO;
import com.cineclick.dto.ExportacionDatosDTO.ArrepentimientoExportado;
import com.cineclick.dto.ExportacionDatosDTO.CompraExportada;
import com.cineclick.dto.ExportacionDatosDTO.ConsultaExportada;
import com.cineclick.dto.ExportacionDatosDTO.EntradaExportada;
import com.cineclick.dto.ExportacionDatosDTO.FuncionExportada;
import com.cineclick.dto.ExportacionDatosDTO.PagoExportado;
import com.cineclick.model.Compra;
import com.cineclick.model.Funcion;
import com.cineclick.model.Pago;
import com.cineclick.model.Usuario;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.ConsultaSoporteRepository;
import com.cineclick.repository.SolicitudArrepentimientoRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportacionDatosService {

    private final SesionUsuario sesion;
    private final VerificadorDeContrasena verificador;
    private final CuentaService cuentaService;
    private final CompraRepository compraRepository;
    private final SolicitudArrepentimientoRepository arrepentimientoRepository;
    private final ConsultaSoporteRepository consultaRepository;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public ExportacionDatosDTO exportar(String contrasena, HttpServletRequest request) {
        Usuario usuario = sesion.actual(request);
        verificador.verificar(usuario, contrasena, "contrasena", request);
        CuentaResponseDTO cuenta = cuentaService.armar(usuario);
        Long clienteId = cuenta.clienteId();

        List<CompraExportada> compras = clienteId == null ? List.of() : compraRepository.findByClienteId(clienteId).stream()
            .sorted(Comparator.comparing(Compra::getFechaCompra).reversed())
            .map(ExportacionDatosService::compra)
            .toList();
        List<ArrepentimientoExportado> arrepentimientos = clienteId == null ? List.of()
            : arrepentimientoRepository.findByCompraClienteIdOrderByFechaDesc(clienteId).stream()
                .map(s -> new ArrepentimientoExportado(s.getCodigo(), s.getCompra().getId(), s.getFecha(), s.getMotivo(),
                    s.getEstado(), s.getMontoReembolso(), s.getFechaReembolso()))
                .toList();
        List<ConsultaExportada> consultas = consultaRepository.buscarDelUsuario(usuario.getEmail(), clienteId).stream()
            .map(c -> new ConsultaExportada(c.getNumero(), c.getFecha(), c.getMotivo(),
                c.getCompra() == null ? null : c.getCompra().getId(), c.getMensaje(), c.getEstado()))
            .toList();

        return new ExportacionDatosDTO(
            LocalDateTime.now(reloj),
            new ExportacionDatosDTO.Cuenta(cuenta.nombre(), cuenta.apellido(), cuenta.email(), cuenta.rol(), cuenta.clienteDesde(),
                cuenta.telefono(), cuenta.fechaNacimiento(), cuenta.genero(), cuenta.cinePreferidoNombre(),
                usuario.getAceptoTerminosEn(), cuenta.contrasenaModificadaEn(), cuenta.recibirNovedades()),
            compras,
            arrepentimientos,
            consultas
        );
    }

    private static CompraExportada compra(Compra compra) {
        Funcion funcion = compra.getFuncion();
        FuncionExportada datosFuncion = new FuncionExportada(funcion.getPelicula().getTitulo(), funcion.getSala().getCine().getNombre(),
            funcion.getSala().getNombre(), funcion.getFechaHoraInicio(), funcion.getFormato(), funcion.getIdioma());
        List<EntradaExportada> entradas = compra.getEntradas().stream()
            .map(e -> new EntradaExportada(e.getCodigoQr(), e.getButaca().codigo(), e.getTipo(), e.getPrecioUnitario(), e.getEstado(), e.getUsadaEn()))
            .toList();
        Pago pago = compra.getPago();
        PagoExportado datosPago = pago == null ? null : new PagoExportado(pago.getEstado(), pago.getMetodoPago(), pago.getMonto(),
            pago.getFechaPago(), pago.getMarcaTarjeta(), pago.getUltimos4(), pago.getFechaReembolso());
        return new CompraExportada(compra.getId(), compra.getFechaCompra(), compra.getEstado(), compra.getTotal(),
            compra.getPromocion() == null ? null : compra.getPromocion().getCodigo(), datosFuncion, entradas, datosPago);
    }
}
