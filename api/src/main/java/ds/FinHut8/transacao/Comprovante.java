package ds.FinHut8.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Comprovante(Integer id, Integer transacaoId, Integer prestacaoId, ComprovanteTipo tipo,
        String arquivoRef, String numeroNotaFiscal, BigDecimal valor, LocalDate dataDocumento,
        OffsetDateTime criadoEm) {}
