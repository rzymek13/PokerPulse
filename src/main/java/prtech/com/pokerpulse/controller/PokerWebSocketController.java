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

    // Istniejąca metoda dla akcji gry
//    @MessageMapping("/game/{gameId}/action")
//    public void handlePlayerAction(@DestinationVariable Long gameId, @Payload ActionDTO actionDTO) {
//        Game updatedGame = gameService.performAction(gameId, actionDTO);
//        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedGame);
//    }

    // Nowa metoda dla czatu
    @MessageMapping("/game/{gameId}/chat")
    public void handleChatMessage(@DestinationVariable Long gameId, @Payload ChatMessage message) {
        GameRoom room = gameService.getRoomById(gameId.intValue());
        room.getChatHistory().add(message);
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/chat", message);
    }
}