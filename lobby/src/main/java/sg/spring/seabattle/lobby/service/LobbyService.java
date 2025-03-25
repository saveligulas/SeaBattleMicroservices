package sg.spring.seabattle.lobby.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.apache.commons.lang3.concurrent.CircuitBreakingException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import sg.spring.seabattle.lobby.domain.Lobby;
import sg.spring.seabattle.lobby.persistence.LobbyRepository;

import java.util.UUID;

@Service
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final GameService gameService;

    public LobbyService(LobbyRepository lobbyRepository, GameService gameService) {
        this.lobbyRepository = lobbyRepository;
        this.gameService = gameService;
    }

    //TODO: add expiration via redis to lobby
    public String createLobby() {
        String id = UUID.randomUUID().toString();

        Lobby lobby = new Lobby();
        lobby.setId(id);

        lobbyRepository.save(lobby);

        return id;
    }

    public void joinLobby(String lobbyId, String userId) {
        Lobby lobby = lobbyRepository.findById(lobbyId).orElseThrow(() -> new RuntimeException("Lobby not found"));
        lobby.addUser(userId);
        lobbyRepository.save(lobby);
    }

    public String startGame(String lobbyId) {

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new RuntimeException("Lobby not found"));

        String gameId = gameService.createGame(lobby.getPlayerOneId(), lobby.getPlayerTwoId());

        lobbyRepository.deleteById(lobbyId);
        return gameId;
    }
}
