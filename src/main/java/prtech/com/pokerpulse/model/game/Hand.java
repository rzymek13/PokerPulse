package prtech.com.pokerpulse.model.game;

import lombok.Data;
import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.card.Deck;
import prtech.com.pokerpulse.model.player.Player;

import java.util.List;

@Data
public class Hand {
    private String id;
    private List<Player> players;
    private Deck deck = new Deck();
    private List<Card> communityCards = deck.initializeShuffledDeck().subList(0, 5);



}
