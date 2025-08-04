package prtech.com.pokerpulse.service;

import org.springframework.stereotype.Service;
import prtech.com.pokerpulse.model.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PlayerService {

    private final List<Player> players = new ArrayList<>();

    public List<Player> getPlayers() {
        return players;
    }

    public Player register(String username, String password) {
        if (players.stream().anyMatch(u -> u.getUsername().equals(username))) {
            throw new IllegalArgumentException("Username already exists");
        }
        Player player = new Player();
        player.setId(UUID.randomUUID().toString());
        player.setUsername(username);
        player.setPassword(password);
        players.add(player);
        return player;
    }

    public Player login(String username, String password) {
        return players.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }

    public Player findByName(String username) {
        return players.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}
