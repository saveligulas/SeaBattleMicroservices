package sg.spring.seabattle.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class
})
public class SeaBattleV2Application {

    public static void main(String[] args) {
        SpringApplication.run(SeaBattleV2Application.class, args);
    }

}
