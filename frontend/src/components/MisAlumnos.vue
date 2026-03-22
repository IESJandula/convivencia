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
          <th>Acciones</th>
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
          <td>
            <button class="btn-ver" @click="verParte(parte)">Ver parte</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="parteSeleccionado" class="modal-overlay" @click.self="cerrarDetalle">
      <div class="modal-detalle">
        <div class="modal-header">
          <h3>Detalle del Parte</h3>
          <button class="btn-cerrar" @click="cerrarDetalle">×</button>
        </div>

        <div class="detalle-grid">
          <p><strong>Fecha:</strong> {{ formatearFecha(parteSeleccionado.fecha) }}</p>
          <p><strong>Alumno:</strong> {{ parteSeleccionado.alumno?.nombre }} {{ parteSeleccionado.alumno?.apellidos }}</p>
          <p><strong>Curso:</strong> {{ parteSeleccionado.alumno?.curso }} {{ parteSeleccionado.alumno?.grupo }}</p>
          <p><strong>Profesor:</strong> {{ parteSeleccionado.profesor?.nombre }}</p>
          <p><strong>Gravedad:</strong> {{ parteSeleccionado.gravedad }}</p>
          <p><strong>Estado:</strong> {{ parteSeleccionado.estado }}</p>
          <p><strong>Cómputo:</strong> {{ parteSeleccionado.estadoComputo }}</p>
          <p><strong>Conducta:</strong> {{ parteSeleccionado.conducta?.codigo }} - {{ parteSeleccionado.conducta?.descripcion }}</p>
          <p><strong>Medida tomada:</strong> {{ parteSeleccionado.medidaTomada || 'Sin medida' }}</p>
          <p><strong>Tareas:</strong> {{ parteSeleccionado.tareas || 'Sin tareas' }}</p>
        </div>

        <div class="descripcion">
          <h4>Descripción</h4>
          <p>{{ parteSeleccionado.descripcion || 'Sin descripción' }}</p>
        </div>
      </div>
    </div>
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
      parteSeleccionado: null,
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
    },
    verParte(parte) {
      this.parteSeleccionado = parte
    },
    cerrarDetalle() {
      this.parteSeleccionado = null
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
.btn-ver {
  border: 1px solid #0b4e6b;
  background: white;
  color: #0b4e6b;
  border-radius: 6px;
  padding: 0.35rem 0.6rem;
  cursor: pointer;
  font-weight: 600;
}
.btn-ver:hover { background: #e6f2f7; }

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 999;
}

.modal-detalle {
  width: min(760px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: white;
  border-radius: 10px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.25);
  padding: 1rem 1.2rem;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.btn-cerrar {
  border: none;
  background: transparent;
  font-size: 1.8rem;
  line-height: 1;
  cursor: pointer;
  color: #374151;
}

.detalle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.5rem 1rem;
  margin-bottom: 0.9rem;
}

.detalle-grid p,
.descripcion p {
  margin: 0;
}

.descripcion {
  border-top: 1px solid #e5e7eb;
  padding-top: 0.7rem;
}
</style>
