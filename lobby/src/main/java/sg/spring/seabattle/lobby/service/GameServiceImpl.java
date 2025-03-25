package sg.spring.seabattle.lobby.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class GameServiceImpl implements GameService {
    private final RestTemplate restTemplate;
    private final String gameServiceUrl;

    @Autowired
    public GameServiceImpl(RestTemplate restTemplate,
                           @Value("${game.service.url}") String gameServiceUrl) {
        this.restTemplate = restTemplate;
        this.gameServiceUrl = gameServiceUrl;
    }


    @Override
    public String createGame(String playerRedId, String playerBlueId) {
        CreateTwoPlayerGameDTO body = new CreateTwoPlayerGameDTO();
        body.setRedPlayer(playerRedId);
        body.setBluePlayer(playerBlueId);

        try {
            return restTemplate.postForEntity(gameServiceUrl + "game", body, String.class).getBody();
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Game service connection failed", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }
}
