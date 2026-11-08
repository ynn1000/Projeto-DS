package ds.FinHut8.user;

import org.springframework.stereotype.Service;

import java.util.List;

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
}
