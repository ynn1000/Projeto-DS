package ds.FinHut8.transacao;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class TransacaoRepository {

    private final JdbcClient jdbc;

    public TransacaoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Transacao> listar(Integer caixaId) {
        return jdbc.sql("""
                SELECT id, descricao, tipo, valor, data_movimento, categoria_id, criado_em
                FROM public.transacao
                WHERE caixa_id = :caixaId
                ORDER BY data_movimento DESC, id DESC
                """)
                .param("caixaId", caixaId)
                .query(this::mapTransacao)
                .list();
    }

    public boolean categoriaAtivaDoCaixa(Integer caixaId, Integer categoriaId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.categoria_orcamentaria cat
                    JOIN public.caixa c ON c.empresa_junior_id = cat.empresa_junior_id
                    WHERE c.id = :caixaId AND cat.id = :categoriaId AND cat.ativa
                )
                """)
                .param("caixaId", caixaId)
                .param("categoriaId", categoriaId)
                .query(Boolean.class)
                .single();
    }

    public Transacao inserir(Integer caixaId, String descricao, TipoTransacao tipo,
                             BigDecimal valor, LocalDate dataMovimento, Integer categoriaId) {
        return jdbc.sql("""
                INSERT INTO public.transacao
                    (caixa_id, categoria_id, descricao, tipo, valor, data_movimento)
                VALUES
                    (:caixaId, :categoriaId, :descricao, :tipo, :valor, :dataMovimento)
                RETURNING id, descricao, tipo, valor, data_movimento, categoria_id, criado_em
                """)
                .param("caixaId", caixaId)
                .param("categoriaId", categoriaId, Types.INTEGER)
                .param("descricao", descricao)
                .param("tipo", tipo.name())
                .param("valor", valor)
                .param("dataMovimento", dataMovimento)
                .query(this::mapTransacao)
                .single();
    }

    private Transacao mapTransacao(ResultSet rs, int rowNum) throws SQLException {
        return new Transacao(
                rs.getInt("id"),
                rs.getString("descricao"),
                TipoTransacao.valueOf(rs.getString("tipo")),
                rs.getBigDecimal("valor"),
                rs.getObject("data_movimento", LocalDate.class),
                rs.getObject("categoria_id", Integer.class),
                rs.getObject("criado_em", OffsetDateTime.class)
        );
    }
}
