import { createRouter, createWebHistory } from 'vue-router';
import Login from '../components/Login.vue';
import Register from '../components/Register.vue';
import RoomList from '../components/RoomList.vue';

const routes = [
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/roomList', component: RoomList },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;