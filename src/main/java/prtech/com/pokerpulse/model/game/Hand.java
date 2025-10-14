package prtech.com.pokerpulse.model.game;

import lombok.Data;
import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.card.Rank;
import prtech.com.pokerpulse.model.card.Suit;
import prtech.com.pokerpulse.model.player.Player;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class Hand {
    private String id;
    private List<Card> deck = initializeShuffledDeck();
    private List<Card> communityCards = new ArrayList<>();
    private Map<Player, List<Card>> playerHands = new HashMap<>();
    private int pot = 0;
    private String stage = "PREFLOP";
    private Player currentPlayer;
    private int smallBlind = 10;
    private int bigBlind = 20;

    private Map<Stage, List<Action>> actionsByStage = new HashMap<>() {{
        put(Stage.PREFLOP, new ArrayList<>());
        put(Stage.FLOP, new ArrayList<>());
        put(Stage.TURN, new ArrayList<>());
        put(Stage.RIVER, new ArrayList<>());
    }};


    public Hand(List<Player> players) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("At least two players are required to start a hand.");
        }
        this.id = UUID.randomUUID().toString();
        this.communityCards.addAll(deck.subList(0, 5));
        deck.subList(0, 5).clear();
        this.playerHands = players.stream()
                .collect(Collectors.toMap(
                        player -> player,
                        player -> {
                            List<Card> hand = new ArrayList<>();
                            hand.add(deck.remove(0));
                            hand.add(deck.remove(0));
                            return hand;
                        }
                ));
    }


    private List<Card> initializeShuffledDeck() {
        ArrayList<Card> shuffledDeck = new ArrayList<>();
        for (Rank r : Rank.values()) {
            for (Suit s : Suit.values()) {
                shuffledDeck.add(new Card(r, s));
                Collections.shuffle(shuffledDeck);
            }
        }
        return shuffledDeck;
    }

    @Override
    public String toString() {
        return "Hand{" +
                "\n" + "id='" + id + '\'' +
                "\n" + ", communityCards=" + communityCards +
                "\n" + ", playerHands=" + playerHands +
                "\n" + ", pot=" + pot +
                "\n" + ", stage='" + stage + '\'' +
                "\n" + ", currentPlayer=" + currentPlayer +
                "\n" + ", smallBlind=" + smallBlind +
                "\n" + ", bigBlind=" + bigBlind +
                '}';
    }
}
