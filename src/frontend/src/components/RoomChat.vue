<template>
  <div class="poker-table">
    <h1>Stół pokerowy (Gra #{{ gameId }})</h1>
    <!-- <div class="game-info">
      <h2>Karty wspólne</h2>
      <ul v-if="gameState.communityCards.length">
        <li v-for="card in gameState.communityCards" :key="card.id">
          {{ card.rank }} of {{ card.suit }}
        </li>
      </ul>
      <p v-else>Brak kart wspólnych</p>
      <h2>Pula: {{ gameState.pot }}</h2>
    </div>
    <div class="player-hand">
      <h2>Twoje karty</h2>
      <ul v-if="playerHand.length">
        <li v-for="card in playerHand" :key="card.id">
          {{ card.rank }} of {{ card.suit }}
        </li>
      </ul>
      <p v-else>Brak kart</p>
    </div> -->
    <!-- <div class="actions">
      <button @click="performAction('check', 0)" :disabled="isActing">Check</button>
      <button @click="performAction('call', 0)" :disabled="isActing">Call</button>
      <button @click="performAction('raise', raiseAmount)" :disabled="isActing || !raiseAmount">
        Raise
      </button>
      <input v-model.number="raiseAmount" type="number" placeholder="Kwota raise" />
      <button @click="performAction('fold', 0)" :disabled="isActing">Fold</button>
    </div> -->
    <div class="chat">
      <h2>Czat</h2>
      <div class="chat-messages">
        <div v-for="message in chatMessages" :key="message.id">
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
  name: 'PokerTable',
  props: {
    gameId: {
      type: [String, Number],
      required: true,
    },
  },
  data() {
    return {
      gameState: { communityCards: [], pot: 0, players: [] },
      // playerHand: [],
      playerId: Number(localStorage.getItem('playerId')) || 1, // Zakładam, że playerId jest w localStorage
      // raiseAmount: 10,
      // isActing: false,
      chatMessages: [],
      chatMessage: '',
      isSending: false,
      stompClient: null,
    };
  },
  async mounted() {
    await this.fetchGameState();
    this.connectWebSocket();
  },
  beforeUnmount() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  },
  methods: {
    // async fetchGameState() {
    //   try {
    //     const response = await axios.get(`/api/game/${this.gameId}`, {
    //       headers: { Authorization: `Bearer ${localStorage.getItem('jwt')}` },
    //     });
    //     this.gameState = response.data;
    //     this.playerHand = this.gameState.players.find(
    //       (player) => player.id === this.playerId
    //     )?.hand || [];
    //   } catch (error) {
    //     console.error('Error fetching game state:', error);
    //     alert('Błąd pobierania stanu gry: ' + (error.response?.data?.message || error.message));
    //   }
    // },
    connectWebSocket() {
      const socket = new SockJS('/poker');
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: { Authorization: `Bearer ${localStorage.getItem('jwt')}` },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('Connected to WebSocket');
          // Subskrypcja stanu gry
          this.stompClient.subscribe(`/topic/game/${this.gameId}`, (message) => {
            this.gameState = JSON.parse(message.body);
            this.playerHand = this.gameState.players.find(
              (player) => player.id === this.playerId
            )?.hand || [];
          });
          // Subskrypcja czatu
          this.stompClient.subscribe(`/topic/game/${this.gameId}/chat`, (message) => {
            const chatMessage = JSON.parse(message.body);
            this.chatMessages.push({
              id: Date.now(), // Unikalne ID dla wiadomości
              username: chatMessage.username,
              content: chatMessage.content,
              timestamp: chatMessage.timestamp || new Date(),
            });
            // Auto-scroll do najnowszej wiadomości
            this.$nextTick(() => {
              const chatDiv = this.$el.querySelector('.chat-messages');
              chatDiv.scrollTop = chatDiv.scrollHeight;
            });
          });
        },
        onStompError: (error) => {
          console.error('WebSocket error:', error);
          alert('Błąd połączenia WebSocket: ' + error);
        },
      });
      this.stompClient.activate();
    },
    async performAction(action, amount) {
      this.isActing = true;
      try {
        const actionDTO = { action, amount, playerId: this.playerId };
        this.stompClient.publish({
          destination: `/app/game/${this.gameId}/action`,
          body: JSON.stringify(actionDTO),
        });
      } catch (error) {
        console.error('Error performing action:', error);
        alert('Błąd wykonywania akcji: ' + error.message);
      } finally {
        this.isActing = false;
      }
    },
    async sendChatMessage() {
      if (!this.chatMessage.trim()) return;
      this.isSending = true;
      try {
        const message = {
          username: localStorage.getItem('username') || 'Anonim',
          content: this.chatMessage.trim(),
          timestamp: new Date().toISOString(),
        };
        this.stompClient.publish({
          destination: `/app/game/${this.gameId}/chat`,
          body: JSON.stringify(message),
        });
        this.chatMessage = ''; // Resetuj pole
      } catch (error) {
        console.error('Error sending chat message:', error);
        alert('Błąd wysyłania wiadomości: ' + error.message);
      } finally {
        this.isSending = false;
      }
    },
    formatDate(timestamp) {
      return new Date(timestamp).toLocaleTimeString();
    },
  },
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