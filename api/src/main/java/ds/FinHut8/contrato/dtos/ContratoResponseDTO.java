package ds.FinHut8.contrato.dtos;

import ds.FinHut8.contrato.Contrato;
import ds.FinHut8.contrato.enums.StatusContrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Representação pública dos dados do contrato, sem expor a entidade de persistência. */
public record ContratoResponseDTO(
        Integer id,
        Integer projetoId,
        String clienteNome,
        String arquivoRef,
        BigDecimal valorTotal,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusContrato status
) {
    public ContratoResponseDTO(Contrato contrato) {
        this(
                contrato.id(),
                contrato.projetoId(),
                contrato.clienteNome(),
                contrato.arquivoRef(),
                contrato.valorTotal(),
                contrato.dataInicio(),
                contrato.dataFim(),
                contrato.status()
        );
    }
}
