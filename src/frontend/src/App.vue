<script setup>
import Login from './components/Login.vue';
import Register from './components/Register.vue';
</script>

<template>
  <div id="app">
    <nav class="nav">
      <div class="links">
        <router-link to="/login" data-testid="home-LoginPage">Logowanie</router-link>
        <router-link to="/register" data-testid="home-registerPage">Rejestracja</router-link>
        <router-link to="/roomList">Lobby</router-link>
      </div>
      <div style="opacity:.8">PokerPulse</div>
    </nav>

    <div class="container">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
/**** page-level tweaks if needed ****/
</style>
