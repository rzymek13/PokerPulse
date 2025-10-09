package prtech.com.pokerpulse.model.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.chat.ChatMessage;

import java.util.ArrayList;
import java.util.List;



@Data
@RequiredArgsConstructor
@Table("game_rooms")
public class GameRoom {

    @Id @Column("game_room_id") private Long gameRoomId;
    @Column("room_name") private String roomName;

    @Transient
    private List<Player> players = new ArrayList<>();
    @Transient
    private List<ChatMessage> chatHistory = new ArrayList<>();

//    private PokerGame game;



    public GameRoom(String roomName) {
        this.roomName = roomName;
    }

}


