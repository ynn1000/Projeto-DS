package ds.FinHut8.caixa;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CaixaRepository {

    private final JdbcClient jdbc;

    private static final String SELECT_CAIXA = """
            SELECT
                c.id,
                c.saldo_inicial,
                v.saldo_atual,
                c.limite_despesas
            FROM public.caixa c
            JOIN public.vw_saldo_caixa v
                ON v.caixa_id = c.id
            """;

    public CaixaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Busca caixa
    public Optional<Caixa> find() {

        List<Caixa> caixas = jdbc.sql(SELECT_CAIXA)
                .query(this::mapCaixa)
                .list();

        if (caixas.isEmpty()) {
            return Optional.empty();
        }

        if (caixas.size() > 1) {
            throw new IllegalStateException(
                    "O sistema deve possuir apenas um caixa."
            );
        }

        return Optional.of(caixas.get(0));
    }

    // Atualiza somente o limite de despesas
    public int atualizarLimite(Integer id, BigDecimal limite) {

        return jdbc.sql("""
                UPDATE public.caixa
                SET limite_despesas = :limite
                WHERE id = :id
                """)
                .param("limite", limite)
                .param("id", id)
                .update();
    }

    // Converte o resultado SQL em um objeto Caixa
    private Caixa mapCaixa(ResultSet rs, int rowNum)
            throws SQLException {

        return new Caixa(
                rs.getInt("id"),
                rs.getBigDecimal("saldo_inicial"),
                rs.getBigDecimal("saldo_atual"),
                rs.getBigDecimal("limite_despesas")
        );
    }
}