package com.cineclick.exception;

import com.cineclick.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarDuplicado(RecursoDuplicadoException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponseDTO> manejarCredenciales(CredencialesInvalidasException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), List.of());
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
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Hay datos invalidos", request.getRequestURI(), detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> manejarJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El JSON enviado no es valido", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> manejarTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return construirRespuesta(
            HttpStatus.BAD_REQUEST,
            "Valor invalido para " + ex.getName() + ": " + ex.getValue(),
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> manejarParametroFaltante(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return construirRespuesta(
            HttpStatus.BAD_REQUEST,
            "Falta el parametro " + ex.getParameterName(),
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> manejarRutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.NOT_FOUND, "No existe ese endpoint", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return construirRespuesta(
            HttpStatus.METHOD_NOT_ALLOWED,
            ex.getMethod() + " no esta permitido en este endpoint",
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarContentType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return construirRespuesta(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "El body tiene que ser JSON (Content-Type: application/json)",
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> manejarIntegridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        return construirRespuesta(
            HttpStatus.CONFLICT,
            "El dato ya existe o esta en uso",
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarGeneral(Exception ex, HttpServletRequest request) {
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado en el servidor", request.getRequestURI(), List.of());
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
