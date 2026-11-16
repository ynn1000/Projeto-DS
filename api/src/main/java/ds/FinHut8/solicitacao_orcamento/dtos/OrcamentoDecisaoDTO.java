package ds.FinHut8.solicitacao_orcamento.dtos;

import ds.FinHut8.solicitacao_orcamento.enums.OrcamentoStatus;

/** Corpo do PATCH /orcamentos/{id}/status (feito pelo admin). */
public record OrcamentoDecisaoDTO(
        OrcamentoStatus status,
        String justificativa // obrigatória quando REJEITADO
) {}
