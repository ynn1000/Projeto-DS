package ds.FinHut8.caixa.dtos;

import ds.FinHut8.caixa.Caixa;
import java.math.BigDecimal;

public record CaixaResponseDTO(
        BigDecimal saldoInicial,
        BigDecimal saldoAtual,
        BigDecimal limiteDespesas
) {
    public CaixaResponseDTO(Caixa caixa) {
        this(
                caixa.saldoInicial(),
                caixa.saldoAtual(),
                caixa.limiteDespesas()
        );
    }
}