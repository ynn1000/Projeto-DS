package ds.FinHut8.user.dtos;

import ds.FinHut8.user.enums.Curso;

public record UserUpdateDTO(String nome, String senha, String email, String matricula, Curso curso) {}
