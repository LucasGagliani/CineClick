package com.cineclick.service;

import com.cineclick.dto.CompraRequestDTO;
import com.cineclick.dto.CompraResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.EntradaSolicitadaDTO;
import com.cineclick.exception.ButacaNoDisponibleException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.exception.ReservaNoActivaException;
import com.cineclick.model.Butaca;
import com.cineclick.model.Cliente;
import com.cineclick.model.Compra;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.Funcion;
import com.cineclick.model.Promocion;
import com.cineclick.model.TipoEntrada;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.EntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompraService {

    private static final String RESTRICCION_BUTACA = "uk_entrada_funcion_butaca_vigente";

    private static final String ALFABETO_CODIGO = "0123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
    private static final int LARGO_CODIGO = 8;

    private final CompraRepository compraRepository;
    private final EntradaRepository entradaRepository;
    private final ButacaRepository butacaRepository;
    private final ClienteService clienteService;
    private final FuncionService funcionService;
    private final PromocionService promocionService;
    private final VencimientoReservaService vencimientoReservaService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public CompraResponseDTO crearCompra(CompraRequestDTO dto) {
        validarButacasSinDuplicados(dto.butacasIds());
        List<TipoEntrada> tipos = tiposPorButaca(dto.entradas(), dto.butacasIds().size());
        Cliente cliente = clienteService.buscarEntidad(dto.clienteId());
        Funcion funcion = funcionService.buscarEntidad(dto.funcionId());
        if (!funcion.estaDisponibleParaVenta()) {
            throw new ReglaNegocioException("La funcion no esta disponible para la venta");
        }
        vencimientoReservaService.cancelarVencidas(funcion.getId());

        List<Butaca> butacas = ordenarComoSeSolicitaron(butacaRepository.findAllById(dto.butacasIds()), dto.butacasIds());
        validarButacasDisponibles(funcion, butacas, dto.butacasIds());

        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setFuncion(funcion);

        if (dto.codigoPromocion() != null && !dto.codigoPromocion().isBlank()) {
            Promocion promocion = promocionService.validarPromocionParaCompra(dto.codigoPromocion());
            compra.setPromocion(promocion);
        }

        Set<String> codigosGenerados = new HashSet<>();
        for (int i = 0; i < butacas.size(); i++) {
            Entrada entrada = new Entrada();
            entrada.setButaca(butacas.get(i));
            entrada.setTipo(tipos.get(i));
            entrada.setPrecioUnitario(tipos.get(i).precioPara(funcion.getPrecioBase()));
            entrada.setCodigoQr(generarCodigoEntrada(codigosGenerados));
            compra.agregarEntrada(entrada);
        }
        compra.calcularTotal();

        try {
            return DtoMapper.toCompraResponse(compraRepository.saveAndFlush(compra));
        } catch (DataIntegrityViolationException ex) {
            if (esButacaDuplicada(ex)) {
                throw new ButacaNoDisponibleException("Una o mas butacas ya fueron compradas para esta funcion");
            }
            throw ex;
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

    @Transactional
    public CompraResponseDTO aplicarPromocion(Long id, String codigo) {
        Compra compra = buscarEntidad(id);
        verificarReservaActiva(compra);
        compra.setPromocion(promocionService.validarPromocionParaCompra(codigo));
        compra.calcularTotal();
        return DtoMapper.toCompraResponse(compra);
    }

    @Transactional(readOnly = true)
    public Compra buscarEntidad(Long id) {
        return compraRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada con id " + id));
    }

    public void verificarReservaActiva(Compra compra) {
        if (!compra.estaReservaActiva(LocalDateTime.now())) {
            throw new ReservaNoActivaException("La reserva vencio o ya no esta activa");
        }
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

    private static boolean esButacaDuplicada(DataIntegrityViolationException ex) {
        String mensaje = ex.getMostSpecificCause().getMessage();
        return mensaje != null && mensaje.toLowerCase(Locale.ROOT).contains(RESTRICCION_BUTACA);
    }

    private void validarButacasSinDuplicados(List<Long> butacasIds) {
        Set<Long> idsUnicos = new HashSet<>(butacasIds);
        if (idsUnicos.size() != butacasIds.size()) {
            throw new ReglaNegocioException("No se puede seleccionar dos veces la misma butaca");
        }
    }

    private String generarCodigoEntrada(Set<String> codigosGenerados) {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(LARGO_CODIGO);
            for (int i = 0; i < LARGO_CODIGO; i++) {
                sb.append(ALFABETO_CODIGO.charAt(random.nextInt(ALFABETO_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (codigosGenerados.contains(codigo) || entradaRepository.existsByCodigoQr(codigo));
        codigosGenerados.add(codigo);
        return codigo;
    }

    private List<TipoEntrada> tiposPorButaca(List<EntradaSolicitadaDTO> entradas, int cantidadButacas) {
        int cantidadEntradas = entradas.stream().mapToInt(EntradaSolicitadaDTO::cantidad).sum();
        if (cantidadEntradas != cantidadButacas) {
            throw new ReglaNegocioException(
                "La cantidad de entradas (" + cantidadEntradas + ") no coincide con la cantidad de butacas elegidas (" + cantidadButacas + ")"
            );
        }
        List<TipoEntrada> tipos = new ArrayList<>();
        for (EntradaSolicitadaDTO entrada : entradas) {
            for (int i = 0; i < entrada.cantidad(); i++) {
                tipos.add(entrada.tipo());
            }
        }
        return tipos;
    }

    private static List<Butaca> ordenarComoSeSolicitaron(List<Butaca> butacas, List<Long> idsSolicitados) {
        Map<Long, Butaca> porId = butacas.stream().collect(Collectors.toMap(Butaca::getId, Function.identity()));
        return idsSolicitados.stream().map(porId::get).filter(Objects::nonNull).toList();
    }
}
