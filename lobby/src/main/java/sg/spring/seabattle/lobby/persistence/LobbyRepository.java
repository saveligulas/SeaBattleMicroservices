package sg.spring.seabattle.lobby.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import sg.spring.seabattle.lobby.domain.Lobby;

@Repository
public interface LobbyRepository extends CrudRepository<Lobby, String> {
}
