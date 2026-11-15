package ds.FinHut8.transacao.dtos;

import ds.FinHut8.transacao.CategoriaTransacao;
import ds.FinHut8.transacao.TipoTransacao;
import ds.FinHut8.transacao.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TransacaoResponseDTO(
        Integer id,
        String descricao,
        TipoTransacao tipo,
        CategoriaTransacao categoriaTransacao,
        BigDecimal valor,
        LocalDate dataMovimento,
        Integer categoriaOrcamentariaId,
        Integer subverbaId,
        OffsetDateTime criadoEm
) {
    public TransacaoResponseDTO(Transacao transacao) {
        this(
                transacao.id(),
                transacao.descricao(),
                transacao.tipo(),
                transacao.categoriaTransacao(),
                transacao.valor(),
                transacao.dataMovimento(),
                transacao.categoriaOrcamentariaId(),
                transacao.subverbaId(),
                transacao.criadoEm()
        );
    }
}
