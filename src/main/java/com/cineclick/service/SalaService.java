package com.cineclick.service;

import com.cineclick.dto.DtoMapper;
import com.cineclick.dto.SalaRequestDTO;
import com.cineclick.dto.SalaResponseDTO;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.exception.RecursoEnUsoException;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.Butaca;
import com.cineclick.model.Cine;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoButaca;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SalaService {

    private final SalaRepository salaRepository;
    private final FuncionRepository funcionRepository;
    private final CompraRepository compraRepository;
    private final CineService cineService;

    @Transactional(readOnly = true)
    public List<SalaResponseDTO> listarTodas() {
        return salaRepository.findAll().stream()
            .sorted(Comparator.comparing((Sala sala) -> sala.getCine().getNombre()).thenComparing(Sala::getNumero))
            .map(DtoMapper::toSalaResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SalaResponseDTO> listarDelCine(Long cineId) {
        cineService.buscarEntidad(cineId);
        return salaRepository.findByCineIdAndActivaTrue(cineId).stream()
            .map(DtoMapper::toSalaResponse)
            .toList();
    }

    @Transactional
    public SalaResponseDTO crear(Long cineId, SalaRequestDTO dto) {
        Cine cine = cineService.buscarEntidad(cineId);
        Sala sala = new Sala();
        copiarDatos(dto, sala, cineId);
        List<Butaca> butacas = dto.plano() != null ? dto.plano().crearButacas(sala) : butacasUniformes(dto);
        butacas.forEach(sala::agregarButaca);
        cine.agregarSala(sala);
        return DtoMapper.toSalaResponse(salaRepository.save(sala));
    }

    @Transactional
    public SalaResponseDTO actualizar(Long id, SalaRequestDTO dto) {
        Sala sala = buscarEntidad(id);
        copiarDatos(dto, sala, sala.getCine().getId());
        return DtoMapper.toSalaResponse(salaRepository.save(sala));
    }

    @Transactional
    public SalaResponseDTO cambiarEstado(Long id, boolean activa) {
        Sala sala = buscarEntidad(id);
        sala.setActiva(activa);
        return DtoMapper.toSalaResponse(salaRepository.save(sala));
    }

    @Transactional
    public void eliminar(Long id) {
        Sala sala = buscarEntidad(id);
        List<String> asociados = new ArrayList<>();
        if (funcionRepository.existsBySalaId(id)) {
            asociados.add("funciones");
        }
        if (compraRepository.existsByFuncionSalaId(id)) {
            asociados.add("compras");
        }
        if (!asociados.isEmpty()) {
            throw new RecursoEnUsoException("La sala tiene " + String.join(", ", asociados) + " asociadas", asociados);
        }
        sala.getCine().getSalas().remove(sala);
        salaRepository.delete(sala);
    }

    @Transactional(readOnly = true)
    public Sala buscarEntidad(Long id) {
        return salaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Sala no encontrada con id " + id));
    }

    private void copiarDatos(SalaRequestDTO dto, Sala sala, Long cineId) {
        String nombre = Normalizador.texto(dto.nombre());
        String clave = Normalizador.clave(nombre);
        List<Sala> otras = salaRepository.findByCineId(cineId).stream()
            .filter(otra -> !Objects.equals(otra.getId(), sala.getId()))
            .toList();
        if (otras.stream().anyMatch(otra -> Normalizador.clave(otra.getNombre()).equals(clave))) {
            throw new CampoInvalidoException("nombre", "Ya existe una sala con ese nombre en este cine");
        }
        if (otras.stream().anyMatch(otra -> otra.getNumero().equals(dto.numero()))) {
            throw new CampoInvalidoException("numero", "Ya existe una sala con ese número en este cine");
        }
        sala.setNombre(nombre);
        sala.setNumero(dto.numero());
        sala.setTipo(dto.tipo());
    }

    private List<Butaca> butacasUniformes(SalaRequestDTO dto) {
        if (dto.cantidadFilas() == null) {
            throw new CampoInvalidoException("cantidadFilas", "Completá la cantidad de filas");
        }
        if (dto.butacasPorFila() == null) {
            throw new CampoInvalidoException("butacasPorFila", "Completá las butacas por fila");
        }
        if (dto.cantidadFilas() > 26) {
            throw new ReglaNegocioException("La cantidad de filas no puede superar 26");
        }
        List<Butaca> butacas = new ArrayList<>();
        for (int fila = 0; fila < dto.cantidadFilas(); fila++) {
            for (int numero = 1; numero <= dto.butacasPorFila(); numero++) {
                Butaca butaca = new Butaca();
                butaca.setFila(String.valueOf((char) ('A' + fila)));
                butaca.setNumero(numero);
                butaca.setColumna(numero);
                butaca.setTipo(TipoButaca.NORMAL);
                butacas.add(butaca);
            }
        }
        return butacas;
    }
}
