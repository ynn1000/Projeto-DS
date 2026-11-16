package ds.FinHut8.solicitacao_orcamento.dtos;

import java.math.BigDecimal;

/** Corpo do POST /orcamentos (solicitação feita pelo usuário). */
public record OrcamentoRequestDTO(
        Integer categoriaId,
        String titulo,
        String descricao,
        BigDecimal valorSolicitado,
        String arquivoRef // opcional
) {}
