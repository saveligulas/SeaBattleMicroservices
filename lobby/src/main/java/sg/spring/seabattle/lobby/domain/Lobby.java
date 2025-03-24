package sg.spring.seabattle.lobby.domain;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("Lobby")
@Getter
@Setter
public class Lobby {
    private String id;
    private String playerOneId;
    private String playerTwoId;

    public void addUser(String userId) {
        if (playerOneId == null) {
            playerOneId = userId;
            return;
        }

        if (playerTwoId == null) {
            playerTwoId = userId;
            return;
        }

        throw new IllegalStateException("Lobby is already full");
    }
}
