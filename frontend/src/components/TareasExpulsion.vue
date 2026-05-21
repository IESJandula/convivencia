<template>
  <div class="card tareas-page">
    <transition name="toast-fade">
      <div v-if="toastVisible" :class="['creation-toast', toastTipo === 'success' ? 'ok' : 'err']">
        {{ toastMensaje }}
      </div>
    </transition>

    <div class="header-expulsion">
      <div>
        <h2>📚 Tareas de Expulsión</h2>
        <p class="subtitulo">Gestiona y envia las tareas del alumnado expulsado de forma centralizada.</p>
      </div>
      <div class="stats-panel">
        <div class="stat-chip pendiente">
          <span class="stat-label">Pendientes</span>
          <strong>{{ pendientes }}</strong>
        </div>
        <div class="stat-chip completada">
          <span class="stat-label">Completadas</span>
          <strong>{{ completadas }}</strong>
        </div>
      </div>
    </div>

    <div v-if="mensaje && mensajeTipo === 'error'" class="alert alert-error">
      {{ mensaje }}
    </div>

    <div v-if="activeTab === 'pendientes' && tareasPendientes.length" class="task-grid">
      <article
        v-for="tarea in tareasPendientes"
        :key="tarea.id"
        :class="['task-card', tarea.estado === 'COMPLETADA' ? 'task-card-ok' : 'task-card-pending']"
      >
        <div class="task-head">
          <div>
            <h3>
              <button class="alumno-link" @click="abrirDetalleAlumno(tarea)">
                {{ tarea.alumnoNombreCompleto }}
              </button>
            </h3>
            <p class="asignatura">{{ tarea.asignatura }}</p>
          </div>
          <span :class="['pill', tarea.estado === 'COMPLETADA' ? 'pill-ok' : 'pill-pending']">
            {{ tarea.estado === 'COMPLETADA' ? 'Completada' : 'Pendiente' }}
          </span>
        </div>

        <div class="meta-row">
          <div class="meta-item">
            <span class="meta-label">Desde</span>
            <strong>{{ formatearFecha(tarea.fechaInicioExpulsion) }}</strong>
          </div>
          <div class="meta-item">
            <span class="meta-label">Hasta</span>
            <strong>{{ formatearFecha(tarea.fechaFinExpulsion) }}</strong>
          </div>
        </div>

        <label class="editor-label">Tarea del profesor</label>
        <textarea
          v-model="ediciones[tarea.id]"
          class="input-tarea"
          :disabled="tarea.estado === 'COMPLETADA' && !esModoEdicion(tarea.id)"
          rows="4"
          placeholder="Escribe aquí las tareas para el alumno durante la expulsión..."
        ></textarea>

        <div class="card-actions">
          <button
            class="btn-enviar"
            :disabled="(tarea.estado === 'COMPLETADA' && !esModoEdicion(tarea.id)) || cargandoId === tarea.id"
            @click="guardarTarea(tarea.id)"
          >
            {{ cargandoId === tarea.id ? 'Enviando...' : (tarea.estado === 'COMPLETADA' ? (esModoEdicion(tarea.id) ? 'Guardar cambios' : 'Enviadas') : 'Enviar tareas') }}
          </button>
        </div>
      </article>
    </div>

    <div v-if="activeTab === 'pendientes' && totalPages > 1" class="pagination">
      <button class="pagination-btn" :disabled="page === 0" @click="cambiarPagina(-1)">Anterior</button>
      <span class="pagination-info">Pagina {{ page + 1 }} de {{ totalPages }}</span>
      <button class="pagination-btn" :disabled="page >= totalPages - 1" @click="cambiarPagina(1)">Siguiente</button>
    </div>

    <p v-if="activeTab === 'pendientes' && !tareasPendientes.length" class="sin-datos">No tienes tareas de expulsión pendientes.</p>

    <div v-if="activeTab === 'enviadas' && tareasCompletadasList.length" class="task-grid sent-grid">
      <article
        v-for="tarea in tareasCompletadasList"
        :key="tarea.id"
        :class="['task-card', 'task-card-ok']"
      >
        <div class="task-head">
          <div>
            <h3>
              <button class="alumno-link" @click="abrirDetalleAlumno(tarea)">
                {{ tarea.alumnoNombreCompleto }}
              </button>
            </h3>
            <p class="asignatura">{{ tarea.asignatura }}</p>
          </div>
          <span class="pill pill-ok">Completada</span>
        </div>

        <div class="meta-row">
          <div class="meta-item">
            <span class="meta-label">Desde</span>
            <strong>{{ formatearFecha(tarea.fechaInicioExpulsion) }}</strong>
          </div>
          <div class="meta-item">
            <span class="meta-label">Hasta</span>
            <strong>{{ formatearFecha(tarea.fechaFinExpulsion) }}</strong>
          </div>
        </div>

        <div class="meta-item" v-if="tarea.fechaCompletada">
          <span class="meta-label">Completada el</span>
          <strong>{{ formatearFechaHora(tarea.fechaCompletada) }}</strong>
        </div>

        <label class="editor-label">Tarea enviada</label>
        <textarea
          v-model="ediciones[tarea.id]"
          class="input-tarea"
          :disabled="tarea.estado === 'COMPLETADA' && !esModoEdicion(tarea.id)"
          rows="4"
        ></textarea>

        <div class="card-actions">
          <button
            class="btn-enviar"
            :disabled="(tarea.estado === 'COMPLETADA' && !esModoEdicion(tarea.id)) || cargandoId === tarea.id"
            @click="guardarTarea(tarea.id)"
          >
            {{ cargandoId === tarea.id ? 'Enviando...' : (tarea.estado === 'COMPLETADA' ? (esModoEdicion(tarea.id) ? 'Guardar cambios' : 'Enviadas') : 'Enviar tareas') }}
          </button>
          <button
            class="btn-modificar"
            :disabled="cargandoId === tarea.id"
            @click="toggleModoEdicion(tarea)"
          >
            {{ esModoEdicion(tarea.id) ? 'Cancelar' : 'Modificar tareas' }}
          </button>
        </div>
      </article>
    </div>

    <div v-if="activeTab === 'enviadas' && totalPages > 1" class="pagination">
      <button class="pagination-btn" :disabled="page === 0" @click="cambiarPagina(-1)">Anterior</button>
      <span class="pagination-info">Pagina {{ page + 1 }} de {{ totalPages }}</span>
      <button class="pagination-btn" :disabled="page >= totalPages - 1" @click="cambiarPagina(1)">Siguiente</button>
    </div>

    <p v-if="activeTab === 'enviadas' && !tareasCompletadasList.length" class="sin-datos">No tienes tareas enviadas.</p>

    <div v-if="modalAlumno" class="modal-overlay" @click="cerrarDetalleAlumno">
      <div class="modal-detalle" @click.stop>
        <div class="modal-detalle-head">
          <div>
            <h3>Detalle del alumno</h3>
            <p>{{ modalAlumno.alumnoNombreCompleto }}</p>
          </div>
          <button class="modal-close" @click="cerrarDetalleAlumno">Cerrar</button>
        </div>

        <div class="modal-rango-fechas">
          <span><strong>Desde:</strong> {{ formatearFecha(modalAlumno.fechaInicioExpulsion) }}</span>
          <span><strong>Hasta:</strong> {{ formatearFecha(modalAlumno.fechaFinExpulsion) }}</span>
        </div>

        <div class="modal-tareas-lista">
          <article v-for="item in modalAlumno.tareas" :key="item.id" class="modal-tarea-item">
            <div class="modal-tarea-row">
              <strong>{{ item.asignatura || 'Sin asignatura' }}</strong>
              <span :class="['pill', item.estado === 'COMPLETADA' ? 'pill-ok' : 'pill-pending']">
                {{ item.estado === 'COMPLETADA' ? 'Completada' : 'Pendiente' }}
              </span>
            </div>
            <p><strong>Profesor:</strong> {{ item.profesorNombre || 'Sin nombre' }} ({{ item.profesorEmail }})</p>
            <p><strong>Tarea:</strong> {{ item.descripcionTarea || 'Sin tarea definida' }}</p>
          </article>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { API_URL } from '@/config/api'
const TEXTO_TAREA_AUTOGENERADA = 'tarea pendiente por expulsión del alumno'

export default {
  name: 'TareasExpulsion',
  data() {
    return {
      profesorEmail: '',
      profesorNombre: '',
      tareas: [],
      ediciones: {},
      modosEdicion: {},
      mensaje: '',
      mensajeTipo: 'success',
      toastVisible: false,
      toastMensaje: '',
      toastTipo: 'success',
      toastTimer: null,
      cargandoId: null,
      modalAlumno: null,
      activeTab: 'pendientes',
      page: 0,
      size: 4,
      totalElements: 0,
      totalPages: 0,
      resumenPendientes: null,
      resumenCompletadas: null,
      resumenTotal: null
    }
  },
  computed: {
    tareasPendientes() {
      return this.tareas
    },
    tareasCompletadasList() {
      return this.tareas
    },
    pendientes() {
      if (this.resumenPendientes !== null) {
        return this.resumenPendientes
      }
      return this.tareas.filter(t => t.estado !== 'COMPLETADA').length
    },
    completadas() {
      if (this.resumenCompletadas !== null) {
        return this.resumenCompletadas
      }
      return this.tareas.filter(t => t.estado === 'COMPLETADA').length
    }
  },
  mounted() {
    const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
    this.profesorEmail = profesor?.email || ''
    this.profesorNombre = profesor?.nombre || ''
    this.sincronizarTabDesdeRuta()
    this.cargarTareas()
    this.cargarResumen()
  },
  watch: {
    '$route.query.tab'() {
      this.sincronizarTabDesdeRuta()
      this.page = 0
      this.cargarTareas()
    }
  },
  methods: {
    sincronizarTabDesdeRuta() {
      const tab = this.$route?.query?.tab
      this.activeTab = tab === 'enviadas' ? 'enviadas' : 'pendientes'
    },
    estadoFiltroActual() {
      return this.activeTab === 'enviadas' ? 'COMPLETADA' : 'PENDIENTE'
    },
    cambiarPagina(delta) {
      const nuevaPagina = this.page + delta
      if (nuevaPagina < 0 || nuevaPagina >= this.totalPages) {
        return
      }
      this.page = nuevaPagina
      this.cargarTareas(true)
    },
    async cargarResumen() {
      if (!this.profesorEmail) return
      try {
        const { data } = await axios.get(`${API_URL}/tareas-expulsion/profesor/${this.profesorEmail}/resumen`)
        this.resumenPendientes = Number(data?.pendientes ?? 0)
        this.resumenCompletadas = Number(data?.completadas ?? 0)
        this.resumenTotal = Number(data?.total ?? 0)
        this.emitirPendientes()
      } catch {
        this.resumenPendientes = null
        this.resumenCompletadas = null
        this.resumenTotal = null
      }
    },
    async cargarTareas(preservarPagina = false) {
      if (!this.profesorEmail) return
      try {
        const params = {
          page: this.page,
          size: this.size,
          estado: this.estadoFiltroActual()
        }
        if (!preservarPagina) {
          this.page = 0
          params.page = 0
        }
        const { data } = await axios.get(`${API_URL}/tareas-expulsion/profesor/${this.profesorEmail}`, { params })
        const payload = data || {}
        this.tareas = Array.isArray(payload.content) ? payload.content : (Array.isArray(payload) ? payload : [])
        this.totalElements = Number(payload.totalElements || this.tareas.length)
        this.totalPages = Number(payload.totalPages || 1)
        this.ediciones = Object.fromEntries(
          this.tareas.map(t => [t.id, this.normalizarDescripcionParaEdicion(t.descripcionTarea)])
        )
        this.modosEdicion = {}
        this.emitirPendientes()
      } catch (error) {
        this.tareas = []
        this.totalElements = 0
        this.totalPages = 0
        const status = error?.response?.status
        const detalle = error?.response?.data?.error || error?.response?.data?.message
        this.mensaje = status
          ? `Error al cargar tareas de expulsión (HTTP ${status})${detalle ? `: ${detalle}` : ''}`
          : 'Error al cargar tareas de expulsión.'
        this.mensajeTipo = 'error'
        this.emitirPendientes()
      }
    },
    async guardarTarea(tareaId) {
      this.mensaje = ''
      this.cargandoId = tareaId
      try {
        const descripcionLimpia = this.normalizarDescripcionParaEdicion(this.ediciones[tareaId])
        await axios.patch(`${API_URL}/tareas-expulsion/${tareaId}`, {
          profesorEmail: this.profesorEmail,
          descripcionTarea: descripcionLimpia,
          completar: true
        })
        this.mensaje = ''
        this.mensajeTipo = 'success'
        this.mostrarToast('Tareas enviadas con exito', 'success')
        if (this.activeTab === 'pendientes') {
          await this.$router.replace({ path: '/tareas-expulsion', query: { tab: 'enviadas' } })
        } else {
          await this.cargarTareas(true)
        }
        await this.cargarResumen()
      } catch (error) {
        this.mensaje = error?.response?.data?.error || 'No se pudo actualizar la tarea.'
        this.mensajeTipo = 'error'
        this.mostrarToast('No se pudo enviar la tarea', 'error')
      } finally {
        this.cargandoId = null
      }
    },
    formatearFecha(fecha) {
      if (!fecha) return ''
      return new Date(fecha + 'T00:00:00').toLocaleDateString('es-ES')
    },
    formatearFechaHora(fechaHora) {
      if (!fechaHora) return ''
      return new Date(fechaHora).toLocaleString('es-ES')
    },
    normalizarDescripcionParaEdicion(descripcion) {
      const texto = (descripcion || '').trim()
      if (!texto) return ''

      if (texto.toLowerCase().startsWith(TEXTO_TAREA_AUTOGENERADA)) {
        return ''
      }

      return texto
    },
    esModoEdicion(tareaId) {
      return this.modosEdicion[tareaId] === true
    },
    toggleModoEdicion(tarea) {
      const tareaId = tarea.id
      const activado = !this.esModoEdicion(tareaId)
      this.modosEdicion = {
        ...this.modosEdicion,
        [tareaId]: activado
      }

      if (!activado) {
        this.ediciones = {
          ...this.ediciones,
          [tareaId]: this.normalizarDescripcionParaEdicion(tarea.descripcionTarea)
        }
      }
    },
    async abrirDetalleAlumno(tarea) {
      if (!tarea?.expulsionId) {
        return
      }

      this.modalAlumno = {
        alumnoNombreCompleto: tarea.alumnoNombreCompleto,
        fechaInicioExpulsion: tarea.fechaInicioExpulsion,
        fechaFinExpulsion: tarea.fechaFinExpulsion,
        tareas: []
      }

      try {
        const { data } = await axios.get(`${API_URL}/tareas-expulsion/expulsion/${tarea.expulsionId}`)
        const tareasAlumno = Array.isArray(data) ? data : []
        this.modalAlumno = {
          ...this.modalAlumno,
          tareas: tareasAlumno.sort((a, b) => (a.asignatura || '').localeCompare(b.asignatura || ''))
        }
      } catch {
        this.modalAlumno = {
          ...this.modalAlumno,
          tareas: []
        }
      }
    },
    cerrarDetalleAlumno() {
      this.modalAlumno = null
    },
    mostrarToast(texto, tipo = 'success') {
      if (this.toastTimer) {
        clearTimeout(this.toastTimer)
      }
      this.toastMensaje = texto
      this.toastTipo = tipo
      this.toastVisible = true
      this.toastTimer = setTimeout(() => {
        this.toastVisible = false
      }, 3000)
    },
    emitirPendientes() {
      const count = this.resumenPendientes !== null
        ? this.resumenPendientes
        : this.pendientes
      window.dispatchEvent(new CustomEvent('tareas-expulsion-pendientes', {
        detail: { count }
      }))
    }
  },
  beforeUnmount() {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer)
    }
  }
}
</script>

<style scoped>
.creation-toast {
  position: fixed;
  top: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1200;
  padding: 12px 18px;
  border-radius: 8px;
  font-weight: 700;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  border: 1px solid transparent;
}

.creation-toast.ok {
  background: linear-gradient(135deg, #0c9b58 0%, #0d7a4a 100%);
  color: #ffffff;
}

.creation-toast.err {
  background: linear-gradient(135deg, #d43d2f 0%, #b42318 100%);
  color: #ffffff;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -8px);
}

.tareas-page {
  border-radius: 14px;
}

.header-expulsion {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 3px solid #667eea;
}

h2 {
  margin: 0;
}

.subtitulo {
  margin: 0.35rem 0 0;
  color: #5f6b7a;
  font-size: 0.95rem;
}

.stats-panel {
  display: flex;
  gap: 0.6rem;
}


.stat-chip {
  min-width: 120px;
  border-radius: 10px;
  padding: 0.55rem 0.75rem;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  border: 1px solid transparent;
}

.stat-chip strong {
  font-size: 1.15rem;
  line-height: 1.1;
}

.stat-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 700;
  opacity: 0.85;
}

.stat-chip.pendiente {
  background: #fff7ed;
  color: #9a3412;
  border-color: #fed7aa;
}

.stat-chip.completada {
  background: #ecfeff;
  color: #0f766e;
  border-color: #99f6e4;
}

.alert {
  margin: 1rem 0;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-weight: 600;
}

.alert-error {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
  margin-top: 0.8rem;
}

.task-card {
  background: #ffffff;
  border: 1px solid #dbe5f4;
  border-radius: 14px;
  padding: 0.95rem;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.task-card-pending {
  border-left: 5px solid #f59e0b;
}

.task-card-ok {
  border-left: 5px solid #2563eb;
}

.task-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.task-head h3 {
  margin: 0;
  font-size: 1rem;
  color: #0f172a;
}

.alumno-link {
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  color: #0f172a;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  text-align: left;
}

.alumno-link:hover {
  color: #4338ca;
  text-decoration: underline;
}

.asignatura {
  margin: 0.2rem 0 0;
  font-size: 0.86rem;
  color: #475569;
  font-weight: 600;
}

.meta-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.meta-item {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 10px;
  padding: 0.45rem 0.55rem;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.meta-label {
  font-size: 0.7rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #64748b;
  font-weight: 700;
}

.meta-item strong {
  color: #1e293b;
  font-size: 0.86rem;
}

.editor-label {
  font-size: 0.8rem;
  color: #334155;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.input-tarea {
  width: 100%;
  min-width: 0;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0.55rem;
  font-family: inherit;
  font-size: 0.9rem;
}

.input-tarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.14);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.btn-enviar {
  background: linear-gradient(135deg, #0f766e 0%, #0f766e 40%, #115e59 100%);
  color: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.15);
  border-radius: 10px;
  padding: 0.55rem 1rem;
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(15, 118, 110, 0.26);
  transition: transform 0.16s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.btn-enviar:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 12px 22px rgba(15, 118, 110, 0.34);
}

.btn-enviar:disabled {
  opacity: 0.68;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.btn-modificar {
  background: #ffffff;
  color: #0f766e;
  border: 1px solid #99f6e4;
  border-radius: 10px;
  padding: 0.55rem 0.85rem;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}

.btn-modificar:hover:not(:disabled) {
  background: #0f766e;
  color: #ffffff;
  border-color: #0f766e;
}

.btn-modificar:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.pill {
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  font-weight: 700;
  font-size: 0.78rem;
  letter-spacing: 0.02em;
}

.pill-pending {
  background: #fef3c7;
  color: #92400e;
}

.pill-ok {
  background: #dcfce7;
  color: #166534;
}

.sin-datos {
  color: #64748b;
  margin-top: 1rem;
  padding: 1.1rem;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
}

.sent-grid {
  margin-top: 0.75rem;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1300;
  padding: 1rem;
}

.modal-detalle {
  width: min(760px, 96vw);
  max-height: 85vh;
  overflow: auto;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #dbe5f4;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
  padding: 1rem;
}

.modal-detalle-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0.75rem;
  margin-bottom: 0.75rem;
}

.modal-detalle-head h3 {
  margin: 0;
}

.modal-detalle-head p {
  margin: 0.2rem 0 0;
  color: #334155;
  font-weight: 600;
}

.modal-close {
  background: #ffffff;
  color: #0f766e;
  border: 1px solid #99f6e4;
  border-radius: 8px;
  padding: 0.4rem 0.7rem;
  font-weight: 700;
  cursor: pointer;
}

.modal-close:hover {
  background: #0f766e;
  color: #ffffff;
  border-color: #0f766e;
}

.modal-rango-fechas {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 0.8rem;
  color: #334155;
}

.modal-tareas-lista {
  display: grid;
  gap: 0.65rem;
}

.modal-tarea-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0.7rem;
  background: #f8fafc;
}

.modal-tarea-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.4rem;
}

.modal-tarea-item p {
  margin: 0.25rem 0 0;
  color: #334155;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.8rem;
  margin-top: 0.85rem;
}

.pagination-btn {
  background: #0f766e;
  color: #ffffff;
  border: none;
  padding: 0.45rem 0.9rem;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

.pagination-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.pagination-info {
  font-weight: 700;
  color: #1e293b;
}

@media (max-width: 900px) {
  .header-expulsion {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-panel {
    width: 100%;
  }

  .stat-chip {
    flex: 1;
  }

  .task-grid {
    grid-template-columns: 1fr;
  }

  .meta-row {
    grid-template-columns: 1fr;
  }

  .modal-detalle {
    padding: 0.8rem;
  }

  .modal-detalle-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .modal-close {
    align-self: flex-end;
  }
}

@media (max-width: 640px) {
  .stats-panel {
    width: 100%;
    flex-wrap: wrap;
  }

  .stat-chip {
    flex: 1 1 48%;
    min-width: 0;
  }

  .card-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .btn-enviar,
  .btn-modificar {
    width: 100%;
  }

  .modal-tarea-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
