<template>
  <div class="container room-page">
    <div class="left-panel panel">
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
        <li class="list-item" v-for="room in roomList" :key="room.gameRoomId">
          <div>
            <strong>{{ room.roomName }}</strong>
            <div class="label">ID: {{ room.gameRoomId }}</div>
          </div>
          <div style="display:flex; gap:8px; align-items:center">
            <button class="btn" @click="joinRoom(room.gameRoomId)" :disabled="isJoining">Dołącz</button>
            <button class="btn btn-danger" @click="confirmDelete(room.gameRoomId)">Usuń pokój</button>
          </div>
        </li>
      </ul>
      <p v-else class="label mt-16">Brak dostępnych pokoi.</p>

      <div class="mt-24">
        <button class="btn btn-danger" @click="logout">Wyloguj</button>
      </div>
    </div>
    <div class="right-main">
      <!-- right side can host help, announcements or lobby info -->
      <div class="panel" style="padding:16px">
        <h2>Witaj w PokerPulse</h2>
        <p class="label">Wybierz lub stwórz pokój po lewej stronie, by dołączyć do gry.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api';

const router = useRouter();

const username = ref(sessionStorage.getItem('username') || '');
const roomName = ref('');
const roomList = ref([]);
const isCreating = ref(false);
const isJoining = ref(false);

async function fetchRooms() {
  try {
    const response = await api.get('/api/rooms', {
      headers: { Authorization: `Bearer ${sessionStorage.getItem('jwt') || ''}` },
    });
    roomList.value = response.data;
    roomList.value.forEach((room) => console.log(`Pokój: ${room.roomName} (ID: ${room.gameRoomId})`));
  } catch (error) {
    alert('Błąd pobierania pokoi: ' + (error.response?.data?.message || error.message));
  }
}

async function createRoom() {
  if (!roomName.value.trim()) { alert('Nazwa pokoju nie może być pusta!'); return; }
  isCreating.value = true;
  try {
    console.log(`Tworzenie pokoju: ${roomName.value}`);
    const response = await api.post('/api/rooms', roomName.value, {
      headers: { 'Content-Type': 'text/plain; charset=utf-8', Authorization: `Bearer ${sessionStorage.getItem('jwt') || ''}` },
    });
    console.log(response.data);
    alert(`Pokój utworzony: ${response.data.roomName}`);
    roomName.value = '';
    await fetchRooms();
  } catch (error) {
    alert('Błąd tworzenia pokoju: ' + (error.response?.data?.message || error.message));
  } finally { isCreating.value = false; }
}

async function joinRoom(roomId) {
  console.log('joinRoom arg:', roomId);
  isJoining.value = true;
  try {
    sessionStorage.setItem('roomId', String(roomId));
    await router.push('/GameRoom/' + roomId);
  } finally { isJoining.value = false; }
}

async function deleteRoom(roomId) {
  try {
    await api.delete(`/api/rooms/${roomId}`, { headers: { Authorization: `Bearer ${sessionStorage.getItem('jwt') || ''}` } });
    alert('Pokój usunięty');
    roomList.value = roomList.value.filter(r => r.gameRoomId !== roomId);
  } catch (error) {
    alert('Błąd usuwania pokoju: ' + (error.response?.data?.message || error.message));
  }
}

function confirmDelete(roomId) {
  if (!confirm(`Na pewno usunąć pokój ${roomId}?`)) return;
  deleteRoom(roomId);
}

function logout() {
  sessionStorage.removeItem('username');
  sessionStorage.removeItem('jwt');
  router.push('/');
}

onMounted(() => { fetchRooms(); });
</script>