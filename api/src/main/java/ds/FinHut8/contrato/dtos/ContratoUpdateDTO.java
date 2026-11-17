package ds.FinHut8.contrato.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Substituição dos dados cadastrais do contrato, sem permitir alteração do status. */
public record ContratoUpdateDTO(
        Integer projetoId,
        String clienteNome,
        String arquivoRef,
        BigDecimal valorTotal,
        LocalDate dataInicio,
        LocalDate dataFim
) {
}
