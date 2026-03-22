<template>
  <div class="card">
    <h2>👥 Mi Tutoría</h2>

    <div v-if="mensaje" :class="['alert', mensajeTipo === 'success' ? 'alert-success' : 'alert-error']">
      {{ mensaje }}
    </div>

    <div class="resumen" v-if="grupoTutoria">
      <p><strong>Grupo tutoría:</strong> {{ grupoTutoria.curso }} {{ grupoTutoria.letra }}</p>
      <p><strong>Total partes:</strong> {{ partes.length }}</p>
    </div>

    <div v-else class="sin-datos">
      <p>No tienes un grupo de tutoría asignado.</p>
    </div>

    <table v-if="partes.length">
      <thead>
        <tr>
          <th>Fecha</th>
          <th>Alumno</th>
          <th>Profesor</th>
          <th>Gravedad</th>
          <th>Estado</th>
          <th>Cómputo</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="parte in partes" :key="parte.id">
          <td>{{ formatearFecha(parte.fecha) }}</td>
          <td>{{ parte.alumno.nombre }} {{ parte.alumno.apellidos }}</td>
          <td>{{ parte.profesor.nombre }}</td>
          <td>{{ parte.gravedad }}</td>
          <td>{{ parte.estado }}</td>
          <td>{{ parte.estadoComputo }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import axios from 'axios'
const API_URL = 'http://localhost:8080/api'

export default {
  name: 'MisAlumnos',
  data() {
    return {
      grupoTutoria: null,
      partes: [],
      mensaje: '',
      mensajeTipo: 'success'
    }
  },
  mounted() {
    this.cargarMonitorizacionTutor()
  },
  methods: {
    async cargarMonitorizacionTutor() {
      const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
      if (!profesor?.email) return

      try {
        const { data } = await axios.get(`${API_URL}/monitorizacion/tutor/${profesor.email}`)
        this.grupoTutoria = data.grupoTutoria
        this.partes = data.partes || []

        if (!this.grupoTutoria) {
          this.mensaje = '⚠️ No hay grupo de tutoría asignado a tu usuario.'
          this.mensajeTipo = 'error'
        }
      } catch (error) {
        this.mensaje = '❌ Error al cargar la monitorización de tutoría'
        this.mensajeTipo = 'error'
      }
    },
    formatearFecha(fecha) {
      if (!fecha) return ''
      return new Date(fecha + 'T00:00:00').toLocaleDateString('es-ES')
    }
  }
}
</script>

<style scoped>
.resumen { margin: 1rem 0; padding: 1rem; background: #f8f9fa; border-radius: 8px; }
.sin-datos { margin: 1rem 0; color: #666; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th, td { padding: 0.75rem; border-bottom: 1px solid #e5e7eb; text-align: left; }
.alert { margin: 1rem 0; padding: 0.75rem 1rem; border-radius: 6px; }
.alert-success { background: #dcfce7; color: #166534; }
.alert-error { background: #fee2e2; color: #991b1b; }
</style>
