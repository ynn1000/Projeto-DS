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
    public TransacaoRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    private static final String SELECT = """
            SELECT id, descricao, tipo, categoria_transacao, valor, data_movimento,
                   categoria_id, subverba_id, projeto_id, prestacao_id, registrado_por_id, criado_em
            FROM public.transacao
            """;

    public List<Transacao> listar(Integer caixaId, TipoTransacao tipo, CategoriaTransacao categoriaTransacao,
            Integer categoriaId, Integer projetoId, LocalDate dataInicio, LocalDate dataFim) {
        return jdbc.sql(SELECT + """
                WHERE caixa_id = :caixaId
                  AND (:tipo IS NULL OR tipo = :tipo)
                  AND (:categoriaTransacao IS NULL OR categoria_transacao = :categoriaTransacao)
                  AND (:categoriaId IS NULL OR categoria_id = :categoriaId)
                  AND (:projetoId IS NULL OR projeto_id = :projetoId)
                  AND (:dataInicio IS NULL OR data_movimento >= :dataInicio)
                  AND (:dataFim IS NULL OR data_movimento <= :dataFim)
                ORDER BY data_movimento DESC, id DESC
                """)
                .param("caixaId", caixaId)
                .param("tipo", tipo == null ? null : tipo.name(), Types.VARCHAR)
                .param("categoriaTransacao", categoriaTransacao == null ? null : categoriaTransacao.name(), Types.VARCHAR)
                .param("categoriaId", categoriaId, Types.INTEGER)
                .param("projetoId", projetoId, Types.INTEGER)
                .param("dataInicio", dataInicio, Types.DATE)
                .param("dataFim", dataFim, Types.DATE)
                .query(this::mapTransacao).list();
    }

    public TransacaoResumo resumir(Integer caixaId, LocalDate inicio, LocalDate fim) {
        return jdbc.sql("""
                SELECT CAST(:inicio AS date) AS data_inicio, CAST(:fim AS date) AS data_fim,
                       COALESCE(SUM(valor) FILTER (WHERE tipo = 'RECEITA'), 0) AS receitas,
                       COALESCE(SUM(valor) FILTER (WHERE tipo = 'DESPESA'), 0) AS despesas,
                       COALESCE(SUM(CASE WHEN tipo = 'RECEITA' THEN valor ELSE -valor END), 0) AS resultado,
                       COUNT(*) AS quantidade
                FROM public.transacao
                WHERE caixa_id = :caixaId AND data_movimento BETWEEN :inicio AND :fim
                """)
                .param("caixaId", caixaId).param("inicio", inicio).param("fim", fim)
                .query((rs, row) -> new TransacaoResumo(
                        rs.getObject("data_inicio", LocalDate.class), rs.getObject("data_fim", LocalDate.class),
                        rs.getBigDecimal("receitas"), rs.getBigDecimal("despesas"),
                        rs.getBigDecimal("resultado"), rs.getLong("quantidade")))
                .single();
    }

    public List<ProjetoOpcaoDTO> listarProjetosAtivos(Integer caixaId) {
        return jdbc.sql("""
                SELECT p.id, p.nome FROM public.projeto p
                JOIN public.caixa c ON c.empresa_junior_id = p.empresa_junior_id
                WHERE c.id = :caixaId AND p.status = 'ATIVO'
                ORDER BY p.nome, p.id
                """).param("caixaId", caixaId)
                .query((rs, row) -> new ProjetoOpcaoDTO(rs.getInt("id"), rs.getString("nome"))).list();
    }

    public List<PrestacaoOpcaoDTO> listarPrestacoesPendentes(Integer caixaId) {
        return jdbc.sql("""
                SELECT p.id, ct.id AS contrato_id, pr.id AS projeto_id, pr.nome AS projeto_nome,
                       ct.cliente_nome, p.numero, p.valor, p.data_vencimento
                FROM public.prestacao p
                JOIN public.contrato ct ON ct.id = p.contrato_id
                JOIN public.projeto pr ON pr.id = ct.projeto_id
                JOIN public.caixa c ON c.empresa_junior_id = pr.empresa_junior_id
                WHERE c.id = :caixaId AND p.status = 'PENDENTE'
                ORDER BY p.data_vencimento, p.id
                """).param("caixaId", caixaId)
                .query((rs, row) -> new PrestacaoOpcaoDTO(rs.getInt("id"), rs.getInt("contrato_id"),
                        rs.getInt("projeto_id"), rs.getString("projeto_nome"), rs.getString("cliente_nome"),
                        rs.getInt("numero"), rs.getBigDecimal("valor"),
                        rs.getObject("data_vencimento", LocalDate.class))).list();
    }

    public List<PeriodoOrcamentoOpcaoDTO> listarPeriodosAtivos(Integer caixaId) {
        return jdbc.sql("""
                SELECT DISTINCT o.id, o.nome, o.data_inicio, o.data_fim
                FROM public.orcamento_periodo o
                JOIN public.caixa c ON c.empresa_junior_id = o.empresa_junior_id
                WHERE c.id = :caixaId AND o.status = 'ATIVO'
                ORDER BY o.data_inicio DESC, o.id DESC
                """).param("caixaId", caixaId)
                .query((rs, row) -> new PeriodoOrcamentoOpcaoDTO(rs.getInt("id"), rs.getString("nome"),
                        rs.getObject("data_inicio", LocalDate.class), rs.getObject("data_fim", LocalDate.class))).list();
    }

    public boolean categoriaOrcamentariaAtivaDoCaixa(Integer caixaId, Integer categoriaId) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM public.categoria_orcamentaria cat
                JOIN public.caixa c ON c.empresa_junior_id = cat.empresa_junior_id
                WHERE c.id = :caixaId AND cat.id = :categoriaId AND cat.ativa)
                """).param("caixaId", caixaId).param("categoriaId", categoriaId).query(Boolean.class).single();
    }

    public boolean subverbaValida(Integer caixaId, Integer categoriaId, Integer subverbaId, LocalDate data) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM public.subverba s
                JOIN public.orcamento_periodo o ON o.id = s.orcamento_periodo_id
                JOIN public.caixa c ON c.empresa_junior_id = o.empresa_junior_id
                WHERE c.id = :caixaId AND s.categoria_id = :categoriaId AND s.id = :subverbaId
                  AND :data BETWEEN o.data_inicio AND o.data_fim AND o.status = 'ATIVO')
                """).param("caixaId", caixaId).param("categoriaId", categoriaId)
                .param("subverbaId", subverbaId).param("data", data).query(Boolean.class).single();
    }

    public boolean projetoPertenceAoCaixa(Integer caixaId, Integer projetoId) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM public.projeto p JOIN public.caixa c
                ON c.empresa_junior_id = p.empresa_junior_id WHERE c.id = :caixaId AND p.id = :projetoId)
                """).param("caixaId", caixaId).param("projetoId", projetoId).query(Boolean.class).single();
    }

    public PrestacaoVinculo buscarPrestacaoParaPagamento(Integer prestacaoId) {
        return jdbc.sql("""
                SELECT p.id, p.valor, p.status, ct.projeto_id, p.contrato_id
                FROM public.prestacao p JOIN public.contrato ct ON ct.id = p.contrato_id
                WHERE p.id = :id FOR UPDATE OF p
                """).param("id", prestacaoId)
                .query((rs, row) -> new PrestacaoVinculo(rs.getInt("id"), rs.getBigDecimal("valor"),
                        rs.getString("status"), rs.getInt("projeto_id"), rs.getInt("contrato_id")))
                .optional().orElse(null);
    }

    public Transacao inserir(Integer caixaId, String matriculaUsuario, String descricao, TipoTransacao tipo,
            CategoriaTransacao categoriaTransacao, BigDecimal valor, LocalDate data, Integer categoriaId,
            Integer subverbaId, Integer projetoId, Integer prestacaoId) {
        return jdbc.sql("""
                INSERT INTO public.transacao (caixa_id, registrado_por_id, projeto_id, prestacao_id,
                    categoria_id, subverba_id, descricao, tipo, categoria_transacao, valor, data_movimento)
                VALUES (:caixaId, :matriculaUsuario, :projetoId, :prestacaoId, :categoriaId, :subverbaId,
                    :descricao, :tipo, :categoriaTransacao, :valor, :data)
                RETURNING id, descricao, tipo, categoria_transacao, valor, data_movimento,
                    categoria_id, subverba_id, projeto_id, prestacao_id, registrado_por_id, criado_em
                """)
                .param("caixaId", caixaId).param("matriculaUsuario", matriculaUsuario)
                .param("projetoId", projetoId, Types.INTEGER).param("prestacaoId", prestacaoId, Types.INTEGER)
                .param("categoriaId", categoriaId, Types.INTEGER).param("subverbaId", subverbaId, Types.INTEGER)
                .param("descricao", descricao).param("tipo", tipo.name())
                .param("categoriaTransacao", categoriaTransacao.name()).param("valor", valor).param("data", data)
                .query(this::mapTransacao).single();
    }

    public int marcarPrestacaoPaga(Integer prestacaoId) {
        return jdbc.sql("UPDATE public.prestacao SET status = 'PAGA' WHERE id = :id AND status = 'PENDENTE'")
                .param("id", prestacaoId).update();
    }

    public List<Comprovante> listarComprovantes(Integer caixaId, Integer transacaoId) {
        return jdbc.sql("""
                SELECT cp.id, cp.transacao_id, cp.prestacao_id, cp.tipo, cp.arquivo_ref, cp.numero_nota_fiscal,
                       cp.valor, cp.data_documento, cp.criado_em
                FROM public.comprovante cp JOIN public.transacao t ON t.id = cp.transacao_id
                WHERE t.caixa_id = :caixaId AND cp.transacao_id = :transacaoId
                ORDER BY cp.criado_em DESC, cp.id DESC
                """).param("caixaId", caixaId).param("transacaoId", transacaoId)
                .query(this::mapComprovante).list();
    }

    public boolean transacaoExisteNoCaixa(Integer caixaId, Integer transacaoId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM public.transacao WHERE caixa_id = :caixaId AND id = :id)")
                .param("caixaId", caixaId).param("id", transacaoId).query(Boolean.class).single();
    }

    public boolean prestacaoExisteNoCaixa(Integer caixaId, Integer prestacaoId) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM public.prestacao p
                JOIN public.contrato ct ON ct.id = p.contrato_id
                JOIN public.projeto pr ON pr.id = ct.projeto_id
                JOIN public.caixa c ON c.empresa_junior_id = pr.empresa_junior_id
                WHERE c.id = :caixaId AND p.id = :prestacaoId)
                """).param("caixaId", caixaId).param("prestacaoId", prestacaoId)
                .query(Boolean.class).single();
    }

    public List<Comprovante> listarComprovantesPrestacao(Integer caixaId, Integer prestacaoId) {
        return jdbc.sql("""
                SELECT cp.id, NULL::integer AS transacao_id, cp.prestacao_id, cp.tipo, cp.arquivo_ref,
                       cp.numero_nota_fiscal, cp.valor, cp.data_documento, cp.criado_em
                FROM public.comprovante cp JOIN public.prestacao p ON p.id = cp.prestacao_id
                JOIN public.contrato ct ON ct.id = p.contrato_id
                JOIN public.projeto pr ON pr.id = ct.projeto_id
                JOIN public.caixa c ON c.empresa_junior_id = pr.empresa_junior_id
                WHERE c.id = :caixaId AND cp.prestacao_id = :prestacaoId
                ORDER BY cp.criado_em DESC, cp.id DESC
                """).param("caixaId", caixaId).param("prestacaoId", prestacaoId)
                .query(this::mapComprovantePrestacao).list();
    }

    public Comprovante inserirComprovantePrestacao(Integer prestacaoId, ComprovanteTipo tipo, String arquivoRef,
            String numeroNotaFiscal, BigDecimal valor, LocalDate dataDocumento) {
        return jdbc.sql("""
                INSERT INTO public.comprovante (prestacao_id, tipo, arquivo_ref, numero_nota_fiscal, valor, data_documento)
                VALUES (:prestacaoId, :tipo, :arquivoRef, :numeroNotaFiscal, :valor, :dataDocumento)
                RETURNING id, NULL::integer AS transacao_id, prestacao_id, tipo, arquivo_ref, numero_nota_fiscal, valor, data_documento, criado_em
                """)
                .param("prestacaoId", prestacaoId).param("tipo", tipo.name()).param("arquivoRef", arquivoRef)
                .param("numeroNotaFiscal", numeroNotaFiscal, Types.VARCHAR).param("valor", valor, Types.NUMERIC)
                .param("dataDocumento", dataDocumento, Types.DATE).query(this::mapComprovantePrestacao).single();
    }

    private Comprovante mapComprovantePrestacao(ResultSet rs, int row) throws SQLException {
        return new Comprovante(rs.getInt("id"), null, rs.getObject("prestacao_id", Integer.class),
                ComprovanteTipo.valueOf(rs.getString("tipo")), rs.getString("arquivo_ref"),
                rs.getString("numero_nota_fiscal"), rs.getBigDecimal("valor"),
                rs.getObject("data_documento", LocalDate.class), rs.getObject("criado_em", OffsetDateTime.class));
    }

    public Comprovante inserirComprovante(Integer transacaoId, ComprovanteTipo tipo, String arquivoRef,
            String numeroNotaFiscal, BigDecimal valor, LocalDate dataDocumento) {
        return jdbc.sql("""
                INSERT INTO public.comprovante (transacao_id, tipo, arquivo_ref, numero_nota_fiscal, valor, data_documento)
                VALUES (:transacaoId, :tipo, :arquivoRef, :numeroNotaFiscal, :valor, :dataDocumento)
                RETURNING id, transacao_id, NULL::integer AS prestacao_id, tipo, arquivo_ref, numero_nota_fiscal, valor, data_documento, criado_em
                """)
                .param("transacaoId", transacaoId).param("tipo", tipo.name()).param("arquivoRef", arquivoRef)
                .param("numeroNotaFiscal", numeroNotaFiscal, Types.VARCHAR).param("valor", valor, Types.NUMERIC)
                .param("dataDocumento", dataDocumento, Types.DATE).query(this::mapComprovante).single();
    }

    private Transacao mapTransacao(ResultSet rs, int row) throws SQLException {
        return new Transacao(rs.getInt("id"), rs.getString("descricao"), TipoTransacao.valueOf(rs.getString("tipo")),
                CategoriaTransacao.valueOf(rs.getString("categoria_transacao")), rs.getBigDecimal("valor"),
                rs.getObject("data_movimento", LocalDate.class), rs.getObject("categoria_id", Integer.class),
                rs.getObject("subverba_id", Integer.class), rs.getObject("projeto_id", Integer.class),
                rs.getObject("prestacao_id", Integer.class), rs.getString("registrado_por_id"),
                rs.getObject("criado_em", OffsetDateTime.class));
    }
    private Comprovante mapComprovante(ResultSet rs, int row) throws SQLException {
        return new Comprovante(rs.getInt("id"), rs.getObject("transacao_id", Integer.class),
                rs.getObject("prestacao_id", Integer.class), ComprovanteTipo.valueOf(rs.getString("tipo")),
                rs.getString("arquivo_ref"), rs.getString("numero_nota_fiscal"), rs.getBigDecimal("valor"),
                rs.getObject("data_documento", LocalDate.class), rs.getObject("criado_em", OffsetDateTime.class));
    }
    public record PrestacaoVinculo(Integer id, BigDecimal valor, String status, Integer projetoId, Integer contratoId) {}
}
