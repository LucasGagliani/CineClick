package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.PromocionRequestDTO;
import com.cineclick.dto.PromocionResponseDTO;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.PromocionRechazadaException;
import com.cineclick.exception.RecursoEnUsoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Cliente;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.Funcion;
import com.cineclick.model.Promocion;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Reglas de las promociones. Toda la validacion pasa por validarPromocionParaCompra (al aplicar el
 * codigo) y por confirmarUso (al pagar, dentro de la transaccion del pago).
 *
 * Semantica de cada regla:
 * - Vigencia: se compara con el momento de la compra (al aplicar el codigo y otra vez al pagar).
 * - Dias de la semana: se comparan con el dia de la FUNCION, no con el de la compra. Sin dias = todos.
 * - Usos por usuario: compras CONFIRMADAS del cliente con esa promocion; las canceladas y las
 *   arrepentidas no cuentan.
 * - Primera compra: el cliente no tiene ninguna compra CONFIRMADA (las arrepentidas no cuentan).
 * - Tope total: usosActuales cuenta las compras pagadas con la promocion. Se incrementa al confirmar
 *   el pago (no al aplicar el codigo: las reservas que vencen no consumen cupos) y baja si se aprueba
 *   un arrepentimiento.
 * - Entradas con descuento y monto maximo: ver Promocion.descuentosPorEntrada (las N entradas de mayor
 *   precio; el total del descuento se recorta al monto maximo).
 */
@Service
@RequiredArgsConstructor
public class PromocionService {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final PromocionRepository promocionRepository;
    private final CompraRepository compraRepository;
    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<PromocionResponseDTO> listarTodas(Long usuarioId) {
        Optional<Cliente> cliente = clienteDe(usuarioId);
        return promocionRepository.findAllByOrderByVigenciaHastaDesc().stream()
            .map(promocion -> conUso(promocion, cliente))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PromocionResponseDTO> listarVigentes(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        Optional<Cliente> cliente = clienteDe(usuarioId);
        return promocionRepository.findByActivaTrue().stream()
            .filter(promocion -> promocion.estaVigente(ahora) && !promocion.estaAgotada())
            .sorted(Comparator.comparing(Promocion::getVigenciaHasta))
            .map(promocion -> conUso(promocion, cliente))
            .toList();
    }

    @Transactional
    public PromocionResponseDTO crear(PromocionRequestDTO dto) {
        Promocion promocion = new Promocion();
        copiarDatos(dto, promocion);
        return DtoMapper.toPromocionResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public PromocionResponseDTO actualizar(Long id, PromocionRequestDTO dto) {
        Promocion promocion = buscarEntidad(id);
        copiarDatos(dto, promocion);
        return DtoMapper.toPromocionResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public PromocionResponseDTO cambiarEstado(Long id, boolean activa) {
        Promocion promocion = buscarEntidad(id);
        promocion.setActiva(activa);
        return DtoMapper.toPromocionResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public void eliminar(Long id) {
        Promocion promocion = buscarEntidad(id);
        if (compraRepository.existsByPromocionId(id)) {
            throw new RecursoEnUsoException("La promocion tiene compras asociadas", List.of("compras"));
        }
        promocionRepository.delete(promocion);
    }

    // Al aplicar el codigo a una reserva: todas las reglas, en orden (1 a 7), cada una con su mensaje
    public Promocion validarPromocionParaCompra(String codigo, Cliente cliente, Funcion funcion, LocalDateTime ahora) {
        Promocion promocion = promocionRepository.findByCodigoIgnoreCase(codigo.trim())
            .orElseThrow(() -> new PromocionRechazadaException("Ese código no existe"));
        validarCondiciones(promocion, funcion, ahora);
        validarUsuario(promocion, cliente);
        if (promocion.estaAgotada()) {
            throw new PromocionRechazadaException("Esta promoción se agotó");
        }
        return promocion;
    }

    // Al confirmar el pago, dentro de su transaccion: primero las reglas que no dependen del usuario,
    // despues el incremento atomico (toma el lock de la fila y serializa las confirmaciones concurrentes;
    // si no afecta filas, se agoto) y recien despues las reglas del usuario. Si alguna de estas falla, se
    // deshace el incremento.
    public void confirmarUso(Compra compra, LocalDateTime ahora) {
        Promocion promocion = compra.getPromocion();
        validarCondiciones(promocion, compra.getFuncion(), ahora);
        if (promocionRepository.incrementarUso(promocion.getId()) == 0) {
            throw new PromocionRechazadaException("Esta promoción se agotó");
        }
        try {
            validarUsuario(promocion, compra.getCliente());
        } catch (PromocionRechazadaException e) {
            promocionRepository.decrementarUso(promocion.getId());
            throw e;
        }
    }

    // Un arrepentimiento aprobado devuelve el uso
    public void devolverUso(Promocion promocion) {
        promocionRepository.decrementarUso(promocion.getId());
    }

    private void validarCondiciones(Promocion promocion, Funcion funcion, LocalDateTime ahora) {
        if (!promocion.isActiva()) {
            throw new PromocionRechazadaException("Esta promoción no está disponible");
        }
        if (ahora.isBefore(promocion.getVigenciaDesde())) {
            throw new PromocionRechazadaException("Empieza el " + promocion.getVigenciaDesde().format(DIA_MES)
                + " a las " + promocion.getVigenciaDesde().format(HORA));
        }
        if (ahora.isAfter(promocion.getVigenciaHasta())) {
            throw new PromocionRechazadaException("Esta promoción es válida del " + promocion.getVigenciaDesde().format(DIA_MES)
                + " al " + promocion.getVigenciaHasta().format(DIA_MES));
        }
        DayOfWeek diaFuncion = funcion.getFechaHoraInicio().getDayOfWeek();
        if (!promocion.valeElDia(diaFuncion)) {
            throw new PromocionRechazadaException("Esta promoción vale " + diasEnTexto(promocion)
                + ", y tu función es el " + nombreDia(diaFuncion));
        }
    }

    private void validarUsuario(Promocion promocion, Cliente cliente) {
        motivoNoUsable(promocion, cliente).ifPresent(motivo -> {
            throw new PromocionRechazadaException(motivo);
        });
    }

    private Optional<String> motivoNoUsable(Promocion promocion, Cliente cliente) {
        if (promocion.isSoloPrimeraCompra() && compraRepository.existsByClienteIdAndEstado(cliente.getId(), EstadoCompra.CONFIRMADA)) {
            return Optional.of("Esta promoción es solo para tu primera compra");
        }
        if (promocion.getUsosMaximosPorUsuario() != null
            && compraRepository.countByClienteIdAndPromocionIdAndEstado(cliente.getId(), promocion.getId(), EstadoCompra.CONFIRMADA)
                >= promocion.getUsosMaximosPorUsuario()) {
            return Optional.of("Ya usaste esta promoción");
        }
        return Optional.empty();
    }

    // Con sesion de cliente, cada promocion dice si ese cliente la puede usar y, si no, por que
    private PromocionResponseDTO conUso(Promocion promocion, Optional<Cliente> cliente) {
        if (cliente.isEmpty()) {
            return DtoMapper.toPromocionResponse(promocion);
        }
        Optional<String> motivo = motivoNoUsable(promocion, cliente.get());
        return DtoMapper.toPromocionResponse(promocion, motivo.isEmpty(), motivo.orElse(null));
    }

    private Optional<Cliente> clienteDe(Long usuarioId) {
        return usuarioId == null ? Optional.empty() : clienteRepository.findByUsuarioId(usuarioId);
    }

    private static String diasEnTexto(Promocion promocion) {
        List<String> dias = promocion.getDiasSemana().stream().sorted().map(PromocionService::nombreDiaPlural).toList();
        String lista = dias.size() == 1 ? dias.get(0) : String.join(", ", dias.subList(0, dias.size() - 1)) + " y " + dias.get(dias.size() - 1);
        return "los " + lista;
    }

    private static String nombreDia(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, ES);
    }

    private static String nombreDiaPlural(DayOfWeek dia) {
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY ? nombreDia(dia) + "s" : nombreDia(dia);
    }

    @Transactional(readOnly = true)
    public Promocion buscarPorCodigo(String codigo) {
        return promocionRepository.findByCodigoIgnoreCase(codigo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Promocion no encontrada con codigo " + codigo));
    }

    private Promocion buscarEntidad(Long id) {
        return promocionRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Promocion no encontrada con id " + id));
    }

    private void copiarDatos(PromocionRequestDTO dto, Promocion promocion) {
        if (!dto.vigenciaHasta().isAfter(dto.vigenciaDesde())) {
            throw new CampoInvalidoException("vigenciaHasta", "Tiene que ser posterior al inicio");
        }
        if (dto.usosMaximosTotales() != null && dto.usosMaximosTotales() < promocion.getUsosActuales()) {
            throw new CampoInvalidoException("usosMaximosTotales", "No puede ser menor que los usos que ya tuvo (" + promocion.getUsosActuales() + ")");
        }
        String codigo = dto.codigo().trim().toUpperCase(Locale.ROOT);
        promocionRepository.findByCodigoIgnoreCase(codigo)
            .filter(otra -> !Objects.equals(otra.getId(), promocion.getId()))
            .ifPresent(otra -> {
                throw new CampoInvalidoException("codigo", "Ya existe una promoción con ese código");
            });
        promocion.setCodigo(codigo);
        promocion.setTitulo(Normalizador.texto(dto.titulo()));
        promocion.setDescripcion(Normalizador.texto(dto.descripcion()));
        promocion.setPorcentajeDescuento(dto.porcentajeDescuento());
        promocion.setVigenciaDesde(dto.vigenciaDesde());
        promocion.setVigenciaHasta(dto.vigenciaHasta());
        promocion.setDiasSemana(dto.diasSemana() == null ? new HashSet<>() : new HashSet<>(dto.diasSemana()));
        promocion.setUsosMaximosTotales(dto.usosMaximosTotales());
        promocion.setUsosMaximosPorUsuario(dto.usosMaximosPorUsuario());
        promocion.setSoloPrimeraCompra(dto.soloPrimeraCompra());
        promocion.setMaxEntradasConDescuento(dto.maxEntradasConDescuento());
        promocion.setMontoMaximoDescuento(dto.montoMaximoDescuento());
        promocion.setActiva(dto.activa());
    }
}
