package prtech.com.pokerpulse.model.pan;

import jakarta.validation.constraints.NotNull;

public record PanChallengeRequest(@NotNull Long playerId) {
}
