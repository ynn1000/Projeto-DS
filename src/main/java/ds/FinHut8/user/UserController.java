package ds.FinHut8.user;

import ds.FinHut8.user.dtos.UserResponseDTO;
import ds.FinHut8.user.dtos.UserUpdateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UserController {
    private final UserService service;

    public UserController(UserService service){
        this.service = service;
    }

    @GetMapping(params = "email")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@RequestParam String email){
        UserResponseDTO user = service.getUserByEmail(email);

        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>>  getAllUsers(){
        List<UserResponseDTO> users = service.getAll();

        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable UUID id, @RequestBody UserUpdateDTO user){
        UserResponseDTO dto = service.updateUser(id, user);

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponseDTO> deleteUser(@PathVariable UUID id){
        UserResponseDTO dto = service.deleteUser(id);

        return ResponseEntity.ok(dto);
    }
}
