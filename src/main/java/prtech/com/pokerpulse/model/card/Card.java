package prtech.com.pokerpulse.model.card;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Card {

    private Rank rank;
    private Suit suit;

    @Override
    public String toString() {
        return  "((rank=" + rank.getSymbol() +
                " suit=" + suit.getSymbol()+"))";
    }
}
