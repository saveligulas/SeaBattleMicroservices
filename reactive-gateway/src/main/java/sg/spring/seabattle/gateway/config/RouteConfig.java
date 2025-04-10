package sg.spring.seabattle.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Lobby Service Route
                .route("lobby-service", r -> r
                        .path("/api/v1/lobby/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(config -> config
                                        .setName("lobbyServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback")))
                        .uri("lb://lobby-service"))
                
                // Game Service Route
                .route("game-service", r -> r
                        .path("/api/v1/game/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("gameServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback")))
                        .uri("lb://game-service"))
                
                // Auth Service Route
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .stripPrefix(3)
                                .circuitBreaker(config -> config
                                        .setName("authServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback")))
                        .uri("lb://auth-service"))
                .build();
    }
}
