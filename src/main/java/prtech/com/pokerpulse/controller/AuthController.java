package prtech.com.pokerpulse.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prtech.com.pokerpulse.service.PlayerService;
import prtech.com.pokerpulse.model.player.Player;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
@Slf4j
public class AuthController {
    private final PlayerService playerService;

    public AuthController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {

        Player player = playerService.register(request.getUsername(), request.getPassword());
        log.info("Registered new player: {}", player.getUsername());
        return ResponseEntity.ok(player.getUsername() + " registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest request) {
        Player player = playerService.login(request.getUsername(), request.getPassword());
        log.info("Player logged in: {}", player.getUsername());
        return ResponseEntity.ok(player.getUsername() + " logged in successfully");
    }

    @GetMapping("/players")
    public ResponseEntity<List<Player>> getAllPlayers() {
        if (playerService.getPlayers().isEmpty()) {
            return ResponseEntity.noContent().build();
        } else return ResponseEntity.ok(playerService.getPlayers());

    }
}

@Data
class AuthRequest {
    private String username;
    private String password;
}