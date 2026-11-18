package ds.FinHut8.transacao.dtos;

import ds.FinHut8.transacao.Comprovante;
import ds.FinHut8.transacao.ComprovanteTipo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ComprovanteResponseDTO(Integer id, Integer transacaoId, Integer prestacaoId, ComprovanteTipo tipo,
        String arquivoRef, String numeroNotaFiscal, BigDecimal valor, LocalDate dataDocumento, OffsetDateTime criadoEm) {
    public ComprovanteResponseDTO(Comprovante c) {
        this(c.id(), c.transacaoId(), c.prestacaoId(), c.tipo(), c.arquivoRef(), c.numeroNotaFiscal(),
                c.valor(), c.dataDocumento(), c.criadoEm());
    }
}
