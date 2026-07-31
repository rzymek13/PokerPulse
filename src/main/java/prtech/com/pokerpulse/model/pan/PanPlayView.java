package prtech.com.pokerpulse.model.pan;

import prtech.com.pokerpulse.model.card.Rank;

public record PanPlayView(
        Long playerId,
        String username,
        Rank declaredRank,
        Rank actualRank,
        int cardCount
) {
}
