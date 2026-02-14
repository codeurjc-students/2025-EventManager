<template>
  <div class="pagination">
    <button 
      @click="prevPage" 
      :disabled="currentPage === 1" 
      class="pagination-button"
    >
      &lt; Prev
    </button>
    
    <span class="pagination-info">
      Page {{ currentPage }} of {{ totalPages }}
    </span>
    
    <button 
      @click="nextPage" 
      :disabled="currentPage === totalPages" 
      class="pagination-button"
    >
      Next &gt;
    </button>
    
    <select v-model="pageSize" @change="updatePageSize" class="pagination-select">
      <option v-for="size in pageSizes" :key="size" :value="size">
        {{ size }} per page
      </option>
    </select>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, watch } from 'vue';

export default defineComponent({
  name: 'Pagination',
  props: {
    totalItems: {
      type: Number,
      required: true
    },
    currentPage: {
      type: Number,
      required: true
    },
    pageSize: {
      type: Number,
      required: true
    }
  },
  setup(props, { emit }) {
    const totalPages = ref(Math.ceil(props.totalItems / props.pageSize));
    const pageSizes = [10, 50, 100];
    const pageSize = ref(props.pageSize);

    const nextPage = () => {
      if (props.currentPage < totalPages.value) {
        emit('update:currentPage', props.currentPage + 1);
      }
    };

    const prevPage = () => {
      if (props.currentPage > 1) {
        emit('update:currentPage', props.currentPage - 1);
      }
    };

    const updatePageSize = () => {
      emit('update:pageSize', pageSize.value);
      totalPages.value = Math.ceil(props.totalItems / pageSize.value);
      emit('update:currentPage', 1); // Reset to first page on page size change
    };

    watch(() => props.totalItems, () => {
      totalPages.value = Math.ceil(props.totalItems / pageSize.value);
    });

    watch(() => props.pageSize, (newVal) => {
      pageSize.value = newVal;
      totalPages.value = Math.ceil(props.totalItems / newVal);
    });

    return {
      totalPages,
      pageSizes,
      pageSize,
      nextPage,
      prevPage,
      updatePageSize
    };
  }
});
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0;
}

.pagination-button {
  padding: 10px;
  margin: 0 5px;
  font-family: inherit;
}

.pagination-info {
  margin: 0 10px;
}

.pagination-select {
  margin-left: 10px;
  font-family: inherit;
}
</style>