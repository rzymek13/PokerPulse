package prtech.com.pokerpulse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.repository.PlayerRepository;

import java.util.List;

@Service
@Slf4j
public class PlayerService {

    @Autowired PlayerRepository playerRepository;
//    List<Player> players = getPlayers();



    public List<Player> getPlayers() {
        return playerRepository.findAll();
    }

    public Player register(String username, String password) {
        if (playerRepository.findAll().stream().anyMatch(u -> u.getUsername().equals(username)))
        {
            throw new IllegalArgumentException("Username already exists");
        }
        Player newPlayer = new Player(username, password);
        playerRepository.save(newPlayer);
        log.info("Registering new player: {}", newPlayer.getUsername());
        return newPlayer;
    }

    public Player login(String username, String password) {
        return playerRepository.findAll()
                .stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }

    public Player findByName(String username) {
        return playerRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}
