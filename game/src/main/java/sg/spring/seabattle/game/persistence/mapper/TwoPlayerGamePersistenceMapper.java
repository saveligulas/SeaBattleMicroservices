package sg.spring.seabattle.game.persistence.mapper;

import sg.spring.core.mapper.IDomainPersistenceMapper;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerColor;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerGame;
import sg.spring.seabattle.game.persistence.TwoPlayerGameNode;

public class TwoPlayerGamePersistenceMapper implements IDomainPersistenceMapper<TwoPlayerGame, TwoPlayerGameNode> {
    public static final TwoPlayerGamePersistenceMapper INSTANCE = new TwoPlayerGamePersistenceMapper();

    private TwoPlayerGamePersistenceMapper() {
    }


    @Override
    public TwoPlayerGame toDomain(TwoPlayerGameNode entity) {
        if (entity == null) {
            return null;
        }

        TwoPlayerGame game = new TwoPlayerGame();
        game.setIdentifier(entity.getIdentifier());

        //TODO: DRY this
        game.setPlayerRed(TwoPlayerGamePlayerPersistenceMapper.INSTANCE.toDomain(entity.getPlayers()
                .stream()
                .filter(p -> p.getColor().equals(TwoPlayerColor.RED))
                .findFirst()
                .orElse(null)));
        game.setPlayerBlue(TwoPlayerGamePlayerPersistenceMapper.INSTANCE.toDomain(entity.getPlayers()
                .stream()
                .filter(p -> p.getColor().equals(TwoPlayerColor.BLUE))
                .findFirst()
                .orElse(null)));
        game.setAllowedShips(entity.getAllowedShips());
        game.setGamePhase(entity.getPhase());
        game.setActivePlayer(entity.getActivePlayer());

        return game;
    }

    @Override
    public TwoPlayerGameNode toEntity(TwoPlayerGame domain) {
        if (domain == null) {
            return null;
        }

        TwoPlayerGameNode entity = new TwoPlayerGameNode();
        entity.setId(domain.getIdentifier());
        entity.getPlayers().add(TwoPlayerGamePlayerPersistenceMapper.INSTANCE.toEntity(domain.getPlayerRed()));
        entity.getPlayers().add(TwoPlayerGamePlayerPersistenceMapper.INSTANCE.toEntity(domain.getPlayerBlue()));
        entity.setAllowedShips(domain.getAllowedShips());
        entity.setPhase(domain.getGamePhase());
        entity.setActivePlayer(domain.getActivePlayerColor());

        return entity;
    }
}
