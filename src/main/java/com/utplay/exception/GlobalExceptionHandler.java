package com.utplay.exception;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler({ ForbiddenException.class, AccessDeniedException.class })
    ResponseEntity<ApiError> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Revise los datos enviados", request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "La operacion entra en conflicto con datos existentes", request, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado. Intente nuevamente", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> manejarRutaNoEncontrada(
            NoResourceFoundException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED,
                "El método HTTP no está permitido para esta ruta", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> manejarParametroFaltante(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio: " + ex.getParameterName(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor no válido", request, null);
    }

    private ResponseEntity<ApiError> respuesta(
            HttpStatus status,
            String mensaje,
            HttpServletRequest request,
            Map<String, String> errores) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI(),
                errores);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ExcepcionIntentos.class)
    ResponseEntity<ApiError> manejarDemasiadosIntentos(
            ExcepcionIntentos ex, HttpServletRequest request) {
        ApiError cuerpo = new ApiError(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaReintentar()))
                .body(cuerpo);
    }
}
