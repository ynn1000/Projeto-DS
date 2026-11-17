package ds.FinHut8.contrato;

import ds.FinHut8.contrato.enums.StatusContrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação de leitura/escrita da tabela public.contrato.
 *
 * Os relacionamentos são representados pelos identificadores, conforme o padrão
 * JDBC adotado nos demais módulos do projeto.
 */
public record Contrato(
        Integer id,
        Integer projetoId,
        String clienteNome,
        String arquivoRef,
        BigDecimal valorTotal,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusContrato status
) {
}
