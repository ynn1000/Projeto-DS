package ds.FinHut8.user;

import ds.FinHut8.user.dtos.UserResponseDTO;
import ds.FinHut8.user.dtos.UserUpdateDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public UserResponseDTO getUserByEmail(String email){
        User user = repository.findByEmail(email).orElseThrow(()-> new RuntimeException("usuário não encontrado" + email));
        UserResponseDTO dto = new UserResponseDTO(user);

        return dto;
    }

    public UserResponseDTO getUserById(UUID id){
        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("usuário não encontrado" + id));
        UserResponseDTO dto = new UserResponseDTO(user);

        return dto;
    }

    public List<UserResponseDTO> getAll(){
        return repository.fetchAllUsers()
                .stream()
                .map(user -> new UserResponseDTO(
                        user.getId(),
                        user.getNome(),
                        user.getEmail(),
                        user.isAtivo(),
                        user.getDataCriacao(),
                        user.getMatricula(),
                        user.getRole(),
                        user.getCurso(),
                        user.getSemestreIngresso()
                ))
                .toList();
    }

    public UserResponseDTO registerUser(User user){
        String encryptedPassword = new BCryptPasswordEncoder().encode(user.getSenha());
        user.setSenha(encryptedPassword);

        repository.postUser(user);

        User registeredUser = repository.findByEmail(user.getEmail())
                                        .orElseThrow(()-> new RuntimeException("erro ao consultar o usuário" +  user.getEmail()));
        UserResponseDTO dto = new UserResponseDTO(registeredUser);

        return dto;
    }

    public UserResponseDTO updateUser(UUID id, UserUpdateDTO user){
        repository.updateUser(id, user);
        return this.getUserById(id);
    }

    public UserResponseDTO deleteUser(UUID id){
        User user = repository.dropUser(id);
        UserResponseDTO dto = new UserResponseDTO(user);

        return dto;
    }
}
