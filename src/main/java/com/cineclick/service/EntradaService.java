package com.cineclick.service;

import com.cineclick.dto.ValidacionEntradaResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.Funcion;
import com.cineclick.model.ResultadoValidacion;
import com.cineclick.repository.EntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntradaService {

    private final EntradaRepository entradaRepository;

    @Transactional
    public ValidacionEntradaResponseDTO validar(String codigo, Long funcionId) {
        Entrada entrada = entradaRepository.findByCodigoQrIgnoreCase(codigo.trim())
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe una entrada con codigo " + codigo));

        if (!entrada.getFuncion().getId().equals(funcionId)) {
            return respuesta(ResultadoValidacion.OTRA_FUNCION, entrada);
        }
        if (entrada.getCompra().getEstado() != EstadoCompra.CONFIRMADA) {
            return respuesta(ResultadoValidacion.NO_CONFIRMADA, entrada);
        }
        int filas = entradaRepository.marcarUsadaSiEstaEmitida(entrada.getId(), funcionId);
        return respuesta(filas == 1 ? ResultadoValidacion.VALIDA : ResultadoValidacion.YA_USADA, entrada);
    }

    private ValidacionEntradaResponseDTO respuesta(ResultadoValidacion resultado, Entrada entrada) {
        Funcion funcion = entrada.getFuncion();
        return new ValidacionEntradaResponseDTO(
            resultado,
            entrada.getCodigoQr(),
            funcion.getId(),
            funcion.getPelicula().getTitulo(),
            funcion.getSala().getCine().getNombre(),
            funcion.getSala().getNombre(),
            funcion.getFechaHoraInicio(),
            entrada.getButaca().codigo()
        );
    }
}
