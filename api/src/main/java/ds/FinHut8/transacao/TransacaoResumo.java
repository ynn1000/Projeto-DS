package ds.FinHut8.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoResumo(LocalDate dataInicio, LocalDate dataFim, BigDecimal receitas,
                              BigDecimal despesas, BigDecimal resultado, long quantidade) {}
