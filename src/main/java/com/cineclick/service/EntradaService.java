package com.cineclick.service;

import com.cineclick.dto.ValidacionEntradaResponseDTO;
import com.cineclick.exception.RecursoNoEncontradoException;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.Funcion;
import com.cineclick.model.MotivoFueraDeHorario;
import com.cineclick.model.ResultadoValidacion;
import com.cineclick.repository.EntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EntradaService {

    private final EntradaRepository entradaRepository;
    private final Clock reloj;

    @Value("${cineclick.ingreso.minutos-antes:60}")
    private int minutosAntes;

    @Value("${cineclick.ingreso.minutos-despues:30}")
    private int minutosDespues;

    @Transactional
    public ValidacionEntradaResponseDTO validar(String codigo, Long cineId) {
        String limpio = codigo == null ? "" : codigo.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        Entrada entrada = entradaRepository.findByCodigoQrIgnoreCase(limpio)
            .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese codigo"));
        Funcion funcion = entrada.getFuncion();

        if (entrada.getEstado() == EstadoEntrada.CANCELADA || entrada.getCompra().getEstado() != EstadoCompra.CONFIRMADA) {
            return respuesta(ResultadoValidacion.ANULADA, null, entrada, entrada.getUsadaEn());
        }
        if (entrada.getEstado() == EstadoEntrada.USADA) {
            return respuesta(ResultadoValidacion.YA_USADA, null, entrada, entrada.getUsadaEn());
        }
        if (!funcion.getSala().getCine().getId().equals(cineId)) {
            return respuesta(ResultadoValidacion.OTRO_CINE, null, entrada, null);
        }
        LocalDateTime ahora = LocalDateTime.now(reloj).withNano(0);
        Optional<MotivoFueraDeHorario> fuera = motivoFueraDeHorario(funcion.getFechaHoraInicio(), ahora, minutosAntes, minutosDespues);
        if (fuera.isPresent()) {
            return respuesta(ResultadoValidacion.FUERA_DE_HORARIO, fuera.get(), entrada, null);
        }
        if (entradaRepository.marcarUsadaSiEstaEmitida(entrada.getId(), ahora) == 1) {
            return respuesta(ResultadoValidacion.VALIDA, null, entrada, ahora);
        }
        return respuesta(ResultadoValidacion.YA_USADA, null, entrada, entradaRepository.usadaEn(entrada.getId()).orElse(null));
    }

    public static Optional<MotivoFueraDeHorario> motivoFueraDeHorario(LocalDateTime inicio, LocalDateTime ahora, int minutosAntes, int minutosDespues) {
        boolean mismoDia = ahora.toLocalDate().equals(inicio.toLocalDate());
        if (ahora.isBefore(inicio.minusMinutes(minutosAntes))) {
            return Optional.of(mismoDia ? MotivoFueraDeHorario.DEMASIADO_TEMPRANO : MotivoFueraDeHorario.OTRO_DIA);
        }
        if (ahora.isAfter(inicio.plusMinutes(minutosDespues))) {
            return Optional.of(mismoDia ? MotivoFueraDeHorario.FUNCION_TERMINADA : MotivoFueraDeHorario.OTRO_DIA);
        }
        return Optional.empty();
    }

    private ValidacionEntradaResponseDTO respuesta(ResultadoValidacion resultado, MotivoFueraDeHorario motivo, Entrada entrada, LocalDateTime usadaEn) {
        Funcion funcion = entrada.getFuncion();
        return new ValidacionEntradaResponseDTO(
            resultado,
            motivo,
            entrada.getCodigoQr(),
            funcion.getPelicula().getTitulo(),
            funcion.getSala().getCine().getId(),
            funcion.getSala().getCine().getNombre(),
            funcion.getSala().getNombre(),
            entrada.getButaca().codigo(),
            funcion.getFechaHoraInicio(),
            entrada.getTipo(),
            usadaEn,
            funcion.getFechaHoraInicio().minusMinutes(minutosAntes),
            funcion.getFechaHoraInicio().plusMinutes(minutosDespues)
        );
    }
}
