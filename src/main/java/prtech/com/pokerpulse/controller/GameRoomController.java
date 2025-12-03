package prtech.com.pokerpulse.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.service.GameService;
import jakarta.validation.constraints.NotBlank;


import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin
@Slf4j
public class GameRoomController {
    private final GameService gameService;

    public GameRoomController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<GameRoom>> getRooms() {
        log.info("GameRoomController : Fetching all game rooms from database{}",gameService.getAllRooms());
        return ResponseEntity.ok(gameService.getAllRooms());
    }
    @GetMapping("/memory")
    public ResponseEntity<List<GameRoom>> getRoomsFromMem() {
        log.info("GameRoomController : Fetching all game rooms from memory{}",gameService.getRooms());
        return ResponseEntity.ok(gameService.getRooms().values().stream().toList());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoom> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(gameService.getRoomById(roomId));
    }
    @GetMapping("/memory/{roomId}")
    public ResponseEntity<GameRoom> getRoomFromMem(@PathVariable Long roomId) {
        return ResponseEntity.ok(gameService.getRooms().get(roomId));
    }

    @PostMapping
    public ResponseEntity<GameRoom> createRoom(@RequestBody @NotBlank String roomName) {
        GameRoom room = gameService.createRoom(roomName);
        log.info("GameRoomController : Room created: {}", roomName);
        return ResponseEntity.ok(room);
    }
    @DeleteMapping("/{roomId}")
    public ResponseEntity<String> deleteRoom(@PathVariable Long roomId) {
        gameService.deleteRoom(roomId);
        log.info("GameRoomController : Room deleted: {}", roomId);
        return ResponseEntity.ok("Room deleted successfully");
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<GameRoom> joinRoom(@PathVariable Long roomId, @RequestBody @NotBlank String username) {
        GameRoom room = gameService.joinRoom(roomId, username);
        log.info("GameRoomController : Player {} is attempting to join room {}", username, roomId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<GameRoom> leaveRoom(@PathVariable Long roomId,  @RequestBody @NotBlank String username) {
        GameRoom room = gameService.leaveRoom(roomId, username);
        log.info("GameRoomController : Player {} is attempting to leave room {}", username, roomId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<GameRoom> startRoom(@PathVariable Long roomId) {
        GameRoom room = gameService.startGame(roomId);
        log.info("GameRoomController : Started game for room {}", roomId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomId}/{playerId}/privateCards")
    public ResponseEntity<List<?>> getPrivateCards(@PathVariable Long roomId, @PathVariable Long playerId) {
        List<?> privateCards = gameService.dealPrivateCards(roomId, playerId);
        log.info("GameRoomController : Fetching private cards for player {} in room {}", playerId, roomId);
        return ResponseEntity.ok(privateCards);
    }
}
