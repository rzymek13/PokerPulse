<template>
  <div class="container">
    <div class="panel" style="max-width:520px; margin: 40px auto;">
      <h1>Rejestracja</h1>
      <p class="subtitle">Utwórz konto i przejdź do lobby</p>
      <form class="form" @submit.prevent="register">
        <div class="field">
          <label class="label" for="username">Nazwa użytkownika</label>
          <input class="input" type="text" id="username" v-model="username" required data-testid="register-username" />
        </div>
        <div class="field">
          <label class="label" for="password">Hasło</label>
          <input class="input" type="password" id="password" v-model="password" required data-testid="register-password" />
        </div>
        <div class="grid" style="grid-template-columns: 1fr auto; align-items:center;">
          <small class="label">Masz już konto? <router-link to="/login">Zaloguj się</router-link></small>
          <button class="btn btn-primary" type="submit" data-testid="register-submitButton">Zarejestruj</button>
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
    async register() {
      try {
        const response = await api.post('/api/auth/register', {
          username: this.username,
          password: this.password,
        });
        alert(response.data);
        const login = await api.post('/api/auth/login', {
          username: this.username,
          password: this.password,
        });
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