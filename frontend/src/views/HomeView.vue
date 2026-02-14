<template>
  <MainLayout>
    <div class="home-page">
      <div class="home-container">
        <!-- Cuadro de bienvenida -->
        <div class="welcome-box">
          <h1>Bienvenido a EventManager</h1>
          <p>Tu solución completa para la gestión de eventos</p>
        </div>
        
        <!-- Cuadro de menú de navegación -->
        <div class="menu-box">
          <!-- Sección de Ajustes de Usuario -->
          <div class="menu-section">
            <h3 class="menu-title">Ajustes de usuario</h3>
            <div class="menu-links">
              <router-link to="/usuario/actualizar-perfil" class="menu-link">
                Actualizar perfil
              </router-link>
              <router-link to="/usuario/actualizar-clave" class="menu-link">
                Actualizar contraseña
              </router-link>
            </div>
          </div>

          <!-- Sección de Eventos -->
          <div class="menu-section">
            <h3 class="menu-title">Eventos</h3>
            <div class="menu-links">
              <router-link to="/eventos" class="menu-link">
                Información de un evento
              </router-link>
              <router-link to="/crear-evento" class="menu-link">
                Crear nuevo evento
              </router-link>
              <router-link to="/inscribirse-evento" class="menu-link">
                Unirse a un evento
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import MainLayout from '@/layouts/MainLayout.vue';

export default defineComponent({
  name: 'HomeView',
  components: {
    MainLayout,
  },
});
</script>

<style scoped>
.home-page {
  /* Ocupa toda la altura disponible restando el header */
  min-height: calc(100vh - 60px);
  height: calc(100vh - 60px);
  
  /* Configuración del fondo para que se adapte perfectamente */
  background-image: url('@/assets/images/PantallaPrincipal.jpg');
  background-size: 75% auto; /* La imagen ocupa solo los 3/4 derechos */
  background-position: right center; /* Posicionar a la derecha y centrada verticalmente */
  background-repeat: no-repeat;
  background-attachment: scroll;
  
  /* Sin padding */
  padding: 0;
  
  /* Evita desbordamiento */
  overflow: hidden;
  
  /* Usar flexbox para posicionar el contenedor */
  display: flex;
  align-items: stretch; /* Estirar elementos a toda la altura */
}

.home-container {
  /* Contenedor pegado al borde izquierdo, ocupando 1/4 de la pantalla */
  width: 25%;
  max-width: 25%;
  min-width: 400px; /* Ancho mínimo para legibilidad */
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0; /* Sin espacio entre los recuadros */
  flex-shrink: 0; /* No se encoge */
  height: 100%; /* Ocupar toda la altura */
}

.welcome-box {
  background: rgba(255, 255, 255, 0.95);
  padding: 40px;
  border-radius: 0; /* Sin bordes redondeados en los lados */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  text-align: center;
  margin: 0;
  border-bottom: 1px solid #e0e0e0; /* Línea separadora */
  flex-shrink: 0; /* No se encoge */
}

.welcome-box h1 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 42px;
  font-weight: 700;
}

.welcome-box p {
  margin: 0;
  color: #666;
  font-size: 20px;
}

.menu-box {
  background: rgba(255, 255, 255, 0.95);
  padding: 40px;
  border-radius: 0; /* Sin bordes redondeados */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  gap: 30px;
  margin: 0;
  /* Ocupa todo el espacio vertical restante */
  flex: 1;
  overflow-y: auto; /* Scroll si el contenido es muy grande */
}

.menu-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.menu-title {
  margin: 0;
  color: #333;
  font-size: 24px;
  font-weight: bold;
  padding-bottom: 10px;
  border-bottom: 2px solid #e0e0e0;
}

.menu-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: 10px;
}

.menu-link {
  color: #5564eb;
  text-decoration: none;
  font-size: 18px;
  padding: 10px 15px;
  border-radius: 6px;
  transition: all 0.3s ease;
  background: transparent;
  display: block;
}

.menu-link:hover {
  background: rgba(85, 100, 235, 0.1);
  color: #3d4fc9;
  transform: translateX(5px);
}

.menu-link:active {
  background: rgba(85, 100, 235, 0.2);
}

/* Responsive: en pantallas pequeñas, ocupar más espacio */
@media (max-width: 1200px) {
  .home-container {
    width: 40%;
    max-width: 40%;
    min-width: 350px;
  }
  
  .home-page {
    background-size: 60% auto;
  }
}

@media (max-width: 768px) {
  .home-container {
    width: 100%;
    max-width: 100%;
    min-width: auto;
  }
  
  .home-page {
    background-size: cover;
    background-position: center center;
  }
  
  .welcome-box h1 {
    font-size: 32px;
  }
  
  .menu-title {
    font-size: 20px;
  }
  
  .menu-link {
    font-size: 16px;
  }
}
</style>