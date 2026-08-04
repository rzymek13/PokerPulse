package prtech.com.pokerpulse.model.player;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Player {
    private Long playerId;
    private String username;
    private String password;
//    private boolean ready = false;
//    private List<Card> hand = new ArrayList<>();

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
