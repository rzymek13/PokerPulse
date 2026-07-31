package prtech.com.pokerpulse.model.card;

import java.util.List;

public enum Rank {
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("T"),
    JACK("J"),
    QUEEN("Q"),
    KING("K"),
    ACE("A");

    private final String symbol;

    Rank(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isPanRank() {
        return switch (this) {
            case NINE, TEN, JACK, QUEEN, KING, ACE -> true;
            default -> false;
        };
    }

    public int panOrder() {
        return switch (this) {
            case NINE -> 0;
            case TEN -> 1;
            case JACK -> 2;
            case QUEEN -> 3;
            case KING -> 4;
            case ACE -> 5;
            default -> -1;
        };
    }

    public static List<Rank> panDeckRanks() {
        return List.of(NINE, TEN, JACK, QUEEN, KING, ACE);
    }
}
