package ds.FinHut8.transacao.dtos;

import ds.FinHut8.transacao.CategoriaTransacao;
import ds.FinHut8.transacao.TipoTransacao;
import ds.FinHut8.transacao.Transacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TransacaoResponseDTO(Integer id, String descricao, TipoTransacao tipo,
        CategoriaTransacao categoriaTransacao, BigDecimal valor, LocalDate dataMovimento,
        Integer categoriaOrcamentariaId, Integer subverbaId, Integer projetoId, Integer prestacaoId,
        String registradoPorId, OffsetDateTime criadoEm) {
    public TransacaoResponseDTO(Transacao t) {
        this(t.id(), t.descricao(), t.tipo(), t.categoriaTransacao(), t.valor(), t.dataMovimento(),
                t.categoriaOrcamentariaId(), t.subverbaId(), t.projetoId(), t.prestacaoId(),
                t.registradoPorId(), t.criadoEm());
    }
}
