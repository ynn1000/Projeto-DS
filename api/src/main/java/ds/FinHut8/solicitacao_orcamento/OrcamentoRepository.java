package ds.FinHut8.solicitacao_orcamento;

import ds.FinHut8.solicitacao_orcamento.enums.OrcamentoStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrcamentoRepository {

    private final JdbcClient jdbc;

    private static final String SELECT_ORCAMENTO = """
            SELECT
                s.id,
                s.solicitante_id,
                u.nome AS solicitante_nome,
                s.categoria_id,
                c.nome AS categoria_nome,
                s.titulo,
                s.descricao,
                s.valor_solicitado,
                s.arquivo_ref,
                s.status,
                s.avaliado_por_id,
                s.justificativa_decisao,
                s.data_decisao,
                s.criado_em
            FROM public.solicitacao_orcamento s
            JOIN public.usuario u ON u.matricula = s.solicitante_id
            JOIN public.categoria_orcamentaria c ON c.id = s.categoria_id
            """;

    public OrcamentoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Verifica se a categoria existe e está ativa
    public boolean categoriaAtivaExiste(Integer categoriaId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM public.categoria_orcamentaria
                    WHERE id = :id AND ativa = TRUE
                )
                """)
                .param("id", categoriaId)
                .query(Boolean.class)
                .single();
    }

    // Insere a solicitação (status PENDENTE é o default do banco) e devolve o id gerado
    public Integer inserir(String solicitanteId, Integer categoriaId, String titulo,
                           String descricao, BigDecimal valor, String arquivoRef) {
        return jdbc.sql("""
                INSERT INTO public.solicitacao_orcamento
                    (solicitante_id, categoria_id, titulo, descricao, valor_solicitado, arquivo_ref)
                VALUES
                    (:solicitante, :categoria, :titulo, :descricao, :valor, :arquivo)
                RETURNING id
                """)
                .param("solicitante", solicitanteId)
                .param("categoria", categoriaId)
                .param("titulo", titulo)
                .param("descricao", descricao)
                .param("valor", valor)
                .param("arquivo", arquivoRef, Types.VARCHAR)
                .query(Integer.class)
                .single();
    }

    public Optional<Orcamento> findById(Integer id) {
        return jdbc.sql(SELECT_ORCAMENTO + " WHERE s.id = :id")
                .param("id", id)
                .query(this::mapOrcamento)
                .optional();
    }

    public List<Orcamento> findBySolicitante(String solicitanteId) {
        return jdbc.sql(SELECT_ORCAMENTO + " WHERE s.solicitante_id = :solicitante ORDER BY s.criado_em DESC")
                .param("solicitante", solicitanteId)
                .query(this::mapOrcamento)
                .list();
    }

    // status == null lista todas
    public List<Orcamento> findAll(OrcamentoStatus status) {
        if (status == null) {
            return jdbc.sql(SELECT_ORCAMENTO + " ORDER BY s.criado_em DESC")
                    .query(this::mapOrcamento)
                    .list();
        }

        return jdbc.sql(SELECT_ORCAMENTO + " WHERE s.status = :status ORDER BY s.criado_em DESC")
                .param("status", status.name())
                .query(this::mapOrcamento)
                .list();
    }

    // Atualiza o status só se ele ainda não tiver sido alterado
    // Retorna o número de linhas alteradas, 0 = o status mudou no meio do caminho
    public int atualizarStatus(Integer id, OrcamentoStatus statusEsperado, OrcamentoStatus novoStatus,
                               String avaliadoPorId, String justificativa, OffsetDateTime dataDecisao) {
        return jdbc.sql("""
                UPDATE public.solicitacao_orcamento
                SET status = :novo,
                    avaliado_por_id = :avaliador,
                    justificativa_decisao = :justificativa,
                    data_decisao = :data
                WHERE id = :id AND status = :esperado
                """)
                .param("novo", novoStatus.name())
                .param("avaliador", avaliadoPorId, Types.VARCHAR)
                .param("justificativa", justificativa, Types.VARCHAR)
                .param("data", dataDecisao, Types.TIMESTAMP_WITH_TIMEZONE)
                .param("id", id)
                .param("esperado", statusEsperado.name())
                .update();
    }

    private Orcamento mapOrcamento(ResultSet rs, int rowNum) throws SQLException {
        return new Orcamento(
                rs.getInt("id"),
                rs.getString("solicitante_id"),
                rs.getString("solicitante_nome"),
                rs.getInt("categoria_id"),
                rs.getString("categoria_nome"),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getBigDecimal("valor_solicitado"),
                rs.getString("arquivo_ref"),
                OrcamentoStatus.valueOf(rs.getString("status")),
                rs.getString("avaliado_por_id"),
                rs.getString("justificativa_decisao"),
                rs.getObject("data_decisao", OffsetDateTime.class),
                rs.getObject("criado_em", OffsetDateTime.class)
        );
    }
}