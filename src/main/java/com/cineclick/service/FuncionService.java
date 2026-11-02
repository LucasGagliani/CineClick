package com.cineclick.service;

import com.cineclick.dto.ButacaResponseDTO;
import com.cineclick.dto.CombinablesResponseDTO;
import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.FuncionRequestDTO;
import com.cineclick.dto.FuncionResponseDTO;
import com.cineclick.dto.PrecioEntradaDTO;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.RecursoEnUsoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Butaca;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import com.cineclick.model.Pelicula;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoEntrada;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.EntradaRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.PeliculaRepository;
import com.cineclick.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuncionService {

    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final ButacaRepository butacaRepository;
    private final EntradaRepository entradaRepository;
    private final VencimientoReservaService vencimientoReservaService;
    private final CompraRepository compraRepository;

    @Value("${cineclick.funciones.minutos-limpieza:20}")
    private int minutosLimpieza;

    @Value("${cineclick.funciones.minutos-publicidad:15}")
    private int minutosPublicidad;

    @Value("${cineclick.combinables.margen-minutos:10}")
    private int margenMinutos;

    @Value("${cineclick.combinables.ventana-horas:3}")
    private int ventanaHoras;

    @Transactional(readOnly = true)
    public List<FuncionResponseDTO> listar(Long peliculaId, Long cineId, LocalDate fecha, FormatoFuncion formato, boolean incluirInactivos) {
        LocalDateTime desde = fecha == null ? null : fecha.atStartOfDay();
        LocalDateTime hasta = fecha == null ? null : fecha.plusDays(1).atStartOfDay();
        return toResponses(funcionRepository.buscarConFiltros(peliculaId, cineId, !incluirInactivos, formato, desde, hasta));
    }

    // Funciones del mismo cine y dia, de otras peliculas y con butacas libres, que terminan antes del inicio
    // de la elegida o empiezan despues de su fin, con al menos el margen y dentro de la ventana. El fin es
    // estimado: inicio + duracion + publicidad (el horario publicado incluye la tanda previa).
    @Transactional(readOnly = true)
    public CombinablesResponseDTO combinables(Long id) {
        Funcion elegida = buscarEntidad(id);
        LocalDateTime inicioElegida = elegida.getFechaHoraInicio();
        LocalDateTime finElegida = finEstimado(elegida);
        LocalDate dia = inicioElegida.toLocalDate();
        LocalDateTime ahora = LocalDateTime.now();
        List<Funcion> candidatas = funcionRepository.buscarConFiltros(
                null, elegida.getSala().getCine().getId(), true, null, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()
            ).stream()
            .filter(f -> f.getEstado() == EstadoFuncion.PROGRAMADA)
            .filter(f -> !f.getPelicula().getId().equals(elegida.getPelicula().getId()))
            .filter(f -> f.getFechaHoraInicio().isAfter(ahora))
            .toList();
        Map<Long, Integer> libres = toResponses(candidatas).stream()
            .collect(Collectors.toMap(FuncionResponseDTO::id, FuncionResponseDTO::butacasLibres));
        long ventana = ventanaHoras * 60L;
        List<CombinablesResponseDTO.Combinable> antes = new ArrayList<>();
        List<CombinablesResponseDTO.Combinable> despues = new ArrayList<>();
        for (Funcion otra : candidatas) {
            if (libres.getOrDefault(otra.getId(), 0) <= 0) {
                continue;
            }
            long entreAntes = Duration.between(finEstimado(otra), inicioElegida).toMinutes();
            long entreDespues = Duration.between(finElegida, otra.getFechaHoraInicio()).toMinutes();
            if (entreAntes >= margenMinutos && entreAntes <= ventana) {
                antes.add(combinable(otra, entreAntes, libres.get(otra.getId())));
            } else if (entreDespues >= margenMinutos && entreDespues <= ventana) {
                despues.add(combinable(otra, entreDespues, libres.get(otra.getId())));
            }
        }
        Comparator<CombinablesResponseDTO.Combinable> porCercania = Comparator.comparingLong(CombinablesResponseDTO.Combinable::minutosEntre);
        return new CombinablesResponseDTO(
            new CombinablesResponseDTO.Referencia(elegida.getId(), elegida.getPelicula().getTitulo(), inicioElegida, finElegida),
            antes.stream().sorted(porCercania).limit(4).toList(),
            despues.stream().sorted(porCercania).limit(4).toList()
        );
    }

    private LocalDateTime finEstimado(Funcion funcion) {
        return funcion.getFechaHoraInicio().plusMinutes(funcion.getPelicula().getDuracionMinutos() + (long) minutosPublicidad);
    }

    private CombinablesResponseDTO.Combinable combinable(Funcion funcion, long minutosEntre, int butacasLibres) {
        return new CombinablesResponseDTO.Combinable(
            funcion.getId(),
            funcion.getPelicula().getId(),
            funcion.getPelicula().getTitulo(),
            funcion.getPelicula().getUrlPoster(),
            funcion.getFormato(),
            funcion.getIdioma(),
            funcion.getSala().getNombre(),
            funcion.getFechaHoraInicio(),
            finEstimado(funcion),
            minutosEntre,
            butacasLibres
        );
    }

    @Transactional(readOnly = true)
    public FuncionResponseDTO buscarPorId(Long id) {
        return toResponses(List.of(buscarEntidad(id))).get(0);
    }

    @Transactional
    public FuncionResponseDTO crear(FuncionRequestDTO dto) {
        Funcion funcion = new Funcion();
        copiarDatos(dto, funcion);
        validarHorario(funcion, null);
        return toResponses(List.of(funcionRepository.save(funcion))).get(0);
    }

    @Transactional
    public FuncionResponseDTO actualizar(Long id, FuncionRequestDTO dto) {
        Funcion funcion = buscarEntidad(id);
        copiarDatos(dto, funcion);
        validarHorario(funcion, id);
        return toResponses(List.of(funcionRepository.save(funcion))).get(0);
    }

    @Transactional
    public FuncionResponseDTO cancelar(Long id) {
        Funcion funcion = buscarEntidad(id);
        funcion.cambiarEstado(EstadoFuncion.CANCELADA);
        return toResponses(List.of(funcionRepository.save(funcion))).get(0);
    }

    @Transactional
    public void eliminar(Long id) {
        Funcion funcion = buscarEntidad(id);
        if (compraRepository.existsByFuncionId(id)) {
            throw new RecursoEnUsoException("La funcion tiene compras asociadas", List.of("compras"));
        }
        funcionRepository.delete(funcion);
    }

    @Transactional
    public List<ButacaResponseDTO> obtenerButacasDisponibles(Long funcionId) {
        Funcion funcion = buscarEntidad(funcionId);
        vencimientoReservaService.cancelarVencidas(funcionId);
        Set<Long> butacasOcupadas = entradaRepository.findByFuncionId(funcionId).stream()
            .filter(entrada -> entrada.getEstado() != EstadoEntrada.CANCELADA)
            .map(entrada -> entrada.getButaca().getId())
            .collect(Collectors.toSet());
        return butacaRepository.findBySalaIdAndActivaTrue(funcion.getSala().getId()).stream()
            .map(butaca -> DtoMapper.toButacaResponse(butaca, !butacasOcupadas.contains(butaca.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PrecioEntradaDTO> listarPrecios(Long funcionId) {
        Funcion funcion = buscarEntidad(funcionId);
        return Arrays.stream(TipoEntrada.values())
            .map(tipo -> DtoMapper.toPrecioEntrada(tipo, funcion.getPrecioBase()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Funcion buscarEntidad(Long id) {
        return funcionRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Funcion no encontrada con id " + id));
    }

    public void validarHorario(Funcion funcion, Long idIgnorado) {
        if (!funcion.getPelicula().getFormatosDisponibles().contains(funcion.getFormato())) {
            throw new CampoInvalidoException("formato", "La película no está disponible en ese formato");
        }
        if (!funcion.getSala().isActiva() || !funcion.getSala().getCine().isActivo()) {
            throw new CampoInvalidoException("salaId", "La sala o su cine están inactivos");
        }
        funcion.calcularHorarioFin(minutosLimpieza);
        if (funcion.getFechaHoraInicio().isBefore(LocalDateTime.now())) {
            throw new CampoInvalidoException("fechaHoraInicio", "La función tiene que ser en el futuro");
        }
        if (funcion.getFechaHoraInicio().getMinute() % 5 != 0) {
            throw new CampoInvalidoException("fechaHoraInicio", "El horario tiene que terminar en 0 o 5 (por ejemplo, 19:30 o 22:15)");
        }
        funcion.setFechaHoraInicio(funcion.getFechaHoraInicio().withSecond(0).withNano(0));
        funcion.calcularHorarioFin(minutosLimpieza);
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
        funcionRepository.findBySalaId(funcion.getSala().getId()).stream()
            .filter(existente -> idIgnorado == null || !existente.getId().equals(idIgnorado))
            .filter(existente -> existente.getEstado() != EstadoFuncion.CANCELADA)
            .filter(existente -> funcion.getFechaHoraInicio().isBefore(existente.getFechaHoraFin())
                && funcion.getFechaHoraFin().isAfter(existente.getFechaHoraInicio()))
            .findFirst()
            .ifPresent(existente -> {
                throw new CampoInvalidoException("fechaHoraInicio", "Se superpone con " + existente.getPelicula().getTitulo()
                    + " (" + existente.getFechaHoraInicio().format(hora) + " a " + existente.getFechaHoraFin().format(hora)
                    + ", con " + minutosLimpieza + " min de limpieza) en esta sala");
            });
    }

    private List<FuncionResponseDTO> toResponses(List<Funcion> funciones) {
        if (funciones.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> vendidasPorFuncion = contarPorId(entradaRepository.contarVigentesPorFuncion(
            funciones.stream().map(Funcion::getId).toList()
        ));
        Map<Long, Long> capacidadPorSala = contarPorId(butacaRepository.contarActivasPorSala(
            funciones.stream().map(funcion -> funcion.getSala().getId()).collect(Collectors.toSet())
        ));
        return funciones.stream()
            .map(funcion -> {
                long capacidad = capacidadPorSala.getOrDefault(funcion.getSala().getId(), 0L);
                long vendidas = vendidasPorFuncion.getOrDefault(funcion.getId(), 0L);
                return DtoMapper.toFuncionResponse(funcion, (int) Math.max(0, capacidad - vendidas), (int) capacidad);
            })
            .toList();
    }

    private Map<Long, Long> contarPorId(Collection<Object[]> filas) {
        Map<Long, Long> conteo = new HashMap<>();
        for (Object[] fila : filas) {
            conteo.put((Long) fila[0], (Long) fila[1]);
        }
        return conteo;
    }

    private void copiarDatos(FuncionRequestDTO dto, Funcion funcion) {
        Pelicula pelicula = peliculaRepository.findById(dto.peliculaId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Pelicula no encontrada con id " + dto.peliculaId()));
        Sala sala = salaRepository.findById(dto.salaId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Sala no encontrada con id " + dto.salaId()));
        funcion.setPelicula(pelicula);
        funcion.setSala(sala);
        funcion.setFechaHoraInicio(dto.fechaHoraInicio());
        funcion.setPrecioBase(dto.precioBase());
        funcion.setFormato(dto.formato());
        funcion.setIdioma(dto.idioma());
    }
}
