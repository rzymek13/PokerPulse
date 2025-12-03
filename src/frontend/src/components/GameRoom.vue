<template>
  <div class="container">
    <div class="game-page container">
      <div class="main-area">
        <div class="board-wrapper" style="flex-direction:column; gap:12px; align-items:center; justify-content:flex-start;">
          <GameBoard 
            :players="players" 
            :myPrivateCards="myPrivateCards"
            :username="username"
            :communityCards="communityCards"
          />

          <div class="action-controls" style="margin-top:6px; display:flex; justify-content:center; width:100%;">
            <div v-if="lastHandsCount > 0" style="display:flex; gap:8px; align-items:center;">
              <div class="label">Tura: <strong>{{ currentPlayerName || '...' }}</strong></div>
              <button class="btn" @click="sendPlayerAction('CHECK')" :disabled="!isMyTurn || !stompClient?.connected">Check</button>
              <button class="btn" @click="sendPlayerAction('CALL')" :disabled="!isMyTurn || !stompClient?.connected">Call</button>
              <input type="number" v-model.number="raiseAmount" min="1" style="width:100px;" :disabled="!isMyTurn" />
              <button class="btn btn-warning" @click="sendPlayerAction('RAISE', raiseAmount)" :disabled="!isMyTurn || raiseAmount<=0">Bet</button>
              <button class="btn btn-danger" @click="sendPlayerAction('FOLD')" :disabled="!isMyTurn">Fold</button>
            </div>
          </div>
        </div>

        <aside class="chat-wrapper panel">
          
        <h2>Gracze</h2>
        <div style="display:flex; align-items:center; justify-content:space-between; gap:12px">
          <ul class="list horizontal-players" style="flex:1">
          <li class="list-item" v-for="p in players" :key="p.playerId || p.username">
              <strong>{{ p.username }}</strong>      
          </li>
          </ul>
          <div style="min-width:160px; text-align:right">
            <div v-if="lastHandsCount === 0">
              <button class="btn btn-success" @click="startGame" :disabled="isSending || !stompClient?.connected || players.length < 2">Start game</button>
            </div>
            <div v-else>
              <div class="label">Gra rozpoczęta — rozdano {{ lastHandsCount }} ręk.</div>
            </div>
          </div>
        </div>
      
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
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import GameBoard from './GameBoard.vue'

import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue';
import api from '../api';
import { SOCKJS_URL } from '../config';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const username = ref(sessionStorage.getItem('username') || '');
const roomId = ref(sessionStorage.getItem('roomId') || null);

// UI state
const chatMessages = ref([]);
const chatMessage = ref('');
const players = ref([]);
const myPrivateCards = ref([]);
const communityCards = ref([]);
const raiseAmount = ref(0);
const currentPlayerId = ref(null);
const currentPlayerName = ref(null);
const isMyTurn = computed(() => {
  const me = players.value.find(p => p.username === username.value);
  return !!(me && me.playerId && currentPlayerId.value && me.playerId === currentPlayerId.value);
});
// Track how many hands the server reports for the room — used to avoid fetching private cards
// before the game has been started by the server.
const lastHandsCount = ref(0);

// stomp client + subs
const stompClient = ref(null);
const subs = reactive({ room: null, game: null, private: null });
const isSending = ref(false);

function normalizePlayers(list) {
  return (list || []).map(p => ({
    ...p,
    playerId: p.playerId || p.player_id || p.id || p.playerID || null,
    username: p.username || p.name || (p.player && p.player.username) || '',
    hand: p.hand || p.cards || p.playerHand || [],
  }));
}

function ensureClient() {
  return new Promise((resolve) => {
    if (stompClient.value && stompClient.value.connected) return resolve();
    if (stompClient.value) {
      stompClient.value.onConnect = () => resolve();
      return;
    }
    const socket = new SockJS(SOCKJS_URL);
    const client = new Client({ webSocketFactory: () => socket, reconnectDelay: 5000 });
    client.onConnect = () => {
      stompClient.value = client;
      window.stompClient = client;
      resolve();
    };
    client.onStompError = (err) => console.error('STOMP error', err);
    client.activate();
  });
}

function subscribePrivateTopic(playerId) {
  if (!playerId || !stompClient.value) return;
  try { subs.private && subs.private.unsubscribe(); } catch (_) {}
  try {
    subs.private = stompClient.value.subscribe(`/topic/privateCards/${playerId}`, frame => {
      try {
        const payload = JSON.parse(frame.body);
        console.log('Received private cards via WS for player', playerId, payload);
        myPrivateCards.value = payload || [];
      } catch (e) { console.error('Failed to parse private cards frame', e); }
    });
  } catch (e) { console.error('Failed to subscribe to privateCards topic', e); }
}

function subscribeRoomTopic() {
  if (!roomId.value || !stompClient.value) return;
  try { subs.room && subs.room.unsubscribe(); } catch (_) {}
  subs.room = stompClient.value.subscribe(`/topic/room/${roomId.value}`, (frame) => {
    try {
      // The controller now broadcasts the full, sanitized GameRoom object.
      const room = JSON.parse(frame.body);
      console.log('Received room update via WS:', room);
      // Update players list (sanitized - no private hands)
      players.value = normalizePlayers(room.players || room.players || []);
      console.log('Parsed players from room:', players.value.map(p => ({ id: p.playerId, username: p.username })));

      // Extract community cards from the first hand if available
      if (room.hands && Array.isArray(room.hands) && room.hands.length > 0) {
        communityCards.value = room.hands[0].communityCards || [];
        console.log('Updated community cards:', communityCards.value);
        // Update current player info if provided by the room/hands
        try {
          const hand0 = room.hands[0];
          const cp = hand0.currentPlayer || hand0.current_player || hand0.current || null;
          if (cp) {
            // cp might be an object or a primitive id
            if (typeof cp === 'object') {
              currentPlayerId.value = cp.playerId || cp.player_id || cp.id || cp.playerID || null;
              currentPlayerName.value = cp.username || cp.name || null;
            } else {
              currentPlayerId.value = cp;
              currentPlayerName.value = null;
            }
          }
        } catch (e) { console.debug('No currentPlayer info in room hands', e); }

        // Fallback: if server didn't provide currentPlayer, pick first non-folded player
        if (!currentPlayerId.value) {
          const first = players.value && players.value.length > 0 ? players.value[0] : null;
          if (first && first.playerId) {
            currentPlayerId.value = first.playerId;
            currentPlayerName.value = first.username;
            console.debug('Falling back to first player as currentPlayer:', currentPlayerId.value);
          }
        }
        console.log('Current player resolved to:', { id: currentPlayerId.value, name: currentPlayerName.value });
      }

      // If server sent hands (game started), fetch private cards for current player
      if (room.hands && Array.isArray(room.hands) && room.hands.length > 0) {
            const me = players.value.find(p => p.username === username.value);
            if (me && me.playerId) {
              console.log('Room contains hands -> will subscribe/fetch private cards for', me.playerId);
              // prefer websocket push for private cards
              subscribePrivateTopic(me.playerId);
              // fallback to REST in case WS private topic is not supported
              fetchPrivateCards(me.playerId);
            }
      }

      // If the server created hands (game start), trigger fetching private cards for current player.
      const handsCount = room.hands && Array.isArray(room.hands) ? room.hands.length : 0;
      if (handsCount > 0 && handsCount !== lastHandsCount.value) {
        lastHandsCount.value = handsCount;
        const me = players.value.find(p => p.username === username.value);
        if (me && me.playerId) {
          console.log('Detected new hands for room - subscribing/fetching private cards for player:', me.playerId);
          subscribePrivateTopic(me.playerId);
          fetchPrivateCards(me.playerId);
        }
      }

      // Update chat history if provided (array of messages)
      if (room.chatHistory && Array.isArray(room.chatHistory)) {
        // sync to chatMessages reactive store (append any new ones)
        chatMessages.value = (room.chatHistory || []).map(m => {
          if (!m.sender && m.username) m.sender = { username: m.username };
          return m;
        });
      }

    } catch (e) { console.error('Failed to parse room update', e); }
  });
}

async function joinRoomAndLoadPlayers() {
  if (!roomId.value || !username.value) return;
  try {
    const res = await api.post(`/api/rooms/${roomId.value}/join`, username.value, { headers: { 'Content-Type': 'text/plain; charset=utf-8' } });
    const room = res?.data || (await api.get(`/api/rooms/memory/${roomId.value}`)).data;
    console.log('Joined room state:', room);
    // show full room state for now while debugging / joining
    console.log('Joined room initial state:', room);
    players.value = normalizePlayers(room.players || []);
    lastHandsCount.value = room.hands && Array.isArray(room.hands) ? room.hands.length : 0;
  } catch (e) { console.error('Failed join/get room:', e); }
}

// The websocket controller now broadcasts room state on /topic/room/{roomId}.
// Private cards are fetched via REST: GET /api/rooms/{roomId}/{playerId}/privateCards
async function fetchPrivateCards(playerId) {
  if (!roomId.value || !playerId) return;
  try {
    const res = await api.get(`/api/rooms/${roomId.value}/${playerId}/privateCards`);
    myPrivateCards.value = res.data || [];
    console.log('Fetched private cards via REST:', myPrivateCards.value);
  } catch (e) {
    console.error('Failed to fetch private cards:', e);
  }
}

function sendChatMessage() {
  if (!chatMessage.value.trim() || !stompClient.value?.connected) return;
  const message = { sender: { username: username.value }, content: chatMessage.value, timestamp: new Date().toISOString() };
  isSending.value = true;
  try {
    // New controller expects room updates to /app/room/{roomId} as partial GameRoom payloads.
    const payload = { chatHistory: [message] };
    stompClient.value.publish({ destination: `/app/room/${roomId.value}`, body: JSON.stringify(payload) });
    chatMessage.value = '';
  } catch (e) { console.error('chat publish failed', e); }
  finally { isSending.value = false; }
}

async function startGame() {
  const me = players.value.find(p => p.username === username.value);
  const playerId = me && me.playerId;
  if (!playerId) return console.warn('Cannot start game: missing playerId');

  // Preferred: try REST start endpoint if server exposes it (future-proof). If 404 - fallback to websocket action.
  try {
    await api.post(`/api/rooms/${roomId.value}/start`);
    console.log('Requested start game via REST');
  } catch (e) {
    // Not necessarily an error if REST isn't available — fall back to websocket command
    console.debug('REST start not available or failed — sending room action via WS', e?.response?.status);
    try {
      // ensure we are subscribed to our privateCards topic before requesting start
      if (stompClient.value?.connected) {
        subscribePrivateTopic(playerId);
      }
      const payload = { action: 'startGame', initiator: playerId };
      if (stompClient.value?.connected) stompClient.value.publish({ destination: `/app/room/${roomId.value}`, body: JSON.stringify(payload) });
    } catch (ex) { console.error('Fallback startGame via WS failed', ex); }
  }

  // After starting the game we need to fetch private cards for the current player using REST endpoint.
  // await fetchPrivateCards(playerId);
}

function sendPlayerAction(decision, amount) {
  const me = players.value.find(p => p.username === username.value);
  const playerId = me && me.playerId;
  if (!playerId) return console.warn('Cannot send action: missing playerId');
  if (!stompClient.value || !stompClient.value.connected) return console.warn('STOMP not connected');
  const payload = { action: 'playerAction', playerId, decision };
  if (amount !== undefined && amount !== null) payload.amount = Number(amount);
  try {
    stompClient.value.publish({ destination: `/app/room/${roomId.value}`, body: JSON.stringify(payload) });
    console.log('Sent playerAction via WS:', payload);
  } catch (e) { console.error('Failed to publish playerAction', e); }
}


function sendLeaveRequest() {
  if (!roomId.value || !username.value) return;
  try { if (navigator?.sendBeacon) { const blob = new Blob([username.value], { type: 'text/plain;charset=utf-8' }); navigator.sendBeacon(`/api/rooms/${roomId.value}/leave`, blob); return; } }
  catch (e) { console.warn('sendBeacon failed', e); }
  try { fetch(`/api/rooms/${roomId.value}/leave`, { method: 'POST', headers: { 'Content-Type': 'text/plain; charset=utf-8' }, body: username.value, keepalive: true }); }
  catch (e) { console.warn('leave fetch failed', e); }
}

function formatDate(ts) { return new Date(ts).toLocaleString(); }
function formatCards(cards) { return (cards || []).map(c => `${c.rank?.symbol || c.rank} ${c.suit?.symbol || c.suit}`).join(', '); }

onMounted(async () => {
  stompClient.value = window.stompClient;
  try {
    await ensureClient();
    subscribeRoomTopic();
    await joinRoomAndLoadPlayers();
    // If the joined room already has hands (game started), fetch private cards for current player
    const me = players.value.find(p => p.username === username.value);
    if (me && me.playerId) {
      // subscribe to private topic so WS pushes are received
      subscribePrivateTopic(me.playerId);
      if (lastHandsCount.value > 0) fetchPrivateCards(me.playerId);
    }
    window.addEventListener('beforeunload', sendLeaveRequest);
  } catch (e) { console.error('GameRoom mounted error:', e); }
});

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', sendLeaveRequest);
  try { Object.values(subs).forEach(s => { try { s && s.unsubscribe(); } catch (_) {} }); } catch (_) {}
  try { sendLeaveRequest(); } catch (_) {}
});
</script>
