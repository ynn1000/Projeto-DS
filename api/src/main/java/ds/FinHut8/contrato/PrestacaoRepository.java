package ds.FinHut8.contrato;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PrestacaoRepository {
    private final JdbcClient jdbc;

    public PrestacaoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Prestacao> listarPorContrato(Integer contratoId) {
        return jdbc.sql("""
                SELECT id, contrato_id, numero, valor, data_vencimento, status
                FROM public.prestacao
                WHERE contrato_id = :contratoId
                ORDER BY numero, id
                """)
                .param("contratoId", contratoId)
                .query(this::map)
                .list();
    }

    public Optional<Prestacao> buscarPorId(Integer contratoId, Integer prestacaoId) {
        return jdbc.sql("""
                SELECT id, contrato_id, numero, valor, data_vencimento, status
                FROM public.prestacao
                WHERE contrato_id = :contratoId AND id = :prestacaoId
                """)
                .param("contratoId", contratoId)
                .param("prestacaoId", prestacaoId)
                .query(this::map)
                .optional();
    }

    public Prestacao inserir(Integer contratoId, Integer numero,
                             java.math.BigDecimal valor, java.time.LocalDate vencimento) {
        return jdbc.sql("""
                INSERT INTO public.prestacao (contrato_id, numero, valor, data_vencimento, status)
                VALUES (:contratoId, :numero, :valor, :vencimento, 'PENDENTE')
                RETURNING id, contrato_id, numero, valor, data_vencimento, status
                """)
                .param("contratoId", contratoId)
                .param("numero", numero)
                .param("valor", valor)
                .param("vencimento", vencimento)
                .query(this::map)
                .single();
    }

    public int atualizarPendente(Integer contratoId, Integer prestacaoId, Integer numero,
                                 java.math.BigDecimal valor, java.time.LocalDate vencimento) {
        return jdbc.sql("""
                UPDATE public.prestacao
                SET numero = :numero, valor = :valor, data_vencimento = :vencimento
                WHERE contrato_id = :contratoId
                  AND id = :prestacaoId
                  AND status = 'PENDENTE'
                """)
                .param("contratoId", contratoId)
                .param("prestacaoId", prestacaoId)
                .param("numero", numero)
                .param("valor", valor)
                .param("vencimento", vencimento)
                .update();
    }

    public int cancelar(Integer contratoId, Integer prestacaoId) {
        return jdbc.sql("""
                UPDATE public.prestacao
                SET status = 'CANCELADA'
                WHERE contrato_id = :contratoId
                  AND id = :prestacaoId
                  AND status = 'PENDENTE'
                """)
                .param("contratoId", contratoId)
                .param("prestacaoId", prestacaoId)
                .update();
    }

    public boolean possuiComprovante(Integer prestacaoId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM public.comprovante WHERE prestacao_id = :id)")
                .param("id", prestacaoId)
                .query(Boolean.class)
                .single();
    }

    public boolean possuiTransacao(Integer prestacaoId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM public.transacao WHERE prestacao_id = :id)")
                .param("id", prestacaoId)
                .query(Boolean.class)
                .single();
    }

    private Prestacao map(ResultSet rs, int rowNum) throws SQLException {
        return new Prestacao(
                rs.getInt("id"), rs.getInt("contrato_id"), rs.getInt("numero"),
                rs.getBigDecimal("valor"), rs.getObject("data_vencimento", java.time.LocalDate.class),
                StatusPrestacao.valueOf(rs.getString("status"))
        );
    }
}
