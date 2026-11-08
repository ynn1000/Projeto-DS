package ds.FinHut8.user;

import ds.FinHut8.user.enums.Curso;
import ds.FinHut8.user.enums.UserRole;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
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

    public List<User> fetchAllUsers(){
        return jdbc.sql("""
                SELECT * from usuario
                """)
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
                .list();
    }

    public void postUser(User user){
        jdbc.sql("""
                INSERT INTO usuario (
                    senha_hash,
                    ativo,
                    email,
                    nome,
                    matricula,
                    criado_em,
                    semestre_ingresso,
                    role,
                    curso
                ) VALUES (
                    :senha,
                    :ativo,
                    :email,
                    :nome,
                    :matricula,
                    NOW(),
                    :semestreIngresso,
                    :role,
                    :curso
                );
                """)
                .param("senha", user.getSenha())
                .param("ativo", user.isAtivo())
                .param("email", user.getEmail())
                .param("nome", user.getNome())
                .param("matricula", user.getMatricula())
                .param("semestreIngresso", user.getSemestreIngresso())
                .param("role", user.getRole().name())
                .param("curso", user.getCurso().name())
                .update();
    }
}
