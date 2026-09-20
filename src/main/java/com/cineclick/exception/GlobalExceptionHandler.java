package com.cineclick.exception;

import com.cineclick.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarNotFound(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ButacaNoDisponibleException.class)
    public ResponseEntity<ErrorResponseDTO> manejarButacaNoDisponible(ButacaNoDisponibleException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> manejarReglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Datos de entrada invalidos", request.getRequestURI(), detalles);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> manejarIntegridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        return construirRespuesta(
            HttpStatus.CONFLICT,
            "No se pudo completar la operacion porque viola una restriccion de la base de datos",
            request.getRequestURI(),
            List.of("Verifique valores duplicados, por ejemplo una butaca ya vendida para esa funcion")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarGeneral(Exception ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request.getRequestURI(), List.of());
    }

    private ResponseEntity<ErrorResponseDTO> construirRespuesta(HttpStatus status, String mensaje, String path, List<String> detalles) {
        ErrorResponseDTO response = new ErrorResponseDTO(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            mensaje,
            path,
            detalles
        );
        return ResponseEntity.status(status).body(response);
    }
}
