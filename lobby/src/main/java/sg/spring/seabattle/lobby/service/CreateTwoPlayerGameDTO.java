package sg.spring.seabattle.lobby.service;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Data
public class CreateTwoPlayerGameDTO {
    private String redPlayer;
    private String bluePlayer;
    @Nullable
    private List<Integer> shipSizes;
}
