package br.com.presentya.service;

import br.com.presentya.domain.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final long horas;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.expiracao-horas:8}") long horas) {
        this.encoder = encoder;
        this.horas = horas;
    }

    public String gerar(Usuario u) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("presentya")
                .issuedAt(agora)
                .expiresAt(agora.plus(horas, ChronoUnit.HOURS))
                .subject(u.getId().toString())
                .claim("nome", u.getNome())
                .claim("email", u.getEmail())
                .claim("roles", List.of(u.getPerfil().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
