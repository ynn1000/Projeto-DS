package ds.FinHut8.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User getUserByEmail(String email){
        return repository.findByEmail(email).orElseThrow(()-> new RuntimeException("usuario não encontrado" + email));
    }
}
