package ds.FinHut8.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UserController {
    private final UserService service;

    public UserController(UserService service){
        this.service = service;
    }

    @GetMapping(params = "email")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email){
        User user = service.getUserByEmail(email);

        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>>  getAllUsers(){
        List<User> users = service.getAll();

        return ResponseEntity.ok(users);
    }
}
