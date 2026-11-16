package ds.FinHut8.solicitacao_orcamento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ds.FinHut8.solicitacao_orcamento.enums.OrcamentoStatus;

/** Representa uma linha da tabela solicitacao_orcamento (com nomes vindos de JOIN). */
public record Orcamento(
        Integer id,
        Integer solicitanteId,
        String solicitanteNome,
        Integer categoriaId,
        String categoriaNome,
        String titulo,
        String descricao,
        BigDecimal valorSolicitado,
        String arquivoRef,
        OrcamentoStatus status,
        Integer avaliadoPorId,
        String justificativaDecisao,
        OffsetDateTime dataDecisao,
        OffsetDateTime criadoEm
) {}
