package prtech.com.pokerpulse.model.game;

import lombok.Data;
import prtech.com.pokerpulse.model.player.Player;

@Data
public class Action {
    private Player player;
    private Decision decision;
    private int amount;
}
