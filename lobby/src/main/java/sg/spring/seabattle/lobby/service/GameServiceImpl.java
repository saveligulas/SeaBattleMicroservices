package sg.spring.seabattle.lobby.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    @CircuitBreaker(name = "gameService", fallbackMethod = "createGameFallback")
    public String createGame(String playerRedId, String playerBlueId) {
        CreateTwoPlayerGameDTO body = new CreateTwoPlayerGameDTO();
        body.setRedPlayer(playerRedId);
        body.setBluePlayer(playerBlueId);
        
        return restTemplate.postForEntity(gameServiceUrl + "game", body, String.class).getBody();
    }
    
    private String createGameFallback(String playerRedId, String playerBlueId, Exception ex) {
        throw new GameServiceException("Game service temporarily unavailable", ex);
    }
}
