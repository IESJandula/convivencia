<template>
  <div id="app-container">
    <header v-if="user" class="navbar">
      <nav>
        <span class="welcome-msg">Bienvenido, <strong>{{ user.first_name }}</strong></span>
        <button @click="logout" class="btn-logout">Cerrar Sesión</button>
      </nav>
    </header>

    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

const user = ref(null);
const router = useRouter();

// 1. Función para leer el usuario del almacenamiento local
const updateUserData = () => {
  const data = localStorage.getItem('user');
  if (data) {
    try {
      user.value = JSON.parse(data);
    } catch (e) {
      console.error("Error al leer los datos del usuario");
      user.value = null;
    }
  } else {
    user.value = null;
  }
};

// 2. Al montar el componente, revisamos si ya estaba logueado
onMounted(() => {
  updateUserData();
  // Escuchamos cambios en el localStorage por si el login ocurre en otra pestaña/componente
  window.addEventListener('storage', updateUserData);
  // Escuchamos un evento personalizado por si el login ocurre en esta misma sesión
  window.addEventListener('user-logged-in', updateUserData);
});

// Limpieza al destruir el componente
onUnmounted(() => {
  window.removeEventListener('storage', updateUserData);
  window.removeEventListener('user-logged-in', updateUserData);
});

// 3. Función de salida
const logout = () => {
  localStorage.removeItem('user'); // Borramos los datos
  user.value = null;               // Limpiamos el estado reactivo
  router.push('/login');           // Enviamos al usuario al Login
};
</script>

<style scoped>
/* Estilos modernos y limpios */
#app-container {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #333;
  max-width: 1200px;
  margin: 0 auto;
}

.navbar {
  background: #2c3e50;
  color: white;
  padding: 1rem 2rem;
  border-radius: 8px;
  margin-bottom: 2rem;
  display: flex;
  justify-content: flex-end;
}

.welcome-msg {
  margin-right: 20px;
  font-size: 0.9rem;
}

.btn-logout {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-logout:hover {
  background: #c0392b;
}

main {
  padding: 0 10px;
}
</style>