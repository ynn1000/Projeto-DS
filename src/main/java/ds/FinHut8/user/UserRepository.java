package ds.FinHut8.user;

import ds.FinHut8.user.dtos.UserUpdateDTO;
import ds.FinHut8.user.enums.Curso;
import ds.FinHut8.user.enums.UserRole;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

                            user.setId(rs.getObject("id", UUID.class));
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

                    user.setId(rs.getObject("id", UUID.class));
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

    public void updateUser(UUID id, UserUpdateDTO dto) {
        StringBuilder sql = new StringBuilder("UPDATE usuario SET ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if(dto.nome() != null){
            sql.append("nome = :nome, ");
            params.addValue("nome", dto.nome());
        }

        if(dto.senha() != null){
            sql.append("senha_hash = :senha, ");
            params.addValue("senha", dto.senha());
        }

        if(dto.email() != null){
            sql.append("email = :email, ");
            params.addValue("email", dto.email());
        }

        if(dto.matricula() != null){
            sql.append("matricula = :matricula, ");
            params.addValue("matricula", dto.matricula());
        }

        if (dto.curso() != null) {
            sql.append("curso = :curso, ");
            params.addValue("curso", dto.curso().name());
        }

        sql.setLength(sql.length() - 2);

        sql.append(" WHERE id = :id");
        params.addValue("id", id);

        jdbc.sql(sql.toString())
                .params(params.getValues())
                .update();
    }

    public void dropUser(UUID id){
        jdbc.sql("""
                DELETE FROM usuario WHERE id = :id
                """)
                .param("id", id)
                .update();
    }
}
