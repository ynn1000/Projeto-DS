package ds.FinHut8.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ds.FinHut8.user.enums.Curso;
import ds.FinHut8.user.enums.UserRole;
import jakarta.annotation.Nullable;
import lombok.Data;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
public class User implements UserDetails {
    private UUID id;
    private String senha;
    private String nome;
    private String email;
    private boolean ativo;
    private OffsetDateTime dataCriacao;
    private String matricula;
    private UserRole role;
    private Curso curso;
    private String semestreIngresso;

    public User(){}
    public User(String nome, String email, String senha, String role, String curso, String matricula, String semestreIngresso){
            this.nome = nome;
            this.email = email;
            this.senha = senha;
            this.role = UserRole.valueOf(role);
            this.curso = Curso.valueOf(curso);
            this.matricula = matricula;
            this.semestreIngresso = semestreIngresso;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @JsonIgnore
    @Override
    public @Nullable String getPassword() {
        return senha;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return nome;
    }
}
