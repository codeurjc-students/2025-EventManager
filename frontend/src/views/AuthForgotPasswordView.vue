<template>
  <MainLayout>
    <div class="forgot-password-page">
      <div class="forgot-password-container">
        <h2>¿Ha olvidado su contraseña?</h2>
        <form @submit.prevent="submitForm">
          <div class="form-group">
            <label for="email">Email *</label>
            <input
              type="email"
              id="email"
              v-model="form.email"
              required
            />
          </div>
          <div class="form-group">
            <label for="username">Usuario *</label>
            <input
              type="text"
              id="username"
              v-model="form.username"
              required
            />
          </div>
          <div class="form-group">
            <label for="newPassword">Nueva contraseña *</label>
            <input
              type="password"
              id="newPassword"
              v-model="form.newPassword"
              required
            />
          </div>
          <div class="form-group">
            <label for="newPasswordConfirm">Confirmar nueva contraseña *</label>
            <input
              type="password"
              id="newPasswordConfirm"
              v-model="form.newPasswordConfirm"
              required
            />
          </div>
          <button type="submit" class="btn-primary">Cambiar contraseña</button>
        </form>
        <div v-if="auth.error" class="error-message">{{ auth.error }}</div>
        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
        
        <div class="divider"></div>
        
        <router-link to="/iniciar-sesion" class="btn-secondary">
          Volver al inicio de sesión
        </router-link>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth.store';

const form = ref({
  email: '',
  username: '',
  newPassword: '',
  newPasswordConfirm: ''
});
const successMessage = ref('');
const auth = useAuthStore();

const submitForm = async () => {
  successMessage.value = '';
  auth.clearError();

  const success = await auth.forgotPassword({ ...form.value });
  if (success) {
    successMessage.value = 'Contraseña cambiada correctamente. Ya puede iniciar sesión con la nueva contraseña.';
    form.value.email = '';
    form.value.username = '';
    form.value.newPassword = '';
    form.value.newPasswordConfirm = '';
  }
};
</script>

<style scoped>
.forgot-password-page {
  /* Ocupa toda la altura de la ventana (sin header) */
  min-height: 100vh;
  height: 100vh;
  
  /* Configuración del fondo para que se adapte perfectamente */
  background-image: url('@/assets/images/CambiarClave.jpg');
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

.forgot-password-container {
  max-width: 450px;
  width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.forgot-password-container h2 {
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

.success-message {
  color: green;
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
</style>