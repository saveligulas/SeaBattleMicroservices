package sg.spring.seabattle.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import sg.spring.seabattle.gateway.auth.BasicAuthorizationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, @Autowired BasicAuthorizationFilter authorizationFilter) throws Exception {
        http.addFilterBefore(authorizationFilter, AuthorizationFilter.class);

        http.cors(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/proxied-api-docs/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/game/**", "/api/v1/lobby/**").authenticated()
                .anyRequest().permitAll()
        );

        return http.build();
    }
}

