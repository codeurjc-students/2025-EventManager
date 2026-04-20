<template>
  <MainLayout>
    <div class="login-page">
      <div class="login-container">
        <h2>Iniciar sesión</h2>
        <form @submit.prevent="loginUser">
          <div class="form-group">
            <label for="username">Usuario *</label>
            <input type="text" v-model="username" required />
          </div>
          <div class="form-group">
            <label for="password">Contraseña *</label>
            <input type="password" v-model="password" required />
          </div>
          <button type="submit" class="btn-primary">Entrar</button>
        </form>
        <div v-if="auth.error" class="error-message">{{ auth.error }}</div>
        
        <div class="divider"></div>
        
        <router-link to="/registro" class="btn-secondary">
          Crear una cuenta
        </router-link>
        
        <div class="forgot-password-link">
          <router-link to="/clave-olvidada">¿Olvidaste tu contraseña?</router-link>
        </div>
      </div>
    </div>

    <div v-if="showSessionExpired" class="popup-overlay" @click.self="closeSessionExpired">
      <div class="popup-content">
        <div class="popup-header">
          <h3>Sesión expirada</h3>
          <button class="close-button" @click="closeSessionExpired">✕</button>
        </div>
        <p class="popup-text">¡Vaya! Parece que su sesión ha expirado. Por favor vuelva a iniciar sesión.</p>
        <div class="popup-actions">
          <button type="button" class="btn-primary popup-btn" @click="closeSessionExpired">Entendido</button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';

const username = ref('');
const password = ref('');
const router = useRouter();
const auth = useAuthStore();
const showSessionExpired = ref(false);

const loginUser = async () => {
  const success = await auth.login(username.value, password.value);
  if (success) {
    // Redirigir a la página principal después de un login exitoso
    sessionStorage.removeItem('sessionExpired');
    router.push('/');
  }
};

const closeSessionExpired = () => {
  showSessionExpired.value = false;
  sessionStorage.removeItem('sessionExpired');
};

onMounted(() => {
  if (sessionStorage.getItem('sessionExpired') === 'true') {
    showSessionExpired.value = true;
  }
});
</script>

<style scoped>
.login-page {
  /* Ocupa toda la altura de la ventana (sin header) */
  min-height: 100vh;
  height: 100vh;
  
  /* Configuración del fondo para que se adapte perfectamente */
  background-image: url('@/assets/images/InicioSesion.jpg');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  
  /* Centrado del contenido */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  
  /* Evita desbordamiento */
  overflow: auto;
}

.login-container {
  max-width: 450px;
  width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.login-container h2 {
  margin-top: 0;
  margin-bottom: 30px;
  color: #333;
  text-align: center;
  font-size: 28px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  font-family: inherit;
}

.form-group input:focus {
  outline: none;
  border-color: #5564eb;
}

.btn-primary {
  width: 100%;
  background-color: #5564eb;
  color: white;
  padding: 14px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: background-color 0.3s ease;
}

.btn-primary:hover {
  background-color: #3d4fc9;
}

.error-message {
  color: red;
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
}

.divider {
  height: 1cm;
}

.btn-secondary {
  display: block;
  width: 100%;
  background-color: #5564eb;
  color: white;
  padding: 14px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  transition: background-color 0.3s ease;
}

.btn-secondary:hover {
  background-color: #3d4fc9;
}

.forgot-password-link {
  margin-top: 20px;
  text-align: center;
}

.forgot-password-link a {
  color: #5564eb;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.forgot-password-link a:hover {
  color: #3d4fc9;
  text-decoration: underline;
}

.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.popup-content {
  background: white;
  border-radius: 12px;
  padding: 30px;
  max-width: 520px;
  width: 90%;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.popup-header h3 {
  margin: 0;
  color: #333;
  font-size: 22px;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.close-button:hover {
  color: #333;
}

.popup-text {
  margin: 10px 0 20px 0;
  color: #333;
  text-align: center;
  font-size: 15px;
}

.popup-actions {
  display: flex;
  justify-content: center;
}

.popup-btn {
  width: auto;
  min-width: 140px;
}
</style>