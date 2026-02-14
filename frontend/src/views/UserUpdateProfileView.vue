<template>
  <MainLayout>
    <div class="update-profile-page">
      <div class="update-profile-container">
        <h2>Actualizar perfil</h2>
        
        <!-- Indicador de carga -->
        <div v-if="loading" class="loading-message">
          Cargando información del usuario...
        </div>
        
        <form v-else @submit.prevent="updateProfile">
          <div class="form-group">
            <label for="firstName">Nombre *</label>
            <input type="text" id="firstName" v-model="user.firstName" :disabled="!isEditing" required />
          </div>
          <div class="form-group">
            <label for="lastName">Apellidos *</label>
            <input type="text" id="lastName" v-model="user.lastName" :disabled="!isEditing" required />
          </div>
          <div class="form-group">
            <label for="email">Email *</label>
            <input type="email" id="email" v-model="user.email" :disabled="!isEditing" required />
          </div>
          <div class="form-group">
            <label for="phoneNumber">Número de teléfono *</label>
            <input type="text" id="phoneNumber" v-model="user.phoneNumber" :disabled="!isEditing" required />
          </div>
          
          <!-- Botón Editar (solo visible cuando no estamos editando) -->
          <button v-if="!isEditing" type="button" class="btn-primary" @click="enableEditing">
            Editar
          </button>
          
          <!-- Botones Guardar y Cancelar (solo visibles cuando estamos editando) -->
          <div v-if="isEditing" class="button-group">
            <button type="submit" class="btn-primary">Guardar cambios</button>
            <button type="button" class="btn-secondary" @click="cancelEditing">Cancelar</button>
          </div>
        </form>
        <div v-if="message" class="success-message">{{ message }}</div>
        <div v-if="error" class="error-message">{{ error }}</div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, onActivated } from 'vue';
import { useUserStore } from '../stores/user.store';
import { useAuthStore } from '../stores/auth.store';
import { UserDTO } from '../types/user.d';
import MainLayout from '../layouts/MainLayout.vue';

export default defineComponent({
  name: 'UpdateProfileView',
  components: {
    MainLayout,
  },
  setup() {
    const userStore = useUserStore();
    const authStore = useAuthStore();
    const user = ref<UserDTO>({
      userId: 0,
      username: '',
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
    });
    const originalUser = ref<UserDTO>({
      userId: 0,
      username: '',
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
    });
    const message = ref<string | null>(null);
    const error = ref<string | null>(null);
    const isEditing = ref<boolean>(false);
    const loading = ref<boolean>(false);

    const loadUserData = async () => {
      const currentUser = authStore.user.value;
      
      if (!currentUser || !currentUser.username) {
        console.error('No hay usuario autenticado');
        error.value = 'No hay usuario autenticado.';
        return;
      }
      
      try {
        loading.value = true;
        
        await authStore.loadUserProfile();
        
        if (authStore.user.value) {
          user.value = { ...authStore.user.value } as UserDTO;
          originalUser.value = { ...authStore.user.value } as UserDTO;
        } else {
          console.error('No se pudo cargar la información del usuario');
          error.value = 'No se pudo cargar la información del usuario.';
        }
      } catch (err: any) {
        console.error('Error al cargar información del usuario:', err);
        console.error('Status:', err.response?.status);
        console.error('Data:', err.response?.data);
        error.value = 'Error al cargar la información del usuario.';
      } finally {
        loading.value = false;
      }
    };

    onMounted(async () => {
      await loadUserData();
    });

    onActivated(async () => {
      await loadUserData();
    });

    const enableEditing = () => {
      isEditing.value = true;
      message.value = null;
      error.value = null;
    };

    const cancelEditing = async () => {
      // Al cancelar, recargar los datos actuales desde el backend
      isEditing.value = false;
      message.value = null;
      error.value = null;
      
      // Recargar datos frescos del backend
      await loadUserData();
    };

    const updateProfile = async () => {
      // Limpiar mensajes previos
      message.value = null;
      error.value = null;
      
      try {
        const userId = authStore.user.value?.userId;
        
        if (userId) {
          await userStore.updateUserInformation(userId, user.value);
          
          // Actualizar los datos con la respuesta del backend (desde el store)
          if (userStore.user.value) {
            user.value = { ...userStore.user.value };
            originalUser.value = { ...userStore.user.value };
            
            // Actualizar el store de autenticación con los nuevos datos
            authStore.user.value = { ...userStore.user.value };
            
            // Actualizar también el localStorage para persistir los cambios (usando la misma clave que auth.store.ts)
            localStorage.setItem('authUser', JSON.stringify(userStore.user.value));
          }
          
          message.value = 'Perfil actualizado correctamente.';
          error.value = null;
          isEditing.value = false; // Volver al modo de solo lectura
        } else {
          console.error('No se encontró el userId');
          error.value = 'No se ha encontrado el usuario autenticado.';
        }
      } catch (err) {
        console.error('Error al actualizar el perfil:', err);
        error.value = 'Error al actualizar el perfil.';
        message.value = null;
      }
    };

    return {
      user,
      message,
      error,
      loading,
      isEditing,
      enableEditing,
      cancelEditing,
      updateProfile,
    };
  },
});
</script>

<style scoped>
.update-profile-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  
  /* Configuración del fondo */
  background-image: url('@/assets/images/ActualizarPerfil.jpg');
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

.update-profile-container {
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

.update-profile-container h2 {
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

.form-group input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  color: #666;
}

.form-group input:focus {
  outline: none;
  border-color: #5564eb;
}

.button-group {
  display: flex;
  gap: 10px;
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

.button-group .btn-primary {
  flex: 1;
  width: auto;
}

.btn-primary:hover {
  background-color: #3d4fc9;
}

.btn-secondary {
  flex: 1;
  background-color: #6c757d;
  color: white;
  padding: 14px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: background-color 0.3s ease;
}

.btn-secondary:hover {
  background-color: #5a6268;
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

.loading-message {
  color: #5564eb;
  text-align: center;
  font-size: 16px;
  padding: 20px;
  font-weight: 500;
}

/* Responsive */
@media (max-width: 768px) {
  .update-profile-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .update-profile-container h2 {
    font-size: 24px;
  }
}
</style>
