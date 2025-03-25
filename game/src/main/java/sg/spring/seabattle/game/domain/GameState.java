package sg.spring.seabattle.game.domain;

import lombok.Getter;
import lombok.Setter;
import sg.spring.core.domain.UUIDDomainIdentifier;

import java.util.UUID;

@Getter
@Setter
public abstract class GameState extends UUIDDomainIdentifier {
    protected GamePhase gamePhase = GamePhase.SETUP;

    protected GameState() {
        this(UUID.randomUUID());
    }

    protected GameState(UUID uuid) {
        super(uuid);
    }

    public abstract boolean isPlayerTurn(IGamePlayer gamePlayer);
    public abstract boolean isShipsSetup();
}
