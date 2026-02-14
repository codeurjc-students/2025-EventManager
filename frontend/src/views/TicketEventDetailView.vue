<template>
  <MainLayout>
    <div class="event-detail-page">
      <div class="event-detail-container">
        <h2>Detalles del evento</h2>
        
        <!-- Indicador de carga -->
        <div v-if="loading">
          <Loader />
        </div>
        
        <!-- Mensaje de error -->
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <!-- Contenido principal -->
        <div v-if="!loading && !error && event && ticket">
          <!-- Botones de acción -->
          <div class="actions-section">
            <h3>Acciones disponibles</h3>
            <div class="actions-buttons">
              <!-- Acciones para anfitrión -->
              <template v-if="ticket.role === 'HOST'">
                <button class="action-btn" @click="goToEditEvent">
                  Modificar información del evento
                </button>
                <button class="action-btn" @click="goToEditAttendance">
                  Modificar información de asistencia
                </button>
                <button class="action-btn" @click="goToGuests">
                  Consultar invitados
                </button>
                <button class="action-btn" @click="goToGifts">
                  Consultar lista de regalos
                </button>
              </template>
              
              <!-- Acciones para invitado -->
              <template v-else-if="ticket.role === 'GUEST'">
                <button class="action-btn" @click="goToEditAttendance">
                  Modificar información de asistencia
                </button>
                <button 
                  v-if="ticket.invitationConfirmation" 
                  class="action-btn" 
                  @click="goToGifts"
                >
                  Consultar lista de regalos
                </button>
              </template>
            </div>
          </div>

          <!-- Información del evento -->
          <section class="info-section">
            <h3>Información del evento</h3>
            <table class="info-table">
              <tr>
                <th>Código del evento</th>
                <td>{{ event.eventCode }}</td>
              </tr>
              <tr>
                <th>Nombre</th>
                <td>{{ event.eventName }}</td>
              </tr>
              <tr>
                <th>Descripción</th>
                <td>{{ event.description || 'Sin descripción' }}</td>
              </tr>
              <tr>
                <th>Lugar</th>
                <td>{{ event.place }}</td>
              </tr>
              <tr>
                <th>Fecha</th>
                <td>{{ formatDate(event.date) }}</td>
              </tr>
              <tr>
                <th>Estado</th>
                <td>{{ event.status }}</td>
              </tr>
              <tr v-if="event.hostUsername">
                <th>Anfitrión</th>
                <td>{{ event.hostUsername }}</td>
              </tr>
            </table>
          </section>

          <!-- Información de la entrada -->
          <section class="info-section">
            <h3>Información de la entrada</h3>
            <table class="info-table">
              <tr>
                <th>Rol</th>
                <td>{{ getRoleLabel(ticket.role) }}</td>
              </tr>
              <tr>
                <th>Estado de asistencia</th>
                <td>{{ getBooleanStatusLabel(ticket.assistConfirmation) }}</td>
              </tr>
              <tr>
                <th>Confirmación de invitación</th>
                <td>{{ getBooleanStatusLabel(ticket.invitationConfirmation) }}</td>
              </tr>
            </table>
          </section>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useTicketStore } from '../stores/ticket.store';
import { useAuthStore } from '../stores/auth.store';
import Loader from '../components/Loader.vue';
import MainLayout from '../layouts/MainLayout.vue';

export default defineComponent({
  name: 'TicketEventDetailView',
  components: {
    Loader,
    MainLayout,
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const ticketStore = useTicketStore();
    const authStore = useAuthStore();

    const event = ref<any>(null);
    const ticket = ref<any>(null);
    const loading = ref<boolean>(false);
    const error = ref<string | null>(null);

    const eventCode = route.params.eventCode as string;
    const ticketId = Number(route.params.ticketId);

    const loadEventDetail = async () => {
      loading.value = true;
      error.value = null;
      
      try {
        const userId = authStore.user.value?.userId;
        
        if (!userId) {
          error.value = 'No se ha encontrado el usuario autenticado.';
          return;
        }
        
        const response = await ticketStore.fetchTicketDetail(eventCode, ticketId, userId);
        
        if (response) {
          ticket.value = response.ticket;
          event.value = response.event;
        } else {
          error.value = 'No se pudo cargar la información del evento.';
        }
      } catch (err: any) {
        error.value = err.response?.data?.message || 'Error al obtener los detalles del evento';
      } finally {
        loading.value = false;
      }
    };

    onMounted(async () => {
      await loadEventDetail();
    });

    const formatDate = (dateStr: string) => {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      return date.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    };

    const getRoleLabel = (role: string) => {
      const roleMap: Record<string, string> = {
        'HOST': 'Anfitrión',
        'GUEST': 'Invitado'
      };
      return roleMap[role] || role;
    };

    const getBooleanStatusLabel = (value: boolean | null | undefined) => {
      if (value === null || value === undefined) {
        return 'PENDIENTE';
      }
      return value ? 'CONFIRMADA' : 'RECHAZADA';
    };

    // Navegación a otras vistas
    const goToEditEvent = () => {
      router.push({ 
        name: 'ModificarEvento', 
        params: { eventCode },
        state: { event: event.value }
      });
    };

    const goToEditAttendance = () => {
      router.push({ 
        name: 'ModificarAsistencia', 
        params: { eventCode, ticketId } 
      });
    };

    const goToGuests = () => {
      router.push({ 
        name: 'ConsultarInvitados', 
        params: { eventCode } 
      });
    };

    const goToGifts = () => {
      router.push({ 
        name: 'EventGifts', 
        params: { eventCode } 
      });
    };

    return {
      event,
      ticket,
      loading,
      error,
      formatDate,
      getRoleLabel,
      getBooleanStatusLabel,
      goToEditEvent,
      goToEditAttendance,
      goToGuests,
      goToGifts,
    };
  },
});
</script>

<style scoped>
.event-detail-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  
  /* Configuración del fondo */
  background-image: url('@/assets/images/DetallesEntrada.jpg');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  
  /* Padding y espaciado */
  padding: 30px 20px;
  padding-top: calc(30px + 1cm);
  
  /* Evita desbordamiento */
  overflow: auto;
}

.event-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.event-detail-container h2 {
  margin-top: 0;
  margin-bottom: 30px;
  color: #333;
  text-align: center;
  font-size: 28px;
}

/* Sección de acciones */
.actions-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e0e0e0;
}

.actions-section h3 {
  color: #333;
  font-size: 22px;
  margin-bottom: 15px;
}

.actions-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.action-btn {
  flex: 1 1 calc(50% - 6px);
  min-width: 200px;
  background-color: #5564eb;
  color: white;
  padding: 14px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
  text-align: center;
}

.action-btn:hover {
  background-color: #3d4fc9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(85, 100, 235, 0.3);
}

/* Secciones de información */
.info-section {
  margin-bottom: 30px;
}

.info-section h3 {
  color: #333;
  font-size: 22px;
  margin-bottom: 15px;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
}

.info-table tr {
  border-bottom: 1px solid #e0e0e0;
}

.info-table tr:last-child {
  border-bottom: none;
}

.info-table th,
.info-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 16px;
}

.info-table th {
  background: #f5f5f5;
  color: #333;
  font-weight: 600;
  width: 250px;
  text-align: center;
}

.info-table td {
  color: #555;
}

/* Mensaje de error */
.error-message {
  color: #b91c1c;
  margin-top: 15px;
  margin-bottom: 15px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  background: #fee2e2;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #fca5a5;
}

/* Responsive */
@media (max-width: 768px) {
  .event-detail-page {
    padding-top: 20px;
  }

  .event-detail-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .event-detail-container h2 {
    font-size: 24px;
  }

  .actions-section h3,
  .info-section h3 {
    font-size: 20px;
  }

  .actions-buttons {
    flex-direction: column;
  }

  .action-btn {
    flex: 1 1 100%;
    min-width: auto;
  }

  .info-table th {
    width: 150px;
    font-size: 14px;
  }

  .info-table td {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .event-detail-container {
    padding: 20px;
  }

  .info-table th,
  .info-table td {
    display: block;
    width: 100%;
  }

  .info-table th {
    background: #e8e8e8;
    border-bottom: none;
    padding-bottom: 8px;
  }

  .info-table td {
    padding-top: 8px;
    padding-bottom: 16px;
  }
}
</style>