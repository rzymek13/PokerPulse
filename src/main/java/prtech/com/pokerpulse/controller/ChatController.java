package prtech.com.pokerpulse.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.service.GameService;
import prtech.com.pokerpulse.model.chat.ChatMessage;

@Controller
@CrossOrigin
@Slf4j
public class ChatController {

    private final GameService gameService;

    public ChatController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable Long roomId, @Payload ChatMessage message) {
        GameRoom room = gameService.getRoomById(roomId);
        room.getChatHistory().add(message);
        log.info("wiadomość wysłana przez ChatController: {} w pokoju :{}", message.getContent(), roomId);
        return message;
    }
}