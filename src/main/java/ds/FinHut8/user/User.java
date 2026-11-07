package ds.FinHut8.user;

import jakarta.annotation.Nullable;
import lombok.Data;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Data
public class User implements UserDetails {
    private String id;
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return nome;
    }
}
