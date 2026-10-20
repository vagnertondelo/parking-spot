package com.api.parkingcontrol.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Corpo padrão das respostas de erro da API.
 * O campo "errors" só aparece em erros de validação (campo -> mensagem).
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {

    private int status;

    private String message;

    private LocalDateTime timestamp;

    private Map<String, String> errors;

    public ErrorResponseDto(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now(ZoneId.of("UTC"));
    }

}
