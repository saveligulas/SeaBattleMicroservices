package sg.spring.seabattle.game.domain.twoplayer;

import lombok.Getter;
import lombok.Setter;
import sg.spring.seabattle.game.domain.GameMap;
import sg.spring.seabattle.game.domain.IGamePlayer;
import sg.spring.seabattle.game.domain.ShipPart;

@Getter
@Setter
public class TwoPlayerGamePlayer implements IGamePlayer {
    private String id;
    private TwoPlayerColor color;
    private String name;
    private GameMap opponentMap;

    public TwoPlayerGamePlayer() {
    }

    public TwoPlayerGamePlayer(TwoPlayerColor color, String name) {
        this.color = color;
        this.name = name;
    }

    @Override
    public int getGameIdentifier() {
        return color.ordinal();
    }

    @Override
    public GameMap getOpponentMap() { // map from the OPPONENT where the OPPONENTS ships are placed
        return opponentMap;
    }
    
    public boolean hasWon() {
        if (opponentMap == null) {
            return false;
        }

        for (ShipPart part : opponentMap.getShipParts()) {
            if (part != null && part != ShipPart.MISSED && !part.isHit()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Player(");
        sb.append("name='").append(name).append("'");
        sb.append(", color=").append(color);
        
        if (id != null) {
            sb.append(", id='").append(id).append("'");
        }
        
        sb.append(", mapStatus=");
        if (opponentMap != null) {
            sb.append("SET_UP");
        } else {
            sb.append("NOT_SET_UP");
        }
        
        sb.append(")");
        return sb.toString();
    }
}
