<template>
  <MainLayout>
    <div class="event-tickets-page">
      <div class="event-tickets-container">
        <h2>Lista de invitados</h2>
        
        <!-- Filtros de búsqueda -->
        <div class="filters-section">
          <div class="filters-row">
            <div class="filter-group">
              <label for="invitation-filter">Invitación confirmada:</label>
              <select id="invitation-filter" v-model="filters.invitationConfirmation" class="filter-select">
                <option :value="null">Todas</option>
                <option :value="true">Confirmada</option>
                <option :value="false">Denegada</option>
                <option value="pending">Pendiente</option>
              </select>
            </div>
            
            <div class="filter-group">
              <label for="assist-filter">Asistencia confirmada:</label>
              <select id="assist-filter" v-model="filters.assistConfirmation" class="filter-select">
                <option :value="null">Todas</option>
                <option :value="true">Confirmada</option>
                <option :value="false">Denegada</option>
                <option value="pending">Pendiente</option>
              </select>
            </div>
            
            <button class="search-button" @click="searchTickets">
              Buscar
            </button>
          </div>
        </div>

        <!-- Indicador de carga -->
        <div v-if="loading" class="loader-container">
          <Loader />
        </div>
        
        <!-- Mensaje de error -->
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <!-- Tabla de resultados -->
        <div v-if="!loading && tickets.length > 0" class="table-container">
          <table class="event-table">
            <thead>
              <tr>
                <th>ID Entrada</th>
                <th>Nombre</th>
                <th>Apellidos</th>
                <th>Invitación</th>
                <th>Asistencia</th>
                <th>Acompañantes</th>
                <th>Notas</th>
                <th v-if="canEditTickets">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ticket in tickets" :key="ticket.ticketId">
                <td>{{ ticket.ticketId }}</td>
                <td>{{ getUserFirstName(ticket) }}</td>
                <td>{{ getUserLastName(ticket) }}</td>
                <td>{{ getConfirmationLabel(ticket.invitationConfirmation) }}</td>
                <td>{{ getConfirmationLabel(ticket.assistConfirmation) }}</td>
                <td>{{ ticket.guestNumber || 1 }}</td>
                <td class="notes-cell">{{ ticket.notes || '-' }}</td>
                <td v-if="canEditTickets" class="action-cell">
                  <button class="edit-icon-btn" @click="openEditPopup(ticket)" title="Modificar información de asistencia">
                    ➤
                  </button>
                  <button v-if="ticket.role !== 'HOST'" class="make-host-btn" @click="openMakeHostPopup(ticket)" title="Hacer anfitrión">
                    ⬆
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          
          <!-- Paginación personalizada -->
          <div class="pagination-container">
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
        
        <!-- Mensaje cuando no hay resultados -->
        <div v-if="!loading && hasSearched && tickets.length === 0" class="no-results">
          No se encontraron entradas que coincidan con los filtros seleccionados.
        </div>
        
        <!-- Mensaje inicial -->
        <div v-if="!loading && !hasSearched" class="initial-message">
          Utiliza los filtros y presiona "Buscar" para consultar la lista de invitados.
        </div>
      </div>
    </div>
    
    <!-- Popup para editar información de asistencia -->
    <EditAttendancePopup
      v-model:isVisible="showEditPopup"
      :invitationConfirmation="selectedTicket?.invitationConfirmation ?? null"
      @confirm="handleConfirmEdit"
    />
    
    <!-- Popup para hacer anfitrión -->
    <MakeHostPopup
      v-model:isVisible="showMakeHostPopup"
      :userName="selectedTicketUserName"
      @confirm="handleConfirmMakeHost"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useTicketStore } from '../stores/ticket.store';
import { useAuthStore } from '../stores/auth.store';
import Loader from '../components/Loader.vue';
import EditAttendancePopup from '../components/EditAttendancePopup.vue';
import MakeHostPopup from '../components/MakeHostPopup.vue';
import MainLayout from '../layouts/MainLayout.vue';
import type { TicketDTO } from '../types/ticket';

const route = useRoute();
const ticketStore = useTicketStore();
const authStore = useAuthStore();

const tickets = ref<TicketDTO[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const currentPage = ref(1);
const pageSize = ref(10);
const totalTickets = ref(0);
const totalPages = ref(1);
const hasSearched = ref(false);

const filters = ref({
  invitationConfirmation: null as boolean | string | null,
  assistConfirmation: null as boolean | string | null,
});

const showEditPopup = ref(false);
const selectedTicket = ref<TicketDTO | null>(null);
const currentUserRole = ref<string | null>(null);
const showMakeHostPopup = ref(false);
const selectedTicketUserName = ref<string>('');

const eventCode = route.params.eventCode as string;

// Verificar si el usuario puede editar tickets (es HOST o ANFITRION)
const canEditTickets = computed(() => {
  return currentUserRole.value === 'HOST';
});

const getUserFirstName = (ticket: TicketDTO): string => {
  if (ticket.userId && typeof ticket.userId === 'object') {
    return ticket.userId.firstName || '-';
  }
  return '-';
};

const getUserLastName = (ticket: TicketDTO): string => {
  if (ticket.userId && typeof ticket.userId === 'object') {
    return ticket.userId.lastName || '-';
  }
  return '-';
};

const getConfirmationLabel = (value: boolean | null | undefined): string => {
  if (value === null || value === undefined) {
    return 'PENDIENTE';
  }
  return value ? 'CONFIRMADA' : 'DENEGADA';
};

const buildSearchParams = () => {
  const searchParts: string[] = [];
  
  // Filtro de invitación confirmada
  if (filters.value.invitationConfirmation === 'pending') {
    searchParts.push('invitationConfirmation=null');
  } else if (filters.value.invitationConfirmation !== null) {
    searchParts.push(`invitationConfirmation=${filters.value.invitationConfirmation}`);
  }
  
  // Filtro de asistencia confirmada
  if (filters.value.assistConfirmation === 'pending') {
    searchParts.push('assistConfirmation=null');
  } else if (filters.value.assistConfirmation !== null) {
    searchParts.push(`assistConfirmation=${filters.value.assistConfirmation}`);
  }
  
  return searchParts.length > 0 ? searchParts.join(',') : undefined;
};

const searchTickets = async () => {
  currentPage.value = 1;
  await fetchTickets(1);
  hasSearched.value = true;
};

const fetchTickets = async (page: number = 1) => {
  loading.value = true;
  error.value = null;
  try {
    const search = buildSearchParams();
    const params: any = { 
      page, 
      pageSize: pageSize.value
    };
    
    // Solo añadir search si hay filtros
    if (search) {
      params.search = search;
    }
    
    const response = await ticketStore.fetchTickets(eventCode, params);
    tickets.value = response.data || [];
    totalTickets.value = response.total || 0;
    totalPages.value = response.page?.totalPages || Math.ceil(response.total / pageSize.value) || 1;
    currentPage.value = page;
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error al obtener las entradas';
    tickets.value = [];
    totalTickets.value = 0;
  } finally {
    loading.value = false;
  }
};

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
    fetchTickets(currentPage.value);
  }
};

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    fetchTickets(currentPage.value);
  }
};

const onPageSizeChange = () => {
  currentPage.value = 1;
  fetchTickets(1);
};

const openEditPopup = (ticket: TicketDTO) => {
  selectedTicket.value = ticket;
  showEditPopup.value = true;
};

const handleConfirmEdit = async (invitationConfirmation: boolean) => {
  if (!selectedTicket.value) return;
  
  loading.value = true;
  error.value = null;
  
  try {
    const updateData = {
      invitationConfirmation,
    };
    
    await ticketStore.editTicket(eventCode, selectedTicket.value.ticketId, updateData);
    
    // Recargar la lista
    await fetchTickets(currentPage.value);
    
    showEditPopup.value = false;
    selectedTicket.value = null;
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error al actualizar la entrada';
  } finally {
    loading.value = false;
  }
};

const openMakeHostPopup = (ticket: TicketDTO) => {
  selectedTicket.value = ticket;
  const firstName = getUserFirstName(ticket);
  const lastName = getUserLastName(ticket);
  selectedTicketUserName.value = `${firstName} ${lastName}`;
  showMakeHostPopup.value = true;
};

const handleConfirmMakeHost = async () => {
  if (!selectedTicket.value) return;
  
  loading.value = true;
  error.value = null;
  
  try {
    const updateData = {
      role: 'HOST',
      guestNumber: selectedTicket.value.guestNumber,
      invitationConfirmation: selectedTicket.value.invitationConfirmation,
      assistConfirmation: selectedTicket.value.assistConfirmation,
      notes: selectedTicket.value.notes || ''
    };
    
    await ticketStore.editTicket(eventCode, selectedTicket.value.ticketId, updateData);
    
    // Recargar la lista
    await fetchTickets(currentPage.value);
    
    showMakeHostPopup.value = false;
    selectedTicket.value = null;
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error al hacer anfitrión al usuario';
  } finally {
    loading.value = false;
  }
};

// Cargar el rol del usuario actual en el evento
const loadCurrentUserRole = async () => {
  try {
    // Cargar información del usuario si no está cargada
    if (!authStore.user.value?.userId) {
      await authStore.loadUserInfo();
    }
    
    const userId = authStore.user.value?.userId;
    if (!userId) return;
    
    // Obtener el ticket del usuario actual para conocer su rol
    const response = await ticketStore.fetchTickets(eventCode, {
      page: 1,
      pageSize: 1,
      search: `userId.userId=${userId}`
    });
    
    if (response.data && response.data.length > 0) {
      currentUserRole.value = response.data[0].role;
    }
  } catch (err) {
    console.error('Error al obtener el rol del usuario:', err);
  }
};

onMounted(async () => {
  await loadCurrentUserRole();
});
</script>

<style scoped>
.event-tickets-page {
  min-height: calc(100vh - 60px);
  background-image: url('@/assets/images/ConsultarInvitados.jpg');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  padding: 30px 20px;
  padding-top: 90px;
  overflow: auto;
}

.event-tickets-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.event-tickets-container h2 {
  margin-top: 0;
  margin-bottom: 30px;
  color: #333;
  text-align: center;
  font-size: 28px;
}

/* Filtros */
.filters-section {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.filters-row {
  display: flex;
  gap: 15px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-group {
  flex: 1;
  min-width: 200px;
}

.filter-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 600;
  font-size: 14px;
}

.filter-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  color: #333;
  background: white;
  cursor: pointer;
}

.filter-select:focus {
  outline: none;
  border-color: #5564eb;
}

.search-button {
  padding: 10px 30px;
  background-color: #5564eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 42px;
}

.search-button:hover {
  background-color: #3d4fc9;
  transform: translateY(-2px);
}

/* Tabla */
.table-container {
  overflow-x: auto;
}

.event-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.event-table th,
.event-table td {
  border: 1px solid #e0e0e0;
  padding: 12px;
  text-align: left;
  font-size: 14px;
}

.event-table th {
  background: #f5f5f5;
  color: #333;
  font-weight: 600;
  text-align: center;
}

.event-table tbody tr:hover {
  background: #f9f9f9;
}

.notes-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-cell {
  text-align: center;
  width: 120px;
  white-space: nowrap;
}

.edit-icon-btn,
.make-host-btn {
  background: #5564eb;
  color: white;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 5px;
  vertical-align: middle;
}

.edit-icon-btn:hover,
.make-host-btn:hover {
  background: #3d4fc9;
  transform: scale(1.1);
}

.make-host-btn {
  background: #10b981;
}

.make-host-btn:hover {
  background: #059669;
}

/* Mensajes */
.loader-container {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.error-message {
  color: #b91c1c;
  margin: 20px 0;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  background: #fee2e2;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #fca5a5;
}

.no-results,
.initial-message {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 16px;
}

.initial-message {
  background: #e8f4ff;
  border-radius: 8px;
  color: #0066cc;
  font-weight: 500;
}

/* Paginación */
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
  .event-tickets-container {
    padding: 20px;
  }
  
  .filters-row {
    flex-direction: column;
  }
  
  .filter-group {
    width: 100%;
  }
  
  .search-button {
    width: 100%;
  }
  
  .event-table {
    font-size: 12px;
  }
  
  .event-table th,
  .event-table td {
    padding: 8px;
  }
  
  .pagination-container {
    flex-direction: column;
    gap: 15px;
  }
}
</style>