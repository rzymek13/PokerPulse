<template>
  <section class="panel players-panel">
    <div class="players-panel__header">
      <div>
        <h2>Gracze</h2>
        <p class="subtitle">{{ players.length }} osób w pokoju</p>
      </div>
      <div class="label" v-if="currentPlayerUsername">
        Tura: <strong>{{ currentPlayerUsername }}</strong>
      </div>
    </div>

    <ul class="players-list" v-if="players.length">
      <li
        v-for="player in players"
        :key="player.playerId"
        class="player-seat"
        :class="{ current: player.currentTurn, me: player.playerId === myPlayerId }"
      >
        <div class="player-seat__avatar" aria-hidden="true">{{ playerInitial(player.username) }}</div>
        <div class="player-seat__identity">
          <strong>{{ player.username }}</strong>
          <span class="player-seat__you" v-if="player.playerId === myPlayerId">Ty</span>
        </div>
        <div class="player-seat__status">
          <span class="player-seat__turn" v-if="player.currentTurn"><i></i> Ruch</span>
          <span class="player-seat__cards" v-else>{{ player.handSize }} kart</span>
        </div>
      </li>
    </ul>

    <p v-else class="subtitle">Brak graczy w pokoju.</p>
  </section>
</template>

<script setup>
defineProps({
  players: {
    type: Array,
    default: () => [],
  },
  myPlayerId: {
    type: [String, Number, null],
    default: null,
  },
  currentPlayerUsername: {
    type: String,
    default: '',
  },
});

function playerInitial(username) {
  return (username || '?').trim().charAt(0).toUpperCase();
}
</script>

<style scoped>
.players-panel { padding: 0; border: 0; background: transparent; box-shadow: none; }
.players-panel__header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.players-panel__header h2 { margin: 0; font-size: 1rem; }.players-panel__header .subtitle { margin: 2px 0 0; }
.players-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 8px; }
.player-seat { display: grid; grid-template-columns: 34px minmax(0, 1fr) auto; align-items: center; gap: 9px; min-width: 0; padding: 9px 10px; border: 1px solid rgba(148, 163, 184, .2); border-radius: 12px; background: rgba(7, 13, 28, .42); transition: border-color .18s ease, transform .18s ease, background .18s ease; }
.player-seat:hover { transform: translateY(-1px); border-color: rgba(107, 220, 255, .46); }
.player-seat__avatar { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 10px; background: linear-gradient(135deg, #315a8b, #1f365c); color: #dff5ff; font-size: .84rem; font-weight: 800; }
.player-seat__identity { display: flex; align-items: center; gap: 6px; min-width: 0; }.player-seat__identity strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: .86rem; }.player-seat__you { padding: 2px 5px; border-radius: 5px; background: rgba(107, 220, 255, .15); color: #8ee8ff; font-size: .65rem; font-weight: 800; text-transform: uppercase; }
.player-seat__status { justify-self: end; font-size: .7rem; white-space: nowrap; }.player-seat__cards { color: var(--muted); }.player-seat__turn { display: inline-flex; align-items: center; gap: 5px; color: #bbf7d0; font-weight: 700; }.player-seat__turn i { width: 6px; height: 6px; border-radius: 50%; background: var(--success); box-shadow: 0 0 8px var(--success); }
.player-seat.current { border-color: rgba(126, 224, 192, .78); background: rgba(16, 100, 77, .24); box-shadow: inset 0 0 0 1px rgba(126, 224, 192, .12); }.player-seat.current .player-seat__avatar { background: linear-gradient(135deg, #168467, #0c5a4b); }.player-seat.me { border-color: rgba(107, 220, 255, .54); }
</style>
