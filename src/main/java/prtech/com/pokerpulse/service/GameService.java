package prtech.com.pokerpulse.service;

import org.springframework.stereotype.Service;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class GameService {

    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();

    public List<GameRoom> getAllRooms() {
        return List.copyOf(gameRooms.values());
    }

    public GameRoom getRoomById(String roomId) {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("Room with ID " + roomId + " not found");
        }
        return room;
    }
    public GameRoom createRoom(String roomId) {
        GameRoom room = new GameRoom();
        room.setRoomId(roomId);
        gameRooms.put(roomId, room);
        return room;
    }

    public GameRoom joinRoom(String roomId, Player player) {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.getPlayers().add(player);
        return room;
    }
    public ChatMessage sendMessage(String roomId, ChatMessage message) {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.getChatHistory().add(message);
        return message;
    }
}
