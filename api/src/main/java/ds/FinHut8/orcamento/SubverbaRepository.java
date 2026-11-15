package ds.FinHut8.orcamento;

import ds.FinHut8.orcamento.dtos.SubverbaDTO;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class SubverbaRepository {
    private static final String DISTRIBUICAO_DO_CAIXA = """
            FROM public.distribuicao_orcamento d
            JOIN public.orcamento_periodo o ON o.id = d.orcamento_periodo_id
            JOIN public.categoria_orcamentaria cat ON cat.id = d.categoria_id
                 AND cat.empresa_junior_id = o.empresa_junior_id
            JOIN public.caixa c ON c.empresa_junior_id = o.empresa_junior_id
            WHERE c.id = :caixaId AND d.orcamento_periodo_id = :periodoId
              AND d.categoria_id = :categoriaId
            """;

    private final JdbcClient jdbc;

    public SubverbaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean distribuicaoExiste(int caixaId, int periodoId, int categoriaId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 " + DISTRIBUICAO_DO_CAIXA + ")")
                .param("caixaId", caixaId)
                .param("periodoId", periodoId)
                .param("categoriaId", categoriaId)
                .query(Boolean.class)
                .single();
    }

    public List<SubverbaDTO> listar(int periodoId, int categoriaId) {
        return jdbc.sql("""
                SELECT id, orcamento_periodo_id, categoria_id, nome, valor_reservado,
                       valor_utilizado, saldo_subverba
                FROM public.vw_consumo_subverba
                WHERE orcamento_periodo_id = :periodoId AND categoria_id = :categoriaId
                ORDER BY nome, id
                """)
                .param("periodoId", periodoId)
                .param("categoriaId", categoriaId)
                .query(this::map)
                .list();
    }

    public Optional<SubverbaDTO> criar(int caixaId, int periodoId, int categoriaId,
                                      String nome, BigDecimal valor) {
        return jdbc.sql("""
                INSERT INTO public.subverba (orcamento_periodo_id, categoria_id, nome, valor_reservado)
                SELECT d.orcamento_periodo_id, d.categoria_id, :nome, :valor
                """ + DISTRIBUICAO_DO_CAIXA + """
                  AND cat.ativa AND o.status <> 'ENCERRADO'
                RETURNING id, orcamento_periodo_id, categoria_id, nome, valor_reservado,
                          0.00 AS valor_utilizado, valor_reservado AS saldo_subverba
                """)
                .param("caixaId", caixaId)
                .param("periodoId", periodoId)
                .param("categoriaId", categoriaId)
                .param("nome", nome)
                .param("valor", valor)
                .query(this::map)
                .optional();
    }

    private SubverbaDTO map(ResultSet rs, int rowNum) throws SQLException {
        return new SubverbaDTO(rs.getInt("id"), rs.getInt("orcamento_periodo_id"),
                rs.getInt("categoria_id"), rs.getString("nome"), rs.getBigDecimal("valor_reservado"),
                rs.getBigDecimal("valor_utilizado"), rs.getBigDecimal("saldo_subverba"));
    }
}
