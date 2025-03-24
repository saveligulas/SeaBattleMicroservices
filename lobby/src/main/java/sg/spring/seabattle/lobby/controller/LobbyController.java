package sg.spring.seabattle.lobby.controller;

import org.springframework.web.bind.annotation.*;
import sg.spring.seabattle.lobby.service.LobbyService;

@RestController
public class LobbyController {
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("lobby")
    public String createLobby() {
        return lobbyService.createLobby();
    }

    @PostMapping("lobby/{id}")
    public void joinLobby(@PathVariable("id") String gameId, @RequestParam("userId") String userId) {
        lobbyService.joinLobby(gameId, userId);
    }

    @PostMapping("lobby/{id}/start")
    public String startLobby(@PathVariable("id") String gameId) {
        return lobbyService.startGame(gameId);
    }
}
