package sg.spring.seabattle.game.domain;

import lombok.Getter;
import lombok.Setter;
import sg.spring.core.domain.UUIDDomainIdentifier;

import java.util.UUID;

@Getter
@Setter
public class ShipPart extends UUIDDomainIdentifier {
    public static ShipPart MISSED = new ShipPart(true);
    private String elementId;

    public ShipPart(UUID uuid) {
        super(uuid);
    }

    private boolean isHit;
    @Getter
    private int[] otherPartsLocations;

    public ShipPart() {
        this(false);
    }

    public ShipPart(boolean isHit) {
        this.isHit = isHit;
    }

    public void hit() {
        if (isHit) {
            throw new ShipPartAlreadyHitException();
        }
        isHit = true;
    }
    
    @Override
    public String toString() {
        if (this == MISSED) {
            return "Miss";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("ShipPart(");
        sb.append("id=").append(getIdentifier());
        sb.append(", status=").append(isHit ? "HIT" : "NOT_HIT");
        
        if (otherPartsLocations != null && otherPartsLocations.length > 0) {
            sb.append(", otherLocations=[")
              .append(java.util.Arrays.toString(otherPartsLocations))
              .append("]");
        }
        
        sb.append(")");
        return sb.toString();
    }
}
