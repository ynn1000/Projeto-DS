package ds.FinHut8.infra;

import ds.FinHut8.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private final UserService userService;

    public AuthorizationService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        try {
            return userService.getUserEntityByEmail(email);
        } catch (RuntimeException e) {
            throw new UsernameNotFoundException("Usuário não encontrado: " + email, e);
        }
    }
}