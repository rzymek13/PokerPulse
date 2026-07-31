package prtech.com.pokerpulse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.card.Rank;
import prtech.com.pokerpulse.model.card.Suit;
import prtech.com.pokerpulse.model.pan.*;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PanGameService {

    private final GameService gameService;
    private final Map<Long, PanGameState> states = new ConcurrentHashMap<>();

    public PanGameService(GameService gameService) {
        this.gameService = gameService;
    }

    public PanGameState getStateOrThrow(Long roomId) {
        PanGameState state = states.get(roomId);
        if (state == null) {
            throw new IllegalArgumentException("No Pan game initialized for room " + roomId);
        }
        return state;
    }

    public PanGameSnapshot getSnapshot(Long roomId) {
        GameRoom room = resolveRoom(roomId);
        PanGameState state = states.get(roomId);
        if (state == null) {
            state = new PanGameState(roomId);
            state.setPhase(PanPhase.WAITING);
            state.setRequiredRank(Rank.NINE);
            state.setLastMessage("Game not started yet");
        }
        return snapshot(state, room);
    }

    public Map<Long, List<Card>> getPrivateHands(Long roomId) {
        PanGameState state = states.get(roomId);
        if (state == null) {
            return Map.of();
        }
        Map<Long, List<Card>> result = new LinkedHashMap<>();
        state.getHands().forEach((playerId, hand) -> result.put(playerId, List.copyOf(hand)));
        return result;
    }

    public List<Card> getPrivateHand(Long roomId, Long playerId) {
        PanGameState state = states.get(roomId);
        if (state == null) {
            return List.of();
        }
        List<Card> hand = state.getHands().get(playerId);
        if (hand == null) {
            throw new IllegalArgumentException("Player " + playerId + " is not part of room " + roomId);
        }
        return List.copyOf(hand);
    }

    public synchronized PanGameSnapshot startGame(Long roomId) {
        GameRoom room = resolveRoom(roomId);
        List<Player> players = List.copyOf(room.getPlayers());
        if (players.size() < 2 || players.size() > 4) {
            throw new IllegalArgumentException("Pan requires 2-4 players");
        }

        PanGameState state = new PanGameState(roomId);
        state.setPhase(PanPhase.IN_PROGRESS);
        state.setTurnOrder(players.stream().map(Player::getPlayerId).collect(Collectors.toCollection(ArrayList::new)));

        Map<Long, List<Card>> hands = new LinkedHashMap<>();
        players.forEach(player -> hands.put(player.getPlayerId(), new ArrayList<>()));
        state.setHands(hands);

        List<Card> deck = createPanDeck();
        Collections.shuffle(deck);
        for (int i = 0; i < deck.size(); i++) {
            Player recipient = players.get(i % players.size());
            state.getHands().get(recipient.getPlayerId()).add(deck.get(i));
        }

        Long starterId = findPlayerWithCard(state, Rank.NINE, Suit.HEARTS);
        if (starterId == null) {
            starterId = players.get(0).getPlayerId();
        }
        state.setCurrentPlayerId(starterId);
        state.setRequiredRank(Rank.NINE);
        state.setPile(new ArrayList<>());
        state.setPileHistory(new ArrayList<>());
        state.getPileHistory().add(new ArrayList<>(state.getPile()));
        state.setWinnerPlayerId(null);
        state.setLastPlay(null);
        state.setLastMessage("Game started");
        state.setOpeningMovePending(true);

        states.put(roomId, state);
        log.info("Pan game started in room {} with {} players", roomId, players.size());
        return snapshot(state, room);
    }

    public synchronized PanGameSnapshot playCards(Long roomId, Long playerId, List<String> cardCodes, Rank declaredRank) {
        PanGameState state = getStateOrThrow(roomId);
        validateActiveState(state);
        validateCurrentPlayer(state, playerId);
        if (cardCodes == null || cardCodes.isEmpty() || cardCodes.size() > 4) {
            throw new IllegalArgumentException("You must play between 1 and 4 cards");
        }

        List<Card> hand = state.getHands().get(playerId);
        if (hand == null) {
            throw new IllegalArgumentException("Player " + playerId + " is not part of room " + roomId);
        }

        List<Card> selected = validatePlay(state, hand, cardCodes, declaredRank);
        Rank actualRank = selected.get(0).getRank();
        hand.removeAll(selected);
        Player actor = gameService.getPlayerById(playerId);
        recordPlay(state, actor, selected, declaredRank, actualRank);

        if (hand.isEmpty()) {
            state.setPhase(PanPhase.FINISHED);
            state.setWinnerPlayerId(playerId);
            state.setCurrentPlayerId(playerId);
            state.setLastMessage(actor.getUsername() + " won the game");
            log.info("Pan game in room {} finished. Winner: {}", roomId, actor.getUsername());
            return snapshot(state, resolveRoom(roomId));
        }

        state.setCurrentPlayerId(nextPlayerWithCards(state, playerId));
        state.setLastMessage(actor.getUsername() + " played " + selected.size() + " card(s) as " + (declaredRank != null ? declaredRank : actualRank)
                + ". Next player: " + getUsernameOrFallback(roomId, state.getCurrentPlayerId()));
        return snapshot(state, resolveRoom(roomId));
    }

    public synchronized PanGameSnapshot drawCards(Long roomId, Long playerId) {
        PanGameState state = getStateOrThrow(roomId);
        validateActiveState(state);
        validateCurrentPlayer(state, playerId);

        List<Card> hand = state.getHands().get(playerId);
        if (hand == null) {
            throw new IllegalArgumentException("Player " + playerId + " is not part of room " + roomId);
        }

        int available = Math.max(0, state.getPile().size() - 1);
        int drawCount = Math.min(3, available);
        if (drawCount > 0) {
            List<Card> drawn = new ArrayList<>(state.getPile().subList(1, 1 + drawCount));
            hand.addAll(drawn);
            state.getPile().subList(1, 1 + drawCount).clear();
            state.getPileHistory().add(new ArrayList<>(state.getPile()));
            state.setLastMessage(gameService.getPlayerById(playerId).getUsername() + " drew " + drawCount + " card(s)");
        } else {
            state.setLastMessage(gameService.getPlayerById(playerId).getUsername() + " could not draw cards from the pile");
        }

        state.setRequiredRank(currentTableRank(state));
        state.setCurrentPlayerId(nextPlayerWithCards(state, playerId));
        return snapshot(state, resolveRoom(roomId));
    }

    public synchronized PanGameSnapshot collectCards(Long roomId, Long playerId) {
        PanGameState state = getStateOrThrow(roomId);
        validateActiveState(state);
        validateCurrentPlayer(state, playerId);
        if (state.getPile().size() < 2) {
            throw new IllegalArgumentException("Not enough cards on the table to collect");
        }

        Player actor = gameService.getPlayerById(playerId);
        List<Card> hand = state.getHands().get(playerId);
         
        // Keep the first card (9♥) on the pile, collect up to 3 from the top (end of list)
        int collectCount = Math.min(3, state.getPile().size() - 1);
        List<Card> cardsToCollect = new ArrayList<>(
                state.getPile().subList(state.getPile().size() - collectCount, state.getPile().size())
        );
         
        log.debug("Collecting {} cards from top of pile. Pile size before: {}. Cards to collect: {}", 
                collectCount, state.getPile().size(), cardsToCollect.stream().map(Card::getCode).toList());
         
        hand.addAll(cardsToCollect);
         
        // Remove collected cards from top of pile
        for (int i = 0; i < collectCount; i++) {
            state.getPile().remove(state.getPile().size() - 1);
        }
         
        log.debug("Pile after collection: {}. Pile size after: {}", 
                state.getPile().stream().map(Card::getCode).toList(), state.getPile().size());
        
        state.getPileHistory().add(new ArrayList<>(state.getPile()));
        state.setLastPlayedCards(new ArrayList<>());
        state.setLastPlay(null);
        state.setRequiredRank(Rank.NINE);
        state.setOpeningMovePending(false);
        state.setLastMessage(actor.getUsername() + " collected " + collectCount + " card(s) from the table");

        log.info("Player {} collected {} cards from pile in room {}", playerId, collectCount, roomId);

        state.setCurrentPlayerId(nextPlayerWithCards(state, playerId));
        state.setLastMessage(actor.getUsername() + " collected " + collectCount + " card(s) from the table. Next player: " + getUsernameOrFallback(roomId, state.getCurrentPlayerId()));
        return snapshot(state, resolveRoom(roomId));
    }

    public boolean isActive(Long roomId) {
        PanGameState state = states.get(roomId);
        return state != null && state.getPhase() == PanPhase.IN_PROGRESS;
    }

    private void validateActiveState(PanGameState state) {
        if (state.getPhase() != PanPhase.IN_PROGRESS) {
            throw new IllegalArgumentException("Game is not in progress");
        }
    }

    private GameRoom resolveRoom(Long roomId) {
        GameRoom room = gameService.getRooms().get(roomId);
        if (room != null) {
            return room;
        }
        return gameService.getRoomById(roomId);
    }

    private void validateCurrentPlayer(PanGameState state, Long playerId) {
        if (!Objects.equals(state.getCurrentPlayerId(), playerId)) {
            throw new IllegalArgumentException("It is not player " + playerId + "'s turn");
        }
    }

    private List<Card> validatePlay(PanGameState state, List<Card> hand, List<String> cardCodes, Rank declaredRank) {
        List<Card> selected = extractCards(hand, cardCodes);
        Rank actualRank = selected.get(0).getRank();
        if (selected.stream().anyMatch(card -> card.getRank() != actualRank)) {
            throw new IllegalArgumentException("You can only play cards of the same rank");
        }
        if (declaredRank != null && !declaredRank.isPanRank()) {
            throw new IllegalArgumentException("Declared rank must be between 9 and Ace");
        }
        if (actualRank.panOrder() < state.getRequiredRank().panOrder()) {
            throw new IllegalArgumentException("You cannot play lower than the card on the table");
        }
        validateOpeningMove(state, selected, declaredRank);
        return selected;
    }

    private void validateOpeningMove(PanGameState state, List<Card> selected, Rank declaredRank) {
        if (!state.isOpeningMovePending()) return;
        boolean isNineOfHearts = selected.size() == 1
                && selected.get(0).getRank() == Rank.NINE
                && selected.get(0).getSuit() == Suit.HEARTS;
        if (!isNineOfHearts) {
            throw new IllegalArgumentException("Opening move must be a single 9 hearts");
        }
        if (declaredRank != null && declaredRank != Rank.NINE) {
            throw new IllegalArgumentException("First move must be declared as NINE");
        }
    }

    private void recordPlay(PanGameState state, Player actor, List<Card> selected, Rank declaredRank, Rank actualRank) {
        Rank announcedRank = declaredRank != null ? declaredRank : actualRank;
        state.getPile().addAll(selected);
        state.getPileHistory().add(new ArrayList<>(state.getPile()));
        state.setLastPlayedCards(new ArrayList<>(selected));
        state.setLastPlay(new PanPlayView(actor.getPlayerId(), actor.getUsername(), announcedRank, actualRank, selected.size()));
        state.setLastMessage(actor.getUsername() + " played " + selected.size() + " card(s) as " + announcedRank);
        state.setRequiredRank(currentTableRank(state));
        state.setOpeningMovePending(false);
    }

    private List<Card> createPanDeck() {
        List<Card> deck = new ArrayList<>();
        for (Rank rank : Rank.panDeckRanks()) {
            for (Suit suit : Suit.values()) {
                deck.add(new Card(rank, suit));
            }
        }
        return deck;
    }

    private Long findPlayerWithCard(PanGameState state, Rank rank, Suit suit) {
        return state.getHands().entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(card -> card.getRank() == rank && card.getSuit() == suit))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private List<Card> extractCards(List<Card> hand, List<String> cardCodes) {
        List<Card> selected = new ArrayList<>();
        for (String code : cardCodes) {
            Card card = hand.stream()
                    .filter(c -> c.getCode().equalsIgnoreCase(code))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Card " + code + " is not in your hand"));
            if (selected.contains(card)) {
                throw new IllegalArgumentException("Duplicate card " + code);
            }
            selected.add(card);
        }
        return selected;
    }

    private Long nextPlayerWithCards(PanGameState state, Long currentPlayerId) {
        if (state.getTurnOrder().isEmpty()) {
            throw new IllegalStateException("No turn order configured");
        }
        int currentIndex = state.getTurnOrder().indexOf(currentPlayerId);
        if (currentIndex < 0) {
            throw new IllegalArgumentException("Current player is not in turn order");
        }

        for (int step = 1; step <= state.getTurnOrder().size(); step++) {
            Long candidate = state.getTurnOrder().get((currentIndex + step) % state.getTurnOrder().size());
            List<Card> candidateHand = state.getHands().get(candidate);
            if (candidateHand != null && !candidateHand.isEmpty()) {
                return candidate;
            }
        }

        return currentPlayerId;
    }

    private Rank currentTableRank(PanGameState state) {
        if (state.getPile().isEmpty()) {
            return Rank.NINE;
        }
        return state.getPile().get(state.getPile().size() - 1).getRank();
    }

    private String getUsernameOrFallback(Long roomId, Long playerId) {
        try {
            return gameService.getPlayerById(playerId).getUsername();
        } catch (Exception ex) {
            return "player-" + playerId + "@room-" + roomId;
        }
    }

    private PanGameSnapshot snapshot(PanGameState state, GameRoom room) {
        List<PanPlayerView> players = room.getPlayers().stream()
                .map(player -> new PanPlayerView(
                        player.getPlayerId(),
                        player.getUsername(),
                        state.getHands().getOrDefault(player.getPlayerId(), List.of()).size(),
                        Objects.equals(state.getCurrentPlayerId(), player.getPlayerId())
                ))
                .toList();

        String currentUsername = state.getCurrentPlayerId() == null ? null : getUsernameOrFallback(room.getGameRoomId(), state.getCurrentPlayerId());
        String winnerUsername = state.getWinnerPlayerId() == null ? null : getUsernameOrFallback(room.getGameRoomId(), state.getWinnerPlayerId());

        return new PanGameSnapshot(
                room.getGameRoomId(),
                state.getPhase(),
                state.getCurrentPlayerId(),
                currentUsername,
                state.getRequiredRank(),
                state.getPile().size(),
                state.getWinnerPlayerId(),
                winnerUsername,
                state.getLastMessage(),
                state.getLastPlay(),
                state.getPile().stream().skip(Math.max(0, state.getPile().size() - 3)).toList(),
                players
        );
    }
}
