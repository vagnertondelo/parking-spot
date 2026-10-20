package com.api.parkingcontrol.controllers;

import com.api.parkingcontrol.dtos.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte as exceções da API em respostas JSON no formato de ErrorResponseDto:
 * { "status": 400, "message": "...", "timestamp": "...", "errors": { ... } }
 * Nenhum stack trace ou detalhe do banco é enviado ao cliente (apenas registrado no log).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // Conflito de unicidade detectado pelo próprio banco (ex.: duas requisições simultâneas com a mesma placa)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "Não foi possível salvar: já existe uma vaga com esta placa ou este número de vaga.");
    }

    // Campo de ordenação inexistente em /parking-spot/order?sort=...
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Object> handlePropertyReference(PropertyReferenceException ex) {
        return error(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido: '" + ex.getPropertyName() + "'.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor.");
    }

    // Ponto único usado pelo ResponseEntityExceptionHandler para todas as exceções padrão do Spring MVC
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatus status, WebRequest request) {
        var error = new ErrorResponseDto(status.value(), messageFor(ex, status));
        if (ex instanceof BindException) {
            Map<String, String> errors = new LinkedHashMap<>();
            for (FieldError fieldError : ((BindException) ex).getFieldErrors()) {
                errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
            }
            error.setErrors(errors);
        }
        return super.handleExceptionInternal(ex, error, headers, status, request);
    }

    private String messageFor(Exception ex, HttpStatus status) {
        if (ex instanceof BindException) {
            return "Dados inválidos. Verifique os campos informados.";
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Corpo da requisição ausente ou com JSON inválido.";
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "Formato não suportado. Envie o cabeçalho 'Content-Type: application/json'.";
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return "Método HTTP não suportado para este endpoint.";
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return "Valor inválido para o parâmetro '" + ((MethodArgumentTypeMismatchException) ex).getName() + "'.";
        }
        if (ex instanceof TypeMismatchException) {
            return "Valor inválido para o parâmetro '" + ((TypeMismatchException) ex).getPropertyName() + "'.";
        }
        if (ex instanceof NoHandlerFoundException) {
            return "Endpoint não encontrado.";
        }
        return status.is5xxServerError() ? "Erro interno no servidor." : "Requisição inválida.";
    }

    private ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponseDto(status.value(), message));
    }

}
