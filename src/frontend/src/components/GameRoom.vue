<template>
  <div class="poker-table">
    <h2>gracz : {{ username }}</h2>

    <div class="chat">
      <h2>Czat</h2>
      <div class="chat-messages">
        <div v-for="message in chatMessages" :key="message.timestamp">
          <strong>{{ message.username }}:</strong> {{ message.content }}
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
    stompClient: null,
    isSending: false,
    };
  },
  async mounted() {
    this.username = localStorage.getItem('username');
    this.gameId = this.$route.params.id;
    console.log(localStorage.getItem('username'))
    this.stompClient = window.stompClient;
      if (this.stompClient && this.gameId) {
    this.stompClient.subscribe(`/topic/room/${this.gameId}`, (message) => {
      const msg = JSON.parse(message.body);
      this.chatMessages.push(msg);
    });
  }
  },
  beforeUnmount() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  },
  methods: {
  sendChatMessage() {
    if (!this.chatMessage.trim()) return;
    const message = {
      username: this.username,
      content: this.chatMessage,
      timestamp: new Date().toISOString(),
    };
    this.stompClient.publish({
      destination: `/app/chat/${this.gameId}`,
      body: JSON.stringify(message),
    });
    this.chatMessage = '';
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