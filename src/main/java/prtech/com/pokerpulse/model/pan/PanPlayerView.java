package prtech.com.pokerpulse.model.pan;

public record PanPlayerView(
        Long playerId,
        String username,
        int handSize,
        boolean currentTurn
) {
}
