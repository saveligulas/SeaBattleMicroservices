package sg.spring.seabattle.game.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sg.spring.seabattle.game.service.TwoPlayerGameService;

import java.util.UUID;

@Component
public class GameQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(GameQueueListener.class);
    
    private final TwoPlayerGameService gameService;

    @Autowired
    public GameQueueListener(TwoPlayerGameService gameService) {
        this.gameService = gameService;
    }

    @RabbitListener(queues = "${game.queue.name}")
    public void receiveMessage(String message) {
        logger.info("Received message from queue: {}", message);
        
        UUID gameId = gameService.processGameCreationMessage(message);
        
        if (gameId != null) {
            logger.info("Successfully processed game creation: {}", gameId);
        } else {
            logger.error("Failed to process game creation message");
        }
    }
}
