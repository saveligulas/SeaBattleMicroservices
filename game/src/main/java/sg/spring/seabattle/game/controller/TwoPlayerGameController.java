package sg.spring.seabattle.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerColor;
import sg.spring.seabattle.game.service.TwoPlayerGameService;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/")
public class TwoPlayerGameController {

    private final TwoPlayerGameService twoPlayerGameService;

    @Autowired
    public TwoPlayerGameController(TwoPlayerGameService twoPlayerGameService) {
        this.twoPlayerGameService = twoPlayerGameService;
    }

    @GetMapping("game/{id}")
    public String getGame(@PathVariable("id") UUID gameId) {
        return twoPlayerGameService.getGame(gameId).toString();
    }

    @PostMapping("game")
    public String createGame(@RequestBody CreateTwoPlayerGameDTO createTwoPlayerGameDTO) {
        return twoPlayerGameService.createGame(
                createTwoPlayerGameDTO.getRedPlayer(),
                createTwoPlayerGameDTO.getBluePlayer(),
                createTwoPlayerGameDTO.getShipSizes())
                .toString();
    }

    @PostMapping("game/{id}/setup")
    public String setupMap(@PathVariable("id") UUID gameId,
                           @RequestHeader("player-id") TwoPlayerColor playerColor,
                           @RequestBody CreateMapDTO createMapDTO) {
        return twoPlayerGameService.setupMap(gameId, playerColor, createMapDTO.ships).toString(); //TODO: change this so it doesnt work with index but with x and y coordinates
    }

    @GetMapping("game/{id}/phase")
    public String getGamePhase(@PathVariable("id") UUID gameId) {
        return twoPlayerGameService.getGamePhase(gameId).toString();
    }
    
    @GetMapping("game/{id}/map/opponent")
    public String viewMapAsOpponent(
            @PathVariable("id") UUID gameId,
            @RequestHeader("player-id") TwoPlayerColor playerColor) {
        return twoPlayerGameService.getMapForOpponent(gameId, playerColor);
    }
    
    @PostMapping("game/{id}/fire")
    public ShotResultDTO fire(
            @PathVariable("id") UUID gameId,
            @RequestHeader("player-id") TwoPlayerColor playerColor,
            @RequestBody ShotDTO shotDTO) {
        return twoPlayerGameService.fireShot(gameId, playerColor, shotDTO.getX(), shotDTO.getY());
    }
    
    @GetMapping("game/{id}/turn")
    public TwoPlayerColor getCurrentTurn(@PathVariable("id") UUID gameId) {
        return twoPlayerGameService.getCurrentTurn(gameId);
    }
    
    @GetMapping("game/{id}/winner")
    public TwoPlayerColor getWinner(@PathVariable("id") UUID gameId) {
        return twoPlayerGameService.getWinner(gameId);
    }
}
