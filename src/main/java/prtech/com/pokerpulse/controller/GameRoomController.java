package prtech.com.pokerpulse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.service.GameService;
import jakarta.validation.constraints.NotBlank;


import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin
public class GameRoomController {
    private final GameService gameService;

    public GameRoomController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<GameRoom>> getRooms() {
        return ResponseEntity.ok(gameService.getAllRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoom> getRoom(@PathVariable Integer roomId) {
        return ResponseEntity.ok(gameService.getRoomById(roomId));
    }

    @PostMapping
    public ResponseEntity<GameRoom> createRoom(@RequestBody @NotBlank String roomName) {
        GameRoom room = gameService.createRoom(roomName);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<GameRoom> joinRoom(@PathVariable Integer roomId, @RequestBody @NotBlank String username) {
        Player player = new Player();
        player.setUsername(username);
        GameRoom room = gameService.joinRoom(roomId, player);
        return ResponseEntity.ok(room);
    }
}
