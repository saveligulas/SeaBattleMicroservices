package sg.spring.seabattle.game.persistence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import sg.spring.core.persistence.provider.neo4j.Neo4jNodeIdentifier;
import sg.spring.seabattle.game.domain.GamePhase;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerColor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Node
@Getter
@Setter
public class TwoPlayerGameNode extends Neo4jNodeIdentifier<UUID> {
    @Relationship(type = "HAS_PLAYER", direction = Relationship.Direction.OUTGOING)
    private Set<TwoPlayerGamePlayerRelation> players = new HashSet<>();
    private GamePhase phase;
    private List<Integer> allowedShips;
    private TwoPlayerColor activePlayer = TwoPlayerColor.RED;

    public TwoPlayerGameNode() {
    }
}
