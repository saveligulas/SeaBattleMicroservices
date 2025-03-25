package sg.spring.seabattle.authorization.config;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity
public class Username {
    @Id
    private String username;

    public Username() {

    }

    public Username(String username) {
        this.username = username;
    }

}
