package sg.spring.seabattle.gateway.auth;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthorizationService {
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isAuthenticated(String token) {
        String url = "http://localhost:9093/validate?userId=" + token;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        return response.getStatusCode().is2xxSuccessful();
    }

}
