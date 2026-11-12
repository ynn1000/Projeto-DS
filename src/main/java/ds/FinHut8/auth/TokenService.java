package ds.FinHut8.auth;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import ds.FinHut8.user.User;
import com.auth0.jwt.JWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user){
        try{
            Algorithm algo = Algorithm.HMAC256(secret);
            String token = JWT
                    .create()
                    .withIssuer("Hut8")
                    .withSubject(user.getEmail())
                    .withExpiresAt(getExpirationDate())
                    .sign(algo);
            return token;
        }
        catch (JWTCreationException e){
            throw new RuntimeException("Erro ao gerar token", e);
        }
    }

    public String validateToken(String token){
        try {
            Algorithm algo = Algorithm.HMAC256(secret);
            return JWT
                    .require(algo)
                    .build()
                    .verify(token)
                    .getSubject();
        }
        catch (JWTVerificationException e) {
            throw new RuntimeException("Token inválido ou expirado", e);
        }
    }

    private Instant getExpirationDate(){
        return LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.of("-03:00"));
    }
}