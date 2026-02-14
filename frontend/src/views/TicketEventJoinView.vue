<template>
  <MainLayout>
    <div class="join-event-page">
      <div class="join-event-container">
        <h2>Unirse a un evento</h2>
        
        <form @submit.prevent="joinEvent">
          <div class="form-group">
            <label for="eventCode">Código del evento *</label>
            <input 
              type="text" 
              id="eventCode" 
              v-model="eventCode" 
              required 
              maxlength="6"
              placeholder="Ej: ABC123"
            />
          </div>

          <div class="form-group">
            <label for="guestNumber">Número de invitados</label>
            <input 
              type="number" 
              id="guestNumber" 
              v-model.number="guestNumber" 
              min="1"
              placeholder="1"
            />
            <small class="field-hint">Indica el número total de personas que asistirán (incluyéndote)</small>
          </div>

          <div class="form-group">
            <label for="notes">Notas adicionales</label>
            <textarea 
              id="notes" 
              v-model="notes" 
              rows="4"
              maxlength="500"
              placeholder="Alergias alimentarias, necesidades especiales, etc."
            ></textarea>
            <small class="field-hint">Opcional - Máximo 500 caracteres</small>
          </div>
          
          <button type="submit" class="btn-primary">Unirse al evento</button>
        </form>
        
        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
import { useTicketStore } from '@/stores/ticket.store';
import { useAuthStore } from '@/stores/auth.store';
import MainLayout from '../layouts/MainLayout.vue';

export default defineComponent({
  name: 'JoinEventView',
  components: {
    MainLayout,
  },
  setup() {
    const ticketStore = useTicketStore();
    const authStore = useAuthStore();
    const eventCode = ref('');
    const guestNumber = ref<number | null>(null);
    const notes = ref('');
    const successMessage = ref('');
    const errorMessage = ref('');

    const joinEvent = async () => {
      // Limpiar mensajes previos
      successMessage.value = '';
      errorMessage.value = '';

      try {
        // Verificar si tenemos el userId, si no, cargar la información completa del usuario
        let userId = authStore.user.value?.userId;
        
        if (!userId) {
          try {
            await authStore.loadUserProfile();
            userId = authStore.user.value?.userId;
          } catch (err) {
            console.error('Error al cargar el perfil del usuario:', err);
            errorMessage.value = 'No se pudo obtener la información del usuario. Por favor, inicia sesión de nuevo.';
            return;
          }
        }
        
        if (userId === undefined) {
          errorMessage.value = 'Usuario no identificado. Por favor inicia sesión de nuevo.';
          return;
        }
        
        const enrollData = {
          eventCode: eventCode.value,
          userId: userId,
          role: 'GUEST', // Por defecto siempre es GUEST
          guestNumber: guestNumber.value || undefined, // Solo enviar si tiene valor
          notes: notes.value.trim() || undefined, // Solo enviar si tiene valor
        };
        
        await ticketStore.enrollUser(enrollData);
        
        // Verificar si hubo error en el store
        if (ticketStore.error.value) {
          // Si hay error, mostrar el mensaje de error
          errorMessage.value = ticketStore.error.value;
          successMessage.value = '';
          // Mantener los datos introducidos por el usuario
        } else {
          // Si la operación fue exitosa, limpiar los campos y mostrar mensaje de éxito
          successMessage.value = 'Te has unido al evento correctamente';
          eventCode.value = '';
          guestNumber.value = null;
          notes.value = '';
          errorMessage.value = '';
        }
      } catch (err: any) {
        console.error('Error al unirse al evento:', err);
        
        // Extraer mensaje de error del backend si está disponible
        const error = err.response?.data?.message || err.response?.data?.error || 'Error al unirse al evento. Por favor verifica el código e inténtalo de nuevo.';
        errorMessage.value = error;
        successMessage.value = '';
        
        // Mantener los datos introducidos por el usuario (no limpiar los campos)
      }
    };
    
    return {
      eventCode,
      guestNumber,
      notes,
      joinEvent,
      successMessage,
      errorMessage,
    };
  },
});
</script>

<style scoped>
.join-event-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  
  /* Configuración del fondo */
  background-image: url('@/assets/images/UnirseAUnEvento.jpg');
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

.join-event-container {
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

.join-event-container h2 {
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

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #5564eb;
}

.field-hint {
  display: block;
  margin-top: 6px;
  color: #666;
  font-size: 13px;
  font-style: italic;
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
  margin-top: 10px;
}

.btn-primary:hover {
  background-color: #3d4fc9;
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
  .join-event-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .join-event-container h2 {
    font-size: 24px;
  }
}
</style>
