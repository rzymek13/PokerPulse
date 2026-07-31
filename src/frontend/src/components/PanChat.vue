<template>
  <aside class="chat-panel panel">
    <header class="chat-panel__header">
      <div>
        <p class="chat-panel__eyebrow">Pokój</p>
        <h2>Czat gry</h2>
      </div>
      <span class="chat-panel__status" :class="connectionStatus"><i></i>{{ connectionStatus }}</span>
    </header>

    <div ref="messagesElement" class="chat-panel__messages" aria-live="polite">
      <p v-if="!messages.length" class="chat-panel__empty">Napisz pierwszą wiadomość.</p>
      <article v-for="message in messages" :key="messageKey(message)" class="chat-message" :class="{ mine: messageUsername(message) === username }">
        <div class="chat-message__meta">
          <strong>{{ messageUsername(message) }}</strong>
          <time>{{ formatTime(message.timestamp) }}</time>
        </div>
        <p>{{ message.content }}</p>
      </article>
    </div>

    <form class="chat-panel__form" @submit.prevent="sendMessage">
      <input v-model="draft" class="input" maxlength="500" placeholder="Napisz wiadomość…" :disabled="connectionStatus !== 'online'" />
      <button class="btn btn-primary" type="submit" :disabled="!draft.trim() || connectionStatus !== 'online'">Wyślij</button>
    </form>
  </aside>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import api from '../api';
import { SOCKJS_URL } from '../config';

const props = defineProps({
  roomId: { type: [String, Number], required: true },
  username: { type: String, required: true },
});

const draft = ref('');
const messages = ref([]);
const connectionStatus = ref('connecting');
const messagesElement = ref(null);
let client;
let subscription;

function messageUsername(message) {
  return message.sender?.username || message.username || 'Gracz';
}
function messageKey(message) {
  return `${message.timestamp}-${messageUsername(message)}-${message.content}`;
}
function formatTime(timestamp) {
  return timestamp ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
}
function scrollToLatest() {
  nextTick(() => { if (messagesElement.value) messagesElement.value.scrollTop = messagesElement.value.scrollHeight; });
}
function updateHistory(room) {
  messages.value = Array.isArray(room.chatHistory) ? room.chatHistory : [];
  scrollToLatest();
}
function sendMessage() {
  const content = draft.value.trim();
  if (!content || !client?.connected) return;
  client.publish({
    destination: `/app/room/${props.roomId}`,
    body: JSON.stringify({ chatHistory: [{ sender: { username: props.username }, content, timestamp: new Date().toISOString() }] }),
  });
  draft.value = '';
}
function connect() {
  client = new Client({ webSocketFactory: () => new SockJS(SOCKJS_URL), reconnectDelay: 3000 });
  client.onConnect = () => {
    connectionStatus.value = 'online';
    subscription = client.subscribe(`/topic/room/${props.roomId}`, frame => {
      try { updateHistory(JSON.parse(frame.body)); } catch (_) { /* ignore malformed messages */ }
    });
  };
  client.onWebSocketClose = () => { connectionStatus.value = 'offline'; };
  client.onStompError = () => { connectionStatus.value = 'offline'; };
  client.activate();
}

async function loadHistory() {
  try { updateHistory((await api.get(`/api/rooms/memory/${props.roomId}`)).data); } catch (_) { /* room updates will still populate history */ }
}

watch(messages, scrollToLatest, { deep: true });
onMounted(async () => { await loadHistory(); connect(); });
onBeforeUnmount(() => { subscription?.unsubscribe(); client?.deactivate(); });
</script>

<style scoped>
.chat-panel { display: grid; grid-template-rows: auto minmax(220px, 1fr) auto; gap: 14px; min-height: 510px; padding: 18px; background: linear-gradient(180deg, #121b32, #0c1427); }
.chat-panel__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.chat-panel__eyebrow { margin: 0; color: var(--primary-2); font-size: .68rem; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }.chat-panel h2 { margin: 4px 0 0; font-size: 1.15rem; }
.chat-panel__status { display: inline-flex; align-items: center; gap: 6px; color: var(--muted); font-size: .72rem; text-transform: capitalize; }.chat-panel__status i { width: 7px; height: 7px; border-radius: 50%; background: var(--danger); }.chat-panel__status.online { color: #bbf7d0; }.chat-panel__status.online i { background: var(--success); box-shadow: 0 0 8px var(--success); }
.chat-panel__messages { min-height: 0; overflow-y: auto; display: grid; align-content: start; gap: 10px; padding: 2px 4px 2px 0; }.chat-panel__empty { color: var(--muted); font-size: .85rem; text-align: center; margin: 24px 0; }
.chat-message { max-width: 92%; padding: 9px 10px; border: 1px solid var(--border); border-radius: 4px 12px 12px; background: rgba(7, 13, 28, .7); }.chat-message.mine { justify-self: end; border-color: rgba(107, 220, 255, .3); border-radius: 12px 4px 12px 12px; background: rgba(47, 93, 141, .28); }.chat-message__meta { display: flex; justify-content: space-between; gap: 10px; color: var(--primary-2); font-size: .72rem; }.chat-message time { color: var(--muted); }.chat-message p { margin: 5px 0 0; overflow-wrap: anywhere; font-size: .86rem; line-height: 1.4; }
.chat-panel__form { display: flex; gap: 8px; }.chat-panel__form .input { min-width: 0; padding: 9px 10px; }.chat-panel__form .btn { padding: 9px 11px; }
</style>
