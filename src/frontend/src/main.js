import { createApp } from 'vue';
import App from './App.vue';
import { createRouter, createWebHistory } from 'vue-router';
import Login from './components/Login.vue';
import Register from './components/Register.vue';
import RoomList from './components/RoomList.vue';
import GameRoom from './components/GameRoom.vue';

const routes = [
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/roomList', component: RoomList },
    { path: '/gameRoom', component: GameRoom }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

createApp(App).use(router).mount('#app');