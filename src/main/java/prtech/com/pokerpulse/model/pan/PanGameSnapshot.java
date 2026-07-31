package prtech.com.pokerpulse.model.pan;

import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.card.Rank;

import java.util.List;

public record PanGameSnapshot(
        Long roomId,
        PanPhase phase,
        Long currentPlayerId,
        String currentPlayerUsername,
        Rank requiredRank,
        int pileSize,
        Long winnerPlayerId,
        String winnerUsername,
        String message,
        PanPlayView lastPlay,
        List<Card> tableCards,
        List<PanPlayerView> players
) {
}
