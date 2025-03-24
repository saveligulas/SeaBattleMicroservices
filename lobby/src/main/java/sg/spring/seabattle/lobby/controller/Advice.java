package sg.spring.seabattle.lobby.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import sg.spring.seabattle.commons.advice.CommonHeaderAdvice;

@ControllerAdvice(assignableTypes = {LobbyController.class})
public class Advice extends CommonHeaderAdvice {
}
