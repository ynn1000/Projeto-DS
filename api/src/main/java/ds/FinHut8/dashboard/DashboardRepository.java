package ds.FinHut8.dashboard;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class DashboardRepository {
    private final JdbcClient jdbc;

    public DashboardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Recebimentos recebimentos(int caixaId) {
        var linhas = jdbc.sql("""
                WITH pendentes AS (
                    SELECT p.id, p.numero, p.valor, p.data_vencimento, ct.id AS contrato_id, ct.cliente_nome
                    FROM public.prestacao p
                    JOIN public.contrato ct ON ct.id = p.contrato_id
                    JOIN public.projeto pj ON pj.id = ct.projeto_id
                    JOIN public.caixa c ON c.empresa_junior_id = pj.empresa_junior_id
                    WHERE c.id = :caixaId AND p.status = 'PENDENTE' AND ct.status = 'ATIVO'
                ), proximas AS (
                    SELECT * FROM pendentes WHERE data_vencimento >= CURRENT_DATE
                    ORDER BY data_vencimento, id LIMIT 3
                )
                SELECT total.previsto, pr.*
                FROM (SELECT COALESCE(sum(valor), 0) AS previsto FROM pendentes) total
                LEFT JOIN proximas pr ON true
                ORDER BY pr.data_vencimento, pr.id
                """)
                .param("caixaId", caixaId)
                .query((rs, linha) -> new LinhaRecebimento(
                        rs.getBigDecimal("previsto"),
                        rs.getObject("id", Integer.class) == null ? null : new Vencimento(
                                rs.getInt("id"), rs.getInt("contrato_id"), rs.getString("cliente_nome"),
                                rs.getInt("numero"), rs.getObject("data_vencimento", LocalDate.class),
                                rs.getBigDecimal("valor"))))
                .list();
        return new Recebimentos(linhas.getFirst().previsto(),
                linhas.stream().map(LinhaRecebimento::vencimento).filter(java.util.Objects::nonNull).toList());
    }

    public OpcoesLancamento opcoesLancamento(int caixaId, LocalDate dataMovimento) {
        var verbas = jdbc.sql("""
                SELECT cat.id, cat.nome
                FROM public.categoria_orcamentaria cat
                JOIN public.caixa c ON c.empresa_junior_id = cat.empresa_junior_id
                WHERE c.id = :caixaId AND cat.ativa
                ORDER BY cat.nome, cat.id
                """)
                .param("caixaId", caixaId)
                .query((rs, linha) -> new VerbaOpcao(rs.getInt("id"), rs.getString("nome")))
                .list();
        var subverbas = jdbc.sql("""
                SELECT s.id, s.nome, s.categoria_id, o.nome AS periodo
                FROM public.subverba s
                JOIN public.orcamento_periodo o ON o.id = s.orcamento_periodo_id
                JOIN public.categoria_orcamentaria cat ON cat.id = s.categoria_id
                     AND cat.empresa_junior_id = o.empresa_junior_id
                JOIN public.caixa c ON c.empresa_junior_id = o.empresa_junior_id
                WHERE c.id = :caixaId AND cat.ativa
                  AND :dataMovimento BETWEEN o.data_inicio AND o.data_fim
                ORDER BY s.nome, o.data_inicio DESC, s.id
                """)
                .param("caixaId", caixaId)
                .param("dataMovimento", dataMovimento)
                .query((rs, linha) -> new SubverbaOpcao(rs.getInt("id"), rs.getString("nome"),
                        rs.getInt("categoria_id"), rs.getString("periodo")))
                .list();
        return new OpcoesLancamento(verbas, subverbas);
    }

    private record LinhaRecebimento(BigDecimal previsto, Vencimento vencimento) {}
    public record Recebimentos(BigDecimal receitaPrevista, List<Vencimento> proximosVencimentos) {}
    public record Vencimento(int id, int contratoId, String clienteNome, int numero,
                             LocalDate dataVencimento, BigDecimal valor) {}
    public record OpcoesLancamento(List<VerbaOpcao> verbas, List<SubverbaOpcao> subverbas) {}
    public record VerbaOpcao(int id, String nome) {}
    public record SubverbaOpcao(int id, String nome, int categoriaOrcamentariaId, String periodo) {}
}
