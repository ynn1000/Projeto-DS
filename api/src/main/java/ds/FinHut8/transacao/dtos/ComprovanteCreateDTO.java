package ds.FinHut8.transacao.dtos;

import ds.FinHut8.transacao.ComprovanteTipo;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ComprovanteCreateDTO(ComprovanteTipo tipo, String arquivoRef, String numeroNotaFiscal,
                                   BigDecimal valor, LocalDate dataDocumento) {}
