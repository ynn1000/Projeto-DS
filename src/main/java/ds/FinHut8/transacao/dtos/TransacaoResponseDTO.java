package ds.FinHut8.transacao.dtos;

import ds.FinHut8.transacao.TipoTransacao;
import ds.FinHut8.transacao.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TransacaoResponseDTO(
        Integer id,
        String descricao,
        TipoTransacao tipo,
        BigDecimal valor,
        LocalDate dataMovimento,
        Integer categoriaId,
        OffsetDateTime criadoEm
) {
    public TransacaoResponseDTO(Transacao transacao) {
        this(
                transacao.id(),
                transacao.descricao(),
                transacao.tipo(),
                transacao.valor(),
                transacao.dataMovimento(),
                transacao.categoriaId(),
                transacao.criadoEm()
        );
    }
}
