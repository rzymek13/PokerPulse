<template>
  <div class="container">
    <div class="panel" style="max-width:720px; margin: 24px auto;">
      <h1>Lobby pokoi</h1>
      <p class="subtitle" v-if="username">Zalogowano jako: <strong>{{ username }}</strong></p>

      <div class="grid grid-2 mt-12" style="align-items:end;">
        <div class="field">
          <label class="label">Nazwa pokoju</label>
          <input class="input" v-model="roomName" placeholder="np. Pokerowcy" @keyup.enter="createRoom" :disabled="isCreating" />
        </div>
        <div>
          <button class="btn btn-primary" @click="createRoom" :disabled="isCreating || !roomName.trim()">Stwórz pokój</button>
        </div>
      </div>

      <ul class="list mt-16" v-if="roomList.length">
        <li class="list-item" v-for="room in roomList" :key="room.roomId">
          <div>
            <strong>{{ room.roomName }}</strong>
            <div class="label">ID: {{ room.roomId }}</div>
          </div>
          <button class="btn" @click="joinRoom(room.roomId)" :disabled="isJoining">Dołącz</button>
        </li>
      </ul>
      <p v-else class="label mt-16">Brak dostępnych pokoi.</p>

      <div class="mt-24">
        <button class="btn btn-danger" @click="logout">Wyloguj</button>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../api';
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
        const response = await api.post('/api/rooms', this.roomName, {
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
        const response = await api.get('/api/rooms', {
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
/* uses global classes from style.css */
</style>