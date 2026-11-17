package ds.FinHut8.contrato;

import ds.FinHut8.contrato.enums.StatusContrato;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ContratoRepository {

    private static final String SELECT_CONTRATO = """
            SELECT id, projeto_id, cliente_nome, arquivo_ref,
                   valor_total, data_inicio, data_fim, status
            FROM public.contrato
            """;

    private final JdbcClient jdbc;

    public ContratoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Contrato inserir(Contrato contrato) {
        return jdbc.sql("""
                INSERT INTO public.contrato
                    (projeto_id, cliente_nome, arquivo_ref, valor_total,
                     data_inicio, data_fim, status)
                VALUES
                    (:projetoId, :clienteNome, :arquivoRef, :valorTotal,
                     :dataInicio, :dataFim, :status)
                RETURNING id, projeto_id, cliente_nome, arquivo_ref,
                          valor_total, data_inicio, data_fim, status
                """)
                .param("projetoId", contrato.projetoId())
                .param("clienteNome", contrato.clienteNome())
                .param("arquivoRef", contrato.arquivoRef(), Types.VARCHAR)
                .param("valorTotal", contrato.valorTotal())
                .param("dataInicio", contrato.dataInicio())
                .param("dataFim", contrato.dataFim())
                .param("status", contrato.status().name())
                .query(this::mapContrato)
                .single();
    }

    public Optional<Contrato> buscarPorId(Integer id) {
        List<Contrato> contratos = jdbc.sql(SELECT_CONTRATO + " WHERE id = :id")
                .param("id", id)
                .query(this::mapContrato)
                .list();

        return contratos.stream().findFirst();
    }

    /** Bloqueia a linha do contrato durante operações que dependem de seu estado atual. */
    public Optional<Contrato> buscarPorIdParaAtualizacao(Integer id) {
        List<Contrato> contratos = jdbc.sql(SELECT_CONTRATO + " WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(this::mapContrato)
                .list();

        return contratos.stream().findFirst();
    }

    /**
     * Lista contratos com filtros opcionais. O filtro textual procura pelo início
     * do nome do cliente, permitindo utilizar o índice funcional correspondente.
     */
    public List<Contrato> listar(Integer projetoId, String clienteNome, StatusContrato status) {
        return jdbc.sql(SELECT_CONTRATO + """
                WHERE (:projetoId IS NULL OR projeto_id = :projetoId)
                  AND (:clienteNome IS NULL OR lower(cliente_nome) LIKE lower(:clienteNome) || '%')
                  AND (:status IS NULL OR status = :status)
                ORDER BY id DESC
                """)
                .param("projetoId", projetoId, Types.INTEGER)
                .param("clienteNome", clienteNome, Types.VARCHAR)
                .param("status", status == null ? null : status.name(), Types.VARCHAR)
                .query(this::mapContrato)
                .list();
    }

    /** Atualiza os dados cadastrais, sem permitir alteração direta do status. */
    public int atualizar(Contrato contrato) {
        return jdbc.sql("""
                UPDATE public.contrato
                SET projeto_id = :projetoId,
                    cliente_nome = :clienteNome,
                    arquivo_ref = :arquivoRef,
                    valor_total = :valorTotal,
                    data_inicio = :dataInicio,
                    data_fim = :dataFim
                WHERE id = :id
                """)
                .param("projetoId", contrato.projetoId())
                .param("clienteNome", contrato.clienteNome())
                .param("arquivoRef", contrato.arquivoRef(), Types.VARCHAR)
                .param("valorTotal", contrato.valorTotal())
                .param("dataInicio", contrato.dataInicio())
                .param("dataFim", contrato.dataFim())
                .param("id", contrato.id())
                .update();
    }

    public int atualizarStatus(Integer id, StatusContrato status) {
        return jdbc.sql("""
                UPDATE public.contrato
                SET status = :status
                WHERE id = :id
                """)
                .param("status", status.name())
                .param("id", id)
                .update();
    }

    public boolean projetoExiste(Integer projetoId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.projeto
                    WHERE id = :projetoId
                )
                """)
                .param("projetoId", projetoId)
                .query(Boolean.class)
                .single();
    }

    /**
     * A política definida para esta etapa permite exclusão física somente quando
     * não há prestações. Transações e comprovantes ligados ao contrato dependem
     * de uma prestação por meio das FKs existentes.
     */
    public boolean possuiPrestacoes(Integer contratoId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.prestacao
                    WHERE contrato_id = :contratoId
                )
                """)
                .param("contratoId", contratoId)
                .query(Boolean.class)
                .single();
    }

    /** Indica se ainda existe obrigação com status PENDENTE vinculada ao contrato. */
    public boolean possuiPrestacoesPendentes(Integer contratoId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.prestacao
                    WHERE contrato_id = :contratoId
                      AND status = 'PENDENTE'
                )
                """)
                .param("contratoId", contratoId)
                .query(Boolean.class)
                .single();
    }

    /**
     * Exclui somente a linha do contrato. As FKs existentes não usam CASCADE;
     * o PostgreSQL rejeitará a operação se ainda houver registros dependentes.
     */
    public int excluir(Integer id) {
        return jdbc.sql("""
                DELETE FROM public.contrato
                WHERE id = :id
                """)
                .param("id", id)
                .update();
    }

    private Contrato mapContrato(ResultSet rs, int rowNum) throws SQLException {
        return new Contrato(
                rs.getInt("id"),
                rs.getInt("projeto_id"),
                rs.getString("cliente_nome"),
                rs.getString("arquivo_ref"),
                rs.getBigDecimal("valor_total"),
                rs.getObject("data_inicio", LocalDate.class),
                rs.getObject("data_fim", LocalDate.class),
                StatusContrato.valueOf(rs.getString("status"))
        );
    }
}
