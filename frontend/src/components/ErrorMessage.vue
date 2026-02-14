<template>
  <div v-if="errorMessage" class="error-message">
    <p>{{ errorMessage }}</p>
    <button @click="closeError">Aceptar</button>
  </div>
</template>

<script>
export default {
  name: 'ErrorMessage',
  props: {
    errorMessage: {
      type: String,
      required: true
    }
  },
  methods: {
    closeError() {
      this.$emit('close');
    }
  }
}
</script>

<style scoped>
.error-message {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
  padding: 15px;
  border-radius: 5px;
  margin: 10px 0;
}
</style>