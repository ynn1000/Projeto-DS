package ds.FinHut8.auth;

import ds.FinHut8.auth.dtos.AuthRequestDTO;
import ds.FinHut8.auth.dtos.AuthResponseDTO;
import ds.FinHut8.user.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public AuthResponseDTO login(AuthRequestDTO dados){
            var usernamePassword = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());
            var auth = this.authenticationManager.authenticate(usernamePassword);
            var token = tokenService.generateToken((User) auth.getPrincipal());

            return new AuthResponseDTO(token);
        }
    }

