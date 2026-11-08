package ds.FinHut8.user;

import ds.FinHut8.user.dtos.UserUpdateDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User getUserByEmail(String email){
        return repository.findByEmail(email).orElseThrow(()-> new RuntimeException("usuário não encontrado" + email));
    }

    public List<User> getAll(){
        return repository.fetchAllUsers();
    }

    public void registerUser(User user){
        repository.postUser(user);
    }

    public void updateUser(UUID id, UserUpdateDTO user){
        repository.updateUser(id, user);
    }
}
