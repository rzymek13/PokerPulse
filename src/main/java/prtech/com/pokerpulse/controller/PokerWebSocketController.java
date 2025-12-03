package prtech.com.pokerpulse.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import prtech.com.pokerpulse.model.room.GameRoom;
import prtech.com.pokerpulse.model.chat.ChatMessage;
import prtech.com.pokerpulse.service.GameService;

@Controller
@Slf4j
@CrossOrigin
public class PokerWebSocketController {


    private final GameService gameService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public PokerWebSocketController(GameService gameService, ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Jedyny kanał WebSocket dla aktualizacji pokoju.
     * Klient wysyła obiekt GameRoom na /app/room/{roomId} (np. zmiana czatu),
     * serwer scali zmiany i rozsyła zaktualizowany, "sanitize'owany" obiekt na /topic/room/{roomId}.
     */
    @MessageMapping("/room/{roomId}")
    public void handleRoomUpdate(@DestinationVariable Long roomId, @Payload JsonNode incoming) {
        GameRoom room = gameService.getRooms().get(roomId);
        if (room == null) {
            log.warn("Próba aktualizacji nieistniejącego pokoju: {}", roomId);
            return;
        }

        // If the client sent an action (e.g. startGame or playerAction) handle it specially
        if (incoming.has("action")) {
            String action = incoming.get("action").asText();
            if ("startGame".equals(action)) {
                log.info("Received startGame action for room {}", roomId);
                GameRoom started = gameService.startGame(roomId);

                // send private hands to each player via dedicated topic
                if (started.getHands() != null && !started.getHands().isEmpty()) {
                    for (var player : started.getPlayers()) {
                        try {
                            Long pid = player.getPlayerId();
                            if (pid == null) continue;
                            var privateCards = gameService.dealPrivateCards(roomId, pid);
                            log.info("Sending private cards to player {} in room {}: {}", pid, roomId, privateCards);
                            messagingTemplate.convertAndSend("/topic/privateCards/" + pid, privateCards);
                        } catch (Exception ex) {
                            log.error("Failed to send private cards to player {} in room {}", player.getPlayerId(), roomId, ex);
                        }
                    }
                }

                // Broadcast sanitized room
                ObjectNode startedNode = objectMapper.valueToTree(started);
                if (startedNode.has("hands") && startedNode.get("hands").isArray()) {
                    ArrayNode hands = (ArrayNode) startedNode.get("hands");
                    for (JsonNode handNode : hands) {
                        if (handNode.isObject()) {
                            ((ObjectNode) handNode).remove("playerHands");
                        }
                    }
                }
                messagingTemplate.convertAndSend("/topic/room/" + roomId, startedNode);
                log.info("Broadcasted sanitized started room {} to /topic/room/{}", roomId, roomId);
                return;
            }

            if ("playerAction".equals(action)) {
                try {
                    Long actingPlayerId = incoming.has("playerId") ? incoming.get("playerId").asLong() : null;
                    String decision = incoming.has("decision") ? incoming.get("decision").asText() : null;
                    Integer amount = incoming.has("amount") ? incoming.get("amount").asInt() : null;
                    log.info("Player action received in room {}: player={}, decision={}, amount={}", roomId, actingPlayerId, decision, amount);

                    // Delegate action processing to GameService
                    GameRoom updated = gameService.processPlayerAction(roomId, actingPlayerId, decision, amount);

                    // Broadcast sanitized room after action
                    ObjectNode updatedNode = objectMapper.valueToTree(updated);
                    if (updatedNode.has("hands") && updatedNode.get("hands").isArray()) {
                        ArrayNode hands = (ArrayNode) updatedNode.get("hands");
                        for (JsonNode handNode : hands) {
                            if (handNode.isObject()) {
                                ((ObjectNode) handNode).remove("playerHands");
                            }
                        }
                    }
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, updatedNode);
                    log.info("Broadcasted playerAction result for room {}", roomId);
                    return;
                } catch (Exception ex) {
                    log.error("Error handling playerAction", ex);
                }
            }
        }

        // Merge chatHistory from incoming JSON if present
        if (incoming.has("chatHistory") && incoming.get("chatHistory").isArray()) {
            try {
                var chatMessages = objectMapper.convertValue(incoming.get("chatHistory"), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<prtech.com.pokerpulse.model.chat.ChatMessage>>() {});
                if (chatMessages != null && !chatMessages.isEmpty()) {
                    room.getChatHistory().addAll(chatMessages);
                    log.info("Dodano {} wiadomości do pokoju {}", chatMessages.size(), roomId);
                }
            } catch (Exception ex) {
                log.warn("Failed to merge chatHistory from incoming JSON for room {}", roomId, ex);
            }
        }

        log.info("Broadcast pokoju {} - przed sanitize: {}", roomId, room);

        ObjectNode node = objectMapper.valueToTree(room);
        if (node.has("hands") && node.get("hands").isArray()) {
            ArrayNode hands = (ArrayNode) node.get("hands");
            for (JsonNode handNode : hands) {
                if (handNode.isObject()) {
                    ((ObjectNode) handNode).remove("playerHands");
                }
            }
        }

        messagingTemplate.convertAndSend("/topic/room/" + roomId, node);
        log.info("Wysłano zaktualizowany, zsanitize'owany pokój {} do /topic/room/{}", roomId, roomId);
    }
    }


