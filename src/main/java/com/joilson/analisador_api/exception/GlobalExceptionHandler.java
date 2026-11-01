package com.joilson.analisador_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Requisição Inválida");
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        return problemDetail;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("navegador headless")) {
            var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível acessar os dados do produto no momento. O site pode estar bloqueando o acesso."
            );
            problemDetail.setTitle("Falha na Extração");
            problemDetail.setProperty("timestamp", System.currentTimeMillis());
            return problemDetail;
        }

        var genericProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor.");
        genericProblemDetail.setTitle("Erro de sistema");
        genericProblemDetail.setProperty("timestamp", System.currentTimeMillis());

        return genericProblemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado. Nossa equipe já foi notificada.");
        problemDetail.setTitle("Erro de Sistema");
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        
        return problemDetail;
    }
}
