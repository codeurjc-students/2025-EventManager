<template>
  <MainLayout>
    <div class="gift-detail-page">
      <div class="gift-detail-container">
        <h2>Detalles del regalo</h2>
        
        <div v-if="loading">
          <Loader />
        </div>
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <div v-if="giftStore.giftDetail.value && !loading">
          <!-- Botones de acción -->
          <div class="actions-bar">
            <button
              class="action-btn"
              :disabled="!canEditGift"
              @click="openEditPopUp"
            >
              Modificar regalo
            </button>
            <button class="action-btn" @click="showContributionPopUp = true">
              Añadir aportación
            </button>
          </div>

          <!-- Información del regalo -->
          <table class="gift-info-table">
            <tr>
              <th>Nombre</th>
              <td>{{ giftStore.giftDetail.value.name }}</td>
            </tr>
            <tr>
              <th>Detalles</th>
              <td>{{ giftStore.giftDetail.value.details || 'Sin detalles' }}</td>
            </tr>
            <tr>
              <th>Precio</th>
              <td>{{ giftStore.giftDetail.value.price }}€</td>
            </tr>
            <tr>
              <th>Recolectado</th>
              <td>{{ giftStore.giftDetail.value.collected }}€</td>
            </tr>
            <tr>
              <th>Estado</th>
              <td>
                <span :class="giftStore.giftDetail.value.paidInFull ? 'status-complete' : 'status-pending'">
                  {{ giftStore.giftDetail.value.paidInFull ? '✓ Completamente financiado' : '⏳ Pendiente de financiar' }}
                </span>
              </td>
            </tr>
            <tr>
              <th>Usuario creador</th>
              <td>
                {{ giftStore.giftDetail.value.creationUser }}
                <span v-if="giftStore.giftDetail.value.createdByHost" class="badge-host"> (Anfitrión)</span>
              </td>
            </tr>
            <tr v-if="giftStore.giftDetail.value.url">
              <th>URL</th>
              <td>
                <a :href="giftStore.giftDetail.value.url" target="_blank">{{ giftStore.giftDetail.value.url }}</a>
              </td>
            </tr>
          </table>

          <!-- Imagen del regalo -->
          <div v-if="giftStore.giftDetail.value.image" class="gift-image">
            <img :src="giftStore.giftDetail.value.image" alt="Regalo" />
          </div>

          <!-- Lista de participantes -->
          <h3 class="participants-title">Participantes en el regalo</h3>
          <table class="participants-table" v-if="giftStore.giftDetail.value.userContributionList && giftStore.giftDetail.value.userContributionList.length">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Email</th>
                <th>Teléfono</th>
                <th>Cantidad aportada</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="participant in giftStore.giftDetail.value.userContributionList" :key="participant.username">
                <td>{{ participant.username }}</td>
                <td>{{ participant.email }}</td>
                <td>{{ participant.phoneNumber }}</td>
                <td>{{ participant.amount }}€</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="no-participants">
            No hay participantes en este regalo.
          </div>
        </div>

        <!-- PopUp para editar regalo -->
        <div v-if="showEditPopUp" class="popup-overlay" @click.self="closeEditPopUp">
          <div class="popup-content">
            <div class="popup-header">
              <h3>Modificar regalo</h3>
              <button class="close-button" @click="closeEditPopUp">✕</button>
            </div>
            <form @submit.prevent="onEditGift">
              <div class="form-group">
                <label for="edit-name">Nombre *</label>
                <input id="edit-name" v-model="editGiftData.name" type="text" required />
              </div>
              <div class="form-group">
                <label for="edit-details">Detalles</label>
                <textarea id="edit-details" v-model="editGiftData.details"></textarea>
              </div>
              <div class="form-group">
                <label for="edit-price">Precio *</label>
                <input id="edit-price" v-model="editGiftData.price" type="number" required min="0" step="0.01" />
              </div>
              <div class="form-group">
                <label for="edit-url">URL del producto</label>
                <input id="edit-url" v-model="editGiftData.url" type="text" />
              </div>
              <div class="form-group">
                <label for="edit-image">Imagen</label>
                <input id="edit-image" type="file" @change="onEditImageChange" accept="image/*" />
                <div v-if="editGiftData.imageName" class="image-name">
                  Imagen seleccionada: {{ editGiftData.imageName }}
                </div>
              </div>
              <div v-if="editGiftError" class="error-message-popup">{{ editGiftError }}</div>
              <div class="popup-actions">
                <button type="submit" class="save-btn">Guardar</button>
                <button type="button" class="cancel-btn" @click="closeEditPopUp">Cancelar</button>
              </div>
            </form>
          </div>
        </div>

        <!-- PopUp para añadir aportación -->
        <div v-if="showContributionPopUp" class="popup-overlay" @click.self="closeContributionPopUp">
          <div class="popup-content">
            <div class="popup-header">
              <h3>Añadir aportación</h3>
              <button class="close-button" @click="closeContributionPopUp">✕</button>
            </div>
            <form @submit.prevent="onAddContribution">
              <div class="form-group">
                <label for="contribution-amount">Cantidad a aportar *</label>
                <input id="contribution-amount" v-model="contributionAmount" type="number" required min="0.01" step="0.01" />
              </div>
              <div v-if="contributionError" class="error-message-popup">{{ contributionError }}</div>
              <div class="popup-actions">
                <button type="submit" class="save-btn">Guardar</button>
                <button type="button" class="cancel-btn" @click="closeContributionPopUp">Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useGiftStore } from '../stores/gift.store';
import { useAuthStore } from '../stores/auth.store';
import Loader from '../components/Loader.vue';
import MainLayout from '../layouts/MainLayout.vue';
import type { UserDTO } from '../types/user';

export default defineComponent({
  name: 'GiftDetailView',
  components: {
    Loader,
    MainLayout,
  },
  setup() {
    const route = useRoute();
    const giftStore = useGiftStore();
    const authStore = useAuthStore() as { user: { value: UserDTO | null } };

    const eventCode = route.params.eventCode as string;
    const giftId = Number(route.params.giftId);

    const loading = ref<boolean>(false);
    const error = ref<string | null>(null);

    // Editar regalo
    const showEditPopUp = ref(false);
    const editGiftData = ref<any>({
      name: '',
      details: '',
      price: '',
      url: '',
      image: null,
      imageName: '',
    });
    const editGiftError = ref<string | null>(null);

    // Añadir aportación
    const showContributionPopUp = ref(false);
    const contributionAmount = ref<number | null>(null);
    const contributionError = ref<string | null>(null);

    // Control de permisos: puede editar si es creador del regalo
    const canEditGift = computed(() => {
      const isCreator = authStore.user.value?.username &&
        giftStore.giftDetail.value?.creationUser &&
        authStore.user.value.username === giftStore.giftDetail.value.creationUser;
      
      return isCreator;
    });

    // Cargar detalles del regalo
    const fetchGiftDetails = async () => {
      loading.value = true;
      error.value = null;
      try {
        await giftStore.fetchGiftDetail(eventCode, giftId);
      } catch (err: any) {
        error.value = err.response?.data?.message || 'Error al obtener los detalles del regalo';
        console.error('Error en fetchGiftDetails:', err);
      } finally {
        loading.value = false;
      }
    };

    // PopUp editar regalo
    const openEditPopUp = () => {
      if (!giftStore.giftDetail.value) return;
      editGiftData.value = {
        name: giftStore.giftDetail.value.name || '',
        details: giftStore.giftDetail.value.details || '',
        price: giftStore.giftDetail.value.price || '',
        url: giftStore.giftDetail.value.url || '',
        image: null,
        imageName: '',
      };
      showEditPopUp.value = true;
      editGiftError.value = null;
    };

    const closeEditPopUp = () => {
      showEditPopUp.value = false;
      editGiftError.value = null;
    };

    const onEditImageChange = (event: Event) => {
      const target = event.target as HTMLInputElement;
      if (target.files && target.files.length > 0) {
        editGiftData.value.image = target.files[0];
        editGiftData.value.imageName = target.files[0].name;
      } else {
        editGiftData.value.image = null;
        editGiftData.value.imageName = '';
      }
    };

    const onEditGift = async () => {
      editGiftError.value = null;
      try {
        if (!editGiftData.value.name || !editGiftData.value.price) {
          editGiftError.value = 'El nombre y el precio son obligatorios.';
          return;
        }
        const giftData: any = {
          name: editGiftData.value.name,
          price: Number(editGiftData.value.price),
          details: editGiftData.value.details,
          url: editGiftData.value.url,
        };
        if (editGiftData.value.image) {
          const toBase64 = (file: File) =>
            new Promise<string>((resolve, reject) => {
              const reader = new FileReader();
              reader.readAsDataURL(file);
              reader.onload = () => resolve(reader.result as string);
              reader.onerror = error => reject(error);
            });
          giftData.image = await toBase64(editGiftData.value.image);
        }
        await giftStore.editGift(eventCode, giftId, giftData);
        await fetchGiftDetails();
        closeEditPopUp();
      } catch (err: any) {
        editGiftError.value = err.response?.data?.message || 'Error al modificar el regalo';
      }
    };

    // PopUp añadir aportación
    const closeContributionPopUp = () => {
      showContributionPopUp.value = false;
      contributionError.value = null;
      contributionAmount.value = null;
    };

    const onAddContribution = async () => {
      contributionError.value = null;
      try {
        if (!contributionAmount.value || contributionAmount.value <= 0) {
          contributionError.value = 'La cantidad debe ser mayor que 0.';
          return;
        }
        
        const userId = authStore.user.value?.userId;
        if (!userId) {
          contributionError.value = 'Usuario no autenticado';
          return;
        }
        
        await giftStore.addContribution(eventCode, giftId, { 
          giftId: giftId,
          userId: userId,
          amount: contributionAmount.value 
        });
        await fetchGiftDetails();
        closeContributionPopUp();
      } catch (err: any) {
        contributionError.value = err.response?.data?.message || 'Error al añadir la aportación';
      }
    };

    onMounted(fetchGiftDetails);

    return {
      giftStore,
      loading,
      error,
      canEditGift,
      showEditPopUp,
      openEditPopUp,
      closeEditPopUp,
      editGiftData,
      editGiftError,
      onEditGift,
      onEditImageChange,
      showContributionPopUp,
      closeContributionPopUp,
      contributionAmount,
      contributionError,
      onAddContribution,
    };
  },
});
</script>

<style scoped>
.gift-detail-page {
  min-height: 100vh;
  background-image: url('@/assets/images/DetallesRegalo.jpg');
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  padding: 20px;
  padding-top: 80px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.gift-detail-container {
  max-width: 900px;
  width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.gift-detail-container h2 {
  margin-top: 0;
  margin-bottom: 30px;
  color: #333;
  text-align: center;
  font-size: 28px;
}

.actions-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-btn {
  padding: 12px 24px;
  background-color: #5564eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover:not(:disabled) {
  background-color: #3d4fc9;
  transform: translateY(-2px);
}

.action-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.gift-info-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 30px;
}

.gift-info-table th,
.gift-info-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.gift-info-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  width: 180px;
  color: #555;
  text-align: center;
}

.gift-info-table td {
  color: #333;
}

.gift-info-table td a {
  color: #6366f1;
  text-decoration: none;
  display: block;
  max-width: 350px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-info-table td a:hover {
  text-decoration: underline;
}

.gift-info-table tr:last-child th,
.gift-info-table tr:last-child td {
  border-bottom: none;
}

.status-complete {
  color: #166534;
  font-weight: 600;
  padding: 4px 8px;
  background: #bbf7d0;
  border-radius: 4px;
  display: inline-block;
}

.status-pending {
  color: #991b1b;
  font-weight: 600;
  padding: 4px 8px;
  background: #fecaca;
  border-radius: 4px;
  display: inline-block;
}

.badge-host {
  background: #5564eb;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.85em;
  font-weight: 600;
  margin-left: 8px;
}

.gift-image {
  margin: 30px 0;
  text-align: center;
}

.gift-image img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.participants-title {
  margin-top: 40px;
  margin-bottom: 20px;
  color: #333;
  font-size: 20px;
}

.participants-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

.participants-table thead {
  background-color: #f8f9fa;
}

.participants-table th,
.participants-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.participants-table th {
  font-weight: 600;
  color: #555;
  text-align: center;
}

.participants-table td {
  color: #333;
}

.no-participants {
  text-align: center;
  padding: 40px;
  color: #888;
  font-style: italic;
}

.error-message {
  color: #991b1b;
  background: #fecaca;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
  text-align: center;
}

.edit-gift-popup,
.contribution-popup {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-width: 400px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-sizing: border-box;
  font-family: inherit;
  font-size: 14px;
}

.form-group textarea {
  min-height: 100px;
  resize: vertical;
}

.image-name {
  margin-top: 8px;
  font-size: 0.9em;
  color: #666;
}

.error-message-popup {
  color: #991b1b;
  background: #fecaca;
  padding: 10px;
  border-radius: 6px;
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
}

.popup-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 25px;
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
  transition: all 0.3s;
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
  transition: all 0.3s;
}

.cancel-btn:hover {
  background: #bdbdbd;
}

.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.popup-content {
  background: white;
  border-radius: 12px;
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
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.popup-header h3 {
  margin: 0;
  color: #333;
  font-size: 22px;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.close-button:hover {
  color: #333;
}
</style>