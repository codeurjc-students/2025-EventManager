<template>
  <MainLayout>
    <div class="update-ticket-page">
      <div class="update-ticket-container">
        <h2>Modificar información de asistencia</h2>
        
        <!-- Indicador de carga -->
        <div v-if="loading" class="loading-message">
          Cargando información de la entrada...
        </div>
        
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <form v-else-if="ticket" @submit.prevent="onSave">
          <div class="form-group">
            <label for="role">Rol en el evento</label>
            <input id="role" type="text" :value="roleMapping[ticket.role] || ticket.role" disabled />
          </div>
          <div class="form-group">
            <label for="guest-number">Número de invitados</label>
            <input id="guest-number" type="number" v-model="ticket.guestNumber" :disabled="!isEditing" min="0" />
          </div>
          <div class="form-group">
            <label for="invitation-confirmation">Confirmación de invitación</label>
            <select id="invitation-confirmation" v-model="invitationConfirmationStatus" disabled>
              <option value="PENDIENTE">Pendiente</option>
              <option value="CONFIRMADA">Confirmada</option>
              <option value="RECHAZADA">Rechazada</option>
            </select>
          </div>
          <div class="form-group">
            <label for="assist-confirmation">Confirmación de asistencia</label>
            <select id="assist-confirmation" v-model="assistConfirmationStatus" :disabled="!isEditing">
              <option value="PENDIENTE">Pendiente</option>
              <option value="CONFIRMADA">Confirmada</option>
              <option value="RECHAZADA">Rechazada</option>
            </select>
          </div>
          <div class="form-group">
            <label for="ticket-notes">Notas</label>
            <textarea id="ticket-notes" v-model="ticket.notes" :disabled="!isEditing"></textarea>
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
import { useTicketStore } from '../stores/ticket.store';
import { useAuthStore } from '../stores/auth.store';
import { UserDTO } from '../types/user.d';
import MainLayout from '../layouts/MainLayout.vue';

export default defineComponent({
  name: 'TicketEventUpdateView',
  components: { MainLayout },
  setup() {
    const route = useRoute();
    const ticketStore = useTicketStore();
    const authStore = useAuthStore() as { user: { value: UserDTO | null } };
    
    const eventCode = route.params.eventCode as string;
    const ticketId = Number(route.params.ticketId);

    const loading = ref(false);
    const error = ref<string | null>(null);
    const successMessage = ref('');
    const isEditing = ref(false);
    const ticket = ref<any>(null);
    const originalTicket = ref<any>(null);
    
    // Estados para los campos booleanos
    const invitationConfirmationStatus = ref<string>('PENDIENTE');
    const assistConfirmationStatus = ref<string>('PENDIENTE');

    // Mapeo de roles para mostrar en castellano
    const roleMapping: Record<string, string> = {
      'HOST': 'Anfitrión',
      'GUEST': 'Invitado'
    };

    // Función para convertir booleano a estado UI
    const booleanToStatus = (value: boolean | null | undefined): string => {
      if (value === null || value === undefined) {
        return 'PENDIENTE';
      }
      return value ? 'CONFIRMADA' : 'RECHAZADA';
    };

    // Función para convertir estado UI a booleano
    const statusToBoolean = (status: string): boolean | null => {
      if (status === 'PENDIENTE') {
        return null;
      }
      return status === 'CONFIRMADA';
    };

    // Cargar datos del ticket
    const loadTicketData = async () => {
      loading.value = true;
      error.value = null;
      try {
        const userId = authStore.user.value?.userId;
        
        if (!userId) {
          error.value = 'No se ha encontrado el usuario autenticado.';
          return;
        }
        
        const response = await ticketStore.fetchTicketDetail(eventCode, ticketId, userId);
        
        if (response && response.ticket) {
          ticket.value = { ...response.ticket };
          originalTicket.value = { ...response.ticket };
          
          // Convertir valores booleanos a estados UI
          invitationConfirmationStatus.value = booleanToStatus(ticket.value.invitationConfirmation);
          assistConfirmationStatus.value = booleanToStatus(ticket.value.assistConfirmation);
        } else {
          error.value = 'No se pudo cargar la información de la entrada.';
        }
      } catch (err: any) {
        error.value = err.response?.data?.message || 'Error al cargar la entrada';
      } finally {
        loading.value = false;
      }
    };

    onMounted(async () => {
      await loadTicketData();
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
      await loadTicketData();
    };

    const onSave = async () => {
      loading.value = true;
      error.value = null;
      successMessage.value = '';
      try {
        const updateData = {
          role: ticket.value.role,
          guestNumber: ticket.value.guestNumber,
          invitationConfirmation: statusToBoolean(invitationConfirmationStatus.value),
          assistConfirmation: statusToBoolean(assistConfirmationStatus.value),
          notes: ticket.value.notes,
        };
        
        const response = await ticketStore.editTicket(eventCode, ticketId, updateData);
        
        if (response && response.ticket) {
          ticket.value = { ...response.ticket };
          originalTicket.value = { ...response.ticket };
          
          // Actualizar estados UI con los nuevos valores
          invitationConfirmationStatus.value = booleanToStatus(response.ticket.invitationConfirmation);
          assistConfirmationStatus.value = booleanToStatus(response.ticket.assistConfirmation);
          
          successMessage.value = 'Entrada actualizada correctamente.';
          isEditing.value = false;
        }
      } catch (err: any) {
        console.error('Error al actualizar el ticket:', err);
        error.value = err.response?.data?.message || 'Error al actualizar la entrada';
      } finally {
        loading.value = false;
      }
    };

    return {
      ticket,
      loading,
      error,
      isEditing,
      enableEdit,
      cancelEdit,
      onSave,
      successMessage,
      invitationConfirmationStatus,
      assistConfirmationStatus,
      roleMapping,
    };
  },
});
</script>

<style scoped>
.update-ticket-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  
  /* Configuración del fondo */
  background-image: url('@/assets/images/ActualizarEntrada.jpg');
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

.update-ticket-container {
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

.update-ticket-container h2 {
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
  font-size: 16px;
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
  .update-ticket-page {
    padding-top: 20px;
  }

  .update-ticket-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .update-ticket-container h2 {
    font-size: 24px;
  }
}
</style>