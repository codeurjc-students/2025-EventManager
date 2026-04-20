import axios from 'axios';
import router from '../router';

// Usa VITE_API_URL si existe; si no, usa el mismo host/origen (producción)
const API_URL = import.meta.env.VITE_API_URL ?? '';

// Cliente para endpoints protegidos (requieren autenticación)
const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true // Importante: esto permite enviar cookies automáticamente
});

// Cliente para endpoints públicos (no requieren autenticación)
const publicApiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true // También habilitamos cookies para que funcione el login
});

// Flag para evitar múltiples intentos de refresh simultáneos
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  for (const prom of failedQueue) {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  }

  failedQueue = [];
};

// Interceptor para manejar respuestas de autenticación
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Si el token ha expirado o no es válido (401 Unauthorized)
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      if (isRefreshing) {
        // Si ya hay un refresh en progreso, añadir a la cola
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => {
            return apiClient(originalRequest);
          })
          .catch((err) => {
            throw err;
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Intentar renovar el token usando el RefreshToken
        const response = await publicApiClient.post('/api/auth/refresh');
        
        if (response.data?.status === 'SUCCESS') {
          isRefreshing = false;
          processQueue(null, 'success');
          
          // Reintentar la petición original
          return apiClient(originalRequest);
        } else {
          throw new Error('Refresh token failed');
        }
      } catch (refreshError) {
        isRefreshing = false;
        processQueue(refreshError, null);
        
        // Limpiar cualquier dato de sesión almacenado
        localStorage.removeItem('authUser');
        sessionStorage.clear();

        // Marcar sesión expirada para mostrar aviso en login
        sessionStorage.setItem('sessionExpired', 'true');

        // Redirigir a la vista de inicio de sesión
        router.replace('/iniciar-sesion');
        
        throw new Error('Sesión expirada. Por favor, inicia sesión nuevamente.');
      }
    }
    
    // Para otros errores, rechazar directamente
    throw error;
  }
);


// ===== AUTH API - ENDPOINTS PÚBLICOS =====
export const registerUser = async (userData: any) => {
  const response = await publicApiClient.post('/api/auth/register', userData);
  return response.data;
};

export const loginUser = async (credentials: any) => {
  const response = await publicApiClient.post('/api/auth/login', credentials);
  return response.data;
};

export const changeForgottenPassword = async (forgottenPasswordData: any) => {
  const response = await publicApiClient.post('/api/auth/forgot-password', forgottenPasswordData);
  return response.data;
};

// ===== AUTH API - ENDPOINTS PROTEGIDOS =====
export const logoutUser = async (): Promise<any> => {
  const response = await apiClient.post('/api/auth/logout');
  return response.data;
};

export const refreshToken = async () => {
  const response = await apiClient.post('/api/auth/refresh');
  return response.data;
};


// ===== USER API - ENDPOINTS PROTEGIDOS =====
export const getAuthenticatedUserProfile = async () => {
  const response = await apiClient.get('/api/user/profile');
  return response.data;
};

export const getUserInformationByUsername = async (username: string) => {
  const response = await apiClient.get('/api/user', { params: { username } });
  return response.data;
};

export const getUserInformation = async (userId: number) => {
  const response = await apiClient.get(`/api/user/${userId}`);
  return response.data;
};

export const updateUser = async (userId: number, userUpdateData: any) => {
  const response = await apiClient.put(`/api/user/${userId}`, userUpdateData);
  return response.data;
};

export const changePassword = async (userId: number, newPassword: string) => {
  const response = await apiClient.put(`/api/user/${userId}/change-password`, newPassword);
  return response.data;
};


// ===== EVENT API - ENDPOINTS PROTEGIDOS =====
export const getEvents = async (params: any) => {
  try {
    const response = await apiClient.get('/api/events', { params });
    return response.data;
  } catch (error: any) {
    throw error;
  }
};

export const createEvent = async (userId: number, eventData: any) => {
  const response = await apiClient.post(`/api/events?userId=${userId}`, eventData);
  return response.data;
};

export const updateEvent = async (eventCode: string, eventData: any) => {
  const response = await apiClient.put(`/api/events/${eventCode}`, eventData);
  return response.data;
};

export const getEventByCode = async (eventCode: string) => {
  const response = await apiClient.get(`/api/events/${eventCode}`);
  return response.data;
};


// ===== TICKET API - ENDPOINTS PROTEGIDOS =====
export const enrollUserInEvent = async (enrollmentData: any) => {
  const response = await apiClient.post('/api/events/enrollment', enrollmentData);
  return response.data;
};

export const getEventInformation = async (eventCode: string, ticketId: number, userId: number) => {
  const response = await apiClient.get(`/api/events/${eventCode}/tickets/${ticketId}`, {
    params: { userId }
  });
  return response.data;
};

export const getEventTickets = async (eventCode: string, params: any) => {
  const response = await apiClient.get(`/api/events/${eventCode}/tickets`, { params });
  return response.data;
};

export const updateTicket = async (eventCode: string, ticketId: number, ticketUpdateData: any) => {
  const response = await apiClient.put(`/api/events/${eventCode}/tickets/${ticketId}`, ticketUpdateData);
  return response.data;
};


// ===== GIFT API - ENDPOINTS PROTEGIDOS =====
export const getGiftDetail = async (eventCode: string, giftId: number) => {
  const response = await apiClient.get(`/api/events/${eventCode}/gifts/${giftId}`);
  return response.data;
};

export const getGifts = async (eventCode: string, params: any = {}) => {
  const resolvedParams = {
    ...params,
    page: params.page ?? 1,
    pageSize: params.pageSize ?? params.size ?? 10
  };
  delete resolvedParams.size;
  const response = await apiClient.get(`/api/events/${eventCode}/gifts`, { params: resolvedParams });
  return response.data;
};

export const createGift = async (eventCode: string, giftData: any) => {
  const response = await apiClient.post(`/api/events/${eventCode}/gifts`, giftData);
  return response.data;
};

export const updateGift = async (eventCode: string, giftId: number, giftData: any) => {
  const response = await apiClient.put(`/api/events/${eventCode}/gifts/${giftId}`, giftData);
  return response.data;
};

export const deleteGift = async (eventCode: string, giftId: number) => {
  const response = await apiClient.delete(`/api/events/${eventCode}/gifts/${giftId}`);
  return response.data;
};

export const addGiftContribution = async (eventCode: string, giftId: number, contributionData: any) => {
  const response = await apiClient.post(`/api/events/${eventCode}/gifts/${giftId}`, contributionData);
  return response.data;
};
