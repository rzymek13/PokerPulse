package prtech.com.pokerpulse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.game.Hand;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.repository.GameRoomRepository;

import java.util.List;

@Service
@Slf4j
public class GameService {

    @Autowired GameRoomRepository repository;

    public List<GameRoom> getAllRooms() {
        return repository.findAll();
    }
    //zrob porzadnie graczy i pokoje

    public GameRoom getRoomById(Long roomId) {
        return repository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room with ID " + roomId + " not found"));
    }

    public GameRoom createRoom(String roomName) {
        GameRoom room = new GameRoom(roomName);

        repository.save(room);
        log.info("GameService  :  Creating new room: {}", roomName);
        return room;
    }

    public GameRoom joinRoom(Long roomId, Player player) {
        GameRoom room = repository.findByRoomId(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        boolean exists = room.getPlayers().stream().anyMatch(p -> p.getUsername().equals(player.getUsername()));
        if (!exists) {
            room.getPlayers().add(player);
        }
//        repository.save(room);
        return room;
    }
//
//    public GameRoom setReady(Integer roomId, String username, boolean ready) {
//        GameRoom room = getRoomById(roomId);
//        room.getPlayers().stream()
//                .filter(p -> p.getUsername().equals(username))
//                .findFirst()
//                .ifPresent(p -> p.setReady(ready));
//        return room;
//    }

    public GameRoom startGame(Long roomId) {
        GameRoom room = getRoomById(roomId);
//        if (room.getPlayers().size() < 2) {
//            throw new IllegalArgumentException("At least two players required to start");
//        }
        // Deal 2 cards to each player from a fresh deck
        Hand hand = new Hand(room.getPlayers());
        log.info("room", room);
        log.info("hand", hand);
        return room;
    }

    public ChatMessage sendMessage(Long roomId, ChatMessage message) {
        GameRoom room = repository.findByRoomId(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.getChatHistory().add(message);
        log.info(" Game Service : Message sent in room {}: {}", roomId, message.getContent());
        return message;
    }
}
