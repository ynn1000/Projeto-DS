package ds.FinHut8.transacao;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PrestacaoOpcaoDTO(Integer id, Integer contratoId, Integer projetoId, String projetoNome,
        String clienteNome, Integer numero, BigDecimal valor, LocalDate dataVencimento) {}
