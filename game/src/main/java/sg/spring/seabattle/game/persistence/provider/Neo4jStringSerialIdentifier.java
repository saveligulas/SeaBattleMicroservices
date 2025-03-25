package sg.spring.seabattle.game.persistence.provider;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import sg.spring.core.identifier.IModelIdentifier;

@Setter
@Getter
//TODO: move to other project
public class Neo4jStringSerialIdentifier implements IModelIdentifier<String> {
    @Id
    @GeneratedValue
    @Nullable
    private String id;

    public Neo4jStringSerialIdentifier() {
    }

    public Neo4jStringSerialIdentifier(String id) {
        this.id = id;
    }

    @Override
    public @Nullable String getIdentifier() {
        return id;
    }

    @Override
    public boolean isGeneratedManually() {
        return false;
    }

    @Override
    public boolean isPersisted() {
        return id != null;
    }
}
