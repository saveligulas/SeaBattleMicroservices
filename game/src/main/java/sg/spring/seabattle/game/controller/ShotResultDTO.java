package sg.spring.seabattle.game.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sg.spring.seabattle.game.domain.ShotResult;
import sg.spring.seabattle.game.domain.twoplayer.TwoPlayerColor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShotResultDTO {
    private ShotResult result;
    private boolean turnEnded;
    private TwoPlayerColor nextPlayer;
    private boolean gameEnded;
    private TwoPlayerColor winner;
}
