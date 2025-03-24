package sg.spring.seabattle.gateway.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ApiDocsProxyController {

    //Had to use this to circumvent CORS protection issues
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/proxied-api-docs/seabattle")
    public ResponseEntity<String> getSeaBattleApiDocs() {
        String url = "http://localhost:9091/v3/api-docs";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response;
    }

    @GetMapping("/proxied-api-docs/lobby")
    public ResponseEntity<String> getLobbyApiDocs() {
        String url = "http://localhost:9092/v3/api-docs";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response;
    }
}
