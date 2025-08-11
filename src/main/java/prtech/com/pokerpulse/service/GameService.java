package prtech.com.pokerpulse.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.stereotype.Service;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class GameService {

    private final Map<Integer, GameRoom> gameRooms = new ConcurrentHashMap<>();

    public List<GameRoom> getAllRooms() {
        return List.copyOf(gameRooms.values());
    }

    public GameRoom getRoomById(Integer roomId) {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("Room with ID " + roomId + " not found");
        }
        return room;
    }
    public GameRoom createRoom(String roomName) {
        GameRoom room = new GameRoom();
        room.setRoomName(roomName);
        System.out.println("Creating room with name: " + roomName+" and ID: " + room.getRoomId());
        gameRooms.put(room.getRoomId(), room);
        return room;
    }

    public GameRoom joinRoom(Integer roomId, Player player) {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.getPlayers().add(player);
        return room;
    }
    public ChatMessage sendMessage(Integer roomId, ChatMessage message) {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.getChatHistory().add(message);
        return message;
    }
}
