package prtech.com.pokerpulse.repository;

import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import java.util.List;

public interface PersistenceRepository {
    List<Player> findPlayers(); Player findPlayer(String username); Player savePlayer(Player player); void deletePlayer(Long id);
    List<GameRoom> findRooms(); GameRoom findRoom(Long id); GameRoom saveRoom(GameRoom room); void deleteRoom(Long id);
}
