package sg.spring.seabattle.game.controller;

import lombok.Data;
import org.yaml.snakeyaml.util.Tuple;

import java.util.List;

@Data
public class CreateMapDTO {
    //TODO: use x and y instead of index
    List<List<Integer>> ships;
}
