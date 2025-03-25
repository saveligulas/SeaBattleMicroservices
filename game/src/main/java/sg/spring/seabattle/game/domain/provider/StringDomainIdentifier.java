package sg.spring.seabattle.game.domain.provider;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import sg.spring.core.identifier.IModelIdentifier;

@Getter
@Setter
public class StringDomainIdentifier {
    @Nullable
    private String identifier;

    public StringDomainIdentifier() {
        this(null);
    }

    public StringDomainIdentifier(@Nullable String identifier) {
        this.identifier = identifier;
    }
}
