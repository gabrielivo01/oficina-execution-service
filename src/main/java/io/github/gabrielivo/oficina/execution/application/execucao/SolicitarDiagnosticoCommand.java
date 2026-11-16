package io.github.gabrielivo.oficina.execution.application.execucao;

public record SolicitarDiagnosticoCommand(String osId, String placa, String observacao, int prioridade) {}
