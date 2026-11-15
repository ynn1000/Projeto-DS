package ds.FinHut8.caixa;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaixaRepositoryTest {

    @Test
    void naoInverteSaldoAtualELimiteDeDespesas() throws Exception {
        var jdbc = mock(JdbcClient.class);
        var comando = mock(JdbcClient.StatementSpec.class);
        var rs = mock(ResultSet.class);

        when(rs.getInt("id")).thenReturn(1);
        when(rs.getBigDecimal("saldo_inicial")).thenReturn(new BigDecimal("1000.00"));
        when(rs.getBigDecimal("saldo_atual")).thenReturn(new BigDecimal("550.00"));
        when(rs.getBigDecimal("limite_despesas")).thenReturn(new BigDecimal("1500.00"));
        when(jdbc.sql(anyString())).thenReturn(comando);
        when(comando.query(ArgumentMatchers.<RowMapper<Caixa>>any())).thenAnswer(invocacao -> {
            RowMapper<Caixa> mapper = invocacao.getArgument(0);
            var consulta = mock(JdbcClient.MappedQuerySpec.class);
            Caixa caixa = mapper.mapRow(rs, 0);
            when(consulta.list()).thenReturn(List.of(caixa));
            return consulta;
        });

        var caixa = new CaixaRepository(jdbc).find().orElseThrow();

        assertEquals(new BigDecimal("1000.00"), caixa.saldoInicial());
        assertEquals(new BigDecimal("550.00"), caixa.saldoAtual());
        assertEquals(new BigDecimal("1500.00"), caixa.limiteDespesas());
    }
}
