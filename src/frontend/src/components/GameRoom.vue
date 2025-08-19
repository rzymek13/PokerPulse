<template>
  <div class="poker-table">
    <h2>gracz : {{ username }}</h2>

    <div class="room-status">
      <h2>Gracze</h2>
      <ul>
        <li v-for="p in players" :key="p.username">
          <label>
            <input type="checkbox" :checked="p.ready" :disabled="p.username !== username" @change="toggleReady($event.target.checked)" />
            {{ p.username }} <span v-if="p.username === roomCreator">(twórca)</span>
          </label>
          <span v-if="p.username === username && p.hand && p.hand.length"> | Twoje karty: {{ formatCards(p.hand) }}</span>
        </li>
      </ul>
      <button v-if="isCreator && allReady" @click="startGame">Start gry</button>
    </div>

    <div class="chat">
      <h2>Czat</h2>
      <div class="chat-messages">
        <div v-for="message in chatMessages" :key="message.timestamp">
          <strong>{{ message.sender && message.sender.username ? message.sender.username : message.username }}:</strong>
          {{ message.content }}
          <span class="timestamp">({{ formatDate(message.timestamp) }})</span>
        </div>
      </div>
      <div class="chat-input">
        <input
          v-model="chatMessage"
          placeholder="Wpisz wiadomość"
          @keyup.enter="sendChatMessage"
          :disabled="isSending"
        />
        <button @click="sendChatMessage" :disabled="isSending || !chatMessage.trim()">
          Wyślij
        </button> 
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export default {

computed: {
isCreator() {
return this.username && this.roomCreator && this.username === this.roomCreator;
},
allReady() {
return this.players.length >= 2 && this.players.every(p => p.ready);
}
},
data() {
return {
chatMessages: [],
  chatMessage: '',
    username: '',
      roomId: null,
      players: [],
      roomCreator: null,
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
          this.roomCreator = room.creatorUsername || null;
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
      const socket = new SockJS('http://localhost:8080/poker');
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
      await axios.post(`/api/rooms/${this.roomId}/join`, this.username, {
        headers: { 'Content-Type': 'text/plain; charset=utf-8' },
      });
      // Pobierz stan pokoju
      const res = await axios.get(`/api/rooms/${this.roomId}`);
      this.players = res.data.players || [];
      this.roomCreator = res.data.creatorUsername || null;
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
  toggleReady(checked) {
    if (!this.stompClient || !this.stompClient.connected) return;
    const dto = { username: this.username, ready: !!checked };
    this.stompClient.publish({
      destination: `/app/game/${this.roomId}/ready`,
      body: JSON.stringify(dto),
    });
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
.poker-table {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
.game-info,
.player-hand {
  margin-bottom: 20px;
}
.actions {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.actions button {
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.actions button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
.actions input {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.chat {
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 10px;
}
.chat-messages {
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f9f9f9;
}
.chat-messages div {
  margin-bottom: 5px;
}
.chat-messages .timestamp {
  font-size: 0.8em;
  color: #666;
}
.chat-input {
  display: flex;
  gap: 10px;
}
.chat-input input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.chat-input button {
  padding: 8px 16px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.chat-input button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
ul {
  list-style: none;
  padding: 0;
}
li {
  margin-bottom: 5px;
}
</style>