package sg.spring.seabattle.lobby.service;

public class GameServiceException extends RuntimeException {
    
    public GameServiceException(String message) {
        super(message);
    }
    
    public GameServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
