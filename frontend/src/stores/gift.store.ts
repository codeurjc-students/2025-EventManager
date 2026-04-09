import { ref } from 'vue';
import { getGiftDetail, getGifts, createGift, updateGift, deleteGift, addGiftContribution } from '../api/index';
import { GiftDTO } from '../types/gift';

export const useGiftStore = () => {
  const gifts = ref<GiftDTO[]>([]);
  const giftDetail = ref<GiftDTO | null>(null);
  const totalGifts = ref<number>(0);
  const loading = ref<boolean>(false);
  const error = ref<string | null>(null);

  const fetchGiftDetail = async (eventCode: string, giftId: number) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await getGiftDetail(eventCode, giftId);
      giftDetail.value = response;
      return response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error fetching gift detail';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const fetchGifts = async (eventCode: string, params: any = {}) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await getGifts(eventCode, params);
      // getGifts ya devuelve response.data, así que response ES el objeto con data y page
      gifts.value = response.data || [];
      totalGifts.value = response.page?.totalElements || 0;
      return { data: response.data, total: response.page?.totalElements || 0 };
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error fetching gifts';
      return { data: [], total: 0 };
    } finally {
      loading.value = false;
    }
  };

  const addGift = async (eventCode: string, giftData: any) => {
    loading.value = true;
    error.value = null;
    try {
      await createGift(eventCode, giftData);
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error creating gift';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const editGift = async (eventCode: string, giftId: number, giftData: any) => {
    loading.value = true;
    error.value = null;
    try {
      await updateGift(eventCode, giftId, giftData);
      const response = await fetchGifts(eventCode);
      return response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error updating gift';
    } finally {
      loading.value = false;
    }
  };

  const removeGift = async (eventCode: string, giftId: number) => {
    loading.value = true;
    error.value = null;
    try {
      await deleteGift(eventCode, giftId);
      const response = await fetchGifts(eventCode);
      return response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error deleting gift';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const addContribution = async (eventCode: string, giftId: number, contributionData: any) => {
  loading.value = true;
  error.value = null;
  try {
    const response = await addGiftContribution(eventCode, giftId, contributionData);
    giftDetail.value = response;
    return response;
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Error adding contribution';
    throw err;
  } finally {
    loading.value = false;
  }
};

  return {
    giftDetail,
    gifts,
    loading,
    error,
    fetchGiftDetail,
    fetchGifts,
    addGift,
    editGift,
    removeGift,
    addContribution
  };
};