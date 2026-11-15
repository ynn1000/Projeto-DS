package ds.FinHut8.orcamento.dtos;

import java.math.BigDecimal;

public record SubverbaDTO(
        Integer id,
        Integer orcamentoPeriodoId,
        Integer categoriaOrcamentariaId,
        String nome,
        BigDecimal valorReservado,
        BigDecimal valorUtilizado,
        BigDecimal saldoSubverba
) {}
