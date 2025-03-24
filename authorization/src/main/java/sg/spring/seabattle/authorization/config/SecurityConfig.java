package sg.spring.seabattle.authorization.config;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain clientSecurityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.oauth2Login(Customizer.withDefaults());
        http.logout((logout) -> {
            var logoutSuccessHandler =
                    new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
            logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");
            logout.logoutSuccessHandler(logoutSuccessHandler);
        });

        http.authorizeHttpRequests(requests -> {
            requests.requestMatchers("/", "/favicon.ico").permitAll();
            requests.requestMatchers("/nice").authenticated();
            requests.anyRequest().denyAll();
        });

        return http.build();
    }
}
