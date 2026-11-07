package ds.FinHut8.user;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc){
        this.jdbc = jdbc;
    }

    public Optional<User> findByEmail(String email){
        return jdbc.sql("""
                SELECT * from usuario WHERE email = :email
                """)
                .param("email", email)
                .query((rs, rowNum) -> {
                            User user = new User();

                            user.setId(rs.getString("id"));
                            user.setSenha(rs.getString("senha_hash"));
                            user.setAtivo(rs.getBoolean("ativo"));
                            user.setEmail(rs.getString("email"));
                            user.setNome(rs.getString("nome"));
                            user.setMatricula(rs.getString("matricula"));
                            user.setDataCriacao(rs.getObject("criado_em", OffsetDateTime.class));
                            user.setSemestreIngresso(rs.getString("semestre_ingresso"));
                            user.setRole(UserRole.valueOf(rs.getString("role")));
                            user.setCurso(Curso.valueOf(rs.getString("curso")));

                            return user;
                        })
                .optional();
    }
}
