package io.github.gabrielivo.oficina.execution.shared;

import io.github.gabrielivo.oficina.execution.application.execucao.TarefaExecucaoService.TarefaNaoEncontradaException;
import io.github.gabrielivo.oficina.execution.application.peca.PecaNaoEncontradaException;
import io.github.gabrielivo.oficina.execution.application.reserva.ReservaPecasService.ReservaNaoEncontradaException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        PecaNaoEncontradaException.class,
        TarefaNaoEncontradaException.class,
        ReservaNaoEncontradaException.class
    })
    public ResponseEntity<Map<String, String>> naoEncontrado(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, String>> requisicaoInvalida(Exception exception) {
        String mensagem = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .findFirst()
                        .orElse("Requisição inválida.")
                : exception.getMessage();
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflito(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", exception.getMessage()));
    }
}
