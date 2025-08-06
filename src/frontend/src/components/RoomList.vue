<template>
  <div class="room-list">
    <h1>Lista pokoi</h1>
    <div class="create-room">
      <input
        v-model="roomName"
        placeholder="Nazwa pokoju"
        @keyup.enter="createRoom"
        :disabled="isCreating"
      />
      <button @click="createRoom" :disabled="isCreating || !roomName.trim()">
        Stwórz
      </button>
    </div>
    <ul v-if="roomList.length">
      <li v-for="room in roomList" :key="room.roomId">
        <span>{{ room.name }} (ID: {{ room.roomId }})</span>
        <button @click="joinRoom(room.roomId)" :disabled="isJoining">
          Dołącz
        </button>
      </li>
    </ul>
    <p v-else>Brak dostępnych pokoi.</p>
    <button class="logout-btn" @click="logout">Wyloguj</button>
  </div>
</template>

<script>
import axios from 'axios';
import { useRouter } from 'vue-router';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';



export default {
  name: 'RoomList',
  setup() {
    const router = useRouter();
    return { router };
  },
  data() {
    return {
      roomName: '',
      roomList: [],
      isCreating: false,
      isJoining: false,
    };
  },
  async mounted() {
    await this.fetchRooms();
  },
  methods: {
    async createRoom() {
      if (!this.roomName.trim()) {
        alert('Nazwa pokoju nie może być pusta!');
        return;
      }
      this.isCreating = true;
      try {
        const response = await axios.post(
          '/api/rooms',
          { name: this.roomName.trim() },
          { headers: { Authorization: `Bearer ${localStorage.getItem('jwt')}` } }
        );
        alert(`Pokój utworzony: ${response.data.name}`);
        this.roomName = ''; // Resetuj pole
        await this.fetchRooms(); // Odśwież listę
      } catch (error) {
        alert(
          'Błąd tworzenia pokoju: ' +
            (error.response?.data?.message || error.message)
        );
      } finally {
        this.isCreating = false;
      }
    },
    async fetchRooms() {
      try {
        const response = await axios.get('/api/rooms', {
          headers: { Authorization: `Bearer ${localStorage.getItem('jwt')}` },
        });
        this.roomList = response.data;
        // Logowanie dla debugowania
        this.roomList.forEach((room) =>
          console.log(`Pokój:  (ID: ${room.roomId})`)
        );
      } catch (error) {
        alert(
          'Błąd pobierania pokoi: ' +
            (error.response?.data?.message || error.message)
        );
      }
    },
async joinRoom(roomId) {
  this.isJoining = true;
  try {
    const socket = new SockJS('http://localhost:8080/poker');
    const stompClient = Stomp.over(socket);

    stompClient.onConnect = () => {
      console.log('Connected to WebSocket');
      // Subskrybuj kanał gry
      stompClient.subscribe(`/topic/room/${roomId}`)
      this.$router.push({ name: 'GameRoom', params: { id: roomId } });
      window.stompClient = stompClient;
    };

    stompClient.onStompError = (error) => {
      console.error('WebSocket error:', error);
    };

    stompClient.activate();
  } catch (error) {
    alert('Błąd dołączania do pokoju: ' + error.message);
  } finally {
    this.isJoining = false;
  }
}
    },
};
</script>

<style scoped>
.room-list {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
  text-align: center;
}
.create-room {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.create-room input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
ul {
  list-style: none;
  padding: 0;
}
li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #eee;
}
button {
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
.logout-btn {
  margin-top: 20px;
  background-color: #dc3545;
}
</style>