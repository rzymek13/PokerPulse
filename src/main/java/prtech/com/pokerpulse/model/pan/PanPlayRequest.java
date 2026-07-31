package prtech.com.pokerpulse.model.pan;

import jakarta.validation.constraints.NotNull;
import prtech.com.pokerpulse.model.card.Rank;

import java.util.List;

public record PanPlayRequest(
        @NotNull Long playerId,
        @NotNull List<String> cardCodes,
        Rank declaredRank
) {
}
