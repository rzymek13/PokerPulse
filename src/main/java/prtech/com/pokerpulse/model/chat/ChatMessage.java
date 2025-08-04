package prtech.com.pokerpulse.model.chat;

import lombok.Data;
import prtech.com.pokerpulse.model.player.Player;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Player sender;
    private String content;
    private String timestamp = LocalDateTime.now().toString();
}
