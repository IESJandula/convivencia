<template>
  <div class="card">
    <h2>📚 Tareas de Expulsión</h2>

    <div class="banner" v-if="pendientes > 0">
      ⚠️ Tienes {{ pendientes }} alumno{{ pendientes > 1 ? 's' : '' }} expulsado{{ pendientes > 1 ? 's' : '' }} pendiente{{ pendientes > 1 ? 's' : '' }} de tareas.
    </div>

    <div v-if="mensaje" :class="['alert', mensajeTipo === 'success' ? 'alert-success' : 'alert-error']">
      {{ mensaje }}
    </div>

    <div class="toolbar">
      <p><strong>Profesor:</strong> {{ profesorNombre }} ({{ profesorEmail }})</p>
      <button class="btn btn-primary" @click="cargarTareas">🔄 Actualizar</button>
    </div>

    <table v-if="tareas.length">
      <thead>
        <tr>
          <th>Expulsión</th>
          <th>Alumno</th>
          <th>Fechas</th>
          <th>Asignatura</th>
          <th>Tarea del profesor</th>
          <th>Estado</th>
          <th>Acción</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tarea in tareas" :key="tarea.id">
          <td>#{{ tarea.expulsionId }}</td>
          <td>{{ tarea.alumnoNombreCompleto }}</td>
          <td>{{ formatearFecha(tarea.fechaInicioExpulsion) }} - {{ formatearFecha(tarea.fechaFinExpulsion) }}</td>
          <td>{{ tarea.asignatura }}</td>
          <td>
            <textarea
              v-model="ediciones[tarea.id]"
              class="input-tarea"
              :disabled="tarea.estado === 'COMPLETADA'"
              rows="3"
              placeholder="Escribe aquí las tareas para el alumno durante la expulsión..."
            ></textarea>
          </td>
          <td>
            <span :class="['pill', tarea.estado === 'COMPLETADA' ? 'pill-ok' : 'pill-pending']">
              {{ tarea.estado }}
            </span>
          </td>
          <td>
            <div class="acciones">
              <button
                class="btn btn-primary"
                :disabled="tarea.estado === 'COMPLETADA' || cargandoId === tarea.id"
                @click="guardarTarea(tarea.id, false)"
              >
                {{ cargandoId === tarea.id ? 'Guardando...' : 'Guardar borrador' }}
              </button>
              <button
                class="btn btn-success"
                :disabled="tarea.estado === 'COMPLETADA' || cargandoId === tarea.id"
                @click="guardarTarea(tarea.id, true)"
              >
                {{ cargandoId === tarea.id ? 'Enviando...' : '✅ Enviar tareas' }}
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-else class="sin-datos">No tienes tareas de expulsión asignadas.</p>
  </div>
</template>

<script>
import axios from 'axios'

const API_URL = 'http://localhost:8080/api'
const TEXTO_TAREA_AUTOGENERADA = 'tarea pendiente por expulsión del alumno'

export default {
  name: 'TareasExpulsion',
  data() {
    return {
      profesorEmail: '',
      profesorNombre: '',
      tareas: [],
      ediciones: {},
      mensaje: '',
      mensajeTipo: 'success',
      cargandoId: null
    }
  },
  computed: {
    pendientes() {
      return this.tareas.filter(t => t.estado !== 'COMPLETADA').length
    }
  },
  mounted() {
    const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
    this.profesorEmail = profesor?.email || ''
    this.profesorNombre = profesor?.nombre || ''
    this.cargarTareas()
  },
  methods: {
    async cargarTareas() {
      if (!this.profesorEmail) return
      try {
        const { data } = await axios.get(`${API_URL}/tareas-expulsion/profesor/${this.profesorEmail}`)
        this.tareas = data || []
        this.ediciones = Object.fromEntries(
          this.tareas.map(t => [t.id, this.normalizarDescripcionParaEdicion(t.descripcionTarea)])
        )
      } catch (error) {
        this.tareas = []
        const status = error?.response?.status
        const detalle = error?.response?.data?.error || error?.response?.data?.message
        this.mensaje = status
          ? `Error al cargar tareas de expulsión (HTTP ${status})${detalle ? `: ${detalle}` : ''}`
          : 'Error al cargar tareas de expulsión.'
        this.mensajeTipo = 'error'
      }
    },
    async guardarTarea(tareaId, completar) {
      this.mensaje = ''
      this.cargandoId = tareaId
      try {
        const descripcionLimpia = this.normalizarDescripcionParaEdicion(this.ediciones[tareaId])
        await axios.patch(`${API_URL}/tareas-expulsion/${tareaId}`, {
          profesorEmail: this.profesorEmail,
          descripcionTarea: descripcionLimpia,
          completar
        })
        this.mensaje = completar
          ? 'Tareas enviadas y marcadas como completadas.'
          : 'Borrador de tarea guardado.'
        this.mensajeTipo = 'success'
        await this.cargarTareas()
      } catch (error) {
        this.mensaje = error?.response?.data?.error || 'No se pudo actualizar la tarea.'
        this.mensajeTipo = 'error'
      } finally {
        this.cargandoId = null
      }
    },
    formatearFecha(fecha) {
      if (!fecha) return ''
      return new Date(fecha + 'T00:00:00').toLocaleDateString('es-ES')
    },
    normalizarDescripcionParaEdicion(descripcion) {
      const texto = (descripcion || '').trim()
      if (!texto) return ''

      if (texto.toLowerCase().startsWith(TEXTO_TAREA_AUTOGENERADA)) {
        return ''
      }

      return texto
    }
  }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin: 1rem 0; }
.banner { margin-top: 1rem; padding: 0.75rem 1rem; border-radius: 6px; background: #fff7ed; color: #9a3412; font-weight: 600; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th, td { padding: 0.75rem; border-bottom: 1px solid #e5e7eb; text-align: left; }
.input-tarea { width: 100%; min-width: 280px; border: 1px solid #d1d5db; border-radius: 6px; padding: 0.5rem; font-family: inherit; }
.acciones { display: flex; gap: 0.5rem; flex-direction: column; }
.alert { margin: 1rem 0; padding: 0.75rem 1rem; border-radius: 6px; }
.alert-success { background: #dcfce7; color: #166534; }
.alert-error { background: #fee2e2; color: #991b1b; }
.pill { padding: 0.2rem 0.6rem; border-radius: 999px; font-weight: 600; font-size: 0.8rem; }
.pill-pending { background: #fef3c7; color: #92400e; }
.pill-ok { background: #dcfce7; color: #166534; }
.sin-datos { color: #666; margin-top: 1rem; }
</style>
