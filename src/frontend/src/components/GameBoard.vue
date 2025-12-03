<template>
  <div class="game-board panel">
    <!-- Top player (opponent) -->
    <div class="player-section top-player" v-if="topPlayer">
      <div class="player-info">{{ topPlayer.username }}</div>
      <div class="player-cards">
        <div class="card hidden-card">🂠</div>
        <div class="card hidden-card">🂠</div>
      </div>
    </div>

    <!-- Community cards (center) -->
    <div class="community-section" v-if="communityCards && communityCards.length > 0">
      <div class="section-label">Community Cards</div>
      <div class="cards-row">
        <div class="card" v-for="(card, idx) in communityCards" :key="idx">
          {{ formatCard(card) }}
        </div>
      </div>
    </div>

    <!-- Bottom player (current user) -->
    <div class="player-section bottom-player" v-if="bottomPlayer">
      <div class="player-info">{{ bottomPlayer.username }} (You)</div>
      <div class="player-cards">
        <div class="card" v-for="(card, idx) in myCards" :key="idx">
          {{ formatCard(card) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

// Props: players list and my private cards
const props = defineProps({
  players: Array,
  myPrivateCards: Array,
  username: String,
  communityCards: Array,
});

// Computed: determine top and bottom players
const topPlayer = computed(() => {
  // Return the first player that's not the current user
  return props.players?.find(p => p.username !== props.username) || null;
});

const bottomPlayer = computed(() => {
  // Return the current user
  return props.players?.find(p => p.username === props.username) || null;
});

const myCards = computed(() => props.myPrivateCards || []);

function formatCard(card) {
  if (!card) return '?';
  return `${card.rank?.symbol || card.rank || '?'}${card.suit?.symbol || card.suit || '?'}`;
}
</script>

<style scoped>
.game-board {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  min-height: 400px;
  padding: 24px;
  gap: 24px;
}

.player-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.top-player { order: 1; }
.community-section { order: 2; text-align: center; }
.bottom-player { order: 3; }

.player-info {
  font-weight: bold;
  color: var(--primary-2);
  font-size: 14px;
}

.player-cards,
.cards-row {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.card {
  width: 60px;
  height: 90px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: linear-gradient(135deg, #1a3a52, #0f2540);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: var(--text);
  font-size: 13px;
  text-align: center;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.card.hidden-card {
  background: linear-gradient(135deg, #2a1a3a, #1a0f2a);
  color: var(--muted);
  font-size: 24px;
}

.section-label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 8px;
  text-transform: uppercase;
}
</style>
