package br.com.presentya.config;

import br.com.presentya.domain.Usuario;
import br.com.presentya.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecretKey jwtKey(@Value("${app.jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) precisa ter pelo menos 32 caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        // O perfil vem do banco a cada requisição: mudar o perfil ou desativar alguém vale na hora.
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> usuarios.findById(Long.valueOf(jwt.getSubject()))
                .filter(Usuario::isAtivo)
                .<Collection<GrantedAuthority>>map(u -> List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name())))
                .orElse(List.of()));

        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/pagamentos/webhook/**", "/api/status").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/produtos/**", "/api/categorias/**",
                        "/api/configuracoes", "/api/imagens/**", "/api/ofertas", "/api/push/chave").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/interessados", "/api/push/inscricoes", "/api/push/cancelar").permitAll()
                // Checkout e acompanhamento de pedido não exigem login (compra como convidado);
                // "/meus" precisa vir antes do padrão abaixo para continuar exigindo login.
                .requestMatchers(HttpMethod.GET, "/api/pedidos/meus").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/pedidos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pedidos/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/sincronizar", "/api/pedidos/*/simular-pagamento").permitAll()
                .requestMatchers("/api/admin/categorias/**", "/api/admin/usuarios/**",
                        "/api/admin/configuracoes/**", "/api/admin/ofertas/**",
                        "/api/admin/interessados/**", "/api/admin/push/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/pedidos/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "TECNICO")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-url}") String frontendUrl) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(frontendUrl.split(",")).map(String::trim).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
