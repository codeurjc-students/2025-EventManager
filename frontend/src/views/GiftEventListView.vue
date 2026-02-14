<template>
  <MainLayout>
    <div class="event-gifts-page">
      <div class="event-gifts-container">
        <h2>Lista de regalos</h2>
        
        <!-- Acciones -->
        <div class="actions-section">
          <button class="add-gift-btn" @click="showAddGiftPopUp = true">
            Añadir nuevo regalo
          </button>
        </div>

        <!-- Filtros -->
        <div class="filters-section">
          <div class="filter-group">
            <label for="filter-status">Estado:</label>
            <select id="filter-status" v-model="filterStatus">
              <option value="all">Todas</option>
              <option value="true">Completamente financiado</option>
              <option value="false">Pendiente recaudación</option>
            </select>
          </div>
          <div class="filter-group">
            <label for="filter-host">Creado por Anfitrión:</label>
            <select id="filter-host" v-model="filterCreatedByHost">
              <option value="all">Todas</option>
              <option value="true">Sí</option>
              <option value="false">No</option>
            </select>
          </div>
          <button class="search-btn" @click="applyFilters">
            Buscar
          </button>
        </div>

        <!-- Indicador de carga -->
        <div v-if="loading" class="loader-container">
          <Loader />
        </div>
        
        <!-- Mensaje de error -->
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <!-- Tabla de regalos -->
        <div v-if="!loading && gifts.length > 0" class="table-container">
          <table class="event-table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Usuario de creación</th>
                <th>Estado de financiación</th>
                <th>Detalles</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="gift in gifts" :key="gift.giftId">
                <td>{{ gift.name }}</td>
                <td>
                  {{ gift.creationUser }}
                  <span v-if="gift.createdByHost" class="badge-host-sm">Anfitrión</span>
                </td>
                <td>
                  <span v-if="gift.paidInFull" class="status-complete-label">
                    ✓ Completamente financiado
                  </span>
                  <span v-else class="status-pending-label">
                    ⏳ Pendiente recaudación
                  </span>
                </td>
                <td class="action-cell">
                  <button
                    class="info-btn"
                    @click="goToGiftDetail(gift.giftId)"
                    title="Más información"
                  >
                    ➤
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
        <div v-if="!loading && gifts.length === 0" class="no-results">
          No hay regalos disponibles para este evento.
        </div>
      </div>
    </div>
    
    <!-- PopUp para añadir regalo (fuera del contenedor con fondo) -->
    <div v-if="showAddGiftPopUp" class="popup-overlay" @click.self="closeAddGiftPopUp">
      <div class="popup-content">
        <div class="popup-header">
          <h3>Añadir nuevo regalo</h3>
          <button class="close-button" @click="closeAddGiftPopUp">✕</button>
        </div>
        <form @submit.prevent="onAddGift">
          <div class="form-group">
            <label for="gift-name">Nombre *</label>
            <input id="gift-name" v-model="newGift.name" required />
          </div>
          <div class="form-group">
            <label for="gift-details">Detalles</label>
            <textarea id="gift-details" v-model="newGift.details"></textarea>
          </div>
          <div class="form-group">
            <label for="gift-price">Precio *</label>
            <input id="gift-price" type="number" v-model="newGift.price" required min="0" step="0.01" />
          </div>
          <div class="form-group">
            <label for="gift-url">URL del producto</label>
            <input id="gift-url" v-model="newGift.url" />
          </div>
          <div class="form-group">
            <label for="gift-image">Imagen</label>
            <input id="gift-image" type="file" @change="onImageChange" accept="image/*" />
            <div v-if="newGift.imageName" class="image-name">
              Imagen seleccionada: {{ newGift.imageName }}
            </div>
          </div>
          <div v-if="addGiftError" class="error-message-popup">{{ addGiftError }}</div>
          <div class="popup-actions">
            <button type="submit" class="save-btn">Guardar</button>
            <button type="button" class="cancel-btn" @click="closeAddGiftPopUp">Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useGiftStore } from '../stores/gift.store';
import { useAuthStore } from '../stores/auth.store';
import Loader from '../components/Loader.vue';
import MainLayout from '../layouts/MainLayout.vue';
import type { GiftDTO } from '../types/gift';
import type { UserDTO } from '../types/user';

const route = useRoute();
const router = useRouter();
const giftStore = useGiftStore();
const authStore = useAuthStore() as { user: { value: UserDTO | null } };

const eventCode = route.params.eventCode as string;
const gifts = ref<GiftDTO[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const currentPage = ref(1);
const pageSize = ref(10);
const totalGifts = ref(0);
const totalPages = ref(1);

// Filtros
const filterStatus = ref<string>('all');
const filterCreatedByHost = ref<string>('all');

// PopUp para añadir regalo
const showAddGiftPopUp = ref(false);
const addGiftError = ref<string | null>(null);
const newGift = ref<any>({
  name: '',
  details: '',
  price: '',
  url: '',
  image: null,
  imageName: '',
});

onMounted(() => {
  fetchGifts();
});

const buildSearchParams = () => {
  const searchParts: string[] = [];
  
  // Filtro de estado (paidInFull)
  if (filterStatus.value !== 'all') {
    searchParts.push(`paidInFull=${filterStatus.value}`);
  }
  
  // Filtro de creado por anfitrión
  if (filterCreatedByHost.value !== 'all') {
    searchParts.push(`createdByHost=${filterCreatedByHost.value}`);
  }
  
  return searchParts.length > 0 ? searchParts.join(',') : undefined;
};

const fetchGifts = async (page: number = 1) => {
  loading.value = true;
  error.value = null;
  try {
    const search = buildSearchParams();
    const params: any = { page, pageSize: pageSize.value };
    
    // Solo añadir search si hay filtros
    if (search) {
      params.search = search;
    }
    
    const response = await giftStore.fetchGifts(eventCode, params);
    gifts.value = response?.data || [];
    totalGifts.value = response?.total || 0;
    totalPages.value = response?.page?.totalPages || Math.ceil((response?.total || 0) / pageSize.value) || 1;
    currentPage.value = page;
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error al obtener los regalos';
  } finally {
    loading.value = false;
  }
};

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
    fetchGifts(currentPage.value);
  }
};

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    fetchGifts(currentPage.value);
  }
};

const onPageSizeChange = () => {
  currentPage.value = 1;
  fetchGifts(1);
};

const applyFilters = () => {
  currentPage.value = 1;
  fetchGifts(1);
};

const goToGiftDetail = (giftId: number) => {
  router.push({
    name: 'DetalleRegalo',
    params: { eventCode, giftId }
  });
};

const closeAddGiftPopUp = () => {
  showAddGiftPopUp.value = false;
  addGiftError.value = null;
  // Limpiar campos
  newGift.value = {
    name: '',
    details: '',
    price: '',
    url: '',
    image: null,
    imageName: '',
  };
};

const onImageChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    newGift.value.image = target.files[0];
    newGift.value.imageName = target.files[0].name;
  } else {
    newGift.value.image = null;
    newGift.value.imageName = '';
  }
};

const onAddGift = async () => {
  addGiftError.value = null;
  try {
    if (!newGift.value.name || !newGift.value.price) {
      addGiftError.value = 'El nombre y el precio son obligatorios.';
      return;
    }
    // Construir objeto regalo según GiftCreateDTO del backend
    const giftData: any = {
      name: newGift.value.name,
      price: Number(newGift.value.price),
      details: newGift.value.details,
      url: newGift.value.url,
      creationUser: authStore.user.value?.username || '',
    };

    // Si hay imagen, convertirla a base64
    if (newGift.value.image) {
      const toBase64 = (file: File) =>
        new Promise<string>((resolve, reject) => {
          const reader = new FileReader();
          reader.readAsDataURL(file);
          reader.onload = () => resolve(reader.result as string);
          reader.onerror = error => reject(error);
        });
      giftData.image = await toBase64(newGift.value.image);
    }

    await giftStore.addGift(eventCode, giftData);
    closeAddGiftPopUp();
    fetchGifts(currentPage.value);
  } catch (err: any) {
    addGiftError.value = err.response?.data?.message || 'Error al crear el regalo';
  }
};
</script>

<style scoped>
.event-gifts-page {
  min-height: 100vh;
  background-image: url('@/assets/images/ConsultarRegalos.jpg');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  padding: 20px;
  padding-top: 80px;
  padding-bottom: 20px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.event-gifts-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.event-gifts-container h2 {
  margin-top: 0;
  margin-bottom: 30px;
  color: #333;
  text-align: center;
  font-size: 28px;
}

/* Acciones */
.actions-section {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  display: flex;
  justify-content: center;
}

.add-gift-btn {
  padding: 10px 30px;
  background-color: #5564eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.add-gift-btn:hover {
  background-color: #3d4fc9;
  transform: translateY(-2px);
}

/* Filtros */
.filters-section {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  display: flex;
  gap: 20px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-group label {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.filter-group select {
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  min-width: 200px;
  transition: border-color 0.2s;
}

.filter-group select:hover {
  border-color: #5564eb;
}

.filter-group select:focus {
  outline: none;
  border-color: #5564eb;
  box-shadow: 0 0 0 3px rgba(85, 100, 235, 0.1);
}

.search-btn {
  padding: 10px 30px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  height: fit-content;
}

.search-btn:hover {
  background-color: #218838;
  transform: translateY(-2px);
}

.status-complete-label {
  color: #28a745;
  font-weight: 600;
  padding: 6px 12px;
  background: #d4edda;
  border-radius: 6px;
  display: inline-block;
  font-size: 14px;
}

.status-pending-label {
  color: #dc3545;
  font-weight: 600;
  padding: 6px 12px;
  background: #f8d7da;
  border-radius: 6px;
  display: inline-block;
  font-size: 14px;
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

.action-cell {
  text-align: center;
  width: 80px;
}

.info-btn {
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
}

.info-btn:hover {
  background: #3d4fc9;
  transform: scale(1.1);
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

.no-results {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 16px;
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
  color: #ffffff;
  cursor: not-allowed;
  opacity: 0.8;
}

.pagination-arrow.disabled {
  background: #6b7280;
  color: #ffffff;
  cursor: not-allowed;
  opacity: 0.8;
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

/* PopUp de añadir regalo */
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.popup-content {
  background: #fff;
  border-radius: 10px;
  padding: 30px;
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.popup-header h3 {
  margin: 0;
  color: #333;
  font-size: 22px;
}

.close-button {
  background: transparent;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.close-button:hover {
  color: #e60000;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-family: inherit;
  font-size: 14px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #5564eb;
}

.form-group textarea {
  min-height: 100px;
  resize: vertical;
}

.image-name {
  margin-top: 8px;
  font-size: 13px;
  color: #666;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 4px;
}

.error-message-popup {
  color: #b91c1c;
  margin-bottom: 15px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  background: #fee2e2;
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #fca5a5;
}

.popup-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.save-btn {
  background: #5564eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 12px 24px;
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.2s;
}

.save-btn:hover {
  background: #3d4fc9;
  transform: translateY(-1px);
}

.cancel-btn {
  background: #e0e0e0;
  color: #333;
  border: none;
  border-radius: 6px;
  padding: 12px 24px;
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #bdbdbd;
}

.badge-host-sm {
  background: #5564eb;
  color: white;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 0.75em;
  font-weight: 600;
  margin-left: 6px;
  white-space: nowrap;
}

.status-paid {
  color: #28a745;
  font-size: 1.2em;
  font-weight: bold;
}

.status-pending-sm {
  color: #dc3545;
  font-size: 1.2em;
}

/* Responsive */
@media (max-width: 768px) {
  .event-gifts-container {
    padding: 20px;
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