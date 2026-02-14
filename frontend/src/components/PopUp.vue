<template>
  <div v-if="isVisible" class="popup-overlay">
    <div class="popup">
      <div class="popup-header">
        <h3>{{ title }}</h3>
        <button class="close-button" @click="closePopup">X</button>
      </div>
      <div class="popup-body">
        <p>{{ message }}</p>
      </div>
      <div class="popup-footer">
        <button class="accept-button" @click="accept">Aceptar</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'PopUp',
  props: {
    title: {
      type: String,
      required: true,
    },
    message: {
      type: String,
      required: true,
    },
    isVisible: {
      type: Boolean,
      required: true,
    },
  },
  methods: {
    closePopup() {
      this.$emit('update:isVisible', false);
    },
    accept() {
      this.closePopup();
      this.$emit('accepted');
    },
  },
});
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
}

.popup {
  background: white;
  border-radius: 8px;
  padding: 20px;
  width: 300px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.close-button {
  background: transparent;
  border: none;
  cursor: pointer;
}

.popup-body {
  margin: 10px 0;
}

.popup-footer {
  display: flex;
  justify-content: flex-end;
}

.accept-button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 10px 15px;
  border-radius: 5px;
  cursor: pointer;
}
</style>