<template>
  <div class="container">
    <div class="panel" style="max-width:520px; margin: 40px auto;">
      <h1>Logowanie</h1>
      <p class="subtitle">Wróć do lobby i dołącz do pokoju</p>
      <form class="form" @submit.prevent="login">
        <div class="field">
          <label class="label" for="username">Nazwa użytkownika</label>
          <input class="input" type="text" id="username" v-model="username" required />
        </div>
        <div class="field">
          <label class="label" for="password">Hasło</label>
          <input class="input" type="password" id="password" v-model="password" required />
        </div>
        <div class="grid" style="grid-template-columns: 1fr auto; align-items:center;">
          <small class="label">Nie masz konta? <router-link to="/register">Zarejestruj się</router-link></small>
          <button class="btn btn-primary" type="submit">Zaloguj</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import api from '../api';

export default {
  data() {
    return {
      username: '',
      password: '',
    };
  },
  methods: {
    async login() {
      try {
        const response = await api.post('/api/auth/login', {
          username: this.username,
          password: this.password,
        });
        alert(response.data);
        // Użyj sessionStorage, aby każdy tab miał własnego użytkownika
        sessionStorage.setItem('username', this.username);
        if (response?.data?.token) {
          sessionStorage.setItem('jwt', response.data.token);
        } else if (response?.data?.jwt) {
          sessionStorage.setItem('jwt', response.data.jwt);
        }
        this.$router.push('/RoomList'); 
      } catch (error) {
        alert('Błąd logowania: ' + error.response.data.message);
      }
    },
  },
};
</script>