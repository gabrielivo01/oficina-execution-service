package io.github.gabrielivo.oficina.execution.presentation.execucao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ConcluirDiagnosticoRequest(@NotEmpty List<@Valid ItemDiagnosticoRequest> itens) {}
