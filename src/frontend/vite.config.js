import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  build: {
    outDir: 'dist',
    assetsDir: '',
  },
  define: {
    global: 'window', // Definiuje global jako window
  },
  server: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
      //   '/poker': {
      //     target: 'ws://localhost:8080',
      //     ws: true,
      //   },
    }
  }
});