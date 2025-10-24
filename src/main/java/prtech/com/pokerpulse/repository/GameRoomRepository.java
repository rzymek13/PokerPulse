package prtech.com.pokerpulse.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import prtech.com.pokerpulse.model.room.GameRoom;

import java.util.List;

@Repository
public interface GameRoomRepository extends CrudRepository<GameRoom, Long> {
    @Query("select * from game_rooms where game_room_id = :roomId")
    GameRoom findByRoomId(Long roomId);

    @Query("select * from game_rooms")
    List<GameRoom> findAll();

    GameRoom save(String roomName);

//    @Query("INSERT INTO game_room_players (game_room_id, player_id) VALUES (:roomId, :playerId)")
//    GameRoom insertPlayerToRoom(Long roomId, Long playerId);
//    //popraw to
    void deleteById(Long roomId);
}
