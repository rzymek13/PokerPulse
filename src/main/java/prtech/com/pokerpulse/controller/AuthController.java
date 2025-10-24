package prtech.com.pokerpulse.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.service.GameService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
@Slf4j
public class AuthController {

    private final GameService gameService;

    public AuthController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {

        Player player = gameService.register(request.getUsername(), request.getPassword());
        log.info("AuthController : Registered new player: {}", player.getUsername());
        return ResponseEntity.ok(player.getUsername() + " registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest request) {
        Player player = gameService.login(request.getUsername(), request.getPassword());
        log.info("AuthController : Player logged in: {}", player.getUsername());
        return ResponseEntity.ok(player.getUsername() + " logged in successfully");
    }

    @GetMapping("/players")
    public ResponseEntity<List<Player>> getAllPlayers() {
        if (gameService.getPlayers().isEmpty()) {
            return ResponseEntity.noContent().build();
        } else return ResponseEntity.ok(gameService.getPlayers());
    }
    @DeleteMapping("/players/{playerId}")
    public ResponseEntity<String> deletePlayer(@PathVariable Long playerId) {
        gameService.deletePlayer(playerId);
        log.info("AuthController : Player deleted: {}", playerId);
        return ResponseEntity.ok("Player deleted successfully");
    }
}

@Data
class AuthRequest {
    private String username;
    private String password;
}