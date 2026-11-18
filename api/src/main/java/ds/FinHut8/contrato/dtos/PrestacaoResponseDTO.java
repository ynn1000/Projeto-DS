package ds.FinHut8.contrato.dtos;

import ds.FinHut8.contrato.Prestacao;
import ds.FinHut8.contrato.StatusPrestacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrestacaoResponseDTO(
        Integer id,
        Integer contratoId,
        Integer numero,
        BigDecimal valor,
        LocalDate dataVencimento,
        StatusPrestacao status
) {
    public PrestacaoResponseDTO(Prestacao prestacao) {
        this(prestacao.id(), prestacao.contratoId(), prestacao.numero(),
                prestacao.valor(), prestacao.dataVencimento(), prestacao.status());
    }
}
