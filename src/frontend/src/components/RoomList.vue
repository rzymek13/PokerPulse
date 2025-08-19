<template>
  <div class="room-list">
    <h1>Lista pokoi</h1>
    <p v-if="username" style="opacity:0.8">Zalogowano jako: {{ username }}</p>
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
        <span>{{ room.roomName }} (ID: {{ room.roomId }})</span>
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
      username: sessionStorage.getItem('username') || '',
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
        console.log(`Tworzenie pokoju: ${this.roomName}`);
        const response = await axios.post('/api/rooms', this.roomName, {
        headers: {
            'Content-Type': 'text/plain; charset=utf-8',
          Authorization: `Bearer ${sessionStorage.getItem('jwt') || ''}`
            },
        });
        console.log(response.data);
        alert(`Pokój utworzony: ${response.data.roomName}`);
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
          headers: { Authorization: `Bearer ${sessionStorage.getItem('jwt') || ''}` },
        });
        this.roomList = response.data;
        // Logowanie dla debugowania
        this.roomList.forEach((room) =>
          console.log(`Pokój: ${room.roomName} (ID: ${room.roomId})`)
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
    sessionStorage.setItem('roomId', String(roomId));
    this.$router.push('/GameRoom');
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