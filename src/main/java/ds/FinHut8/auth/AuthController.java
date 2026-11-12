package ds.FinHut8.auth;

import ds.FinHut8.auth.dtos.AuthRequestDTO;
import ds.FinHut8.auth.dtos.AuthResponseDTO;
import ds.FinHut8.user.User;
import ds.FinHut8.user.UserService;
import ds.FinHut8.user.dtos.UserResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private AuthenticationManager authenticationManager;
    private TokenService tokenService;
    private UserService userService;
    private AuthService authService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService, UserService userService, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO dados){
        if(dados.email().equals("") || dados.senha().equals("")){
            throw new IllegalArgumentException("Preencha todo o formulário de login.");
        }

        AuthResponseDTO token = authService.login(dados);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody User user){
        UserResponseDTO dto = userService.registerUser(user);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }
}
