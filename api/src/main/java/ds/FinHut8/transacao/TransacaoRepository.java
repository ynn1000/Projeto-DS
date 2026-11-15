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
                SELECT id, descricao, tipo, categoria_transacao, valor, data_movimento, categoria_id, subverba_id, criado_em
                FROM public.transacao
                WHERE caixa_id = :caixaId
                ORDER BY data_movimento DESC, id DESC
                """)
                .param("caixaId", caixaId)
                .query(this::mapTransacao)
                .list();
    }

    public boolean categoriaOrcamentariaAtivaDoCaixa(Integer caixaId, Integer categoriaOrcamentariaId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.categoria_orcamentaria cat
                    JOIN public.caixa c ON c.empresa_junior_id = cat.empresa_junior_id
                    WHERE c.id = :caixaId AND cat.id = :categoriaOrcamentariaId AND cat.ativa
                )
                """)
                .param("caixaId", caixaId)
                .param("categoriaOrcamentariaId", categoriaOrcamentariaId)
                .query(Boolean.class)
                .single();
    }

    public boolean subverbaValida(Integer caixaId, Integer categoriaId, Integer subverbaId, LocalDate dataMovimento) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.subverba s
                    JOIN public.orcamento_periodo o ON o.id = s.orcamento_periodo_id
                    JOIN public.caixa c ON c.empresa_junior_id = o.empresa_junior_id
                    WHERE c.id = :caixaId AND s.categoria_id = :categoriaId
                      AND s.id = :subverbaId AND :dataMovimento BETWEEN o.data_inicio AND o.data_fim
                )
                """)
                .param("caixaId", caixaId)
                .param("categoriaId", categoriaId)
                .param("subverbaId", subverbaId)
                .param("dataMovimento", dataMovimento)
                .query(Boolean.class)
                .single();
    }

    public Transacao inserir(Integer caixaId, String descricao, TipoTransacao tipo,
                             CategoriaTransacao categoriaTransacao, BigDecimal valor,
                             LocalDate dataMovimento, Integer categoriaOrcamentariaId,
                             Integer subverbaId) {
        return jdbc.sql("""
                INSERT INTO public.transacao
                    (caixa_id, categoria_id, subverba_id, descricao, tipo, categoria_transacao, valor, data_movimento)
                VALUES
                    (:caixaId, :categoriaOrcamentariaId, :subverbaId, :descricao, :tipo,
                     :categoriaTransacao, :valor, :dataMovimento)
                RETURNING id, descricao, tipo, categoria_transacao, valor, data_movimento, categoria_id, subverba_id, criado_em
                """)
                .param("caixaId", caixaId)
                .param("categoriaOrcamentariaId", categoriaOrcamentariaId, Types.INTEGER)
                .param("subverbaId", subverbaId, Types.INTEGER)
                .param("descricao", descricao)
                .param("tipo", tipo.name())
                .param("categoriaTransacao", categoriaTransacao.name())
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
                CategoriaTransacao.valueOf(rs.getString("categoria_transacao")),
                rs.getBigDecimal("valor"),
                rs.getObject("data_movimento", LocalDate.class),
                rs.getObject("categoria_id", Integer.class),
                rs.getObject("subverba_id", Integer.class),
                rs.getObject("criado_em", OffsetDateTime.class)
        );
    }
}
