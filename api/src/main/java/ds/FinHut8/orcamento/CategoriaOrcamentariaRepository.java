package ds.FinHut8.orcamento;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CategoriaOrcamentariaRepository {
    private final JdbcClient jdbc;
    public CategoriaOrcamentariaRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<CategoriaOrcamentariaDTO> listarAtivasDoCaixa(Integer caixaId) {
        return jdbc.sql("""
                SELECT cat.id, cat.nome, cat.descricao
                FROM public.categoria_orcamentaria cat
                JOIN public.caixa c ON c.empresa_junior_id = cat.empresa_junior_id
                WHERE c.id = :caixaId AND cat.ativa = TRUE
                ORDER BY cat.nome, cat.id
                """)
                .param("caixaId", caixaId)
                .query((rs, row) -> new CategoriaOrcamentariaDTO(
                        rs.getInt("id"), rs.getString("nome"), rs.getString("descricao")))
                .list();
    }
}
