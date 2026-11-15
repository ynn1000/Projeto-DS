package ds.FinHut8.caixa.dtos;

import java.math.BigDecimal;

public record CaixaUpdateDTO(
        BigDecimal limiteDespesas
) {}