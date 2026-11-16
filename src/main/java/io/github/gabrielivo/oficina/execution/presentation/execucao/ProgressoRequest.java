package io.github.gabrielivo.oficina.execution.presentation.execucao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProgressoRequest(@NotBlank @Size(max = 500) String descricao) {}
