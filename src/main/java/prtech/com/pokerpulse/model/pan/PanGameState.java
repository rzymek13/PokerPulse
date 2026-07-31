package prtech.com.pokerpulse.model.pan;

import lombok.Data;
import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.card.Rank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class PanGameState {
    private final Long roomId;
    private PanPhase phase = PanPhase.WAITING;
    private List<Long> turnOrder = new ArrayList<>();
    private Map<Long, List<Card>> hands = new LinkedHashMap<>();
    private List<Card> pile = new ArrayList<>();
    private List<List<Card>> pileHistory = new ArrayList<>();
    private List<Card> lastPlayedCards = new ArrayList<>();
    private Rank requiredRank = Rank.NINE;
    private Long currentPlayerId;
    private Long winnerPlayerId;
    private PanPlayView lastPlay;
    private String lastMessage;
    private boolean openingMovePending = true;

    public PanGameState(Long roomId) {
        this.roomId = roomId;
    }
}
