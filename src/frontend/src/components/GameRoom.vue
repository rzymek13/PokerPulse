<template>
  <div class="container">
    <div class="panel" style="margin: 20px auto;">
      <div class="grid grid-2">
        <div>
          <h2>Gracze</h2>
          <ul class="list">
            <li class="list-item" v-for="p in players" :key="p.username">
              <div>
                <strong>{{ p.username }}</strong>
                <span v-if="p.username === username && p.hand && p.hand.length" class="label"> — Twoje karty: {{ formatCards(p.hand) }}</span>
              </div>
            </li>
          </ul>
          <button class="btn btn-success mt-16" v-if="players.length > 1" @click="startGame">Start gry</button>
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
chatMessages: [],
  chatMessage: '',
    username: '',
      roomId: null,
      players: [],
      stompClient: null,
      subscription: null,
      isSending: false,
    };
  },
  async mounted() {
    this.username = sessionStorage.getItem('username') || '';
    this.roomId = this.$route.params.id || sessionStorage.getItem('roomId');
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
          if (!msg.sender && msg.username) {
            msg.sender = { username: msg.username };
          }
          this.chatMessages.push(msg);
        } catch (e) {
          console.error('Nie można sparsować wiadomości czatu:', e);
        }
      });
      // room state
      this.stompClient.subscribe(`/topic/room/${this.roomId}/state`, (frame) => {
        try {
          const room = JSON.parse(frame.body);
          this.players = room.players || [];
        } catch (e) {
          console.error('Nie można sparsować stanu pokoju:', e);
        }
      });
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
      };
      client.onStompError = (error) => console.error('WebSocket error:', error);
      client.activate();
      this.stompClient = client;
      window.stompClient = client;
    }

    // Dołącz gracza do pokoju po wejściu (REST)
    try {
      await api.post(`/api/rooms/${this.roomId}/join`, this.username, {
        headers: { 'Content-Type': 'text/plain; charset=utf-8' },
      });
      // Pobierz stan pokoju
      const res = await api.get(`/api/rooms/${this.roomId}`);
      this.players = res.data.players || [];

    } catch (e) {
      console.error('Join/get room failed', e);
    }
  },
  beforeUnmount() {
    if (this.subscription) {
      try { this.subscription.unsubscribe(); } catch (_) {}
      this.subscription = null;
    }
    // Nie dezaktywujemy klienta globalnego – inne ekrany mogą go używać
  },
  methods: {
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
    if (!this.stompClient || !this.stompClient.connected) return;
    const dto = { username: this.username };
    this.stompClient.publish({
      destination: `/app/game/${this.roomId}/start`,
      body: JSON.stringify(dto),
    });
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