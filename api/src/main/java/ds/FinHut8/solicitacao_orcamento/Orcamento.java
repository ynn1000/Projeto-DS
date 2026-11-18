package ds.FinHut8.solicitacao_orcamento;

import ds.FinHut8.solicitacao_orcamento.enums.OrcamentoStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Representa uma linha da tabela solicitacao_orcamento (com nomes vindos de JOIN). */
public record Orcamento(
        Integer id,
        String solicitanteId,
        String solicitanteNome,
        Integer categoriaId,
        String categoriaNome,
        String titulo,
        String descricao,
        BigDecimal valorSolicitado,
        String arquivoRef,
        OrcamentoStatus status,
        String avaliadoPorId,
        String justificativaDecisao,
        OffsetDateTime dataDecisao,
        OffsetDateTime criadoEm
) {}