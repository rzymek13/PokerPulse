package prtech.com.pokerpulse.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.service.GameService;
import jakarta.validation.constraints.NotBlank;
import prtech.com.pokerpulse.service.PlayerService;


import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin
@Slf4j
public class GameRoomController {
    private final GameService gameService;
    private final PlayerService playerService;

    public GameRoomController(GameService gameService, PlayerService playerService) {
        this.gameService = gameService;
        this.playerService = playerService;
    }

    @GetMapping
    public ResponseEntity<List<GameRoom>> getRooms() {
        return ResponseEntity.ok(gameService.getAllRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoom> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(gameService.getRoomById(roomId));
    }

    @PostMapping
    public ResponseEntity<GameRoom> createRoom(@RequestBody @NotBlank String roomName) {
        GameRoom room = gameService.createRoom(roomName);
        log.info("GameRoomController : Room created: {}", roomName);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<GameRoom> joinRoom(@PathVariable Long roomId, @RequestBody @NotBlank String username) {
        Player player = playerService.findByName(username);
        GameRoom room = gameService.joinRoom(roomId, player);
        return ResponseEntity.ok(room);
    }
}
