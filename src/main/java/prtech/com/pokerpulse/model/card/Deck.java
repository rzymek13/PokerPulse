package prtech.com.pokerpulse.model.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> shuffledDeck = new ArrayList<>();

    public List<Card> initializeShuffledDeck() {
        for (Rank r : Rank.values()) {
            for (Suit s : Suit.values()) {
                shuffledDeck.add(new Card(r,s));
                Collections.shuffle(shuffledDeck);
            }
        }
        return shuffledDeck;
    }

    @Override
    public String toString() {
        return "Deck{" +
                "shuffledDeck=" + shuffledDeck +
                '}';
    }
}
