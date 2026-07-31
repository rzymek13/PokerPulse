package prtech.com.pokerpulse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import prtech.com.pokerpulse.model.card.Card;
import prtech.com.pokerpulse.model.card.Rank;
import prtech.com.pokerpulse.model.card.Suit;
import prtech.com.pokerpulse.model.pan.PanGameSnapshot;
import prtech.com.pokerpulse.model.pan.PanGameState;
import prtech.com.pokerpulse.model.pan.PanPhase;
import prtech.com.pokerpulse.model.player.Player;
import prtech.com.pokerpulse.model.room.GameRoom;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PanGameServiceTest {

    @Mock
    private GameService gameService;

    @Test
    void startGameDealsTwentyFourCardsAndChoosesNineHeartsOwner() {
        GameRoom room = roomWithPlayers(1L, "room-1",
                new Player("alice", "x"),
                new Player("bob", "y"),
                new Player("carol", "z"));
        Map<Long, Player> playersById = room.getPlayers().stream().collect(Collectors.toMap(Player::getPlayerId, p -> p));

        when(gameService.getRoomById(1L)).thenReturn(room);
        when(gameService.getPlayerById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> playersById.get(invocation.getArgument(0)));

        PanGameService service = new PanGameService(gameService);
        PanGameSnapshot snapshot = service.startGame(1L);

        assertEquals(PanPhase.IN_PROGRESS, snapshot.phase());
        assertEquals(24, snapshot.players().stream().mapToInt(p -> p.handSize()).sum());
        assertTrue(snapshot.players().stream().anyMatch(p -> p.currentTurn()));

        Long currentPlayerId = snapshot.currentPlayerId();
        assertNotNull(currentPlayerId);
        List<Card> currentHand = service.getPrivateHand(1L, currentPlayerId);
        assertTrue(currentHand.stream().anyMatch(card -> card.getRank() == Rank.NINE && card.getSuit() == Suit.HEARTS));
    }

    @Test
    void playerCanPlayCardsAndCollectFromPile() throws Exception {
        GameRoom room = roomWithPlayers(2L, "room-2",
                new Player("alice", "x"),
                new Player("bob", "y"),
                new Player("carol", "z"));
        Map<Long, Player> playersById = room.getPlayers().stream().collect(Collectors.toMap(Player::getPlayerId, p -> p));

        when(gameService.getRoomById(2L)).thenReturn(room);
        when(gameService.getPlayerById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> playersById.get(invocation.getArgument(0)));

        PanGameService service = new PanGameService(gameService);
        PanGameSnapshot started = service.startGame(2L);
        Long starterId = started.currentPlayerId();
        assertNotNull(starterId);

        List<Card> starterHand = service.getPrivateHand(2L, starterId);
        String nineHearts = starterHand.stream()
                .filter(card -> card.getRank() == Rank.NINE && card.getSuit() == Suit.HEARTS)
                .findFirst()
                .orElseThrow()
                .getCode();

        service.playCards(2L, starterId, List.of(nineHearts), Rank.NINE);
        Long blufferId = service.getSnapshot(2L).currentPlayerId();
        assertNotEquals(starterId, blufferId);

        PanGameState state = state(service, 2L);
        boolean injectedNine = ensurePlayerHasBluffNine(state, blufferId);

        String bluffCard = state.getHands().get(blufferId).stream()
                .filter(card -> card.getRank() == Rank.NINE && card.getSuit() != Suit.SPADES)
                .findFirst()
                .orElseThrow()
                .getCode();

        service.playCards(2L, blufferId, List.of(bluffCard), Rank.QUEEN);
        Long collecterId = service.getSnapshot(2L).currentPlayerId();
        assertNotEquals(blufferId, collecterId);

        PanGameSnapshot afterCollect = service.collectCards(2L, collecterId);
        assertNotEquals(collecterId, afterCollect.currentPlayerId());
        assertEquals(1, afterCollect.pileSize());
        assertEquals(Rank.NINE, afterCollect.requiredRank());
    }

    @Test
    void cannotPlayLowerThanTableRank() throws Exception {
        GameRoom room = roomWithPlayers(3L, "room-3",
                new Player("alice", "x"),
                new Player("bob", "y"),
                new Player("carol", "z"));
        Map<Long, Player> playersById = room.getPlayers().stream().collect(Collectors.toMap(Player::getPlayerId, p -> p));

        when(gameService.getRoomById(3L)).thenReturn(room);
        when(gameService.getPlayerById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> playersById.get(invocation.getArgument(0)));

        PanGameService service = new PanGameService(gameService);
        service.startGame(3L);

        PanGameState state = state(service, 3L);
        state.setRequiredRank(Rank.QUEEN);
        state.setOpeningMovePending(false);

        Long playerId = state.getHands().entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(card -> card.getRank().panOrder() < Rank.QUEEN.panOrder()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
        state.setCurrentPlayerId(playerId);

        Card lowCard = state.getHands().get(playerId).stream()
                .filter(card -> card.getRank().panOrder() < Rank.QUEEN.panOrder())
                .findFirst()
                .orElseThrow();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.playCards(3L, playerId, List.of(lowCard.getCode()), Rank.QUEEN)
        );
        assertTrue(ex.getMessage().contains("lower than the card on the table"));
    }

    private GameRoom roomWithPlayers(Long roomId, String roomName, Player... players) {
        GameRoom room = new GameRoom(roomName);
        room.setGameRoomId(roomId);
        long nextPlayerId = 1L;
        for (Player player : players) {
            player.setPlayerId(nextPlayerId++);
        }
        room.setPlayers(new java.util.ArrayList<>(List.of(players)));
        return room;
    }

    @SuppressWarnings("unchecked")
    private PanGameState state(PanGameService service, Long roomId) throws Exception {
        Field statesField = PanGameService.class.getDeclaredField("states");
        statesField.setAccessible(true);
        Map<Long, PanGameState> states = (Map<Long, PanGameState>) statesField.get(service);
        return states.get(roomId);
    }

    private boolean ensurePlayerHasBluffNine(PanGameState state, Long playerId) {
        boolean hasNonSpadeNine = state.getHands().get(playerId).stream()
                .anyMatch(card -> card.getRank() == Rank.NINE && card.getSuit() != Suit.SPADES);
        if (hasNonSpadeNine) {
            return false;
        }

        Map.Entry<Long, List<Card>> donorEntry = state.getHands().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(playerId))
                .filter(entry -> entry.getValue().stream().anyMatch(card -> card.getRank() == Rank.NINE && card.getSuit() != Suit.SPADES))
                .findFirst()
                .orElseThrow();

        Card nine = donorEntry.getValue().stream()
                .filter(card -> card.getRank() == Rank.NINE && card.getSuit() != Suit.SPADES)
                .findFirst()
                .orElseThrow();

        donorEntry.getValue().remove(nine);
        state.getHands().get(playerId).add(nine);
        return true;
    }
}
