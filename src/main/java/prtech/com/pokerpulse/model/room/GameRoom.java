package prtech.com.pokerpulse.model.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import prtech.com.pokerpulse.model.game.Hand;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.chat.ChatMessage;

import java.util.ArrayList;
import java.util.List;



@Data
@RequiredArgsConstructor
public class GameRoom {
    private Long gameRoomId;
    private String roomName;

    private List<Player> players = new ArrayList<>();
    private List<ChatMessage> chatHistory = new ArrayList<>();
    private List<Hand> hands = new ArrayList<>();

//    private PokerGame game;



    public GameRoom(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return "GameRoom{" +
                "gameRoomId=" + gameRoomId +
                "\n" +  ", roomName='" + roomName + '\'' +
                "\n" +  ", players=" + players +
                "\n" + ", chatHistory=" + chatHistory +
                "\n" + ", hands=" + hands +
                '}';
    }
}


