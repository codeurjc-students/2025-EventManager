<template>
  <div v-if="isVisible" class="popup-overlay" @click.self="closePopup">
    <div class="popup">
      <div class="popup-header">
        <h3>Modificar información de asistencia</h3>
        <button class="close-button" @click="closePopup">×</button>
      </div>
      <div class="popup-body">
        <div class="form-group">
          <label for="invitation-confirmation">Confirmación de invitación:</label>
          <div v-if="!isEditing" class="display-value">
            {{ getConfirmationLabel(localInvitationConfirmation) }}
          </div>
          <select id="invitation-confirmation" v-else v-model="localInvitationConfirmation" class="form-select">
            <option :value="true">Confirmada</option>
            <option :value="false">Denegada</option>
          </select>
        </div>
      </div>
      <div class="popup-footer">
        <button v-if="!isEditing" class="edit-button" @click="startEditing">
          Editar
        </button>
        <template v-else>
          <button class="cancel-button" @click="cancelEditing">
            Cancelar
          </button>
          <button class="confirm-button" @click="confirmChanges">
            Confirmar
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{
  isVisible: boolean;
  invitationConfirmation: boolean | null;
}>();

const emit = defineEmits<{
  'update:isVisible': [value: boolean];
  'confirm': [invitationConfirmation: boolean];
}>();

const isEditing = ref(false);
const localInvitationConfirmation = ref<boolean | null>(props.invitationConfirmation);

// Watch for changes in props
watch(() => props.invitationConfirmation, (newValue) => {
  localInvitationConfirmation.value = newValue;
});

watch(() => props.isVisible, (newValue) => {
  if (newValue) {
    // Reset state when popup opens
    isEditing.value = false;
    localInvitationConfirmation.value = props.invitationConfirmation;
  }
});

const getConfirmationLabel = (value: boolean | null): string => {
  if (value === null || value === undefined) {
    return 'PENDIENTE';
  }
  return value ? 'CONFIRMADA' : 'DENEGADA';
};

const closePopup = () => {
  isEditing.value = false;
  emit('update:isVisible', false);
};

const startEditing = () => {
  isEditing.value = true;
};

const cancelEditing = () => {
  isEditing.value = false;
  localInvitationConfirmation.value = props.invitationConfirmation;
};

const confirmChanges = () => {
  if (localInvitationConfirmation.value !== null) {
    emit('confirm', localInvitationConfirmation.value);
  }
  isEditing.value = false;
  emit('update:isVisible', false);
};
</script>

<style scoped>
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.popup {
  background: white;
  border-radius: 10px;
  padding: 25px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e0e0e0;
}

.popup-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.close-button {
  background: transparent;
  border: none;
  font-size: 28px;
  color: #999;
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
  color: #333;
}

.popup-body {
  margin: 20px 0;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 600;
  font-size: 14px;
}

.display-value {
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 6px;
  color: #555;
  font-size: 16px;
  font-weight: 500;
}

.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  color: #333;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.form-select:focus {
  outline: none;
  border-color: #5564eb;
}

.popup-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #e0e0e0;
}

.edit-button,
.cancel-button,
.confirm-button {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-button {
  background-color: #5564eb;
  color: white;
}

.edit-button:hover {
  background-color: #3d4fc9;
}

.cancel-button {
  background-color: #e0e0e0;
  color: #333;
}

.cancel-button:hover {
  background-color: #d0d0d0;
}

.confirm-button {
  background-color: #1e7e34;
  color: white;
}

.confirm-button:hover {
  background-color: #218838;
}
</style>
