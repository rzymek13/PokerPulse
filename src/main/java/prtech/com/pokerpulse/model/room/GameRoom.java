package prtech.com.pokerpulse.model.room;

import lombok.Data;
import prtech.com.pokerpulse.model.game.PokerGame;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import java.util.ArrayList;
import java.util.List;


@Data
public class GameRoom {


    private Integer roomId = (int) (Math.random() * 10000);
    private String roomName;
    private List<Player> players;
    private List<ChatMessage> chatHistory = new ArrayList<>();
    private PokerGame game;

    

}
