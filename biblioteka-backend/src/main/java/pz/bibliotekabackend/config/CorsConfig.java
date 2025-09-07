package pz.bibliotekabackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOriginPatterns(List.of("http://localhost:5173"));

        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));

        cfg.setAllowedHeaders(List.of(
                "Authorization","authorization",
                "Content-Type","content-type",
                "X-Requested-With","Accept","accept",
                "Origin","origin"
        ));

        cfg.setExposedHeaders(List.of(
                "Authorization",
                "X-Total-Count","x-total-count",
                "Content-Range","content-range",
                "Link","link",
                "X-Page","x-page","X-Total-Pages","x-total-pages",
                "X-Per-Page","x-per-page","X-Prev-Page","x-prev-page","X-Next-Page","x-next-page"
        ));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
