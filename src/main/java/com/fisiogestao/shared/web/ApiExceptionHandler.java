package com.fisiogestao.shared.web;

import com.fisiogestao.agenda.SlotsApiController;
import com.fisiogestao.pacientes.PacientesApiController;
import com.fisiogestao.solicitacoes.SolicitacaoFluxoApiController;
import com.fisiogestao.solicitacoes.SolicitacoesApiController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        SlotsApiController.class,
        PacientesApiController.class,
        SolicitacoesApiController.class,
        SolicitacaoFluxoApiController.class
})
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(new ApiErrorResponse(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String message = fieldErrors.values().stream()
                .findFirst()
                .orElse("Existem campos inválidos na requisição.");
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(message, fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("Não foi possível interpretar o corpo da requisição.", Map.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason();
        String message = reason != null && !reason.isBlank()
                ? reason
                : "A requisição não pôde ser processada.";
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(message, Map.of()));
    }

    public record ApiErrorResponse(String message, Map<String, String> fieldErrors) {
    }
}
