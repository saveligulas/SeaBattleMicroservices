package sg.spring.seabattle.lobby.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Primary
@Service
public class AmqpGameServiceImpl implements GameService {

    private final RabbitTemplate rabbitTemplate;
    private final String gameQueueName;

    @Autowired
    public AmqpGameServiceImpl(
            RabbitTemplate rabbitTemplate,
            @Value("${game.queue.name:game-creation-queue}") String gameQueueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.gameQueueName = gameQueueName;
    }

    @Override
    public String createGame(String playerRedId, String playerBlueId) {
        // Generate a game ID
        String gameId = UUID.randomUUID().toString();
        
        // Format the message using semicolons as requested
        // Format: gameId;redPlayerId;bluePlayerId
        String message = String.format("%s;%s;%s", gameId, playerRedId, playerBlueId);
        
        // Send the message to the queue
        rabbitTemplate.convertAndSend(gameQueueName, message);
        
        // Return the generated game ID
        return gameId;
    }
}
