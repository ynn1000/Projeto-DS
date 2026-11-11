package ds.FinHut8.user.dtos;

import ds.FinHut8.user.User;
import ds.FinHut8.user.enums.Curso;
import ds.FinHut8.user.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponseDTO(UUID id, String nome, String email, boolean ativo, OffsetDateTime dataCriacao, String matricula, UserRole role, Curso curso, String semestreIngresso) {
    public UserResponseDTO(User user){
        this(user.getId(),
                user.getNome(),
                user.getEmail(),
                user.isAtivo(),
                user.getDataCriacao(),
                user.getMatricula(),
                user.getRole(),
                user.getCurso(),
                user.getSemestreIngresso()
        );
    }
}