package ds.FinHut8.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Representação da tabela public.prestacao. */
public record Prestacao(
        Integer id,
        Integer contratoId,
        Integer numero,
        BigDecimal valor,
        LocalDate dataVencimento,
        StatusPrestacao status
) {
}
