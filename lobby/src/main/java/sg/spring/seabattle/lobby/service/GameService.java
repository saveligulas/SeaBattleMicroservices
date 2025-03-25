package sg.spring.seabattle.lobby.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "gameService")
public interface GameService {
    String createGame(String playerRedId, String playerBlueId);
}
