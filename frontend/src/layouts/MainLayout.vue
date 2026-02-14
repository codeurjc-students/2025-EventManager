<template>
  <div class="main-layout">
    <BlueHeader />
    <main>
      <slot />
    </main>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import BlueHeader from '../components/BlueHeader.vue';

export default defineComponent({
  name: 'MainLayout',
  components: {
    BlueHeader,
  },
});
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>