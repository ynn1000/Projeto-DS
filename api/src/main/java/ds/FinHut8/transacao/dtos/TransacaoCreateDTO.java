package ds.FinHut8.transacao.dtos;

import ds.FinHut8.transacao.CategoriaTransacao;
import ds.FinHut8.transacao.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoCreateDTO(String descricao, TipoTransacao tipo, CategoriaTransacao categoriaTransacao,
        BigDecimal valor, LocalDate dataMovimento, Integer categoriaOrcamentariaId, Integer subverbaId,
        Integer projetoId, Integer prestacaoId) {}
