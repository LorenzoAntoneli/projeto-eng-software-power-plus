package br.edu.fatec.rp.powerplus.web;

import br.edu.fatec.rp.powerplus.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.put(fe.getField(), fe.getDefaultMessage());
        }
        String primeiraMensagem = campos.values().stream().findFirst().orElse("Dados inválidos no formulário.");
        return build(HttpStatus.BAD_REQUEST, "VALIDACAO_CAMPOS", primeiraMensagem, campos);
    }

    @ExceptionHandler({ConflitoHorarioException.class, DuplicidadeCadastroException.class})
    public ResponseEntity<Map<String, Object>> handleConflito(RegraNegocioException ex) {
        return build(HttpStatus.CONFLICT, "CONFLITO_NEGOCIO", ex.getMessage(), null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleRegraNegocio(RegraNegocioException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_NEGOCIO", ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", ex.getMessage(), null);
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoAutorizado(NaoAutorizadoException ex) {
        return build(HttpStatus.UNAUTHORIZED, "NAO_AUTORIZADO", ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String codigo,
                                                      String mensagem, Map<String, String> campos) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("codigo", codigo);
        body.put("mensagem", mensagem);
        if (campos != null && !campos.isEmpty()) {
            body.put("campos", campos);
        }
        return ResponseEntity.status(status).body(body);
    }
}
