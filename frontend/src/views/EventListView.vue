<template>
  <MainLayout>
    <BlueHeader />
    <div class="event-list-view">
      <div class="event-list-container">
        <h2>Mis eventos</h2>
        <div class="filters">
          <div class="filter-group">
            <label for="role"><strong>Rol *</strong></label>
            <select id="role" v-model="role" required>
              <option value="" disabled>Selecciona rol</option>
              <option value="ANFITRION">Anfitrión</option>
              <option value="INVITADO">Invitado</option>
            </select>
          </div>
          <div class="filter-group filter-group-dates">
            <label for="dateRange"><strong>Fecha</strong></label>
            <div class="date-inputs">
              <input type="date" v-model="dateStart" />
              <span class="date-separator">-</span>
              <input type="date" v-model="dateEnd" />
            </div>
          </div>
          <div class="filter-group">
            <label for="status"><strong>Estado</strong></label>
            <select id="status" v-model="status">
              <option value="">Todos</option>
              <option value="PENDIENTE">Pendiente</option>
              <option value="ACTIVO">Activo</option>
              <option value="CANCELADO">Cancelado</option>
            </select>
          </div>
          <button
            class="search-btn"
            :disabled="!role"
            @click="fetchEvents"
          >
            Buscar
          </button>
        </div>

        <div v-if="loading">
          <Loader />
        </div>
        <div v-if="error" class="error-message">{{ error }}</div>

        <table v-if="hasSearched" class="event-table">
        <thead>
          <tr>
            <th>Código</th>
            <th>Nombre</th>
            <th>Lugar</th>
            <th>Fecha</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="event in events" :key="event.eventId">
            <td>
              <router-link
                :to="{
                  name: 'DetalleEvento',
                  params: { eventCode: event.eventCode, ticketId: event.ticketId }
                }"
                class="info-link"
              >
                {{ event.eventCode }}
              </router-link>
            </td>
            <td>{{ event.eventName }}</td>
            <td>{{ event.place }}</td>
            <td>{{ formatDate(event.date) }}</td>
            <td>{{ event.status }}</td>
          </tr>
        </tbody>
      </table>
      
      <!-- Paginación personalizada -->
      <div v-if="hasSearched" class="pagination-container">
        <div class="pagination-controls">
          <button 
            class="pagination-arrow" 
            :class="{ 'disabled': currentPage === 1 }"
            :disabled="currentPage === 1"
            @click="prevPage"
          >
            ←
          </button>
          <span class="page-info">Página {{ currentPage }}</span>
          <button 
            class="pagination-arrow" 
            :class="{ 'disabled': currentPage >= totalPages }"
            :disabled="currentPage >= totalPages"
            @click="nextPage"
          >
            →
          </button>
        </div>
        <div class="pagination-size">
          <label for="pageSize">Registros por página:</label>
          <select id="pageSize" v-model="pageSize" @change="onPageSizeChange">
            <option :value="10">10</option>
            <option :value="25">25</option>
            <option :value="50">50</option>
          </select>
        </div>
      </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import { useEventStore } from '../stores/event.store';
import Loader from '../components/Loader.vue';
import BlueHeader from '../components/BlueHeader.vue';

const authStore = useAuthStore();
const eventStore = useEventStore();

const role = ref('');
const dateStart = ref('');
const dateEnd = ref('');
const status = ref('');
const currentPage = ref(1);
const pageSize = ref(10);

interface Event {
  eventId: number;
  eventCode: string;
  eventName: string;
  place: string;
  date: string;
  status: string;
  ticketId?: number;
}

const events = ref<Event[]>([]);
const totalPages = ref(1);
const loading = ref(false);
const error = ref<string | null>(null);
const hasSearched = ref(false);

/**
 * Mapea el rol del español (para la UI) al inglés (para el backend)
 */
const mapRoleToBackend = (roleInSpanish: string): string => {
  const roleMap: Record<string, string> = {
    'ANFITRION': 'HOST',
    'INVITADO': 'GUEST'
  };
  return roleMap[roleInSpanish] || roleInSpanish;
};

const fetchEvents = async () => {
  loading.value = true;
  error.value = null;
  
  // Si el usuario no tiene userId, cargar la información completa
  if (!authStore.user.value?.userId) {
    try {
      await authStore.loadUserProfile();
    } catch (err: any) {
      console.error('Error al cargar el perfil del usuario:', err);
      error.value = 'No se pudo cargar la información del usuario';
      loading.value = false;
      return;
    }
  }
  
  // Mapear el rol de español a inglés para el backend
  const roleForBackend = mapRoleToBackend(role.value);

  // Construir filtros de búsqueda con formato LocalDateTime
  let searchFilters: string[] = [];
  if (dateStart.value) {
    // Agregar hora 00:00:00 a la fecha de inicio
    searchFilters.push(`date>=${dateStart.value}T00:00:00`);
  }
  if (dateEnd.value) {
    // Agregar hora 23:59:59 a la fecha de fin
    searchFilters.push(`date<=${dateEnd.value}T23:59:59`);
  }
  if (status.value) searchFilters.push(`status=${status.value}`);

  const params: any = {
    page: currentPage.value,
    pageSize: pageSize.value,
    sortBy: 'date',
    sortDir: 'asc',
    userId: authStore.user.value?.userId,
    role: roleForBackend, // Usar el rol mapeado
  };

  // Solo agregar search si hay filtros
  if (searchFilters.length > 0) {
    params.search = searchFilters.join(',');
  }

  try {
    const response = await eventStore.fetchAllEvents(params);
    
    if (response && response.data) {
      events.value = response.data;
      totalPages.value = response.page?.totalPages || 1;
      hasSearched.value = true; // Marcar que se ha realizado una búsqueda
    } else {
      events.value = [];
      totalPages.value = 1;
      error.value = 'No se recibieron datos de eventos';
    }
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error al obtener los eventos';
  } finally {
    loading.value = false;
  }
};

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
    fetchEvents();
  }
};

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    fetchEvents();
  }
};

const onPageSizeChange = () => {
  // Resetear a la primera página cuando cambia el tamaño de página
  currentPage.value = 1;
  fetchEvents();
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString();
};
</script>

<style scoped>
.event-list-view {
  min-height: calc(100vh - 60px);
  background-image: url('@/assets/images/ListaEventos.jpg');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  padding: 30px 20px;
  padding-top: calc(30px + 1cm); /* Bajamos 1 cm el contenido */
}

.event-list-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.event-list-container h2 {
  margin-top: 0;
  margin-bottom: 30px;
  color: #333;
  text-align: center;
  font-size: 28px;
}

.filters {
  display: flex;
  gap: 20px;
  align-items: flex-end;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
}

.filter-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.filter-group select,
.filter-group input {
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.filter-group select:focus,
.filter-group input:focus {
  outline: none;
  border-color: #5564eb;
}

.filter-group-dates {
  display: flex;
  flex-direction: column;
}

.date-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-inputs input {
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  font-family: inherit;
}

.date-inputs input:focus {
  outline: none;
  border-color: #5564eb;
}

.date-separator {
  color: #333;
  font-weight: 500;
  padding: 0 4px;
  font-size: 16px;
}

.search-btn {
  margin-left: auto;
  padding: 14px 20px;
  background-color: #5564eb;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: background-color 0.3s ease;
}

.search-btn:hover {
  background-color: #3d4fc9;
}

.search-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
  opacity: 0.6;
}

.event-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
}

.event-table th,
.event-table td {
  border: 1px solid #e0e0e0;
  padding: 12px;
  text-align: left;
  font-size: 16px;
}

.event-table th {
  background: #f5f5f5;
  color: #333;
  font-weight: 600;
  text-align: center;
}

.event-table td {
  color: #333;
}

.info-link {
  color: #5564eb;
  text-decoration: underline;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.3s ease;
}

.info-link:hover {
  color: #3d4fc9;
}

.no-events {
  margin-top: 20px;
  color: #666;
  text-align: center;
  font-size: 16px;
}

.error-message {
  color: #ef4444;
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 15px 0;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.pagination-arrow {
  width: 40px;
  height: 40px;
  background: #5564eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.pagination-arrow:hover:not(:disabled) {
  background: #3d4fc9;
  transform: scale(1.05);
}

.pagination-arrow:disabled {
  background: #6b7280;
  color: #e5e7eb;
  cursor: not-allowed;
  opacity: 0.6;
}

.pagination-arrow.disabled {
  background: #6b7280;
  color: #e5e7eb;
  cursor: not-allowed;
  opacity: 0.6;
}

.page-info {
  font-weight: 600;
  font-size: 16px;
  color: #333;
  min-width: 80px;
  text-align: center;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-size label {
  font-weight: 500;
  color: #333;
  font-size: 16px;
}

.pagination-size select {
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  font-size: 16px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.3s ease;
  font-family: inherit;
}

.pagination-size select:hover {
  border-color: #5564eb;
}

.pagination-size select:focus {
  border-color: #5564eb;
  box-shadow: 0 0 0 2px rgba(85, 100, 235, 0.1);
}

/* Responsive */
@media (max-width: 768px) {
  .event-list-container {
    max-width: 90%;
    padding: 30px;
  }
  
  .event-list-container h2 {
    font-size: 24px;
  }
  
  .filters {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-btn {
    margin-left: 0;
  }
}
</style>