package prtech.com.pokerpulse.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.pan.PanGameSnapshot;
import prtech.com.pokerpulse.model.pan.PanPhase;
import prtech.com.pokerpulse.model.pan.PanPlayRequest;
import prtech.com.pokerpulse.model.pan.PanChallengeRequest;
import prtech.com.pokerpulse.model.pan.PanDrawRequest;
import prtech.com.pokerpulse.service.GameService;
import prtech.com.pokerpulse.service.PanGameService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class PanGameWebSocketController {

    private final PanGameService panGameService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public PanGameWebSocketController(PanGameService panGameService, GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.panGameService = panGameService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/pan/{roomId}")
    public void handle(@DestinationVariable Long roomId, @Payload JsonNode incoming) {
        try {
            String action = incoming.has("action") ? incoming.get("action").asText() : "";
            PanGameSnapshot snapshot;
            switch (action) {
                case "startGame" -> snapshot = panGameService.startGame(roomId);
                case "playCards" -> {
                    List<String> cardCodes = new ArrayList<>();
                    if (incoming.has("cardCodes") && incoming.get("cardCodes").isArray()) {
                        incoming.get("cardCodes").forEach(node -> cardCodes.add(node.asText()));
                    }
                    PanPlayRequest request = new PanPlayRequest(
                            incoming.get("playerId").asLong(),
                            cardCodes,
                            incoming.hasNonNull("declaredRank")
                                    ? prtech.com.pokerpulse.model.card.Rank.valueOf(incoming.get("declaredRank").asText().toUpperCase())
                                    : null
                    );
                    snapshot = panGameService.playCards(roomId, request.playerId(), request.cardCodes(), request.declaredRank());
                }
                case "challenge" -> {
                    PanChallengeRequest request = new PanChallengeRequest(incoming.get("playerId").asLong());
                    snapshot = panGameService.collectCards(roomId, request.playerId());
                }
                case "drawCards" -> {
                    PanDrawRequest request = new PanDrawRequest(incoming.get("playerId").asLong());
                    snapshot = panGameService.drawCards(roomId, request.playerId());
                }
                default -> {
                    log.warn("Unknown Pan action for room {}: {}", roomId, action);
                    return;
                }
            }
            broadcast(roomId, snapshot);
        } catch (Exception ex) {
            log.error("Pan WS error in room {}: {}", roomId, ex.getMessage(), ex);
            messagingTemplate.convertAndSend("/topic/pan/" + roomId + "/errors", ex.getMessage());
        }
    }

    private void broadcast(Long roomId, PanGameSnapshot snapshot) {
        messagingTemplate.convertAndSend("/topic/pan/" + roomId, snapshot);
        broadcastPrivateHands(roomId);
        if (snapshot.phase() == PanPhase.FINISHED) {
            messagingTemplate.convertAndSend("/topic/pan/" + roomId + "/finished", snapshot);
        }
    }

    private void broadcastPrivateHands(Long roomId) {
        Map<Long, List<Card>> hands = panGameService.getPrivateHands(roomId);
        hands.forEach((playerId, hand) -> messagingTemplate.convertAndSend("/topic/pan/private/" + playerId, hand));
    }
}
