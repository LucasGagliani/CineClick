package com.cineclick.service;

import com.cineclick.dto.CompraRequestDTO;
import com.cineclick.dto.CompraResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.exception.ButacaNoDisponibleException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Butaca;
import com.cineclick.model.Cliente;
import com.cineclick.model.Compra;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.Funcion;
import com.cineclick.model.Promocion;
import com.cineclick.model.TipoButaca;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.EntradaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final EntradaRepository entradaRepository;
    private final ButacaRepository butacaRepository;
    private final ClienteService clienteService;
    private final FuncionService funcionService;
    private final PromocionService promocionService;

    public CompraService(
        CompraRepository compraRepository,
        EntradaRepository entradaRepository,
        ButacaRepository butacaRepository,
        ClienteService clienteService,
        FuncionService funcionService,
        PromocionService promocionService
    ) {
        this.compraRepository = compraRepository;
        this.entradaRepository = entradaRepository;
        this.butacaRepository = butacaRepository;
        this.clienteService = clienteService;
        this.funcionService = funcionService;
        this.promocionService = promocionService;
    }

    @Transactional
    public CompraResponseDTO crearCompra(CompraRequestDTO dto) {
        validarButacasSinDuplicados(dto.butacasIds());
        Cliente cliente = clienteService.buscarEntidad(dto.clienteId());
        Funcion funcion = funcionService.buscarEntidad(dto.funcionId());
        if (!funcion.estaDisponibleParaVenta()) {
            throw new ReglaNegocioException("La funcion no esta disponible para la venta");
        }

        List<Butaca> butacas = butacaRepository.findAllById(dto.butacasIds());
        validarButacasDisponibles(funcion, butacas, dto.butacasIds());

        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setFuncion(funcion);

        if (dto.codigoPromocion() != null && !dto.codigoPromocion().isBlank()) {
            Promocion promocion = promocionService.validarPromocionParaCompra(dto.codigoPromocion());
            compra.setPromocion(promocion);
        }

        for (Butaca butaca : butacas) {
            Entrada entrada = new Entrada();
            entrada.setButaca(butaca);
            entrada.setPrecioUnitario(calcularPrecioButaca(funcion.getPrecioBase(), butaca));
            entrada.generarCodigoQr();
            compra.agregarEntrada(entrada);
        }
        compra.calcularTotal();

        try {
            return DtoMapper.toCompraResponse(compraRepository.saveAndFlush(compra));
        } catch (DataIntegrityViolationException ex) {
            throw new ButacaNoDisponibleException("Una o mas butacas ya fueron compradas para esta funcion");
        }
    }

    @Transactional(readOnly = true)
    public CompraResponseDTO buscarPorId(Long id) {
        return DtoMapper.toCompraResponse(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarPorCliente(Long clienteId) {
        clienteService.buscarEntidad(clienteId);
        return compraRepository.findByClienteId(clienteId).stream().map(DtoMapper::toCompraResponse).toList();
    }

    @Transactional
    public CompraResponseDTO cancelar(Long id) {
        Compra compra = buscarEntidad(id);
        if (compra.getEstado().name().equals("CANCELADA")) {
            throw new ReglaNegocioException("La compra ya se encuentra cancelada");
        }
        compra.cancelar();
        return DtoMapper.toCompraResponse(compraRepository.save(compra));
    }

    @Transactional(readOnly = true)
    public Compra buscarEntidad(Long id) {
        return compraRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada con id " + id));
    }

    public void validarButacasDisponibles(Funcion funcion, List<Butaca> butacas, List<Long> idsSolicitados) {
        if (butacas.size() != idsSolicitados.size()) {
            throw new RecursoNoEncontradoException("Una o mas butacas solicitadas no existen");
        }
        for (Butaca butaca : butacas) {
            if (!butaca.isActiva()) {
                throw new ButacaNoDisponibleException("La butaca " + butaca.codigo() + " no esta activa");
            }
            if (!butaca.getSala().getId().equals(funcion.getSala().getId())) {
                throw new ReglaNegocioException("La butaca " + butaca.codigo() + " no pertenece a la sala de la funcion");
            }
            boolean ocupada = entradaRepository.existsByFuncionIdAndButacaIdAndEstadoNot(
                funcion.getId(),
                butaca.getId(),
                EstadoEntrada.CANCELADA
            );
            if (ocupada) {
                throw new ButacaNoDisponibleException("La butaca " + butaca.codigo() + " ya no esta disponible");
            }
        }
    }

    private void validarButacasSinDuplicados(List<Long> butacasIds) {
        Set<Long> idsUnicos = new HashSet<>(butacasIds);
        if (idsUnicos.size() != butacasIds.size()) {
            throw new ReglaNegocioException("No se puede seleccionar dos veces la misma butaca");
        }
    }

    private BigDecimal calcularPrecioButaca(BigDecimal precioBase, Butaca butaca) {
        if (butaca.getTipo() == TipoButaca.VIP) {
            return precioBase.multiply(BigDecimal.valueOf(1.30)).setScale(2, RoundingMode.HALF_UP);
        }
        if (butaca.getTipo() == TipoButaca.DISCAPACIDAD) {
            return precioBase.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
        }
        return precioBase.setScale(2, RoundingMode.HALF_UP);
    }
}
