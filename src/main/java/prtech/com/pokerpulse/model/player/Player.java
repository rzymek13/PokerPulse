package prtech.com.pokerpulse.model.player;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@RequiredArgsConstructor
@Table("players")
public class Player {

    @Id @Column("player_id") private Long playerId;
    @Column("username") private String username;
    @Column("password") private String password;
//    private boolean ready = false;
//    private List<Card> hand = new ArrayList<>();

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
