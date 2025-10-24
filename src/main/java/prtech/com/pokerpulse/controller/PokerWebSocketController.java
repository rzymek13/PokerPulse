package prtech.com.pokerpulse.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.service.GameService;

@Controller
@Slf4j
@CrossOrigin
public class PokerWebSocketController {


    private final GameService gameService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public PokerWebSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable Long roomId, @Payload ChatMessage message) {
        GameRoom room = gameService.getRooms().get(roomId);
        room.getChatHistory().add(message);
        log.info("wiadomość wysłana przez PokerWebSocketController: {} w pokoju :{}", message.getContent(), roomId);
        return message;
    }
    @MessageMapping("/game/{roomId}/{playerId}")
    @SendTo("/topic/game/{playerId}")
    public GameRoom blabla(@DestinationVariable Long roomId, @DestinationVariable Long playerId){
        GameRoom room = gameService.getRooms().get(roomId);
        gameService.startGame(roomId);
        log.info("Aktualizacja gry dla gracza: {} w pokoju :{}", playerId, roomId);
        return room;
    }
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //prywatne karty gracza - wysyłane tylko do niego
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //

}