package ds.FinHut8.contrato.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados necessários para cadastrar um contrato. O status inicial é definido pelo Service. */
public record ContratoCreateDTO(
        Integer projetoId,
        String clienteNome,
        String arquivoRef,
        BigDecimal valorTotal,
        LocalDate dataInicio,
        LocalDate dataFim
) {
}
