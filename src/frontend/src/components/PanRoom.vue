<template>
  <div class="container pan-page">
    <header class="pan-header">
      <div class="brand-block">
        <p class="eyebrow">PokerPulse · {{ roomName || `Pokój ${roomId}` }}</p>
        <h1>Pan</h1>
        <p class="game-status" :class="{ 'game-status--active': isMyTurn }">{{ statusLine }}</p>
      </div>
      <div class="game-metrics" aria-label="Stan rozgrywki">
        <div class="metric"><span>Faza</span><strong>{{ phaseLabel }}</strong></div>
        <div class="metric"><span>Stos</span><strong>{{ gameState.pileSize }}</strong></div>
        <div class="connection-state" :class="connectionStatus"><i></i>{{ connectionStatus }}</div>
      </div>
      <PanPlayersList
        class="players-summary"
        :players="gamePlayers"
        :myPlayerId="myPlayerId"
        :currentPlayerUsername="gameState.currentPlayerUsername"
      />
    </header>

    <main class="game-layout">
      <section class="table-surface">
        <div class="table-surface__top">
          <div>
            <p class="eyebrow">Aktualna tura</p>
            <h2>{{ gameState.currentPlayerUsername || 'Oczekiwanie na graczy' }}</h2>
          </div>
        </div>

        <div class="table-stack">
        <div class="stack-area" v-if="tableCards.length">
          <div
            v-for="(card, index) in tableCards"
            :key="`${cardCode(card)}-${index}`"
            class="stack-card"
            :style="{ left: `${index * 22}px`, top: `${index * 2}px`, zIndex: index + 1 }"
          >
            <span class="stack-card__rank">{{ cardRankLabel(card) }}</span>
            <span class="stack-card__suit">{{ suitSymbol(card) }}</span>
            <span class="stack-card__code">{{ cardRankLabel(card) }}{{ suitSymbol(card) }}</span>
          </div>
        </div>
        <div v-else class="stack-empty">
          <p>Stos kart pojawi się tutaj.</p>
          <p class="subtitle">Brak kart na stole.</p>
        </div>

        <div v-if="gameState.lastPlay" class="play-summary">
          <strong>{{ gameState.lastPlay.username }}</strong>
          <span>
            zagrał {{ gameState.lastPlay.cardCount }} kart
          </span>
          <span class="label">Rzeczywisty rank stołu: {{ gameState.lastPlay.actualRank }}</span>
        </div>
        </div>

        <div class="table-message">
          <span class="table-message__label">Informacja z gry</span>
          <p>{{ gameState.message || 'Czekamy na pierwszy ruch.' }}</p>
          <p v-if="gameState.winnerUsername" class="winner-line">Zwycięzca: <strong>{{ gameState.winnerUsername }}</strong></p>
        </div>
      <section class="hand-panel table-hand">
      <div class="hand-header">
        <div>
          <h2>Twoje karty</h2>
          <p class="subtitle">Wybierz od 1 do 4 kart o tej samej randze.</p>
        </div>
        <div class="hand-stats">
          <div><span class="label">Na ręce</span><strong>{{ myHand.length }}</strong></div>
          <div><span class="label">Wybrane</span><strong>{{ selectedCodes.length }}/4</strong></div>
        </div>
      </div>

      <div class="selected-line">
        <span>{{ selectedCodes.length ? `Wybrane: ${selectedCodes.join(', ')}` : 'Wybierz karty, które chcesz zagrać.' }}</span>
        <div class="action-bar">
          <button class="btn btn-primary" @click="startGame" :disabled="!canStartGame || sending">Start gry</button>
          <button class="btn btn-success" @click="playSelectedCards" :disabled="!canPlay || sending">Zagraj{{ selectedCodes.length ? ` (${selectedCodes.length})` : '' }}</button>
          <button class="btn btn-warning" @click="collectCards" :disabled="!canCollect || sending">Sprawdź stos · {{ cardsToCollect }}</button>
        </div>
      </div>

      <div class="hand-stage">
        <div class="hand-cards" v-if="myHand.length">
          <button
            v-for="(card, index) in myHand"
            :key="card.code || cardCode(card)"
            type="button"
            class="card-tile"
            :class="[suitClass(card), { selected: selectedCodes.includes(card.code || cardCode(card)) }]"
            @click="toggleCard(card)"
            :style="{ '--card-index': index, zIndex: selectedCodes.includes(card.code || cardCode(card)) ? 100 : index }"
          >
            <span class="card-top">{{ cardRankLabel(card) }}</span>
            <span class="card-middle">{{ suitSymbol(card) }}</span>
            <span class="card-bottom">{{ cardSuitLabel(card) }}</span>
          </button>
        </div>
        <p v-else class="subtitle">Brak kart. Poczekaj na start gry albo sprawdź połączenie.</p>
      </div>
      </section>
      </section>
      <PanChat :room-id="roomId" :username="username" />
    </main>

    <div v-if="errorMessage" class="error-line" role="alert">{{ errorMessage }}</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import api from '../api';
import { SOCKJS_URL } from '../config';
import PanPlayersList from './PanPlayersList.vue';
import PanChat from './PanChat.vue';

const route = useRoute();
const router = useRouter();

const username = ref(sessionStorage.getItem('username') || '');
const roomId = computed(() => String(route.params.id || sessionStorage.getItem('roomId') || ''));

const roomName = ref('');
const myPlayerId = ref(null);
const roomPlayers = ref([]);
const myHand = ref([]);
const selectedCodes = ref([]);
const connectionStatus = ref('offline');
const errorMessage = ref('');
const sending = ref(false);

const stompClient = ref(null);
let roomSyncTimer = null;
const subs = {
  room: null,
  private: null,
  errors: null,
};

const rankLabels = {
  NINE: '9',
  TEN: '10',
  JACK: 'J',
  QUEEN: 'Q',
  KING: 'K',
  ACE: 'A',
  '9': '9',
  T: '10',
  J: 'J',
  Q: 'Q',
  K: 'K',
  A: 'A',
};
const suitSymbols = {
  HEARTS: '♥',
  DIAMONDS: '♦',
  CLUBS: '♣',
  SPADES: '♠',
  H: '♥',
  D: '♦',
  C: '♣',
  S: '♠',
  0: '♥',
  1: '♦',
  2: '♣',
  3: '♠',
};
const suitNames = {
  HEARTS: 'Kier',
  DIAMONDS: 'Karo',
  CLUBS: 'Trefl',
  SPADES: 'Pik',
  H: 'Kier',
  D: 'Karo',
  C: 'Trefl',
  S: 'Pik',
  0: 'Kier',
  1: 'Karo',
  2: 'Trefl',
  3: 'Pik',
};

const gameState = ref({
  phase: 'WAITING',
  currentPlayerId: null,
  currentPlayerUsername: null,
  requiredRank: 'NINE',
  pileSize: 0,
  winnerPlayerId: null,
  winnerUsername: null,
  message: '',
  lastPlay: null,
  tableCards: [],
  players: [],
});

const tableCards = computed(() => gameState.value.tableCards || []);
const gamePlayers = computed(() => {
  const players = (gameState.value.players && gameState.value.players.length > 0)
    ? gameState.value.players
    : roomPlayers.value;
  return players.map(player => ({
    ...player,
    currentTurn: player.playerId === gameState.value.currentPlayerId,
  }));
});

const currentPlayerId = computed(() => gameState.value.currentPlayerId);
const isMyTurn = computed(() => myPlayerId.value && currentPlayerId.value && myPlayerId.value === currentPlayerId.value);
const canStartGame = computed(() => gameState.value.phase !== 'IN_PROGRESS' && roomPlayers.value.length >= 2 && roomPlayers.value.length <= 5);
const canPlay = computed(() => gameState.value.phase === 'IN_PROGRESS' && isMyTurn.value && selectedCodes.value.length > 0 && selectedCodes.value.length <= 4);
const cardsToCollect = computed(() => Math.min(3, Math.max(0, gameState.value.pileSize - 1)));
const canCollect = computed(() => gameState.value.phase === 'IN_PROGRESS' && isMyTurn.value && gameState.value.pileSize > 1);
const phaseLabel = computed(() => ({
  WAITING: 'Lobby',
  IN_PROGRESS: 'W grze',
  FINISHED: 'Zakończona',
})[gameState.value.phase] || gameState.value.phase);

const statusLine = computed(() => {
  if (gameState.value.phase === 'FINISHED') {
    return gameState.value.winnerUsername ? `Gra skończona — wygrał ${gameState.value.winnerUsername}` : 'Gra skończona.';
  }
  if (gameState.value.phase === 'IN_PROGRESS') {
    return isMyTurn.value
      ? 'Twoja tura.'
      : `Czeka: ${gameState.value.currentPlayerUsername || '...'}`;
  }
  return 'Oczekiwanie na start gry.';
});

function cardCode(card) {
  const rank = card?.rank?.symbol || card?.rank || '';
  const suit = card?.suit?.symbol || card?.suit || '';
  return card?.code || `${rank}${suit}`;
}

function normalizedRank(card) {
  const raw = card?.rank;
  if (!raw) return '';
  if (typeof raw === 'string') return raw.toUpperCase();
  return (raw.name || raw.symbol || '').toString().toUpperCase();
}

function normalizedSuit(card) {
  const raw = card?.suit;
  if (!raw) return '';
  if (typeof raw === 'string') return raw.toUpperCase();
  return (raw.name || raw.symbol || '').toString().toUpperCase();
}

function cardRankLabel(card) {
  const rank = normalizedRank(card);
  return rankLabels[rank] || card?.rank?.symbol || card?.rank || '?';
}

function suitSymbol(card) {
  const suit = normalizedSuit(card);
  return suitSymbols[suit] || '•';
}

function suitClass(card) {
  const suit = normalizedSuit(card);
  return {
    red: suit === 'HEARTS' || suit === 'DIAMONDS' || suit === 'H' || suit === 'D',
    black: suit === 'CLUBS' || suit === 'SPADES' || suit === 'C' || suit === 'S',
  };
}

function cardSuitLabel(card) {
  return suitNames[normalizedSuit(card)] || '';
}

async function joinRoom() {
  if (!roomId.value || !username.value) {
    errorMessage.value = 'Brak roomId albo username. Zaloguj się ponownie.';
    await router.push('/login');
    return;
  }

  sessionStorage.setItem('roomId', roomId.value);

  try {
    const response = await api.post(`/api/rooms/${roomId.value}/join`, username.value, {
      headers: { 'Content-Type': 'text/plain; charset=utf-8' },
    });
    roomName.value = response.data.roomName || '';
    await syncRoomPlayers();
  } catch (error) {
    await syncRoomPlayers();
  }
}

async function syncRoomPlayers() {
  const room = await api.get(`/api/rooms/memory/${roomId.value}`);
  roomName.value = room.data.roomName || roomName.value;
  roomPlayers.value = (room.data.players || []).map(p => ({
    playerId: p.playerId,
    username: p.username,
    handSize: 0,
    currentTurn: false,
  }));
  gameState.value.players = roomPlayers.value;
  const me = roomPlayers.value.find(p => p.username === username.value);
  myPlayerId.value = me?.playerId || myPlayerId.value || null;
}

function ensureClient() {
  return new Promise((resolve, reject) => {
    if (stompClient.value?.connected) {
      resolve();
      return;
    }

    const socket = new SockJS(SOCKJS_URL);
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 3000,
    });

    client.onConnect = () => {
      stompClient.value = client;
      window.stompClient = client;
      connectionStatus.value = 'online';
      resolve();
    };

    client.onStompError = (frame) => {
      errorMessage.value = frame.headers?.message || 'Błąd STOMP';
    };

    client.onWebSocketClose = () => {
      connectionStatus.value = 'offline';
    };

    client.activate();
    connectionStatus.value = 'connecting';
  });
}

function subscribeRoom() {
  if (!roomId.value || !stompClient.value) return;
  try { subs.room?.unsubscribe(); } catch (_) {}
  subs.room = stompClient.value.subscribe(`/topic/pan/${roomId.value}`, (frame) => {
    try {
      const snapshot = JSON.parse(frame.body);
      console.log('🎮 Received game state update:', {
        pileSize: snapshot.pileSize,
        tableCards: snapshot.tableCards?.map(c => c.code),
        phase: snapshot.phase,
        currentPlayerId: snapshot.currentPlayerId,
        message: snapshot.lastMessage
      });
      gameState.value = snapshot;
      if (snapshot.roomId && !roomName.value) {
        roomName.value = `Pokój ${snapshot.roomId}`;
      }
    } catch (error) {
      errorMessage.value = 'Nie udało się odczytać stanu gry.';
    }
  });

  try { subs.errors?.unsubscribe(); } catch (_) {}
  subs.errors = stompClient.value.subscribe(`/topic/pan/${roomId.value}/errors`, (frame) => {
    errorMessage.value = frame.body || 'Błąd gry';
  });
}

function subscribePrivateCards() {
  if (!stompClient.value || !myPlayerId.value) return;
  try { subs.private?.unsubscribe(); } catch (_) {}
  subs.private = stompClient.value.subscribe(`/topic/pan/private/${myPlayerId.value}`, (frame) => {
    try {
      myHand.value = JSON.parse(frame.body) || [];
    } catch (error) {
      errorMessage.value = 'Nie udało się odczytać prywatnych kart.';
    }
  });
}

async function fetchInitialState() {
  try {
    const state = await api.get(`/api/pan/${roomId.value}/state`);
    gameState.value = {
      ...state.data,
      players: state.data.players?.length ? state.data.players : roomPlayers.value,
    };
  } catch (_) {
    gameState.value = {
      phase: 'WAITING',
      currentPlayerId: null,
      currentPlayerUsername: null,
      requiredRank: 'NINE',
      pileSize: 0,
      winnerPlayerId: null,
      winnerUsername: null,
      message: 'Gra jeszcze nie została rozpoczęta.',
      lastPlay: null,
      tableCards: [],
      players: roomPlayers.value,
    };
  }
}

async function fetchInitialHand() {
  if (!myPlayerId.value) return;
  try {
    const hand = await api.get(`/api/pan/${roomId.value}/hands/${myPlayerId.value}`);
    myHand.value = hand.data || [];
  } catch (_) {
    myHand.value = [];
  }
}

function toggleCard(card) {
  const code = cardCode(card);
  if (selectedCodes.value.includes(code)) {
    selectedCodes.value = selectedCodes.value.filter(c => c !== code);
    return;
  }
  if (selectedCodes.value.length >= 4) {
    errorMessage.value = 'W Panie możesz zagrać maksymalnie 4 karty.';
    return;
  }
  selectedCodes.value = [...selectedCodes.value, code];
  errorMessage.value = '';
}

function sendPanAction(payload) {
  if (!stompClient.value?.connected) {
    errorMessage.value = 'Połączenie websocket nieaktywne.';
    return;
  }
  sending.value = true;
  try {
    stompClient.value.publish({
      destination: `/app/pan/${roomId.value}`,
      body: JSON.stringify(payload),
    });
  } finally {
    sending.value = false;
  }
}

function startGame() {
  sendPanAction({ action: 'startGame' });
}

function playSelectedCards() {
  if (!selectedCodes.value.length || selectedCodes.value.length > 4) {
    errorMessage.value = 'Wybierz 1–4 karty.';
    return;
  }
  sendPanAction({
    action: 'playCards',
    playerId: myPlayerId.value,
    cardCodes: selectedCodes.value,
  });
  selectedCodes.value = [];
}

function collectCards() {
  sendPanAction({
    action: 'challenge',
    playerId: myPlayerId.value,
  });
}

function sendLeaveRequest() {
  if (!roomId.value || !username.value) return;
  try {
    fetch(`/api/rooms/${roomId.value}/leave`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain; charset=utf-8' },
      body: username.value,
      keepalive: true,
    });
  } catch (_) {}
}

onMounted(async () => {
  try {
    await joinRoom();
    await syncRoomPlayers();
    await ensureClient();
    subscribeRoom();
    subscribePrivateCards();
    await fetchInitialState();
    await fetchInitialHand();
    roomSyncTimer = window.setInterval(async () => {
      try {
        await syncRoomPlayers();
        if (gameState.value.phase !== 'WAITING') {
          await fetchInitialState();
        }
      } catch (_) {
        // ignore transient refresh errors
      }
    }, 2000);
    window.addEventListener('beforeunload', sendLeaveRequest);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error.message || 'Nie udało się uruchomić gry.';
  }
});

onBeforeUnmount(() => {
  if (roomSyncTimer) {
    clearInterval(roomSyncTimer);
    roomSyncTimer = null;
  }
  window.removeEventListener('beforeunload', sendLeaveRequest);
  sendLeaveRequest();
  try { subs.room?.unsubscribe(); } catch (_) {}
  try { subs.private?.unsubscribe(); } catch (_) {}
  try { subs.errors?.unsubscribe(); } catch (_) {}
});
</script>

<style scoped>
.pan-page {
  display: grid;
  gap: 20px;
  padding-bottom: 20px;
}

.pan-header {
  display: flex;
  justify-content: space-between;
  gap: 30px;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--border);
}

.pan-header > div:first-child {
  flex: 1;
}

.pan-header > div:first-child h1 {
  font-size: 2.5rem;
  margin: 4px 0;
  background: linear-gradient(135deg, #5b8dff, #7ba8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-meta {
  display: grid;
  gap: 12px;
  text-align: right;
  background: rgba(91, 141, 255, 0.05);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 20px;
  min-width: 280px;
}

.header-meta > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.game-field,
.hand-panel,
.footer-panel {
  display: grid;
  gap: 16px;
}

.game-field__header {
  display: flex;
  gap: 16px;
  justify-content: space-between;
  align-items: stretch;
}

.game-field__header h2 {
  color: var(--primary);
  margin: 8px 0;
}

.actions-card,
.status-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(10, 15, 36, 0.8), rgba(15, 23, 42, 0.8));
  backdrop-filter: blur(4px);
}

.actions-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 220px;
  border-right: 4px solid var(--success);
  align-self: flex-start;
}

.actions-card button {
  font-weight: 600;
}

.table-stack {
  display: grid;
  place-items: center;
  position: relative;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(10, 15, 36, 0.8), rgba(15, 23, 42, 0.8));
  backdrop-filter: blur(4px);
  min-height: 240px;
  overflow: hidden;
  border-left: 4px solid var(--warning);
}

.stack-area {
  position: relative;
  width: 220px;
  height: 120px;
  margin-top: 6px;
}

.stack-card {
  position: absolute;
  width: 72px;
  height: 96px;
  border-radius: 12px;
  border: 2px solid #2a3358;
  background: linear-gradient(180deg, #fefefe, #dfe7f4);
  color: #111827;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.32);
  font-weight: 600;
}

.stack-card__rank {
  font-weight: 800;
  font-size: 1.1rem;
}

.stack-card__suit {
  text-align: right;
  font-size: 1.4rem;
}

.stack-card__code {
  font-size: 0.72rem;
  font-weight: 700;
  opacity: 0.7;
  text-align: right;
}

.stack-empty {
  display: grid;
  place-items: center;
  text-align: center;
  min-height: 120px;
}

.play-summary {
  display: grid;
  gap: 6px;
  padding: 12px;
  background: rgba(91, 141, 255, 0.05);
  border-left: 3px solid var(--primary);
  border-radius: 6px;
  margin-top: 8px;
}

.winner-line {
  margin-top: 10px;
  color: var(--success);
}

.status-card {
  border-left: 4px solid var(--primary);
}

.hand-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.hand-header h2 {
  color: var(--primary);
  margin: 0;
}

.selected-line,
.footer-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  padding: 12px;
  background: rgba(91, 141, 255, 0.05);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.hand-stage {
  display: grid;
  place-items: center;
}

.hand-cards {
  position: relative;
  height: 180px;
  width: min(100%, 980px);
  display: flex;
  justify-content: center;
  align-items: flex-end;
  padding-top: 18px;
}

.card-tile {
  position: relative;
  width: 106px;
  height: 140px;
  border-radius: 14px;
  border: 2px solid #2a3358;
  background: linear-gradient(180deg, #fefefe, #dfe7f4);
  color: #111827;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.32);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;
  font-weight: 600;
  margin-left: -34px;
}

.card-tile:first-child {
  margin-left: 0;
}

.card-tile:hover {
  transform: translateY(-2px);
}

.card-tile.selected {
  transform: translateY(-14px);
  box-shadow: 0 16px 32px rgba(91, 141, 255, 0.40);
  outline: 3px solid var(--primary);
  outline-offset: 2px;
  filter: saturate(1.05);
}

.card-tile.red {
  color: #b91c1c;
}

.card-tile.black {
  color: #111827;
}

.card-top {
  font-size: 1.5rem;
  font-weight: 800;
}

.card-middle {
  font-size: 2.25rem;
  text-align: center;
  line-height: 1;
}

.card-bottom {
  font-size: 0.8rem;
  opacity: 0.9;
  text-align: right;
}

.error-line {
  color: #fca5a5;
  font-weight: 500;
}

.footer-panel {
  border-top: 2px solid var(--border);
  padding-top: 16px;
}

.players-panel {
  margin-top: 8px;
}

.players-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.players-panel__header h3 {
  color: var(--primary);
  margin: 0;
}

.players-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 8px;
}

.players-list__item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
  background: linear-gradient(135deg, rgba(10, 15, 36, 0.6), rgba(15, 23, 42, 0.6));
  transition: all 0.15s ease;
}

.players-list__item.current {
  border-color: var(--primary);
  border-width: 2px;
  box-shadow: 0 0 12px rgba(91, 141, 255, 0.2);
}

.players-list__item.me {
  box-shadow: inset 0 0 0 2px var(--primary);
}

.players-list__main,
.players-list__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.players-list__turn {
  color: var(--success);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .game-field__header {
    flex-direction: column;
  }

  .actions-card {
    width: 100%;
  }

  .hand-cards {
    height: 190px;
  }

  .card-tile {
    margin-left: -42px;
  }
}

/* Table-first layout: the game state and available decisions stay in one visual flow. */
.pan-page { gap: 24px; }

.pan-header {
  align-items: center;
  padding: 8px 4px 20px;
  border-bottom: 1px solid var(--border);
}

.brand-block h1 { font-size: clamp(2.2rem, 5vw, 3.3rem) !important; letter-spacing: -0.06em; }
.eyebrow, .table-message__label {
  margin: 0;
  color: var(--primary-2);
  font-size: .72rem;
  font-weight: 800;
  letter-spacing: .12em;
  text-transform: uppercase;
}
.game-status { margin: 4px 0 0; color: var(--muted); font-size: 1rem; }
.game-status--active { color: #bbf7d0; font-weight: 700; }

.game-metrics { display: flex; align-items: stretch; gap: 8px; }
.metric, .connection-state {
  display: grid;
  align-content: center;
  gap: 4px;
  min-width: 82px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: rgba(7, 13, 28, .56);
}
.metric span { color: var(--muted); font-size: .7rem; text-transform: uppercase; letter-spacing: .08em; }
.metric strong { font-size: 1rem; }
.connection-state { display: flex; align-items: center; min-width: auto; text-transform: capitalize; color: var(--muted); font-size: .8rem; }
.connection-state i { width: 8px; height: 8px; border-radius: 50%; background: var(--danger); }
.connection-state.online { color: #bbf7d0; }.connection-state.online i { background: var(--success); box-shadow: 0 0 10px var(--success); }.connection-state.connecting i { background: #fbbf24; }

.game-layout { display: grid; grid-template-columns: minmax(220px, .38fr) minmax(0, 1fr); gap: 20px; align-items: stretch; }
.players-rail { padding: 16px; background: linear-gradient(180deg, rgba(18, 24, 51, .94), rgba(11, 17, 35, .94)); }
.players-rail :deep(.players-panel) { margin: 0; }.players-rail :deep(.players-panel__header) { margin-bottom: 14px; }.players-rail :deep(.players-list__item) { padding: 11px; }

.table-surface {
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(290px, 1fr) auto;
  overflow: hidden;
  border: 1px solid #244e50;
  border-radius: 24px;
  padding: 24px;
  background: radial-gradient(ellipse at center, rgba(15, 105, 82, .78), rgba(5, 42, 40, .93) 68%, #071c26);
  box-shadow: inset 0 0 0 10px rgba(3, 15, 23, .32), inset 0 0 80px rgba(0, 0, 0, .42), 0 18px 38px rgba(0,0,0,.2);
}
.table-surface::before { content: ''; position: absolute; inset: 12px; border: 1px solid rgba(174, 232, 204, .16); border-radius: 17px; pointer-events: none; }
.table-surface__top, .table-message { position: relative; z-index: 1; display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.table-surface__top h2 { margin: 4px 0 0; color: #f0fdf4; font-size: 1.35rem; }.table-surface__top .eyebrow { color: #b7efdc; }
.table-stack { min-height: 0; border: 0; background: transparent; padding: 12px; overflow: visible; }.stack-area { width: 250px; }.stack-card { border-color: #b7c7d9; }.stack-empty { color: #d2e7dd; }.stack-empty .subtitle { color: #9fc5b6; }
.play-summary { position: absolute; right: 18px; bottom: 12px; max-width: 220px; background: rgba(3, 25, 33, .74); border-left-color: #7ee0c0; color: #d8f3e7; }
.table-message { display: block; padding: 13px 15px; border-radius: 12px; background: rgba(3, 25, 33, .54); }.table-message p { margin: 5px 0 0; color: #d8f3e7; }.table-message__label { color: #b7efdc; }.winner-line { color: #bbf7d0 !important; }

.hand-panel { padding: 20px 24px 12px; }.hand-stats { display: flex; gap: 18px; }.hand-stats > div { display: grid; gap: 2px; }.hand-stats strong { font-size: 1.1rem; }.selected-line { padding: 10px 0 12px; border: 0; border-radius: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--muted); }.action-bar { display: flex; gap: 8px; flex-wrap: wrap; }.action-bar .btn { white-space: nowrap; }.hand-cards { height: 172px; }.card-tile { width: 102px; height: 136px; }
.error-line { margin: 0; padding: 12px 16px; border: 1px solid #7f1d1d; border-radius: 12px; background: rgba(127, 29, 29, .22); }

@media (max-width: 780px) {
  .pan-header, .game-layout { grid-template-columns: 1fr; flex-direction: column; }.game-metrics { width: 100%; }.metric, .connection-state { flex: 1; }.game-layout { display: grid; }.players-rail { order: 2; }.table-surface { order: 1; padding: 18px; border-radius: 18px; }.hand-header { flex-wrap: wrap; }.selected-line { align-items: flex-start; flex-direction: column; }.action-bar { width: 100%; }.action-bar .btn { flex: 1; justify-content: center; }.card-tile { width: 82px; height: 116px; margin-left: -30px; }.card-middle { font-size: 1.8rem; }
}

/* One dashboard above a full-width table; the player's hand belongs to the table itself. */
.pan-header {
  display: grid;
  grid-template-columns: auto auto minmax(280px, 1fr);
  align-items: center;
  gap: 20px;
  padding: 0;
  border: 0;
}
.players-summary { margin: 0; min-width: 0; padding: 14px 16px; background: linear-gradient(135deg, rgba(18, 24, 51, .94), rgba(11, 17, 35, .94)); }
.players-summary :deep(.players-panel) { margin: 0; }
.players-summary :deep(.players-panel__header) { display: none; }
.players-summary :deep(.players-list) { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 2px; }
.players-summary :deep(.player-seat) { flex: 1 0 175px; min-width: 175px; }

.game-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 20px; align-items: stretch; }
.table-surface { grid-template-rows: auto minmax(250px, 1fr) auto auto; gap: 8px; max-width: none; }
.table-hand {
  position: relative;
  z-index: 1;
  padding: 16px 18px 0;
  border-top: 1px solid rgba(183, 239, 220, .22);
  background: linear-gradient(180deg, rgba(3, 25, 33, .15), rgba(3, 25, 33, .48));
}
.table-hand .hand-header h2 { color: #f0fdf4; }.table-hand .subtitle, .table-hand .label, .table-hand .selected-line { color: #b7efdc; }
.table-hand .selected-line { border-bottom-color: rgba(183, 239, 220, .22); }

@media (max-width: 980px) {
  .pan-header { grid-template-columns: 1fr 1fr; }
  .players-summary { grid-column: 1 / -1; }
  .game-layout { grid-template-columns: 1fr; }
}

@media (max-width: 780px) {
  .pan-header { display: grid; grid-template-columns: 1fr; gap: 12px; }
  .game-metrics { width: 100%; }.players-summary { grid-column: auto; }
  .table-surface { padding: 16px; }.table-hand { padding: 14px 0 0; }
}
</style>
