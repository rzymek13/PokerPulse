package prtech.com.pokerpulse.model.room;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import prtech.com.pokerpulse.model.game.PokerGame;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.chat.ChatMessage;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
@Table("game_room")
public class GameRoom {

    @Id
    private Long roomId;
    private String roomName;
    private List<Player> players = new ArrayList<>();
    private List<ChatMessage> chatHistory = new ArrayList<>();
//    private PokerGame game;
}
