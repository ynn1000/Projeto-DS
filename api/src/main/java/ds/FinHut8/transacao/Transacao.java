package ds.FinHut8.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Transacao(
        Integer id,
        String descricao,
        TipoTransacao tipo,
        CategoriaTransacao categoriaTransacao,
        BigDecimal valor,
        LocalDate dataMovimento,
        Integer categoriaOrcamentariaId,
        Integer subverbaId,
        OffsetDateTime criadoEm
) {}
