<template>
  <header class="blue-header">
    <div class="header-content">
      <router-link to="/" class="logo">EventManager</router-link>
    </div>
  </header>
</template>

<script lang="ts">
export default {
  name: 'BlueHeader',
};
</script>

<style scoped>
.blue-header {
  background-color: #5564eb; /* Azul Majorelle */
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  display: flex;
  justify-content: flex-start; /* Alineado a la izquierda */
  width: 100%;
  padding-left: 20px; /* Margen izquierdo adicional */
}

.logo {
  color: white;
  font-weight: bold;
  text-decoration: none;
  font-size: 28px;
  transition: opacity 0.3s ease;
}

.logo:hover {
  opacity: 0.8;
}
</style>