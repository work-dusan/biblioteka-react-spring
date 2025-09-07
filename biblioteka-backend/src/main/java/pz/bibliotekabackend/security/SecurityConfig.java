package pz.bibliotekabackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // koristi CorsConfigurationSource iz CorsConfig-a
                .cors(cors -> cors.configurationSource(corsConfigurationSource));

        if (!securityEnabled) {
            // DEV: sve dozvoljeno + eksplicitno pusti OPTIONS
            http.authorizeHttpRequests(reg -> reg
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().permitAll()
            );
        } else {
            // PROD: stateless + JWT + RBAC
            http
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            // preflight uvek dozvoli
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                            // public rute
                            .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                            .requestMatchers(HttpMethod.GET, "/books/**", "/covers/**").permitAll()

                            // /auth/me samo autentifikovan
                            .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()

                            // kreiranje korisnika
                            .requestMatchers(HttpMethod.POST, "/users").permitAll()

                            // admin operacije
                            .requestMatchers(HttpMethod.DELETE, "/books/**", "/users/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.POST,  "/books/**").hasRole("ADMIN")

                            // patch korisnika — mora biti ulogovan
                            .requestMatchers(HttpMethod.PATCH, "/users/**").authenticated()

                            // korisnik i admin mogu da patch-uju knjigu
                            .requestMatchers(HttpMethod.PATCH, "/books/**").hasAnyRole("ADMIN","USER")

                            // naručivanje/vraćanje — samo USER
                            .requestMatchers(HttpMethod.POST,  "/orders/**").hasRole("USER")
                            .requestMatchers(HttpMethod.PATCH, "/orders/**").hasRole("USER")

                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
