<template>
  <MainLayout>
    <div class="register-page">
      <div class="register-container">
        <h2>Crear Cuenta</h2>
        <form @submit.prevent="registerUser">
          <div class="form-group">
            <label for="email">Email *</label>
            <input type="email" v-model="user.email" required />
          </div>
          <div class="form-group">
            <label for="username">Usuario *</label>
            <input type="text" v-model="user.username" required />
          </div>
          <div class="form-group">
            <label for="password">Contraseña *</label>
            <input type="password" v-model="user.password" required />
          </div>
          <div class="form-group">
            <label for="firstName">Nombre *</label>
            <input type="text" v-model="user.firstName" required />
          </div>
          <div class="form-group">
            <label for="lastName">Apellido *</label>
            <input type="text" v-model="user.lastName" required />
          </div>
          <div class="form-group">
            <label for="phoneNumber">Número de Teléfono *</label>
            <input type="text" v-model="user.phoneNumber" required placeholder="+34650123456" />
          </div>
          <button type="submit" class="btn-primary">Registrar</button>
        </form>
        <div v-if="auth.error" class="error-message">{{ auth.error }}</div>
        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
        
        <div class="divider"></div>
        
        <router-link to="/iniciar-sesion" class="btn-secondary">
          Ya tengo una cuenta
        </router-link>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';

const user = ref({
  email: '',
  username: '',
  password: '',
  firstName: '',
  lastName: '',
  phoneNumber: ''
});
const successMessage = ref('');
const router = useRouter();
const auth = useAuthStore();

const registerUser = async () => {
  const success = await auth.register(user.value);
  if (success) {
    successMessage.value = 'Cuenta creada correctamente. Redirigiendo a la página principal...';
    setTimeout(() => {
      router.push('/');
    }, 2000);
  }
};
</script>

<style scoped>
.register-page {
  /* Ocupa toda la altura de la ventana (sin header) */
  min-height: 100vh;
  height: 100vh;
  
  /* Configuración del fondo para que se adapte perfectamente */
  background-image: url('@/assets/images/Registro.jpg');
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

.register-container {
  max-width: 450px;
  width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.register-container h2 {
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