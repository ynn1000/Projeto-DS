package ds.FinHut8.caixa;

import java.math.BigDecimal;

/** Representa um caixa. O saldoAtual é calculado pela view vw_saldo_caixa. */
public record Caixa(
        Integer id, /**Talvez de para tirar isso depois */
        BigDecimal saldoInicial,
        BigDecimal limiteDespesas,
        BigDecimal saldoAtual
) {}