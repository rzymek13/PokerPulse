package prtech.com.pokerpulse.service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.model.game.Hand;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.repository.TableStorageRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
@Data
public class GameService {

    @Autowired
    TableStorageRepository tableStorageRepository;

    Map<Long, GameRoom> rooms = new ConcurrentHashMap<>();


    @PostConstruct
    public void initRooms() {
        try {
            tableStorageRepository.findRooms().forEach(r -> rooms.put(r.getGameRoomId(), r));
            log.info("Initialized rooms map with {} rooms", rooms.size());
        } catch (Exception e) {
            log.warn("Failed to initialize rooms map from repository", e);
        }
    }

    public List<Player> getPlayers() {
        return tableStorageRepository.findPlayers();
    }

    public Player getPlayerById(Long playerId) {
        return tableStorageRepository.findPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Player with ID " + playerId + " not found"));
    }

    public Player getPlayerByUsername(String username) {
        return tableStorageRepository.findPlayers().stream()
                .filter(player -> player.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Player with username " + username + " not found"));
    }

    public void deletePlayer(Long playerId) {
        if (getPlayerById(playerId) == null) {
            throw new IllegalArgumentException("Player not found");
        }
        tableStorageRepository.deletePlayer(playerId);
        log.info("PlayerService : Deleted player with ID {}", playerId);
    }

    public Player register(String username, String password) {
        if (tableStorageRepository.findPlayers().stream().anyMatch(u -> u.getUsername().equals(username))) {
            throw new IllegalArgumentException("Username already exists");
        }
        Player newPlayer = new Player(username, password);
        tableStorageRepository.savePlayer(newPlayer);
        log.info("PlayerService  : Registering new player: {}", newPlayer.getUsername());
        return newPlayer;
    }

    public Player login(String username, String password) {
        return tableStorageRepository.findPlayers()
                .stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }


    public List<GameRoom> getAllRooms() {
        return tableStorageRepository.findRooms();
    }


    public GameRoom getRoomById(Long roomId) {
        return tableStorageRepository.findRoom(roomId);
    }

    public GameRoom createRoom(String roomName) {
        GameRoom room = new GameRoom(roomName);

        tableStorageRepository.saveRoom(room);
        rooms.putIfAbsent(room.getGameRoomId(), room);
        log.info("GameService  :  Creating new room: {}", room);
        return room;
    }

    public void deleteRoom(Long roomId) {
        if (getRoomById(roomId) == null) {
            throw new IllegalArgumentException("Room not found");
        }
        tableStorageRepository.deleteRoom(roomId);
        rooms.remove(roomId);
        log.info("Game Service : Deleted room with ID {}", roomId);
    }

    public GameRoom joinRoom(Long roomId, String username) {
        Player player = getPlayerByUsername(username);
        GameRoom room = rooms.computeIfAbsent(roomId, this::getRoomById);
        if (room.getPlayers().contains(player)) {
            throw new IllegalArgumentException("Player already in room");
        }
        if (room.getPlayers().size() >= 4) {
            throw new IllegalArgumentException("Pan room can contain at most 4 players");
        }

        room.getPlayers().add(player);
        log.info("Game Service : Player {} joined room {}", username, roomId);

        return room;
    }

    public GameRoom leaveRoom(Long roomId, String username) {
        if (getPlayerByUsername(username) == null) {
            throw new IllegalArgumentException("Player not found");
        }
        Player player = getPlayerByUsername(username);
        GameRoom room = rooms.computeIfAbsent(roomId, this::getRoomById);
        if (!room.getPlayers().contains(player)) {
            throw new IllegalArgumentException("Player not in room");
        }
        room.getPlayers().remove(player);
        log.info("Game Service : Player {} left room {}", username, roomId);
        return room;
    }

    public GameRoom startGame(Long roomId) {
        GameRoom room = rooms.get(roomId);
        if (room.getPlayers().size() < 2) {
            throw new IllegalArgumentException("At least two players required to start");
        }
        Hand hand = new Hand(room.getPlayers());
        room.getHands().add(hand);
        log.info("room {}", room);
        log.info("asdasdasd{}", room.getHands());
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

    public List<Card> dealPrivateCards(Long roomId, Long playerId) {

        GameRoom room = rooms.get(roomId);


        Hand firstHand = room.getHands().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hands in room"));

        return firstHand.getPrivateHands(getPlayerById(playerId));
    }

    public GameRoom processPlayerAction(Long roomId, Long playerId, String decision, Integer amount) {
        GameRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Room not found");
        if (room.getHands() == null || room.getHands().isEmpty()) throw new IllegalArgumentException("No active hand in room");

        Hand hand = room.getHands().get(0);

        // ensure contribution map has entry
        hand.getPlayerContributions().putIfAbsent(playerId, 0);

        switch (decision == null ? "" : decision.toUpperCase()) {
            case "FOLD":
                hand.getFoldedPlayers().add(playerId);
                // if only one active player remains -> announce winner
                long active = room.getPlayers().stream().filter(p -> !hand.getFoldedPlayers().contains(p.getPlayerId())).count();
                if (active == 1) {
                    var winner = room.getPlayers().stream().filter(p -> !hand.getFoldedPlayers().contains(p.getPlayerId())).findFirst();
                    ChatMessage msg = new ChatMessage();
                    msg.setContent("Player with id " + playerId + " folded. Winner: " + winner.map(p -> p.getUsername()).orElse("unknown"));
                    msg.setSender(null);
                    room.getChatHistory().add(msg);
                }
                break;
            case "CALL":
                int currentBet = hand.getCurrentBet();
                int contributed = hand.getPlayerContributions().getOrDefault(playerId, 0);
                int needed = Math.max(0, currentBet - contributed);
                hand.getPlayerContributions().put(playerId, contributed + needed);
                hand.setPot(hand.getPot() + needed);
                break;
            case "RAISE":
                if (amount == null || amount <= 0) throw new IllegalArgumentException("Raise amount required");
                int raiseTo = amount;
                int prevContrib = hand.getPlayerContributions().getOrDefault(playerId, 0);
                int needRaise = Math.max(0, raiseTo - prevContrib);
                hand.getPlayerContributions().put(playerId, prevContrib + needRaise);
                hand.setCurrentBet(raiseTo);
                hand.setPot(hand.getPot() + needRaise);
                break;
            case "CHECK":
                // only allowed when player has already matched currentBet
                // nothing to do here for now
                break;
            default:
                // unknown action - ignore
                break;
        }

        // advance current player to next active one
        List<Player> players = room.getPlayers();
        if (players != null && !players.isEmpty()) {
            // find current player index
            Long currId = hand.getCurrentPlayer() != null ? hand.getCurrentPlayer().getPlayerId() : null;
            int startIdx = 0;
            if (currId != null) {
                for (int i = 0; i < players.size(); i++) if (players.get(i).getPlayerId().equals(currId)) { startIdx = i; break; }
            }
            int next = (startIdx + 1) % players.size();
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get((next + i) % players.size());
                if (!hand.getFoldedPlayers().contains(p.getPlayerId())) {
                    hand.setCurrentPlayer(p);
                    break;
                }
            }
        }

        return room;
    }
}
