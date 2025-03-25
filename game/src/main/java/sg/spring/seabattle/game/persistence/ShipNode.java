package sg.spring.seabattle.game.persistence;

import org.springframework.data.neo4j.core.schema.Node;
import sg.spring.core.persistence.provider.neo4j.Neo4jNodeIdentifier;

import java.util.UUID;

@Node
public class ShipNode extends Neo4jNodeIdentifier<UUID> {
}
