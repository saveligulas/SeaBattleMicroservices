package sg.spring.seabattle.game.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Getter
@Setter
public class ShipPartRelation {
    public static final String TYPE = "HAS_HAPPENED";
    @RelationshipId
    private String id;
    private Integer index;
    private boolean isHit;
    @TargetNode
    private ShipNode ship;
}
