package ds.FinHut8.contrato.dtos;

import ds.FinHut8.contrato.enums.StatusContrato;

/** Solicitação explícita de mudança de estado do contrato. */
public record ContratoStatusUpdateDTO(StatusContrato status) {
}
