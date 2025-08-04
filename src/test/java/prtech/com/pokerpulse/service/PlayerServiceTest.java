package prtech.com.pokerpulse.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import prtech.com.pokerpulse.model.player.Player;


class PlayerServiceTest {
    PlayerService playerService = new PlayerService();


     // Assuming this method sets up the service

    @Test
    void register() {
        playerService.register("testUser", "testPassword");
        Assertions.assertNotNull(playerService.findByName("testUser"));
    }

    @Test
    void login() {
        Player testPlayer1 = new Player();
        testPlayer1.setUsername("testUser1");
        testPlayer1.setPassword("testPassword1");
        playerService.getPlayers().add(testPlayer1);
        Assertions.assertNotNull(playerService.login("testUser1", "testPassword1"));


    }
}