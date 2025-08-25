package prtech.com.pokerpulse.model.player;


import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import prtech.com.pokerpulse.model.card.Card;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
@Table("player")
public class Player {

    @Id
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