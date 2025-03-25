package sg.spring.seabattle.game.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameMap {
    private String id; // need both ids for neo4j to handle the relationship with a node correctly
    private String relationshipId;
    private final ShipPart[] shipParts;
    private final int sizeRoot;

    public GameMap() {
        this(8);
    }

    public GameMap(int sizeRoot) {
        shipParts = new ShipPart[sizeRoot * sizeRoot];
        this.sizeRoot = sizeRoot;
    }

    public ShotResult hit(int x, int y){
        return hit(y * (int) Math.sqrt(shipParts.length) + x);
    }

    public ShotResult hit(int index) {
        ShipPart shipPart = shipParts[index];

        if (shipPart != null) {
            shipPart.hit();
            boolean isSunk = true;
            for (int i : shipPart.getOtherPartsLocations()) {
                if (!shipParts[i].isHit()) {
                    isSunk = false;
                    break;
                }
            }
            return isSunk ? ShotResult.HIT_SUNK : ShotResult.HIT;
        }

        shipParts[index] = ShipPart.MISSED;
        return ShotResult.MISS;
    }

    public void placeShipPart(ShipPart shipPart, int index) {
        shipParts[index] = shipPart;
    }

    @Override
    public String toString() {
        return toStringForOwner();
    }

    public String toStringForOwner() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        
        // Add column headers (0-7)
        sb.append("   ");
        for (int x = 0; x < sizeRoot; x++) {
            sb.append(String.format("%2d ", x));
        }
        sb.append("\n");
        
        // Add separator line
        sb.append("   +");
        for (int x = 0; x < sizeRoot; x++) {
            sb.append("---+");
        }
        sb.append("\n");
        
        // Add rows with ship parts
        for (int y = 0; y < sizeRoot; y++) {
            sb.append(String.format("%2d |", y));
            
            for (int x = 0; x < sizeRoot; x++) {
                int index = y * sizeRoot + x;
                ShipPart part = shipParts[index];
                
                if (part == null) {
                    sb.append(" · |"); // Empty water
                } else if (part == ShipPart.MISSED) {
                    sb.append(" ○ |"); // Missed shot
                } else if (part.isHit()) {
                    sb.append(" ✕ |"); // Hit ship part
                } else {
                    sb.append(" █ |"); // Ship part (not hit)
                }
            }
            
            sb.append("\n");
            
            // Add separator line
            sb.append("   +");
            for (int x = 0; x < sizeRoot; x++) {
                sb.append("---+");
            }
            sb.append("\n");
        }
        
        // Add legend
        sb.append("\nLegend: \n")
          .append(" · - Empty water\n")
          .append(" █ - Ship (not hit)\n")
          .append(" ✕ - Ship (hit)\n")
          .append(" ○ - Missed shot\n");
        
        return sb.toString();
    }

    public String toStringForOpponent() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        
        // Add column headers (0-7)
        sb.append("   ");
        for (int x = 0; x < sizeRoot; x++) {
            sb.append(String.format("%2d ", x));
        }
        sb.append("\n");
        
        // Add separator line
        sb.append("   +");
        for (int x = 0; x < sizeRoot; x++) {
            sb.append("---+");
        }
        sb.append("\n");
        
        // Add rows with ship parts - only show hits and misses
        for (int y = 0; y < sizeRoot; y++) {
            sb.append(String.format("%2d |", y));
            
            for (int x = 0; x < sizeRoot; x++) {
                int index = y * sizeRoot + x;
                ShipPart part = shipParts[index];
                
                if (part == ShipPart.MISSED) {
                    sb.append(" ○ |"); // Missed shot
                } else if (part != null && part.isHit()) {
                    sb.append(" ✕ |"); // Hit ship part
                } else {
                    sb.append(" · |"); // Unknown (either empty water or ship not hit)
                }
            }
            
            sb.append("\n");
            
            // Add separator line
            sb.append("   +");
            for (int x = 0; x < sizeRoot; x++) {
                sb.append("---+");
            }
            sb.append("\n");
        }
        
        // Add legend
        sb.append("\nLegend: \n")
          .append(" · - Unknown (water or ship)\n")
          .append(" ✕ - Ship (hit)\n")
          .append(" ○ - Missed shot\n");
        
        return sb.toString();
    }
}
