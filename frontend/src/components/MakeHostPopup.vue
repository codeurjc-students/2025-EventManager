<template>
  <div v-if="isVisible" class="popup-overlay" @click="closePopup">
    <div class="popup-container" @click.stop>
      <div class="popup-content">
        <h3>Confirmar acción</h3>
        <p class="popup-message">Se hará anfitrión al usuario <strong>{{ userName }}</strong>, ¿estás seguro?</p>
        
        <div class="popup-buttons">
          <button class="btn-cancel" @click="closePopup">Cancelar</button>
          <button class="btn-confirm" @click="confirmAction">Aceptar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue';

const props = defineProps<{
  isVisible: boolean;
  userName: string;
}>();

const emit = defineEmits<{
  (e: 'update:isVisible', value: boolean): void;
  (e: 'confirm'): void;
}>();

const closePopup = () => {
  emit('update:isVisible', false);
};

const confirmAction = () => {
  emit('confirm');
  closePopup();
};
</script>

<style scoped>
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.popup-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  max-width: 500px;
  width: 90%;
  padding: 30px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateY(-50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.popup-content h3 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 24px;
  text-align: center;
}

.popup-message {
  color: #555;
  font-size: 16px;
  line-height: 1.5;
  margin-bottom: 30px;
  text-align: center;
}

.popup-message strong {
  color: #5564eb;
  font-weight: 600;
}

.popup-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.btn-cancel,
.btn-confirm {
  padding: 12px 30px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.btn-cancel {
  background: #e5e7eb;
  color: #333;
}

.btn-cancel:hover {
  background: #d1d5db;
}

.btn-confirm {
  background: #5564eb;
  color: white;
}

.btn-confirm:hover {
  background: #3d4fc9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(85, 100, 235, 0.3);
}

@media (max-width: 768px) {
  .popup-container {
    padding: 20px;
  }
  
  .popup-buttons {
    flex-direction: column;
  }
  
  .btn-cancel,
  .btn-confirm {
    width: 100%;
  }
}
</style>
