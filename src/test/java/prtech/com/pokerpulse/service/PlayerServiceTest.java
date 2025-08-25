package prtech.com.pokerpulse.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.repository.PlayerRepository;

//@Transactional
class PlayerServiceTest {

    PlayerRepository repository;

    @Test
    public void createUpdateDeleteOrder() {
        System.out.println(repository.countItems());
        Player player = new Player("testUser", "testPass");
        Player player2 = new Player("testUser2", "testPass2");
        repository.save(player);
        repository.save(player2);
        System.out.println(repository.countItems());
    }
    }


