package pe.edu.vallegrande.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Configuration
public class CorsConfig {

    private static final List<String> STATIC_ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:4200"
    );

    private static final Pattern GITPOD_REGEX = Pattern.compile(
            "^https://4200-[a-z0-9\\-]+\\.ws-[a-z0-9]+\\.gitpod\\.io$"
    );

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight for 1 hour

        CorsConfigurationSource source = new UrlBasedCorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
                String origin = exchange.getRequest().getHeaders().getOrigin();
                if (isAllowedOrigin(origin)) {
                    config.setAllowedOrigins(List.of(origin)); // Solo permitir origen válido
                    return config;
                }
                return null; // Bloquea CORS si el origen no es válido
            }
        };

        return new CorsWebFilter(source);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        return STATIC_ALLOWED_ORIGINS.contains(origin) || GITPOD_REGEX.matcher(origin).matches();
    }
}

