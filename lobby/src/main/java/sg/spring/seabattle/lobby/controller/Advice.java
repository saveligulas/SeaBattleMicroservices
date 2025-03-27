package sg.spring.seabattle.lobby.controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import sg.spring.seabattle.commons.advice.CommonHeaderAdvice;
import sg.spring.seabattle.lobby.service.GameServiceException;

@ControllerAdvice(assignableTypes = {LobbyController.class})
public class Advice extends CommonHeaderAdvice {

    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public String handleCircuitBreakerOpen() {
        return "Game service is temporarily unavailable due to too many failures. Please try again later.";
    }
    
    @ExceptionHandler(GameServiceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public String handleGameServiceException(GameServiceException ex) {
        return ex.getMessage();
    }
}
