<template>
  <MainLayout>
    <div class="update-event-page">
      <div class="update-event-container">
        <h2>Modificar información del evento</h2>
        
        <!-- Indicador de carga -->
        <div v-if="loading" class="loading-message">
          Cargando información del evento...
        </div>
        
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <form v-else-if="event" @submit.prevent="onSave">
        <div class="form-group">
          <label for="event-code">Código del evento</label>
          <input id="event-code" type="text" v-model="event.eventCode" disabled />
        </div>
        <div class="form-group">
          <label for="event-name">Nombre *</label>
          <input id="event-name" type="text" v-model="event.eventName" :disabled="!isEditing" required />
        </div>
        <div class="form-group">
          <label for="event-description">Descripción</label>
          <textarea id="event-description" v-model="event.description" :disabled="!isEditing"></textarea>
        </div>
        <div class="form-group">
          <label for="event-place">Lugar *</label>
          <input id="event-place" type="text" v-model="event.place" :disabled="!isEditing" required />
        </div>
        <div class="form-group">
          <label for="event-date">Fecha y hora *</label>
          <input id="event-date" type="datetime-local" v-model="event.date" :disabled="!isEditing" required />
        </div>
        <div class="form-group">
          <label for="event-status">Estado *</label>
          <select id="event-status" v-model="event.status" :disabled="!isEditing" required>
            <option value="ACTIVO">Activo</option>
            <option value="PENDIENTE">Pendiente</option>
            <option value="CANCELADO">Cancelado</option>
            <option value="FINALIZADO">Finalizado</option>
          </select>
        </div>
        
        <!-- Botón Editar (solo visible cuando no estamos editando) -->
        <button v-if="!isEditing" type="button" class="btn-primary" @click="enableEdit">
          Editar
        </button>
        
        <!-- Botones Guardar y Cancelar (solo visibles cuando estamos editando) -->
        <div v-if="isEditing" class="button-group">
          <button type="submit" class="btn-primary">Guardar cambios</button>
          <button type="button" class="btn-secondary" @click="cancelEdit">Cancelar</button>
        </div>
      </form>
      
      <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useEventStore } from '../stores/event.store';
import MainLayout from '../layouts/MainLayout.vue';

export default defineComponent({
  name: 'EventUpdateView',
  components: { MainLayout },
  setup() {
    const route = useRoute();
    const eventStore = useEventStore();

    const loading = ref(false);
    const error = ref<string | null>(null);
    const successMessage = ref('');
    const isEditing = ref(false);
    
    const eventData = globalThis.history.state?.event;
    const event = ref<any>(eventData ? { ...eventData } : null);
    const originalEvent = ref<any>(eventData ? { ...eventData } : null);
    const eventCode = route.params.eventCode as string;

    // Cargar datos del evento si no están disponibles en el state
    const loadEventData = async () => {
      if (!event.value && eventCode) {
        loading.value = true;
        error.value = null;
        try {
          const eventDetails = await eventStore.fetchEventByCode(eventCode);
          if (eventDetails) {
            event.value = { ...eventDetails };
            originalEvent.value = { ...eventDetails };
          } else {
            error.value = 'No se pudo cargar la información del evento.';
          }
        } catch (err: any) {
          error.value = err.response?.data?.message || 'Error al cargar el evento';
        } finally {
          loading.value = false;
        }
      }
    };

    onMounted(async () => {
      await loadEventData();
    });

    const enableEdit = () => {
      isEditing.value = true;
      error.value = null;
      successMessage.value = '';
    };

    const cancelEdit = async () => {
      isEditing.value = false;
      error.value = null;
      successMessage.value = '';
      
      // Recargar datos del backend
      await loadEventData();
    };

    const onSave = async () => {
      loading.value = true;
      error.value = null;
      successMessage.value = '';
      try {
        const updatedEvent = await eventStore.editEvent(event.value.eventCode, event.value);
        if (updatedEvent && typeof updatedEvent === 'object') {
          event.value = { ...updatedEvent };
          originalEvent.value = { ...updatedEvent };
        }
        successMessage.value = 'Evento actualizado correctamente.';
        isEditing.value = false;
      } catch (err: any) {
        error.value = err.response?.data?.message || 'Error al actualizar el evento';
      } finally {
        loading.value = false;
      }
    };

    return {
      event,
      loading,
      error,
      isEditing,
      enableEdit,
      cancelEdit,
      onSave,
      successMessage,
    };
  },
});
</script>

<style scoped>
.update-event-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  
  /* Configuración del fondo */
  background-image: url('@/assets/images/ActualizarEvento.jpg');
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

.update-event-container {
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

.update-event-container h2 {
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
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.form-group input:disabled,
.form-group textarea:disabled,
.form-group select:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  color: #666;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #5564eb;
}

.form-group textarea {
  min-height: 100px;
  resize: vertical;
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
  color: #15803d;
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  background: #dcfce7;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #86efac;
}

.error-message {
  color: #b91c1c;
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  background: #fee2e2;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #fca5a5;
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
  .update-event-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .update-event-container h2 {
    font-size: 24px;
  }
}
</style>