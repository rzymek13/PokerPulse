package prtech.com.pokerpulse.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("local")
public class InMemoryRepository implements PersistenceRepository {
    private final Map<Long, Player> players = new ConcurrentHashMap<>();
    private final Map<Long, GameRoom> rooms = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    public List<Player> findPlayers() { return new ArrayList<>(players.values()); }
    public Player findPlayer(String username) { return findPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElseThrow(() -> new RuntimeException("Player not found")); }
    public Player savePlayer(Player p) { if (p.getPlayerId() == null) p.setPlayerId(ids.incrementAndGet()); players.put(p.getPlayerId(), p); return p; }
    public void deletePlayer(Long id) { players.remove(id); }
    public List<GameRoom> findRooms() { return new ArrayList<>(rooms.values()); }
    public GameRoom findRoom(Long id) { var room = rooms.get(id); if (room == null) throw new RuntimeException("Room not found"); return room; }
    public GameRoom saveRoom(GameRoom r) { if (r.getGameRoomId() == null) r.setGameRoomId(ids.incrementAndGet()); rooms.put(r.getGameRoomId(), r); return r; }
    public void deleteRoom(Long id) { rooms.remove(id); }
}
