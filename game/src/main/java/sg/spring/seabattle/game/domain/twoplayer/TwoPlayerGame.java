package sg.spring.seabattle.game.domain.twoplayer;

import lombok.Getter;
import lombok.Setter;
import sg.spring.seabattle.game.domain.GamePhase;
import sg.spring.seabattle.game.domain.GameState;
import sg.spring.seabattle.game.domain.IGamePlayer;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TwoPlayerGame extends GameState {
    private TwoPlayerGamePlayer playerRed;
    private TwoPlayerGamePlayer playerBlue;
    private List<Integer> allowedShips;
    private TwoPlayerColor activePlayer;

    public TwoPlayerGame() {
        this(UUID.randomUUID());
    }

    public TwoPlayerGame(UUID uuid) {
        super(uuid);
        allowedShips = List.of(2, 3, 4, 5, 6);
        activePlayer = TwoPlayerColor.RED; // Red player starts by default
    }


    @Override
    public boolean isPlayerTurn(IGamePlayer gamePlayer) {
        if (gamePhase != GamePhase.PLAY) {
            return false;
        }
        
        if (gamePlayer instanceof TwoPlayerGamePlayer twoPlayerGamePlayer) {
            return twoPlayerGamePlayer.getColor() == activePlayer;
        }
        
        return false;
    }

    @Override
    public boolean isShipsSetup() {
        return (playerRed.getOpponentMap() != null && playerBlue.getOpponentMap() != null);
    }

    public TwoPlayerGamePlayer getInversePlayer(TwoPlayerColor playerColor) {
        return playerColor == TwoPlayerColor.RED ? playerBlue : playerRed;
    }
    
    public TwoPlayerGamePlayer getActivePlayer() {
        return activePlayer == TwoPlayerColor.RED ? playerRed : playerBlue;
    }
    
    public TwoPlayerColor getActivePlayerColor() {
        return activePlayer;
    }
    
    public TwoPlayerGamePlayer getPlayerByColor(TwoPlayerColor playerColor) {
        return playerColor == TwoPlayerColor.RED ? playerRed : playerBlue;
    }
    
    public void switchTurn() {
        activePlayer = (activePlayer == TwoPlayerColor.RED) ? 
                      TwoPlayerColor.BLUE : TwoPlayerColor.RED;
    }
    
    public void checkForGameEnd() {
        if (playerRed.hasWon() || playerBlue.hasWon()) {
            gamePhase = GamePhase.END;
        }
    }
    
    public TwoPlayerGamePlayer getWinner() {
        if (gamePhase != GamePhase.END) {
            return null;
        }
        
        if (playerRed.hasWon()) {
            return playerRed;
        } else if (playerBlue.hasWon()) {
            return playerBlue;
        }
        
        throw new IllegalStateException("Game has ended without determining a winner");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("==== TWO PLAYER GAME (ID: ").append(getIdentifier()).append(") ====")
          .append("\n")
          .append("Phase: ").append(getGamePhase())
          .append("\n");
          
        // Current turn info
        if (gamePhase == GamePhase.PLAY) {
            sb.append("Current Turn: ").append(getActivePlayer().getColor())
              .append("\n");
        }
          
        sb.append("\n");

        // Red player info
        sb.append("RED PLAYER: ").append(playerRed.getName())
          .append(" ").append(playerRed.getId() != null ? "(ID: " + playerRed.getId() + ")" : "")
          .append("\n");
          
        if (playerRed.getOpponentMap() != null) {
            sb.append("[RED's View of BLUE's Map (BLUE's ships)]")
              .append(playerRed.getOpponentMap().toStringForOwner());
        } else {
            sb.append("[RED has not set up map yet]\n\n");
        }
        
        sb.append("\n");

        // Blue player info
        sb.append("BLUE PLAYER: ").append(playerBlue.getName())
          .append(" ").append(playerBlue.getId() != null ? "(ID: " + playerBlue.getId() + ")" : "")
          .append("\n");
          
        if (playerBlue.getOpponentMap() != null) {
            sb.append("[BLUE's View of RED's Map (RED's ships)]")
              .append(playerBlue.getOpponentMap().toStringForOwner());
        } else {
            sb.append("[BLUE has not set up map yet]\n\n");
        }
        
        sb.append("\n");
        
        // Allowed ships
        sb.append("Allowed ship sizes: ").append(allowedShips);
        
        return sb.toString();
    }
}
