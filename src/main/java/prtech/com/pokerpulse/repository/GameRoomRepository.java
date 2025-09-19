package prtech.com.pokerpulse.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import prtech.com.pokerpulse.model.room.GameRoom;

@Repository
public interface GameRoomRepository extends CrudRepository<GameRoom, Integer> {
    @Query("select * from game_room where room_id = :roomId")
    GameRoom findByRoomId(Integer roomId);

    GameRoom save(String roomName);
}
