<template>
  <div class="tutoria-page">
    <div class="section-header">
      <span class="section-kicker">Tutoría</span>
      <h2>Seguimiento del Grupo Tutor</h2>
      <p>Consulta rápida del estado disciplinario del alumnado asignado a tu tutoría.</p>
    </div>

    <div v-if="mensaje" :class="['alert', mensajeTipo === 'success' ? 'alert-success' : 'alert-error']">
      {{ mensaje }}
    </div>

    <div class="card-jandula" v-if="grupoTutoria">
      <div class="filtros-fecha">
        <div class="filtros-fecha-group">
          <label for="fecha-desde">Desde</label>
          <input id="fecha-desde" v-model="filtroFechaDesde" type="date" @change="aplicarFiltroFechas" />
        </div>
        <div class="filtros-fecha-group">
          <label for="fecha-hasta">Hasta</label>
          <input id="fecha-hasta" v-model="filtroFechaHasta" type="date" @change="aplicarFiltroFechas" />
        </div>
        <div class="filtros-fecha-actions">
          <button class="btn-filtro btn-filtro-secondary" @click="limpiarFiltroFechas">Limpiar</button>
        </div>
      </div>

      <div class="resumen-grid">
        <article class="metric-card principal">
          <span class="metric-label">Grupo tutoría</span>
          <strong class="metric-value">{{ grupoTutoria.curso }} {{ grupoTutoria.letra }}</strong>
          <small>Asignación activa del curso</small>
        </article>

        <article class="metric-card">
          <span class="metric-label">Total de partes</span>
          <strong class="metric-value">{{ partesFiltrados.length }}</strong>
          <small>Partes del rango seleccionado</small>
        </article>

        <article class="metric-card">
          <span class="metric-label">Pendientes</span>
          <strong class="metric-value">{{ totalPendientes }}</strong>
          <small>Requieren seguimiento</small>
        </article>

        <article class="metric-card">
          <span class="metric-label">Computados / expulsión</span>
          <strong class="metric-value">{{ totalComputados }}</strong>
          <small>Ya tramitados</small>
        </article>
      </div>

      <div class="table-shell" v-if="partesFiltrados.length">
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Alumno</th>
              <th>Profesor</th>
              <th>Gravedad</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="parte in partesPagina" :key="parte.id">
              <td>{{ formatearFecha(parte.fecha) }}</td>
              <td>
                <div class="alumno-col">
                  <strong>{{ parte.alumno.nombre }} {{ parte.alumno.apellidos }}</strong>
                </div>
              </td>
              <td>{{ parte.profesor.nombre }}</td>
              <td>
                <span :class="['gravedad-tag', parte.gravedad === 'GRAVE' ? 'grave' : 'leve']">
                  {{ parte.gravedad }}
                </span>
              </td>
              <td>
                <span :class="['badge', estaComputado(parte) ? 'badge-info' : 'badge-pending']">
                  {{ estaComputado(parte) ? 'EXPULSADO' : 'PENDIENTE' }}
                </span>
              </td>
              <td>
                <button class="btn-ver" @click="verParte(parte)">Ver parte</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="partesFiltrados.length && totalPaginas > 1" class="pagination">
        <button class="pagination-btn" :disabled="pagina === 0" @click="cambiarPagina(-1)">Anterior</button>
        <span class="pagination-info">Pagina {{ pagina + 1 }} de {{ totalPaginas }}</span>
        <button class="pagination-btn" :disabled="pagina >= totalPaginas - 1" @click="cambiarPagina(1)">Siguiente</button>
      </div>

      <div v-if="!partesFiltrados.length" class="empty-inline">
        No hay partes registrados para tu tutoría en el rango seleccionado.
      </div>
    </div>

    <div v-else class="empty-state">
      <h3>Sin grupo de tutoría asignado</h3>
      <p>No hay un grupo vinculado a tu usuario en este momento.</p>
    </div>

    <div v-if="parteSeleccionado" class="modal-overlay" @click.self="cerrarDetalle">
      <div class="modal-detalle">
        <div class="modal-header">
          <h3>Detalle del parte disciplinario</h3>
          <button class="btn-cerrar" @click="cerrarDetalle">×</button>
        </div>

        <div class="detalle-grid">
          <p><strong>Fecha:</strong> {{ formatearFecha(parteSeleccionado.fecha) }}</p>
          <p><strong>Alumno:</strong> {{ parteSeleccionado.alumno?.nombre }} {{ parteSeleccionado.alumno?.apellidos }}</p>
          <p><strong>Curso:</strong> {{ parteSeleccionado.alumno?.curso }} {{ parteSeleccionado.alumno?.grupo }}</p>
          <p><strong>Profesor:</strong> {{ parteSeleccionado.profesor?.nombre }}</p>
          <p><strong>Gravedad:</strong> {{ parteSeleccionado.gravedad }}</p>
          <p><strong>Estado:</strong> {{ estaComputado(parteSeleccionado) ? 'EXPULSADO' : 'PENDIENTE' }}</p>
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
import { API_URL } from '@/config/api'

export default {
  name: 'MisAlumnos',
  computed: {
    partesFiltrados() {
      const desde = this.filtroFechaDesde || ''
      const hasta = this.filtroFechaHasta || ''

      return (this.partes || []).filter(parte => {
        const fechaParte = this.normalizarFecha(parte.fecha)
        if (desde && fechaParte < desde) {
          return false
        }
        if (hasta && fechaParte > hasta) {
          return false
        }
        return true
      })
    },
    totalComputados() {
      return this.partesFiltrados.filter(parte => this.estaComputado(parte)).length
    },
    totalPendientes() {
      return this.partesFiltrados.filter(parte => !this.estaComputado(parte)).length
    },
    totalPaginas() {
      return Math.max(1, Math.ceil(this.partesFiltrados.length / this.pageSize))
    },
    partesPagina() {
      const inicio = this.pagina * this.pageSize
      const fin = inicio + this.pageSize
      return this.partesFiltrados.slice(inicio, fin)
    }
  },
  data() {
    return {
      grupoTutoria: null,
      partes: [],
      pagina: 0,
      pageSize: 10,
      filtroFechaDesde: '',
      filtroFechaHasta: '',
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
        this.pagina = 0
        this.filtroFechaDesde = ''
        this.filtroFechaHasta = ''

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
    normalizarFecha(fecha) {
      if (!fecha) return ''
      return String(fecha).split('T')[0]
    },
    aplicarFiltroFechas() {
      this.pagina = 0
    },
    limpiarFiltroFechas() {
      this.filtroFechaDesde = ''
      this.filtroFechaHasta = ''
      this.pagina = 0
    },
    estaComputado(parte) {
      if (!parte) return false

      return Boolean(
        parte.expulsionId ||
        parte.expulsion ||
        parte.fechaInicioExpulsion ||
        parte.fechaFinExpulsion ||
        parte.estadoComputo === 'COMPUTADO' ||
        parte.estado === 'COMPUTADO'
      )
    },
    verParte(parte) {
      this.parteSeleccionado = parte
    },
    cambiarPagina(delta) {
      const nuevaPagina = this.pagina + delta
      if (nuevaPagina < 0 || nuevaPagina >= this.totalPaginas) {
        return
      }
      this.pagina = nuevaPagina
    },
    cerrarDetalle() {
      this.parteSeleccionado = null
    }
  }
}
</script>

<style scoped>
.tutoria-page {
  display: grid;
  gap: 1rem;
}

.section-header {
  border-radius: 14px;
  padding: 1rem 1.2rem;
  background: linear-gradient(135deg, #0f3d63, #1f6a96);
  color: #fff;
  box-shadow: 0 10px 24px rgba(15, 61, 99, 0.25);
}

.section-kicker {
  display: inline-block;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.9;
}

.section-header h2 {
  margin: 0.25rem 0 0.35rem;
  font-size: 1.35rem;
}

.section-header p {
  margin: 0;
  opacity: 0.95;
  font-size: 0.95rem;
}

.card-jandula {
  background: #fff;
  border: 1px solid #d7e1ea;
  border-radius: 14px;
  box-shadow: 0 10px 28px rgba(27, 44, 62, 0.08);
  padding: 1rem;
}

.filtros-fecha {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  align-items: end;
  margin-bottom: 1rem;
  padding: 0.9rem;
  border: 1px solid #d9e6f2;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fbfe 0%, #eef5fb 100%);
}

.filtros-fecha-group {
  display: grid;
  gap: 0.35rem;
}

.filtros-fecha-group label {
  font-size: 0.78rem;
  font-weight: 700;
  color: #425b73;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.filtros-fecha-group input {
  width: 100%;
  border: 1px solid #cbd7e3;
  border-radius: 10px;
  padding: 0.55rem 0.7rem;
  color: #123a5a;
  background: #ffffff;
}

.filtros-fecha-actions {
  display: flex;
  gap: 0.6rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.btn-filtro {
  background: linear-gradient(135deg, #0f766e 0%, #0f766e 40%, #115e59 100%);
  color: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.15);
  border-radius: 10px;
  padding: 0.55rem 1rem;
  font-size: 0.84rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(15, 118, 110, 0.18);
}

.btn-filtro-secondary {
  background: #ffffff;
  color: #0f766e;
  border: 1px solid #9ccbc7;
  box-shadow: none;
}

.resumen-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.metric-card {
  background: #f8fbfe;
  border: 1px solid #d9e6f2;
  border-radius: 10px;
  padding: 0.75rem;
  display: grid;
  gap: 0.2rem;
}

.metric-card.principal {
  background: #eaf4ff;
  border-color: #b9d8f4;
}

.metric-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #425b73;
  font-weight: 700;
}

.metric-value {
  color: #123a5a;
  font-size: 1.2rem;
}

.metric-card small {
  color: #5d7184;
}

.table-shell {
  border: 1px solid #e1eaf1;
  border-radius: 10px;
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  vertical-align: middle;
}

th {
  background: #f5f8fb;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: #4c6278;
}

tbody tr:hover {
  background: #f8fbff;
}

.alumno-col {
  display: grid;
  gap: 0.1rem;
}

.alumno-col small {
  color: #6b7280;
}

.gravedad-tag {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 700;
}

.gravedad-tag.leve {
  background: #dcfce7;
  color: #166534;
}

.gravedad-tag.grave {
  background: #fee2e2;
  color: #991b1b;
}

.alert {
  padding: 0.75rem 1rem;
  border-radius: 6px;
}

.alert-success {
  background: #dcfce7;
  color: #166534;
}

.alert-error {
  background: #fee2e2;
  color: #991b1b;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.55rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}
.badge-info {
  background: #dbeafe;
  color: #1e40af;
}
.badge-pending {
  background: #fef3c7;
  color: #92400e;
}
.btn-ver {
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

.btn-ver:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 12px 22px rgba(15, 118, 110, 0.34);
}

.pagination {
  margin-top: 0.85rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.8rem;
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
  color: #1e293b;
  font-weight: 700;
}

.empty-inline {
  text-align: center;
  color: #546678;
  padding: 1.1rem 0.8rem;
}

.empty-state {
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 1.2rem;
  text-align: center;
  color: #5f7285;
  background: #f8fafc;
}

.empty-state h3 {
  margin: 0 0 0.35rem;
  color: #2d4b66;
}

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

@media (max-width: 980px) {
  .filtros-fecha {
    grid-template-columns: 1fr;
  }

  .filtros-fecha-actions {
    justify-content: stretch;
  }

  .btn-filtro {
    width: 100%;
  }

  .resumen-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .table-shell {
    overflow-x: auto;
  }

  table {
    min-width: 760px;
  }
}

@media (max-width: 640px) {
  .resumen-grid {
    grid-template-columns: 1fr;
  }
}
</style>
