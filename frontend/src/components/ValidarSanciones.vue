<template>
  <div class="jefatura-page">
    <transition name="toast-fade">
      <div v-if="toastVisible" :class="['creation-toast', toastTipo === 'success' ? 'ok' : 'err']">
        {{ toastMensaje }}
      </div>
    </transition>

    <div class="section-header">
      <span class="section-kicker">Jefatura</span>
      <h2>{{ esVistaExpulsiones ? 'Cartas de Expulsión y PDFs' : 'Validación de Sanciones' }}</h2>
      <p>
        {{ esVistaExpulsiones
          ? 'Controla el estado de las expulsiones y genera cartas PDF cuando proceda.'
          : 'Filtra partes activos y tramita expulsiones de forma segura y ordenada.' }}
      </p>
    </div>

    <div class="card-jandula">
      <div class="resumen-grid" v-if="esVistaExpulsiones">
        <article class="metric-card principal">
          <span class="metric-label">Expulsiones</span>
          <strong class="metric-value">{{ expulsionesPdf.length }}</strong>
          <small>Total en listado</small>
        </article>
        <article class="metric-card">
          <span class="metric-label">Listas para PDF</span>
          <strong class="metric-value">{{ expulsionesListasPdf }}</strong>
          <small>Con tareas completadas</small>
        </article>
        <article class="metric-card">
          <span class="metric-label">Pendientes</span>
          <strong class="metric-value">{{ expulsionesPendientesPdf }}</strong>
          <small>Faltan tareas por validar</small>
        </article>
      </div>

      <div class="resumen-grid" v-else>
        <article class="metric-card principal">
          <span class="metric-label">Partes activos</span>
          <strong class="metric-value">{{ partesActivos.length }}</strong>
          <small>Seleccionables para expulsión</small>
        </article>
        <article class="metric-card">
          <span class="metric-label">Seleccionados</span>
          <strong class="metric-value">{{ selectedPartes.length }}</strong>
          <small>Marcados en tabla</small>
        </article>
        <article class="metric-card">
          <span class="metric-label">Alumno único</span>
          <strong class="metric-value">{{ alumnosSeleccionados }}</strong>
          <small>Debe ser 1 para crear expulsión</small>
        </article>
      </div>

    <div v-if="mensaje" :class="['alert', mensajeTipo === 'success' ? 'alert-success' : 'alert-error']">
      {{ mensaje }}
    </div>

    <div v-if="esVistaExpulsiones" class="pdf-actions">
      <div class="pdf-actions-header">
        <h3>Cartas PDF de expulsión</h3>
        <button class="btn btn-primary" @click="actualizarListadoPdf">🔄 Actualizar PDFs</button>
      </div>
      <p v-if="cargandoExpulsionesPdf && !expulsionesPdf.length" class="sin-datos">Cargando expulsiones...</p>
      <p v-else-if="!expulsionesPdf.length" class="sin-datos">No hay expulsiones registradas todavía.</p>
      <div class="table-shell" v-else>
      <table>
        <thead>
          <tr>
            <th>Alumno</th>
            <th>Curso</th>
            <th>Grupo</th>
            <th>Tareas</th>
            <th>Estado</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="exp in expulsionesPdf" :key="exp.expulsionId">
            <td>{{ exp.alumnoNombreCompleto || '-' }}</td>
            <td>{{ exp.curso || '-' }}</td>
            <td>{{ exp.grupo || '-' }}</td>
            <td>{{ formatearProgresoTareas(exp) }}</td>
            <td>
              <span :class="['pill', exp.puedeGenerarPdf ? 'pill-ok' : 'pill-pending']">
                {{ exp.puedeGenerarPdf ? 'Lista para PDF' : 'Pendiente tareas' }}
              </span>
            </td>
            <td>
              <button
                class="btn btn-primary"
                :disabled="cargandoPdfId === exp.expulsionId"
                @click="descargarPdfExpulsion(exp.expulsionId)"
              >
                {{ cargandoPdfId === exp.expulsionId ? 'Generando...' : 'Generar PDF' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      </div>
    </div>

    <template v-else>
    <div class="filtros-panel">
    <div class="filtros">
      <input v-model="filtros.nombreAlumno" type="text" placeholder="Buscar alumno por nombre" @keyup.enter="cargarPartes" />
      <select v-model="filtros.curso" @change="cargarPartes">
        <option value="">Todos los cursos</option>
        <option v-for="c in cursos" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="filtros.grupo" @change="cargarPartes">
        <option value="">Todos los grupos</option>
        <option v-for="g in grupos" :key="g" :value="g">{{ g }}</option>
      </select>
      <button class="btn btn-primary" @click="cargarPartes">Buscar</button>
    </div>
    </div>

    <div class="expulsion-panel" v-if="partesActivos.length">
      <p><strong>Partes ACTIVO seleccionables:</strong> {{ partesActivos.length }}</p>
      <p><strong>Seleccionados:</strong> {{ selectedPartes.length }}</p>
      <div class="expulsion-form">
        <input v-model="fechaInicio" type="date" />
        <input v-model="fechaFin" type="date" />
        <button class="btn btn-primary" :disabled="guardandoExpulsion" @click="crearExpulsion">
          {{ guardandoExpulsion ? 'Guardando...' : 'Crear expulsión' }}
        </button>
      </div>
    </div>

    <div class="table-shell" v-if="partesActivos.length">
    <table>
      <thead>
        <tr>
          <th>Sel.</th>
          <th>Fecha</th>
          <th>Alumno</th>
          <th>Curso</th>
          <th>Grupo</th>
          <th>Profesor</th>
          <th>Gravedad</th>
          <th>Cómputo</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="parte in partesActivos" :key="parte.id">
          <td>
            <input
              type="checkbox"
              :checked="selectedPartes.includes(parte.id)"
              @change="toggleParte(parte.id)"
            />
          </td>
          <td>{{ formatearFecha(parte.fecha) }}</td>
          <td>{{ parte.alumno.nombre }} {{ parte.alumno.apellidos }}</td>
          <td>{{ parte.alumno.curso }}</td>
          <td>{{ parte.alumno.grupo }}</td>
          <td>{{ parte.profesor.nombre }}</td>
          <td>{{ parte.gravedad }}</td>
          <td>{{ parte.estadoComputo }}</td>
        </tr>
      </tbody>
    </table>
    </div>

    <p v-else class="sin-datos">No hay partes en estado ACTIVO con esos filtros.</p>
    </template>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
const API_URL = 'http://localhost:8080/api'
const JEFATURA_STATE_KEY = 'jefatura_validar_sanciones_state'

export default {
  name: 'ValidarSanciones',
  data() {
    return {
      filtros: {
        nombreAlumno: '',
        curso: '',
        grupo: ''
      },
      partes: [],
      cursos: [],
      grupos: [],
      selectedPartes: [],
      fechaInicio: '',
      fechaFin: '',
      mensaje: '',
      mensajeTipo: 'success',
      toastVisible: false,
      toastMensaje: '',
      toastTipo: 'success',
      toastTimer: null,
      guardandoExpulsion: false,
      expulsionesPdf: [],
      cargandoPdfId: null,
      cargandoExpulsionesPdf: false,
      observadoresActivos: false
    }
  },
  computed: {
    esVistaExpulsiones() {
      return this.$route.path === '/expulsiones'
    },
    partesActivos() {
      return (this.partes || []).filter(p => p.estadoComputo === 'ACTIVO')
    },
    expulsionesListasPdf() {
      return (this.expulsionesPdf || []).filter(exp => exp?.puedeGenerarPdf).length
    },
    expulsionesPendientesPdf() {
      return (this.expulsionesPdf || []).filter(exp => !exp?.puedeGenerarPdf).length
    },
    alumnosSeleccionados() {
      const ids = new Set(
        this.partesActivos
          .filter(parte => this.selectedPartes.includes(parte.id))
          .map(parte => parte?.alumno?.id)
          .filter(Boolean)
      )
      return ids.size
    }
  },
  watch: {
    '$route.path'(nuevaRuta) {
      if (nuevaRuta === '/validar-sanciones' || nuevaRuta === '/expulsiones') {
        this.recargarVistaJefatura()
      }
    }
  },
  mounted() {
    this.restaurarEstadoVista()
    this.recargarVistaJefatura()
    this.activarObservadoresVista()
  },
  activated() {
    this.restaurarEstadoVista()
    this.recargarVistaJefatura()
    this.activarObservadoresVista()
  },
  deactivated() {
    this.guardarEstadoVista()
    this.desactivarObservadoresVista()
  },
  beforeUnmount() {
    this.guardarEstadoVista()
    this.desactivarObservadoresVista()
    if (this.toastTimer) {
      clearTimeout(this.toastTimer)
    }
  },
  methods: {
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
    guardarEstadoVista() {
      try {
        const estado = {
          filtros: this.filtros,
          cursos: this.cursos,
          grupos: this.grupos,
          partes: this.partes,
          selectedPartes: this.selectedPartes,
          fechaInicio: this.fechaInicio,
          fechaFin: this.fechaFin
        }
        sessionStorage.setItem(JEFATURA_STATE_KEY, JSON.stringify(estado))
      } catch {
      }
    },
    restaurarEstadoVista() {
      try {
        const raw = sessionStorage.getItem(JEFATURA_STATE_KEY)
        if (!raw) return
        const estado = JSON.parse(raw)
        if (!estado || typeof estado !== 'object') return

        this.filtros = estado.filtros || this.filtros
        this.cursos = Array.isArray(estado.cursos) ? estado.cursos : this.cursos
        this.grupos = Array.isArray(estado.grupos) ? estado.grupos : this.grupos
        this.partes = Array.isArray(estado.partes) ? estado.partes : this.partes
        this.selectedPartes = Array.isArray(estado.selectedPartes) ? estado.selectedPartes : this.selectedPartes
        this.fechaInicio = estado.fechaInicio || this.fechaInicio
        this.fechaFin = estado.fechaFin || this.fechaFin
      } catch {
      }
    },
    activarObservadoresVista() {
      if (this.observadoresActivos) {
        return
      }
      this.observadoresActivos = true
      document.addEventListener('visibilitychange', this.handleVisibilityChange)
      window.addEventListener('focus', this.handleWindowFocus)
    },
    desactivarObservadoresVista() {
      if (!this.observadoresActivos) {
        return
      }
      this.observadoresActivos = false
      document.removeEventListener('visibilitychange', this.handleVisibilityChange)
      window.removeEventListener('focus', this.handleWindowFocus)
    },
    async recargarVistaJefatura() {
      await Promise.all([
        this.cargarFiltros(),
        this.cargarPartes(),
        this.cargarExpulsionesPdf(true)
      ])
      await this.actualizarElegibilidadFilas()
      this.guardarEstadoVista()
    },
    async actualizarListadoPdf() {
      await this.cargarExpulsionesPdf(true)
      await this.actualizarElegibilidadFilas()
      this.guardarEstadoVista()
    },
    async cargarExpulsionesPdf(forzarRecuperacion = false, reintento = 0) {
      const mostrarCarga = reintento === 0 && (forzarRecuperacion || !this.expulsionesPdf.length)
      if (mostrarCarga) {
        this.cargandoExpulsionesPdf = true
      }
      try {
        const { data } = await axios.get(`${API_URL}/expulsiones/pendientes-pdf`)
        this.expulsionesPdf = data || []
        this.guardarEstadoVista()
      } catch {
        const maxReintentos = forzarRecuperacion ? 8 : 2
        if (reintento < maxReintentos) {
          await new Promise(resolve => setTimeout(resolve, 800))
          return this.cargarExpulsionesPdf(forzarRecuperacion, reintento + 1)
        }
      } finally {
        if (mostrarCarga) {
          this.cargandoExpulsionesPdf = false
        }
      }
    },
    async actualizarElegibilidadFilas() {
      if (!this.expulsionesPdf.length) {
        return
      }

      const actualizadas = await Promise.all(this.expulsionesPdf.map(async (exp) => {
        if (!exp?.expulsionId) {
          return exp
        }

        try {
          const { data } = await axios.get(`${API_URL}/expulsiones/${exp.expulsionId}/puede-generar-pdf`)
          return {
            ...exp,
            puedeGenerarPdf: Boolean(data?.puedeGenerar)
          }
        } catch {
          return exp
        }
      }))

      this.expulsionesPdf = actualizadas
    },
    async cargarFiltros() {
      try {
        const { data } = await axios.get(`${API_URL}/monitorizacion/jefatura/filtros`)
        this.cursos = data?.cursos || []
        this.grupos = data?.grupos || []
        this.guardarEstadoVista()
      } catch (error) {
        this.cursos = []
        this.grupos = []
      }
    },

    async cargarPartes() {
      try {
        const { data } = await axios.get(`${API_URL}/monitorizacion/jefatura`, {
          params: {
            nombreAlumno: this.filtros.nombreAlumno || null,
            curso: this.filtros.curso || null,
            grupo: this.filtros.grupo || null
          }
        })
        this.partes = data || []
        this.selectedPartes = []
        this.guardarEstadoVista()
      } catch (error) {
        this.partes = []
        this.selectedPartes = []
      }
    },
    handleVisibilityChange() {
      if (document.visibilityState === 'visible') {
        this.actualizarListadoPdf()
      }
    },
    handleWindowFocus() {
      this.actualizarListadoPdf()
    },
    toggleParte(parteId) {
      if (this.selectedPartes.includes(parteId)) {
        this.selectedPartes = this.selectedPartes.filter(id => id !== parteId)
      } else {
        this.selectedPartes = [...this.selectedPartes, parteId]
      }
    },
    async crearExpulsion() {
      this.mensaje = ''

      const seleccionados = this.partesActivos.filter(p => this.selectedPartes.includes(p.id))
      if (!seleccionados.length) {
        this.mensaje = 'Selecciona al menos un parte ACTIVO.'
        this.mensajeTipo = 'error'
        return
      }

      const alumnoIds = [...new Set(seleccionados.map(p => p.alumno.id))]
      if (alumnoIds.length !== 1) {
        this.mensaje = 'Solo puedes crear una expulsión para un alumno cada vez.'
        this.mensajeTipo = 'error'
        return
      }

      if (!this.fechaInicio || !this.fechaFin) {
        this.mensaje = 'Debes indicar fecha de inicio y fecha de fin.'
        this.mensajeTipo = 'error'
        return
      }

      const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
      if (!profesor?.email) {
        this.mensaje = 'No se ha encontrado el usuario de jefatura en sesión.'
        this.mensajeTipo = 'error'
        return
      }

      this.guardandoExpulsion = true
      try {
        const parteReferencia = seleccionados[0]
        const alumnoNombreCompleto = `${parteReferencia?.alumno?.nombre || ''} ${parteReferencia?.alumno?.apellidos || ''}`.trim()
        const cursoAlumno = parteReferencia?.alumno?.curso || ''
        const grupoAlumno = parteReferencia?.alumno?.grupo || ''

        const payload = {
          alumnoId: alumnoIds[0],
          jefaturaEmail: profesor.email,
          parteIds: seleccionados.map(p => p.id),
          fechaInicio: this.fechaInicio,
          fechaFin: this.fechaFin
        }

        const { data } = await axios.post(`${API_URL}/expulsiones`, payload)
        const nuevaFila = {
          expulsionId: data.expulsionId,
          alumnoId: alumnoIds[0],
          alumnoNombreCompleto,
          curso: cursoAlumno,
          grupo: grupoAlumno,
          puedeGenerarPdf: false,
          tareasCompletadas: 0,
          tareasTotales: Number(data?.tareasPendientesGeneradas || 0)
        }

        this.expulsionesPdf = [
          nuevaFila,
          ...this.expulsionesPdf.filter(exp => exp.expulsionId !== data.expulsionId)
        ]
        this.guardarEstadoVista()

        this.mensaje = ''
        this.mensajeTipo = 'success'
        this.mostrarToast(`Se ha expulsado al alumno ${alumnoNombreCompleto || 'seleccionado'} con exito.`, 'success')
        this.selectedPartes = []
        await this.cargarPartes()
        await this.actualizarListadoPdf()
      } catch (error) {
        this.mensaje = error?.response?.data?.error || 'Error al crear la expulsión.'
        this.mensajeTipo = 'error'
      } finally {
        this.guardandoExpulsion = false
      }
    },
    async descargarPdfExpulsion(expulsionId) {
      if (!expulsionId) {
        return
      }

      this.cargandoPdfId = expulsionId
      try {
        const response = await axios.get(`${API_URL}/expulsiones/${expulsionId}/pdf`, {
          responseType: 'blob'
        })

        const blob = new Blob([response.data], { type: 'application/pdf' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `carta-expulsion-${expulsionId}.pdf`
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        await this.cargarExpulsionesPdf()
      } catch (error) {
        let detalle = 'No se pudo descargar la carta PDF de expulsión.'
        const blob = error?.response?.data

        if (blob && typeof blob.text === 'function') {
          try {
            const text = await blob.text()
            if (text) {
              try {
                const json = JSON.parse(text)
                detalle = json?.error || json?.message || detalle
              } catch {
                detalle = text
              }
            }
          } catch {
          }
        }

        this.mensaje = detalle
        this.mensajeTipo = 'error'
      } finally {
        this.cargandoPdfId = null
      }
    },
    formatearFecha(fecha) {
      if (!fecha) return ''
      return new Date(fecha + 'T00:00:00').toLocaleDateString('es-ES')
    },
    formatearProgresoTareas(expulsion) {
      const completadas = Number(expulsion?.tareasCompletadas || 0)
      const totales = Number(expulsion?.tareasTotales || 0)
      return `${completadas}/${totales}`
    }
  }
}
</script>

<style scoped>
.jefatura-page {
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

.resumen-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
}

.creation-toast.err {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
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

.filtros-panel {
  background: #f7fafc;
  border: 1px solid #dbe6ef;
  border-radius: 10px;
  padding: 0.8rem;
  margin-bottom: 1rem;
}

.filtros {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr auto;
  gap: 0.75rem;
}

input,
select {
  padding: 0.62rem;
  border: 1px solid #cfd8e3;
  border-radius: 8px;
  background: #fff;
}

.expulsion-panel {
  margin: 1rem 0;
  padding: 1rem;
  background: #f8fafc;
  border: 1px solid #dbe6ef;
  border-radius: 10px;
}

.expulsion-form {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.table-shell {
  border: 1px solid #d3e0eb;
  border-radius: 12px;
  background: #f3f7fb;
  padding: 0.4rem;
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

th,
td {
  padding: 0.8rem 0.75rem;
  border-bottom: 1px solid #dde7ef;
  border-right: 1px solid #e8eff5;
  text-align: left;
  vertical-align: middle;
}

th:last-child,
td:last-child {
  border-right: none;
}

th {
  background: linear-gradient(180deg, #f6f9fc 0%, #edf3f8 100%);
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: #3f556b;
  border-bottom: 2px solid #c9d9e8;
}

tbody tr:nth-child(odd) td {
  background: #ffffff;
}

tbody tr:nth-child(even) td {
  background: #f8fbff;
}

tbody tr:hover td {
  background: #eaf4ff;
}

tbody tr:hover td:first-child {
  box-shadow: inset 3px 0 0 #2f6d96;
}

tbody tr:last-child td {
  border-bottom: none;
}

.sin-datos {
  color: #5f7285;
  margin-top: 1rem;
}

.alert {
  margin: 1rem 0;
  padding: 0.75rem 1rem;
  border-radius: 6px;
}
.alert-success { background: #dcfce7; color: #166534; }
.alert-error { background: #fee2e2; color: #991b1b; }

.btn {
  padding: 0.62rem 1rem;
  border-radius: 9px;
  font-weight: 800;
  letter-spacing: 0.02em;
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-primary {
  background: linear-gradient(135deg, #0f4c5c 0%, #16718a 100%);
  border-color: #0f4c5c;
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 76, 92, 0.3);
}

.pdf-actions { margin: 0.5rem 0 1rem; }

.pdf-actions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.pdf-actions-header h3 {
  margin: 0;
  color: #1f3f5a;
}

.pill {
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-weight: 600;
  font-size: 0.8rem;
}
.pill-pending { background: #fef3c7; color: #92400e; }
.pill-ok { background: #dcfce7; color: #166534; }

@media (max-width: 980px) {
  .resumen-grid {
    grid-template-columns: 1fr;
  }

  .filtros {
    grid-template-columns: 1fr 1fr;
  }

  .filtros button {
    grid-column: 1 / -1;
  }

  .table-shell {
    overflow-x: auto;
  }

  table {
    min-width: 900px;
  }
}

@media (max-width: 640px) {
  .filtros {
    grid-template-columns: 1fr;
  }

  .expulsion-form {
    grid-template-columns: 1fr;
  }
}
</style>
