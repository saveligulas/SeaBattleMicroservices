package sg.spring.seabattle.game.service;

import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import sg.spring.seabattle.game.controller.ShotResultDTO;
import sg.spring.seabattle.game.domain.GameMap;
import sg.spring.seabattle.game.domain.GamePhase;
import sg.spring.seabattle.game.domain.ShipPart;
import sg.spring.seabattle.game.domain.ShotResult;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerColor;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerGame;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerGamePlayer;
import sg.spring.seabattle.game.persistence.TwoPlayerGameNode;
import sg.spring.seabattle.game.persistence.mapper.TwoPlayerGamePersistenceMapper;
import sg.spring.seabattle.game.persistence.repo.TwoPlayerGameNodeRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TwoPlayerGameService {

    private static final Logger logger = LoggerFactory.getLogger(TwoPlayerGameService.class);

    //TODO: abstract this layer so service doesnt have to map this directly
    private final TwoPlayerGameNodeRepository twoPlayerGameNodeRepository;

    public TwoPlayerGameService(TwoPlayerGameNodeRepository twoPlayerGameNodeRepository) {
        this.twoPlayerGameNodeRepository = twoPlayerGameNodeRepository;
    }

    //abstraction for the service
    private TwoPlayerGame retrieveGame(UUID gameId) {
        return TwoPlayerGamePersistenceMapper.INSTANCE.toDomain(twoPlayerGameNodeRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Two player game with id: " + gameId + " not found")));
    }

    //abstraction for the service
    private void saveGame(TwoPlayerGame game) {
        try {
            TwoPlayerGameNode node = TwoPlayerGamePersistenceMapper.INSTANCE.toEntity(game);
            twoPlayerGameNodeRepository.save(node); //ignore might be null here
        } catch (Exception e) { //TODO: catch more specific exceptions and throw different exceptions after
            throw new RuntimeException("Error saving game state: " + e.getMessage(), e);
        }
    }

    public TwoPlayerGame getGame(UUID gameId) {
        return retrieveGame(gameId);
    }
    
    public GamePhase getGamePhase(UUID gameId) {
        return retrieveGame(gameId).getGamePhase();
    }
    
    public boolean isGameSetupComplete(UUID gameId) {
        return retrieveGame(gameId).isShipsSetup();
    }
    
    public String getMapForOpponent(UUID gameId, TwoPlayerColor playerColor) {
        TwoPlayerGame game = retrieveGame(gameId);
        TwoPlayerGamePlayer player = game.getPlayerByColor(playerColor);
        
        if (player.getOpponentMap() == null) {
            return "Map not set up yet for player " + playerColor;
        }
        
        return "Map for player " + playerColor + " (opponent view - only showing hits and misses):\n" + 
               player.getOpponentMap().toStringForOpponent();
    }
    
    public TwoPlayerColor getCurrentTurn(UUID gameId) {
        TwoPlayerGame game = retrieveGame(gameId);
        return game.getActivePlayerColor();
    }
    
    public TwoPlayerColor getWinner(UUID gameId) {
        TwoPlayerGame game = retrieveGame(gameId);
        if (game.getGamePhase() != GamePhase.END || game.getWinner() == null) {
            return null;
        }
        return game.getWinner().getColor();
    }
    
    public ShotResultDTO fireShot(UUID gameId, TwoPlayerColor playerColor, int x, int y) {
        TwoPlayerGame game = retrieveGame(gameId);

        if (game.getGamePhase() != GamePhase.PLAY) {
            throw new IllegalStateException("Cannot fire: Game is not in PLAY phase. Current phase: " + game.getGamePhase());
        }

        TwoPlayerGamePlayer player = game.getPlayerByColor(playerColor);
        if (!game.isPlayerTurn(player)) {
            throw new IllegalStateException("Cannot fire: It's not your turn. Current turn: " + game.getActivePlayerColor());
        }

        GameMap targetMap = player.getOpponentMap();
        if (targetMap == null) {
            throw new IllegalStateException("Cannot fire: Target map is not set up");
        }

        int index = targetMap.getSizeRoot() * y + x;
        if (index < 0 || index >= targetMap.getShipParts().length) {
            throw new IllegalArgumentException("Shot coordinates out of bounds");
        }

        ShotResult result = targetMap.hit(x, y);
        game.checkForGameEnd();
        saveGame(game);

        boolean turnEnded = false;
        if (result == ShotResult.MISS) {
            game.switchTurn();
            turnEnded = true;
            saveGame(game);
        }

        TwoPlayerColor winner = null;
        if (game.getGamePhase() == GamePhase.END && game.getWinner() != null) {
            winner = game.getWinner().getColor();
        }
        
        return new ShotResultDTO(
                result, 
                turnEnded, 
                game.getActivePlayerColor(),
                game.getGamePhase() == GamePhase.END,
                winner
        );
    }

    public UUID createGame(String redPlayer, String bluePlayer, @Nullable List<Integer> allowedShips) {
        TwoPlayerGame twoPlayerGame = new TwoPlayerGame();
        twoPlayerGame.setPlayerRed(new TwoPlayerGamePlayer(TwoPlayerColor.RED, redPlayer));
        twoPlayerGame.setPlayerBlue(new TwoPlayerGamePlayer(TwoPlayerColor.BLUE, bluePlayer));
        if (allowedShips != null) {
            twoPlayerGame.setAllowedShips(allowedShips);
        }

        saveGame(twoPlayerGame);
        return twoPlayerGame.getIdentifier();
    }

    public GameMap setupMap(UUID gameId, TwoPlayerColor playerColor, List<List<Integer>> ships) {
        //TODO: check if the ships are placed correctly and if the size of them is in the game allowed ship sizes
        TwoPlayerGame twoPlayerGame = retrieveGame(gameId);
        TwoPlayerGamePlayer player = twoPlayerGame.getInversePlayer(playerColor);

        if (player.getOpponentMap() != null) {
            throw new IllegalStateException("Map for " + playerColor + " is already set up");
        }
        
        GameMap map = new GameMap();

        for (List<Integer> ship : ships) {
            UUID shipId = UUID.randomUUID();

            for (Integer pos : ship) {
                ShipPart part = new ShipPart(shipId);
                map.placeShipPart(part, pos);
            }

            for (Integer pos : ship) {
                ShipPart part = map.getShipParts()[pos];
                if (part != null) {
                    int[] otherPositions = ship.stream()
                            .filter(p -> !p.equals(pos)) // filters out position of current part
                            .mapToInt(Integer::intValue)
                            .toArray();
                    part.setOtherPartsLocations(otherPositions);
                }
            }
        }

        player.setOpponentMap(map);

        if (twoPlayerGame.isShipsSetup()) {
            twoPlayerGame.setGamePhase(GamePhase.PLAY);
        }
        
        saveGame(twoPlayerGame);
        return map;
    }
    
    /**
     * Processes a game creation message from the message queue.
     * Expected format: gameId;redPlayerId;bluePlayerId
     *
     * @param message the message from the queue
     * @return the UUID of the created game or null if the message couldn't be processed
     */
    public UUID processGameCreationMessage(String message) {
        logger.info("Processing game creation message: {}", message);
        
        try {
            // Parse the message using semicolon delimiter
            String[] parts = message.split(";");
            
            if (parts.length < 3) {
                logger.error("Invalid message format. Expected gameId;redPlayerId;bluePlayerId, but got: {}", message);
                return null;
            }
            
            String gameIdStr = parts[0];
            String redPlayerId = parts[1];
            String bluePlayerId = parts[2];
            
            logger.info("Creating game with ID: {}, Red player: {}, Blue player: {}", 
                    gameIdStr, redPlayerId, bluePlayerId);
            
            // Create the game - for now, we're just using the existing method which generates
            // a new UUID internally. In a full implementation, we'd want to adapt the domain
            // model to accept a predefined UUID.
            UUID createdGameId = createGame(redPlayerId, bluePlayerId, null);
            
            logger.info("Game created with ID: {}", createdGameId);
            return createdGameId;
            
        } catch (Exception e) {
            logger.error("Error processing game creation message: {}", e.getMessage(), e);
            return null;
        }
    }
}
