import { createApp } from 'vue';
import App from './App.vue';
import './style.css';
import { createRouter, createWebHistory } from 'vue-router';
import Login from './components/Login.vue';
import Register from './components/Register.vue';
import RoomList from './components/RoomList.vue';
import GameRoom from './components/GameRoom.vue';
import Home from './components/Home.vue';
import PanRoom from './components/PanRoom.vue';

const routes = [
    { path: '/', component: Home },
    { path: '/login', component: Login, alias: '/Login' },
    { path: '/register', component: Register, alias: '/Register' },
    { path: '/roomList', component: RoomList },
    { path: '/gameRoom/:id', name: 'gameRoom', component: GameRoom, props: true },
    { path: '/gameRoom', component: GameRoom },
    { path: '/panRoom/:id', name: 'panRoom', component: PanRoom, props: true },
    { path: '/panRoom', component: PanRoom },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

createApp(App).use(router).mount('#app');
