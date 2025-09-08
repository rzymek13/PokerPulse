package prtech.com.pokerpulse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.service.GameService;

@Controller
public class PokerWebSocketController {
    @Autowired
    private GameService gameService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Chat (zachowane)
    @MessageMapping("/game/{gameId}/chat")
    public void handleChatMessage(@DestinationVariable Long gameId, @Payload ChatMessage message) {
        gameService.sendMessage(gameId.intValue(), message);
        messagingTemplate.convertAndSend("/topic/room/" + gameId, message);
    }

    // Ready toggle
    public static class ReadyDTO {
        public String username;
        public boolean ready;
    }

//    @MessageMapping("/game/{gameId}/ready")
//    public void handleReady(@DestinationVariable Long gameId, @Payload ReadyDTO dto) {
//        GameRoom room = gameService.setReady(gameId.intValue(), dto.username, dto.ready);
//        messagingTemplate.convertAndSend("/topic/room/" + gameId + "/state", room);
//    }

    // Start game
    public static class StartDTO {
        public String username;
    }

//    @MessageMapping("/game/{gameId}/start")
//    public void handleStart(@DestinationVariable Long gameId, @Payload StartDTO dto) {
//        GameRoom room = gameService.startGame(gameId.intValue(), dto.username);
//        messagingTemplate.convertAndSend("/topic/room/" + gameId + "/state", room);
//    }
}