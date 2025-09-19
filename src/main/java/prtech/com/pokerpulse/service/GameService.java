package prtech.com.pokerpulse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.card.Deck;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.repository.GameRoomRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    @Autowired GameRoomRepository repository;

    public List<GameRoom> getAllRooms() {
        return ((List<GameRoom>) repository.findAll());
    }

    public GameRoom getRoomById(Integer roomId) {
        return repository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room with ID " + roomId + " not found"));
    }

    public GameRoom createRoom(String roomName) {
        GameRoom room = new GameRoom();
        room.setRoomName(roomName);
        repository.save(room);
        return room;
    }

    public GameRoom joinRoom(Integer roomId, Player player) {
        GameRoom room = repository.findByRoomId(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        boolean exists = room.getPlayers().stream().anyMatch(p -> p.getUsername().equals(player.getUsername()));
        if (!exists) {
            room.getPlayers().add(player);
        }
        repository.save(room);
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

//    public GameRoom startGame(Integer roomId, String initiator) {
//        GameRoom room = getRoomById(roomId);
//        if (room.getPlayers().size() < 2) {
//            throw new IllegalArgumentException("At least two players required to start");
//        }
//        // Deal 2 cards to each player from a fresh deck
//        Deck deck = new Deck();
//        List<Card> d = deck.initializeShuffledDeck();
//        int idx = 0;
//        for (Player p : room.getPlayers()) {
//            p.getHand().clear();
//            p.getHand().add(d.get(idx++));
//            p.getHand().add(d.get(idx++));
//        }
//        return room;
//    }

    public ChatMessage sendMessage(Integer roomId, ChatMessage message) {
        GameRoom room = repository.findByRoomId(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.getChatHistory().add(message);
        return message;
    }
}
