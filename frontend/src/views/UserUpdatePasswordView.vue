<template>
  <MainLayout>
    <div class="update-password-page">
      <div class="update-password-container">
        <h2>Cambiar contraseña</h2>
        
        <form @submit.prevent="updatePassword">
          <div class="form-group">
            <label for="currentPassword">Contraseña actual *</label>
            <input type="password" id="currentPassword" v-model="currentPassword" required />
          </div>
          <div class="form-group">
            <label for="newPassword">Nueva contraseña *</label>
            <input type="password" id="newPassword" v-model="newPassword" required />
          </div>
          <div class="form-group">
            <label for="confirmPassword">Confirmar nueva contraseña *</label>
            <input type="password" id="confirmPassword" v-model="confirmPassword" required />
          </div>
          
          <button type="submit" class="btn-primary" :disabled="!isFormValid">Actualizar contraseña</button>
        </form>
        
        <div v-if="message" class="success-message">{{ message }}</div>
        <div v-if="error" class="error-message">{{ error }}</div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from 'vue';
import { useUserStore } from '../stores/user.store';
import { useAuthStore } from '../stores/auth.store';
import { UserDTO } from '../types/user.d';
import MainLayout from '../layouts/MainLayout.vue';

export default defineComponent({
  name: 'UpdatePasswordView',
  components: {
    MainLayout,
  },
  setup() {
    const userStore = useUserStore();
    const authStore = useAuthStore() as { user: { value: UserDTO | null } };
    const currentPassword = ref('');
    const newPassword = ref('');
    const confirmPassword = ref('');
    const message = ref<string | null>(null);
    const error = ref<string | null>(null);

    // Computed property para validar que los 3 campos tengan contenido
    const isFormValid = computed(() => {
      return currentPassword.value.trim() !== '' &&
             newPassword.value.trim() !== '' &&
             confirmPassword.value.trim() !== '';
    });

    const updatePassword = async () => {
      // Limpiar mensajes previos
      message.value = null;
      error.value = null;
      
      // Validar que las contraseñas coincidan
      if (newPassword.value !== confirmPassword.value) {
        error.value = 'Las contraseñas no coinciden.';
        return;
      }
      
      try {
        const userId = authStore.user.value?.userId;
        if (userId === undefined) {
          error.value = 'Usuario no identificado. Por favor inicia sesión de nuevo.';
          return;
        }
        
        const passwordData = {
          password: currentPassword.value,
          newPassword: newPassword.value,
          newPasswordConfirm: confirmPassword.value
        };
        
        await userStore.updateUserPassword(userId, passwordData);
        
        // Verificar si hubo error en el store
        if (userStore.error.value) {
          error.value = userStore.error.value;
          message.value = null;
        } else {
          message.value = 'Contraseña actualizada correctamente.';
          currentPassword.value = '';
          newPassword.value = '';
          confirmPassword.value = '';
          error.value = null;
        }
      } catch (err: any) {
        console.error('Error al actualizar la contraseña:', err);
        
        // Extraer mensaje de error del backend si está disponible
        const errorMessage = err.response?.data?.message || err.response?.data?.error || 'Error al actualizar la contraseña. Inténtalo de nuevo.';
        error.value = errorMessage;
        message.value = null;
        
        // Mantener los datos introducidos por el usuario (no limpiar los campos)
      }
    };

    return {
      currentPassword,
      newPassword,
      confirmPassword,
      message,
      error,
      isFormValid,
      updatePassword,
    };
  },
});
</script>

<style scoped>
.update-password-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  
  /* Configuración del fondo */
  background-image: url('@/assets/images/ActualizarClave.jpg');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  
  /* Posicionamiento relativo con padding superior de 4cm */
  padding: 4cm 20px 20px 20px;
  
  /* Centrado horizontal */
  display: flex;
  justify-content: center;
  
  /* Evita desbordamiento */
  overflow: auto;
}

.update-password-container {
  max-width: 500px;
  width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  /* Altura automática para adaptarse al contenido */
  height: fit-content;
}

.update-password-container h2 {
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
  box-sizing: border-box;
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

.btn-primary:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
  opacity: 0.6;
}

.success-message {
  color: green;
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.error-message {
  color: red;
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .update-password-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .update-password-container h2 {
    font-size: 24px;
  }
}
</style>