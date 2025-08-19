<template>
  <div>
    <h1>Logowanie</h1>
    <form @submit.prevent="login">
      <div>
        <label for="username">Nazwa użytkownika:</label>
        <input type="text" id="username" v-model="username" required />
      </div>
      <div>
        <label for="password">Hasło:</label>
        <input type="password" id="password" v-model="password" required />
      </div>
      <button type="submit">Zaloguj</button>
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
    async login() {
      try {
        const response = await axios.post('/api/auth/login', {
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