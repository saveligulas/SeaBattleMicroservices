package sg.spring.seabattle.game.persistence;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
@Getter
@Setter
public class TwoPlayerGamePlayerNode {
    @Id
    private String name;
    @Relationship(type = "HAS_MAP", direction = Relationship.Direction.OUTGOING)
    private GameMapNodeRelation gameMap;
}
