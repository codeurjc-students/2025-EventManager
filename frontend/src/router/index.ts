import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth.store';
import RegisterView from '../views/AuthRegisterView.vue';
import LoginView from '../views/AuthLoginView.vue';
import ForgotPasswordView from '../views/AuthForgotPasswordView.vue';
import HomeView from '../views/HomeView.vue';
import UpdateProfileView from '../views/UserUpdateProfileView.vue';
import UpdatePasswordView from '../views/UserUpdatePasswordView.vue';
import CreateEventView from '../views/EventCreateView.vue';
import EventListView from '../views/EventListView.vue';
import EventUpdateView from '../views/EventUpdateView.vue';
import TicketEventDetailView from '../views/TicketEventDetailView.vue';
import JoinEventView from '../views/TicketEventJoinView.vue';
import TicketEventUpdateView from '../views/TicketEventUpdateView.vue';
import EventTicketsView from '../views/TicketEventListView.vue';
import EventGiftsView from '../views/GiftEventListView.vue';
import GiftDetailView from '../views/GiftDetailView.vue';

const routes = [
  {
    path: '/',
    name: 'Inicio',
    component: HomeView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/registro',
    name: 'Registro',
    component: RegisterView,
    meta: { requiresAuth: false }, // Ruta pública
  },
  {
    path: '/iniciar-sesion',
    name: 'IniciarSesion',
    component: LoginView,
    meta: { requiresAuth: false }, // Ruta pública
  },
  /*{
    path: '/cerrar-sesion',
    name: 'CerrarSesion',
    component: ,
  },
  {
    path: '/extender-sesion',
    name: 'ExtenderSesion',
    component: ,
  },*/
  {
    path: '/clave-olvidada',
    name: 'ClaveOlvidada',
    component: ForgotPasswordView,
    meta: { requiresAuth: false }, // Ruta pública
  },
  {
    path: '/usuario/actualizar-perfil',
    name: 'ActualizarPerfil',
    component: UpdateProfileView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/usuario/actualizar-clave',
    name: 'ActualizarClave',
    component: UpdatePasswordView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/crear-evento',
    name: 'CrearEvento',
    component: CreateEventView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/inscribirse-evento',
    name: 'InscribirseEvento',
    component: JoinEventView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/eventos',
    name: 'ListaEventos',
    component: EventListView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/eventos/:eventCode/:ticketId',
    name: 'DetalleEvento',
    component: TicketEventDetailView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/eventos/:eventCode/modificar',
    name: 'ModificarEvento',
    component: EventUpdateView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/eventos/:eventCode/tickets/:ticketId/actualizar-asistencia',
    name: 'ModificarAsistencia',
    component: TicketEventUpdateView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/evento/:eventCode/invitados',
    name: 'ConsultarInvitados',
    component: EventTicketsView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/evento/:eventCode/regalos',
    name: 'EventGifts',
    component: EventGiftsView,
    // Ruta privada - requiere autenticación
  },
  {
    path: '/evento/:eventCode/regalo/:giftId',
    name: 'DetalleRegalo',
    component: GiftDetailView,
    // Ruta privada - requiere autenticación
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// Validar sesión con el backend
const isSessionValid = async (authStore: any): Promise<boolean> => {
  try {
    return await authStore.validateSession();
  } catch (err) {
    console.error('Error validating session:', err);
    await authStore.forceLogout();
    return false;
  }
};

// Navigation guard: proteger rutas que requieren autenticación
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();
  const hasLocalAuth = !!localStorage.getItem('authUser');
  const requiresAuth = to.meta.requiresAuth !== false;
  const isLoginPage = to.name === 'IniciarSesion';

  // Rutas públicas
  if (!requiresAuth) {
    // Si el usuario tiene datos locales e intenta ir a login, validar sesión
    if (hasLocalAuth && isLoginPage) {
      const isValid = await isSessionValid(authStore);
      if (isValid) {
        next({ name: 'Inicio' });
        return;
      }
    }
    next();
    return;
  }

  // Rutas privadas
  if (!hasLocalAuth) {
    next({ name: 'IniciarSesion' });
    return;
  }

  // Validar sesión con el backend
  const isValid = await isSessionValid(authStore);
  if (!isValid) {
    next({ name: 'IniciarSesion' });
    return;
  }

  next();
});

export default router;