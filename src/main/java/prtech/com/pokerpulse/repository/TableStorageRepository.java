package prtech.com.pokerpulse.repository;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

@Repository
public class TableStorageRepository {
    private final TableClient users;
    private final TableClient rooms;
    private final AtomicLong ids = new AtomicLong(System.currentTimeMillis());

    public TableStorageRepository(@Value("${azure.storage.account-name}") String accountName) {
        var service = new TableServiceClientBuilder().endpoint("https://" + accountName + ".table.core.windows.net")
                .credential(new DefaultAzureCredentialBuilder().build()).buildClient();
        users = service.createTableIfNotExists("users");
        rooms = service.createTableIfNotExists("rooms");
    }

    public List<Player> findPlayers() { return StreamSupport.stream(users.listEntities().spliterator(), false).map(this::toPlayer).toList(); }
    public Player findPlayer(String username) { try { return toPlayer(users.getEntity("user", username)); } catch (Exception e) { throw new RuntimeException("Player not found", e); } }
    public Player savePlayer(Player p) { if (p.getPlayerId() == null) p.setPlayerId(ids.incrementAndGet()); var e = new TableEntity("user", p.getUsername()); e.addProperty("id", p.getPlayerId()).addProperty("password", p.getPassword()); users.upsertEntity(e); return p; }
    public void deletePlayer(Long id) { findPlayers().stream().filter(p -> p.getPlayerId().equals(id)).findFirst().ifPresent(p -> users.deleteEntity("user", p.getUsername())); }
    public List<GameRoom> findRooms() { return StreamSupport.stream(rooms.listEntities().spliterator(), false).map(this::toRoom).toList(); }
    public GameRoom findRoom(Long id) { return toRoom(rooms.getEntity("room", id.toString())); }
    public GameRoom saveRoom(GameRoom r) { if (r.getGameRoomId() == null) r.setGameRoomId(ids.incrementAndGet()); var e = new TableEntity("room", r.getGameRoomId().toString()); e.addProperty("name", r.getRoomName()); rooms.upsertEntity(e); return r; }
    public void deleteRoom(Long id) { rooms.deleteEntity("room", id.toString()); }
    private Player toPlayer(TableEntity e) { var p = new Player(e.getProperty("username") == null ? e.getRowKey() : e.getProperty("username").toString(), e.getProperty("password").toString()); p.setPlayerId(((Number)e.getProperty("id")).longValue()); return p; }
    private GameRoom toRoom(TableEntity e) { var r = new GameRoom(e.getProperty("name").toString()); r.setGameRoomId(Long.parseLong(e.getRowKey())); return r; }
}
