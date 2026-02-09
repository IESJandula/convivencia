<template>
  <div class="dashboard">
    <header class="dash-header">
      <h1>Panel de Convivencia</h1>
      <p>Usuario: <strong>{{ user.first_name }}</strong> | Rol: <span class="badge">{{ user.role }}</span></p>
    </header>

    <div class="grid-actions">
      <section class="card">
        <h3>Incidencias</h3>
        <button @click="router.push('/crear-parte')">➕ Crear Nuevo Parte</button>
      </section>

      <section v-if="user.role === 'TUTOR' || user.role === 'JEFATURA'" class="card tutor">
        <h3>Mi Tutoría: {{ user.group_name }}</h3>
        <button @click="router.push('/mis-tutorados')">📊 Ver mi grupo</button>
      </section>

      <section v-if="user.role === 'JEFATURA'" class="card admin">
        <h3>Gestión de Centro</h3>
        <button @click="router.push('/jefatura')">⚖️ Validar Expulsiones</button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('user')) || {});
</script>

<style scoped>
.grid-actions { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-top: 20px; }
.card { border: 1px solid #ddd; padding: 20px; border-radius: 10px; background: #fff; }
.tutor { border-left: 5px solid #1976d2; }
.admin { border-left: 5px solid #d32f2f; }
.badge { background: #eee; padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; }
</style>