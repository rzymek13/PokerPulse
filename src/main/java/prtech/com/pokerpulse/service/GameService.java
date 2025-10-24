package prtech.com.pokerpulse.service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.game.Hand;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.repository.GameRoomRepository;
import prtech.com.pokerpulse.repository.PlayerRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
@Data
public class GameService {

    @Autowired
    GameRoomRepository gameRoomRepository;
    @Autowired
    PlayerRepository playerRepository;

    Map<Long, GameRoom> rooms = new ConcurrentHashMap<>();


    @PostConstruct
    public void initRooms() {
        try {
            gameRoomRepository.findAll().forEach(r -> rooms.put(r.getGameRoomId(), r));
            log.info("Initialized rooms map with {} rooms", rooms.size());
        } catch (Exception e) {
            log.warn("Failed to initialize rooms map from repository", e);
        }
    }

    public List<Player> getPlayers() {
        return playerRepository.findAll();
    }

    public Player getPlayerById(Long playerId) {
        return playerRepository.findAll().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Player with ID " + playerId + " not found"));
    }

    public Player getPlayerByUsername(String username) {
        return playerRepository.findAll().stream()
                .filter(player -> player.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Player with username " + username + " not found"));
    }
    public void deletePlayer(Long playerId) {
        if (getPlayerById(playerId) == null) {
            throw new IllegalArgumentException("Player not found");
        }
        playerRepository.deleteById(playerId);
        log.info("PlayerService : Deleted player with ID {}", playerId);
    }

    public Player register(String username, String password) {
        if (playerRepository.findAll().stream().anyMatch(u -> u.getUsername().equals(username))) {
            throw new IllegalArgumentException("Username already exists");
        }
        Player newPlayer = new Player(username, password);
        playerRepository.save(newPlayer);
        log.info("PlayerService  : Registering new player: {}", newPlayer.getUsername());
        return newPlayer;
    }

    public Player login(String username, String password) {
        return playerRepository.findAll()
                .stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }


    public List<GameRoom> getAllRooms() {
        return gameRoomRepository.findAll();
    }

    //zrob porzadnie graczy i pokoje

    public GameRoom getRoomById(Long roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room with ID " + roomId + " not found"));
    }

    public GameRoom createRoom(String roomName) {
        GameRoom room = new GameRoom(roomName);

        gameRoomRepository.save(room);
        rooms.putIfAbsent(room.getGameRoomId(), room);
        log.info("GameService  :  Creating new room: {}", room);
        return room;
    }
    public void deleteRoom(Long roomId) {
        if (getRoomById(roomId) == null) {
            throw new IllegalArgumentException("Room not found");
        }
        gameRoomRepository.deleteById(roomId);
        rooms.remove(roomId);
        log.info("Game Service : Deleted room with ID {}", roomId);
    }

    public GameRoom joinRoom(Long roomId, String username) {

        if (getPlayerByUsername(username) == null) {
            throw new IllegalArgumentException("Player not found");
        }
        Player player = getPlayerByUsername(username);
        if (rooms.get(roomId).getPlayers().contains(player)) {
            throw new IllegalArgumentException("Player already in room");
        }
        if (getRoomById(roomId) == null) {
            throw new IllegalArgumentException("Room not found");
        }
        GameRoom room = rooms.get(roomId);

        room.getPlayers().add(player);
        log.info("Game Service : Player {} joined room {}", username, roomId);

        return room;
    }

    public GameRoom startGame(Long roomId) {
        GameRoom room = rooms.get(roomId);
        if (room.getPlayers().size() < 2) {
            throw new IllegalArgumentException("At least two players required to start");
        }
        Hand hand = new Hand(room.getPlayers());
        log.info("room {}", room);
        log.info("hand {}", hand);
        return room;
    }

    public ChatMessage sendMessage(Long roomId, ChatMessage message) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.getChatHistory().add(message);
        log.info(" Game Service : Message sent in room {}: {}", roomId, message.getContent());
        return message;
    }
}
