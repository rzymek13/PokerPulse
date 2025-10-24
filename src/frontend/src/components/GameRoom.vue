<template>
  <div class="container">
    <div class="panel" style="margin: 20px auto;">
      <div class="grid grid-2">
        <div>
          <h2>Gracze</h2>
          <ul class="list">
            <li class="list-item" v-for="p in players" :key="p.playerId || p.username">
              <div>
                <strong>{{ p.username }}</strong>
                <span v-if="p.username === username && p.hand && p.hand.length" class="label"> — Twoje karty: {{ formatCards(p.hand) }}</span>
              </div>
            </li>
          </ul>
          <p v-if="!players || players.length === 0" class="label mt-8">Brak graczy w pokoju.</p>
          <!-- Dev debug: show players data if needed -->
          <pre v-if="players && players.length === 0" style="margin-top:8px; background:#f7f7f7; padding:8px; font-size:12px">{{ JSON.stringify(players, null, 2) }}</pre>
          <!-- Test/start button: visible when there are at least 2 players -->
          <button class="btn btn-success mt-16" v-if="players.length > 1" @click="startGame">Start gry</button>
          <!-- Temporary always-visible test button (remove when ready) -->
          <button class="btn btn-warning mt-8" @click="startGame">Start gry (test)</button>
        </div>

        <div>
          <h2>Czat</h2>
          <div class="chat-box">
            <div class="chat-messages">
              <div v-for="message in chatMessages" :key="message.timestamp" style="margin-bottom:6px">
                <strong>{{ message.sender && message.sender.username ? message.sender.username : message.username }}:</strong>
                <span> {{ message.content }}</span>
                <span class="label"> ({{ formatDate(message.timestamp) }})</span>
              </div>
            </div>
            <div class="chat-input">
              <input class="input" v-model="chatMessage" placeholder="Wpisz wiadomość" @keyup.enter="sendChatMessage" :disabled="isSending" />
              <button class="btn btn-primary" @click="sendChatMessage" :disabled="isSending || !chatMessage.trim()">Wyślij</button>
            </div>
          </div>
        </div>
      </div>

      <div class="subtitle mt-16">Zalogowano jako: <strong>{{ username }}</strong></div>
    </div>
  </div>
</template>

<script>
import api from '../api';
import { SOCKJS_URL } from '../config';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export default {


  data() {
    return {
      username: sessionStorage.getItem('username') || '',
      chatMessages: [],
      chatMessage: '',

      roomId: null,
      players: [],
      stompClient: null,
      subscription: null,
      gameSubscription: null,
      isSending: false,
    };
  },
  async mounted() {
    this.username = sessionStorage.getItem('username') || '';

    this.roomId = sessionStorage.getItem('roomId');
    console.log(`roomId: ${this.roomId}`);
    this.stompClient = window.stompClient;

    const subscribeToRoom = () => {
      if (!this.roomId || !this.stompClient) return;
      if (this.subscription) {
        try { this.subscription.unsubscribe(); } catch (_) {}
        this.subscription = null;
      }
      // chat
      this.subscription = this.stompClient.subscribe(`/topic/room/${this.roomId}`, (frame) => {
        try {
          const msg = JSON.parse(frame.body);
          console.log('Received chat message:', msg);
          console.log('Room data', this.roomId);
          if (!msg.sender && msg.username) {
            msg.sender = { username: msg.username };
          }
          this.chatMessages.push(msg);
        } catch (e) {
          console.error('Nie można sparsować wiadomości czatu:', e);
        }
      });
      // Note: subscribing to player-specific game topic is done after we know player's id
    };

    if (this.stompClient && this.stompClient.connected) {
      subscribeToRoom();
    } else if (this.stompClient) {
      // RoomList inicjuje połączenie i nawigację po onConnect,
      // więc tutaj tylko czekamy aż połączenie będzie gotowe.
      this.stompClient.onConnect = () => {
        console.log('Connected to WebSocket (GameRoom)');
        subscribeToRoom();
      };
    } else {
      // Fallback: nowe okno/przeglądarka bez globalnego klienta – utwórz połączenie tutaj.
      const socket = new SockJS(SOCKJS_URL);
      const client = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
      });
      client.onConnect = () => {
        console.log('Connected to WebSocket (GameRoom fallback)');
        this.stompClient = client;
        window.stompClient = client;
        subscribeToRoom();
        // game subscription will be attempted after we fetch players
      };
      client.onStompError = (error) => console.error('WebSocket error:', error);
      client.activate();
      this.stompClient = client;
      window.stompClient = client;
    }

    // Dołącz gracza do pokoju po wejściu (REST)
    try {
      console.log('już w pokoju?', this.roomId);
      console.log('już w pokoju gracz - ', this.username);
      await api.post(`/api/rooms/${this.roomId}/join`, this.username, {
        headers: { 'Content-Type': 'text/plain; charset=utf-8' },
      });
      // Pobierz stan pokoju
    const res = await api.get(`/api/rooms/memory/${this.roomId}`);
    console.log('GET /api/rooms response:', res.data);
    this.players = this.normalizePlayers(res.data.players || []);

  // Subscribe to game updates for this player (server sends to /topic/game/{playerId})
  this.ensureGameSubscription();

    } catch (e) {
      console.error('Join/get room failed', e);
    }
  },
  beforeUnmount() {
    if (this.subscription) {
      try { this.subscription.unsubscribe(); } catch (_) {}
      this.subscription = null;
    }
    if (this.gameSubscription) {
      try { this.gameSubscription.unsubscribe(); } catch (_) {}
      this.gameSubscription = null;
    }
    // Nie dezaktywujemy klienta globalnego – inne ekrany mogą go używać
  },
  methods: {
  // Normalize player objects from backend to ensure consistent fields (playerId, username)
  normalizePlayers(players) {
    return players.map(p => {
      // backend may return different casing or nested structures
      const playerId = p.playerId || p.player_id || p.id || p.playerID || null;
      const username = p.username || p.name || (p.player && p.player.username) || '';
      const hand = p.hand || p.cards || p.playerHand || [];
      return { ...p, playerId, username, hand };
    });
  },

  // Subscribe to /topic/game/{playerId} when we have a playerId for current user
  ensureGameSubscription() {
    const me = this.players.find(p => p.username === this.username || p.playerId && String(p.playerId) === String(this.username));
    const playerId = me && me.playerId;
    if (!playerId) {
      console.log('ensureGameSubscription: playerId not found yet');
      return;
    }
    if (!this.stompClient || !this.stompClient.connected) {
      console.log('ensureGameSubscription: stomp client not connected yet');
      return;
    }
    try {
      if (this.gameSubscription) {
        try { this.gameSubscription.unsubscribe(); } catch (_) {}
        this.gameSubscription = null;
      }
      this.gameSubscription = this.stompClient.subscribe(`/topic/game/${playerId}`, (frame) => {
        try {
          const room = JSON.parse(frame.body);
          this.players = this.normalizePlayers(room.players || []);
        } catch (e) {
          console.error('Nie można sparsować stanu pokoju (game update):', e);
        }
      });
      console.log(`Subscribed to /topic/game/${playerId}`);
    } catch (e) {
      console.error('Failed to subscribe to player game topic:', e);
    }
  },
  sendChatMessage() {
    if (!this.chatMessage.trim()) return;
    if (!this.stompClient || !this.stompClient.connected) {
      alert('Połączenie z czatem nie jest aktywne!');
      return;
    }
    const message = {
      sender: { username: this.username },
      content: this.chatMessage,
      timestamp: new Date().toISOString(),
    };
    this.isSending = true;
    try {
      this.stompClient.publish({
        destination: `/app/chat/${this.roomId}`,
        body: JSON.stringify(message),
      });
      // Czekamy na echo z serwera, żeby uniknąć duplikatów
      this.chatMessage = '';
    } finally {
      this.isSending = false;
    }
  },

  startGame() {
    // Find the playerId for the current user
    const me = this.players.find(p => p.username === this.username);
    const playerId = me && (me.playerId || me.player_id || me.id || me.playerID);

    if (!this.stompClient || !this.stompClient.connected) {
      console.warn('STOMP client not connected - cannot start game');
      alert('Połączenie z serwerem nie jest aktywne. Spróbuj ponownie.');
      return;
    }

    if (!playerId) {
      // Backend expects playerId path variable; if we don't have it, try to request start without it
      console.warn('Nie znaleziono playerId dla użytkownika. Próba użycia alternatywnego endpointu.');
      // As a fallback, call the REST API to start the game (if implemented) or notify user
      // Here we try to publish to the room-level start endpoint if server supports it
      try {
        this.stompClient.publish({
          destination: `/app/game/${this.roomId}/0`,
          body: JSON.stringify({ username: this.username }),
        });
        console.log('Wysłano żądanie startu gry (fallback)');
      } catch (e) {
        console.error('Błąd wysyłania startGame (fallback):', e);
      }
      return;
    }

    try {
      // Backend mapping: @MessageMapping("/game/{roomId}/{playerId}")
      this.stompClient.publish({
        destination: `/app/game/${this.roomId}/${playerId}`,
        body: null,
      });
      console.log(`Requested startGame for room ${this.roomId} player ${playerId}`);
    } catch (e) {
      console.error('startGame publish failed', e);
    }
  },
  formatDate(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleString();
  },
  formatCards(cards) {
    return cards.map(c => `${c.rank?.symbol || c.rank} ${c.suit?.symbol || c.suit}`).join(', ');
  }
}
};
</script>

<style scoped>
/* uses global classes from style.css */
</style>