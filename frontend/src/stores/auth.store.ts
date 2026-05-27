import { ref } from 'vue';
import { loginUser, logoutUser, registerUser, refreshToken, changeForgottenPassword, getAuthenticatedUserProfile } from '../api/index';

export const useAuthStore = () => {
  // Intentar cargar el usuario desde localStorage al inicializar
  const storedUser = localStorage.getItem('authUser');
  const user = ref<any>(storedUser ? JSON.parse(storedUser) : null);
  const isAuthenticated = ref(!!storedUser);
  const error = ref<string | null>(null);
  const isValidatingSession = ref(false);

  const register = async (userData: any) => {
    try {
      const authResponse = await registerUser(userData);
      
      // Verificar que la respuesta sea exitosa
      if (authResponse?.status !== 'SUCCESS') {
        const errorMsg = authResponse?.message || authResponse?.error || 'Registration failed';
        throw new Error(errorMsg);
      }
      
      isAuthenticated.value = true;
      error.value = null;
      
      const basicUser = { username: userData.username };
      user.value = basicUser;
      localStorage.setItem('authUser', JSON.stringify(basicUser));
      
      return true;
    } catch (err: any) {
      error.value = err.response?.data?.message || err.message || 'Registration failed';
      isAuthenticated.value = false;
      user.value = null;
      localStorage.removeItem('authUser');
      return false;
    }
  };

  const login = async (username: string, password: string) => {
    try {
      const authResponse = await loginUser({ username, password });
      
      // Verificar que la respuesta sea exitosa (el backend devuelve status: 'SUCCESS' o 'FAILURE')
      if (authResponse?.status !== 'SUCCESS') {
        const errorMsg = authResponse?.message || authResponse?.error || 'Login failed';
        throw new Error(errorMsg);
      }
      
      isAuthenticated.value = true;
      error.value = null;
      
      const basicUser = { username };
      user.value = basicUser;
      localStorage.setItem('authUser', JSON.stringify(basicUser));
      
      return true;
    } catch (err: any) {
      // Capturar el mensaje de error del backend
      const errorMessage = err.response?.data?.message || err.message || 'Error al iniciar sesión';
      error.value = errorMessage;
      isAuthenticated.value = false;
      user.value = null;
      localStorage.removeItem('authUser');
      console.error('Error de login:', errorMessage);
      return false; // Retornamos false en caso de error
    }
  };

  const logout = async () => {
    try {
      await logoutUser();
      user.value = null;
      isAuthenticated.value = false;
      error.value = null;
      
      // Limpiar localStorage
      localStorage.removeItem('authUser');
      
      // Las cookies JWT son removidas por el backend (HttpOnly cookies)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Logout failed';
      
      // Incluso si falla, se limpia el estado local
      user.value = null;
      isAuthenticated.value = false;
      localStorage.removeItem('authUser');
    }
  };

  const refreshSession = async () => {
    try {
      const response = await refreshToken();
      // Las cookies JWT son renovadas por el backend (HttpOnly cookies)
      error.value = null;
      return response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Session refresh failed';
    }
  };

  const forgotPassword = async (forgottenPasswordData: any) => {
    try {
      await changeForgottenPassword(forgottenPasswordData);
      error.value = null;
      return true;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Password change failed';
      return false;
    }
  };

  const clearError = () => {
    error.value = null;
  };

  const loadUserProfile = async () => {
    try {
      const userResponse = await getAuthenticatedUserProfile();
      user.value = userResponse;
      
      // Actualizar localStorage
      localStorage.setItem('authUser', JSON.stringify(userResponse));
      
      return userResponse;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to load user information';
      throw err;
    }
  };

  // Validar sesión actual verificando si las cookies JWT son válidas
  const validateSession = async (): Promise<boolean> => {
    // Si no hay usuario en localStorage, no hay sesión
    if (!user.value) {
      return false;
    }

    try {
      // Intentar cargar el perfil del usuario para verificar si el JWT es válido
      await getAuthenticatedUserProfile();
      
      isAuthenticated.value = true;
      return true;
    } catch (err: any) {
      // Limpiar sesión si el token está expirado o es inválido
      console.error('Session validation failed:', err.response?.data?.message || err.message);
      const hadSessionExpiredFlag = sessionStorage.getItem('sessionExpired') === 'true';
      const isExpired = err.response?.status === 401 || err.response?.status === 403;
      await forceLogout();
      if (isExpired || hadSessionExpiredFlag) {
        sessionStorage.setItem('sessionExpired', 'true');
      }
      return false;
    }
  };

  // Forzar cierre de sesión (sin llamar al backend)
  const forceLogout = async () => {
    user.value = null;
    isAuthenticated.value = false;
    error.value = null;
    localStorage.removeItem('authUser');
    sessionStorage.clear();
  };

  return {
    user,
    isAuthenticated,
    error,
    isValidatingSession,
    register,
    login,
    logout,
    refreshSession,
    forgotPassword,
    clearError,
    loadUserProfile,
    validateSession,
    forceLogout,
  };

};
