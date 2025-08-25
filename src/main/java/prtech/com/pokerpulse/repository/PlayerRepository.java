package prtech.com.pokerpulse.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import prtech.com.pokerpulse.model.player.Player;

import java.util.List;

public interface PlayerRepository extends CrudRepository<Player, Long> {

    // Additional methods specific to PlayerRepository can be added her
    @Query("select count(*) from players")
    int countItems();

    @Query("select * from player")
    List<Player> findAll();
}
