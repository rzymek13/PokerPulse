<script setup>
import Login from './components/Login.vue';
import Register from './components/Register.vue';
</script>

<template>
  <div>
    <router-link to="/login">Logowanie</router-link>
    <router-link to="/register">Rejestracja</router-link>
    <router-view></router-view>
  </div>

</template>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
