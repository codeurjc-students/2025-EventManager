import { ref } from 'vue';
import { getUserInformation, updateUser, changePassword } from '../api/index';
import { UserDTO } from '../types/user';

export const useUserStore = () => {
  const user = ref<UserDTO | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const fetchUserInformation = async (userId: number) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await getUserInformation(userId);
      user.value = response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error fetching user information';
    } finally {
      loading.value = false;
    }
  };

  const updateUserInformation = async (userId: number, userData: UserDTO) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await updateUser(userId, userData);
      user.value = response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error updating user information';
    } finally {
      loading.value = false;
    }
  };

  const updateUserPassword = async (userId: number, passwordData: any) => {
    loading.value = true;
    error.value = null;
    try {
      await changePassword(userId, passwordData);
      // Si llega aquí, la operación fue exitosa
      error.value = null;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.response?.data?.error || 'Error updating user password';
      error.value = errorMessage;
      // Re-lanzar el error para que la vista pueda manejarlo también
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const clearError = () => {
    error.value = null;
  };

  return {
    user,
    loading,
    error,
    fetchUserInformation,
    updateUserInformation,
    updateUserPassword,
    clearError,
  };
};