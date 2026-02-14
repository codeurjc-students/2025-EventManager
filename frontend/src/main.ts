import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth.store';

const app = createApp(App);

// Validar sesión al iniciar la aplicación
const initApp = async () => {
  const authStore = useAuthStore();
  const hasLocalAuth = !!localStorage.getItem('authUser');

  if (hasLocalAuth) {
    try {
      const isValid = await authStore.validateSession();
      if (isValid) {
        console.log('Sesión válida al iniciar la aplicación');
      } else {
        console.log('Sesión inválida al iniciar la aplicación');
      }
    } catch (err) {
      console.error('Error al validar sesión inicial:', err);
      await authStore.forceLogout();
    }
  } else {
    console.log('No hay usuario en localStorage al iniciar');
  }

  // Montar la aplicación
  app.use(router).mount('#app');
};

// Inicializar aplicación
initApp();
