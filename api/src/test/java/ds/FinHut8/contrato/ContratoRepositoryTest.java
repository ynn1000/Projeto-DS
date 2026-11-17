package ds.FinHut8.contrato;

import ds.FinHut8.contrato.enums.StatusContrato;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContratoRepositoryTest {

    @Test
    void mapeiaContratoComTiposJavaCorrespondentesAoPostgresql() throws Exception {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec comando = mock(JdbcClient.StatementSpec.class);
        ResultSet rs = mock(ResultSet.class);

        when(jdbc.sql(anyString())).thenReturn(comando);
        when(comando.param("id", 7)).thenReturn(comando);
        when(rs.getInt("id")).thenReturn(7);
        when(rs.getInt("projeto_id")).thenReturn(3);
        when(rs.getString("cliente_nome")).thenReturn("Empresa Alfa");
        when(rs.getString("arquivo_ref")).thenReturn("contratos/alfa.pdf");
        when(rs.getBigDecimal("valor_total")).thenReturn(new BigDecimal("6000.00"));
        when(rs.getObject("data_inicio", LocalDate.class)).thenReturn(LocalDate.of(2026, 8, 1));
        when(rs.getObject("data_fim", LocalDate.class)).thenReturn(LocalDate.of(2026, 12, 1));
        when(rs.getString("status")).thenReturn("RASCUNHO");

        when(comando.query(any(RowMapper.class))).thenAnswer(invocacao -> {
            @SuppressWarnings("unchecked")
            RowMapper<Contrato> mapper = invocacao.getArgument(0);
            JdbcClient.MappedQuerySpec<Contrato> consulta = mock(JdbcClient.MappedQuerySpec.class);
            when(consulta.list()).thenReturn(List.of(mapper.mapRow(rs, 0)));
            return consulta;
        });

        Contrato contrato = new ContratoRepository(jdbc).buscarPorId(7).orElseThrow();

        assertEquals(7, contrato.id());
        assertEquals(3, contrato.projetoId());
        assertEquals("Empresa Alfa", contrato.clienteNome());
        assertEquals(new BigDecimal("6000.00"), contrato.valorTotal());
        assertEquals(LocalDate.of(2026, 8, 1), contrato.dataInicio());
        assertEquals(LocalDate.of(2026, 12, 1), contrato.dataFim());
        assertEquals(StatusContrato.RASCUNHO, contrato.status());
    }
}
