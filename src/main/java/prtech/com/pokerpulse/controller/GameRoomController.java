package prtech.com.pokerpulse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping
    public ResponseEntity<GameRoom> createRoom(@RequestBody @NotBlank String roomName) {
        GameRoom room = gameService.createRoom(roomName);
        return ResponseEntity.ok(room);
    }
}
