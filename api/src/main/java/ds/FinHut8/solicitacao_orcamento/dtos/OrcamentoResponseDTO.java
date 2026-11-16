package ds.FinHut8.solicitacao_orcamento.dtos;

import ds.FinHut8.solicitacao_orcamento.Orcamento;
import ds.FinHut8.solicitacao_orcamento.enums.OrcamentoStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrcamentoResponseDTO(
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
) {
    public OrcamentoResponseDTO(Orcamento o) {
        this(
                o.id(),
                o.solicitanteId(),
                o.solicitanteNome(),
                o.categoriaId(),
                o.categoriaNome(),
                o.titulo(),
                o.descricao(),
                o.valorSolicitado(),
                o.arquivoRef(),
                o.status(),
                o.avaliadoPorId(),
                o.justificativaDecisao(),
                o.dataDecisao(),
                o.criadoEm()
        );
    }
}
