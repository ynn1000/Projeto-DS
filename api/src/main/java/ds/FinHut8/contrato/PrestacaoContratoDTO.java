package ds.FinHut8.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrestacaoContratoDTO(
        Integer id, Integer contratoId, Integer numero, BigDecimal valor,
        LocalDate dataVencimento, String status, Integer transacaoId, Long quantidadeComprovantes
) {}
