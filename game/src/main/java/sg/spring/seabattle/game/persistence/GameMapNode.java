package sg.spring.seabattle.game.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.HashSet;
import java.util.Set;

@Node
@Getter
@Setter
public class GameMapNode {
    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;
    @Relationship(type = ShipPartRelation.TYPE, direction = Relationship.Direction.OUTGOING)
    private Set<ShipPartRelation> shipParts = new HashSet<>();
    private Integer rootSize;
    private Set<Integer> missedIndexes = new HashSet<>();
}
