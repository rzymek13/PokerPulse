<template>
  <div class="poker-table">
    <h2>gracz : {{ username }}</h2>

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

  data() {
    return {
      chatMessages: [],
      chatMessage: '',
      username: '',
      roomId: null,
      stompClient: null,
      subscription: null,
      isSending: false,
    };
  },
  async mounted() {
    this.username = localStorage.getItem('username');
    this.roomId = this.$route.params.id || localStorage.getItem('roomId');
    console.log(`roomId: ${this.roomId}`);
    this.stompClient = window.stompClient;

    const subscribeToRoom = () => {
      if (!this.roomId || !this.stompClient) return;
      if (this.subscription) {
        try { this.subscription.unsubscribe(); } catch (_) {}
        this.subscription = null;
      }
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
  formatDate(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleString();
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