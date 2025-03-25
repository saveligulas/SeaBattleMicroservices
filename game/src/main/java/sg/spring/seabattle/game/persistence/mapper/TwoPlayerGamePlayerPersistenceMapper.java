package sg.spring.seabattle.game.persistence.mapper;

import org.jspecify.annotations.Nullable;
import sg.spring.core.mapper.IDomainPersistenceMapper;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerGamePlayer;
import sg.spring.seabattle.game.persistence.GameMapNodeRelation;
import sg.spring.seabattle.game.persistence.TwoPlayerGamePlayerNode;
import sg.spring.seabattle.game.persistence.TwoPlayerGamePlayerRelation;

public class TwoPlayerGamePlayerPersistenceMapper implements IDomainPersistenceMapper<TwoPlayerGamePlayer, TwoPlayerGamePlayerRelation> {
    public static final TwoPlayerGamePlayerPersistenceMapper INSTANCE = new TwoPlayerGamePlayerPersistenceMapper();
    
    private TwoPlayerGamePlayerPersistenceMapper() {
    }

    @Override
    public @Nullable TwoPlayerGamePlayer toDomain(@Nullable TwoPlayerGamePlayerRelation entity) {
        if (entity == null) {
            return null;
        }

        TwoPlayerGamePlayer player = new TwoPlayerGamePlayer();
        player.setId(entity.getId());
        player.setColor(entity.getColor());
        player.setName(entity.getPlayer().getName());
        player.setOpponentMap(GameMapPersistenceMapper.INSTANCE.toDomain(entity.getPlayer().getGameMap()));

        return player;
    }

    @Override
    public @Nullable TwoPlayerGamePlayerRelation toEntity(@Nullable TwoPlayerGamePlayer domain) {
        if (domain == null) {
            return null;
        }

        TwoPlayerGamePlayerRelation relation = new TwoPlayerGamePlayerRelation();
        relation.setId(domain.getId());
        relation.setColor(domain.getColor());

        TwoPlayerGamePlayerNode playerNode = new TwoPlayerGamePlayerNode();
        playerNode.setName(domain.getName());
        
        GameMapNodeRelation mapRelation = GameMapPersistenceMapper.INSTANCE.toEntity(domain.getOpponentMap());
        if (mapRelation != null) {
            playerNode.setGameMap(mapRelation);
        }

        relation.setPlayer(playerNode);

        return relation;
    }
}
