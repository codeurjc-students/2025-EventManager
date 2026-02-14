<template>
  <MainLayout>
    <div class="create-event-page">
      <div class="create-event-container">
        <h2>Crear nuevo evento</h2>
        
        <form @submit.prevent="createEvent">
          <div class="form-group">
            <label for="eventName">Nombre del evento *</label>
            <input type="text" id="eventName" v-model="eventName" required />
          </div>
          <div class="form-group">
            <label for="eventDate">Fecha del evento *</label>
            <input type="date" id="eventDate" v-model="eventDate" required />
          </div>
          <div class="form-group">
            <label for="eventTime">Hora del evento *</label>
            <input type="time" id="eventTime" v-model="eventTime" required />
          </div>
          <div class="form-group">
            <label for="eventDescription">Descripción del evento *</label>
            <textarea id="eventDescription" v-model="eventDescription" required rows="4"></textarea>
          </div>
          <div class="form-group">
            <label for="eventLocation">Ubicación del evento *</label>
            <input type="text" id="eventLocation" v-model="eventLocation" required />
          </div>

          <button type="submit" class="btn-primary" :disabled="!isFormValid">Crear evento</button>
        </form>
        
        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from 'vue';
import { useEventStore } from '../stores/event.store';
import { useAuthStore } from '../stores/auth.store';
import MainLayout from '../layouts/MainLayout.vue';

export default defineComponent({
  name: 'CreateEventView',
  components: {
    MainLayout,
  },
  setup() {
    const eventStore = useEventStore();
    const authStore = useAuthStore();
    const eventName = ref('');
    const eventDate = ref('');
    const eventTime = ref('');
    const eventDescription = ref('');
    const eventLocation = ref('');
    const successMessage = ref('');
    const errorMessage = ref('');

    // Computed property para validar que todos los campos tengan contenido
    const isFormValid = computed(() => {
      return eventName.value.trim() !== '' &&
             eventDate.value.trim() !== '' &&
             eventTime.value.trim() !== '' &&
             eventDescription.value.trim() !== '' &&
             eventLocation.value.trim() !== '';
    });

    const createEvent = async () => {
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
        
        // Formatear fecha y hora en formato ISO 8601 (YYYY-MM-DDTHH:mm:ss)
        const formattedDateTime = `${eventDate.value}T${eventTime.value}:00`;
        
        const createdEvent = await eventStore.addEvent(userId, {
          eventName: eventName.value,
          date: formattedDateTime,
          description: eventDescription.value,
          place: eventLocation.value,
          status: 'ACTIVO'
        });
        
        // Verificar si hubo error en el store
        if (eventStore.error.value) {
          // Si hay error, mostrar el mensaje de error
          errorMessage.value = eventStore.error.value;
          successMessage.value = '';
          // Mantener los datos introducidos por el usuario
        } else {
          // Si la creación fue exitosa, limpiar los campos y mostrar mensaje de éxito con el código del evento
          const eventCode = createdEvent?.eventCode || 'desconocido';
          successMessage.value = `El evento con con la etiqueta ${eventCode} se ha creado correctamente`;
          eventName.value = '';
          eventDate.value = '';
          eventTime.value = '';
          eventDescription.value = '';
          eventLocation.value = '';
          errorMessage.value = '';
          
          // NO redirigir automáticamente, dejar que el usuario vea el mensaje y decida cuándo salir
        }
      } catch (err: any) {
        console.error('Error al crear el evento:', err);
        
        // Extraer mensaje de error del backend si está disponible
        const error = err.response?.data?.message || err.response?.data?.error || 'Error al crear el evento. Inténtalo de nuevo.';
        errorMessage.value = error;
        successMessage.value = '';
        
        // Mantener los datos introducidos por el usuario (no limpiar los campos)
      }
    };

    return {
      eventName,
      eventDate,
      eventTime,
      eventDescription,
      eventLocation,
      successMessage,
      errorMessage,
      isFormValid,
      createEvent,
    };
  },
});
</script>

<style scoped>
.create-event-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  
  /* Configuración del fondo */
  background-image: url('@/assets/images/CrearEvento.jpg');
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

.create-event-container {
  max-width: 600px;
  width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  /* Altura automática para adaptarse al contenido */
  height: fit-content;
}

.create-event-container h2 {
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
  .create-event-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .create-event-container h2 {
    font-size: 24px;
  }
}
</style>