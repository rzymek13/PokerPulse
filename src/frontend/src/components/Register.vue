<template>
  <div>
    <h1>Rejestracja</h1>
    <form @submit.prevent="register">
      <div>
        <label for="username">Nazwa użytkownika:</label>
        <input type="text" id="username" v-model="username" required data-testid="register-username" />
      </div>
      <div>
        <label for="password">Hasło:</label>
        <input type="password" id="password" v-model="password" required data-testid="register-password" />
      </div>
      <button type="submit" data-testid="register-submitButton">Zarejestruj</button>
    </form>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      username: '',
      password: '',
    };
  },
  methods: {
    async register() {
      try {
        const response = await axios.post('/api/auth/register', {
          username: this.username,
          password: this.password,
        });
        alert(response.data);
        const login = await axios.post('/api/auth/login', {
          username: this.username,
          password: this.password,
        });
        // Zapamiętaj username (i ewentualnie token, jeśli backend go zwraca) — per tab
        sessionStorage.setItem('username', this.username);
        if (login?.data?.token) {
          sessionStorage.setItem('jwt', login.data.token);
        } else if (login?.data?.jwt) {
          sessionStorage.setItem('jwt', login.data.jwt);
        }
        this.$router.push('/RoomList'); 
      } catch (error) {
        alert('Błąd rejestracji: ' + error.response.data.message);
      }
    },
  },
};
</script>