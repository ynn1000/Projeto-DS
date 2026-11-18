package ds.FinHut8.contrato.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrestacaoCreateDTO(Integer numero, BigDecimal valor, LocalDate dataVencimento) {}
