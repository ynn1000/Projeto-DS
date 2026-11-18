package ds.FinHut8.transacao;
import java.time.LocalDate;
public record PeriodoOrcamentoOpcaoDTO(Integer id, String nome, LocalDate dataInicio, LocalDate dataFim) {}
