import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  build: {
    outDir: 'dist',
    // Copiar archivos públicos (incluyendo imágenes) al dist
    copyPublicDir: true,
  },
  // Configuración para servir archivos estáticos desde public
  publicDir: 'public',
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});