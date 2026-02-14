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
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';

const username = ref('');
const password = ref('');
const router = useRouter();
const auth = useAuthStore();

const loginUser = async () => {
  const success = await auth.login(username.value, password.value);
  if (success) {
    // Redirigir a la página principal después de un login exitoso
    router.push('/');
  }
};
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
</style>