<template>
  <div class="card">
    <h2>⚖️ Monitorización Jefatura</h2>

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
      <table v-else>
        <thead>
          <tr>
            <th>Expulsión</th>
            <th>Alumno</th>
            <th>Curso</th>
            <th>Grupo</th>
            <th>Estado</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="exp in expulsionesPdf" :key="exp.expulsionId">
            <td>#{{ exp.expulsionId }}</td>
            <td>{{ exp.alumnoNombreCompleto || '-' }}</td>
            <td>{{ exp.curso || '-' }}</td>
            <td>{{ exp.grupo || '-' }}</td>
            <td>
              <span :class="['pill', exp.puedeGenerarPdf ? 'pill-ok' : 'pill-pending']">
                {{ exp.puedeGenerarPdf ? 'Lista para PDF' : 'Pendiente tareas' }}
              </span>
            </td>
            <td>
              <button
                class="btn btn-primary"
                :disabled="cargandoPdfId === exp.expulsionId || !exp.puedeGenerarPdf"
                @click="descargarPdfExpulsion(exp.expulsionId)"
              >
                {{ cargandoPdfId === exp.expulsionId ? 'Generando...' : 'Generar PDF' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <template v-else>
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

    <table v-if="partesActivos.length">
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

    <p v-else class="sin-datos">No hay partes en estado ACTIVO con esos filtros.</p>
    </template>
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
  },
  methods: {
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
          puedeGenerarPdf: false
        }

        this.expulsionesPdf = [
          nuevaFila,
          ...this.expulsionesPdf.filter(exp => exp.expulsionId !== data.expulsionId)
        ]
        this.guardarEstadoVista()

        this.mensaje = `Expulsión creada (ID ${data.expulsionId}). Partes computados: ${data.partesComputados}. Tareas generadas: ${data.tareasPendientesGeneradas}. Para descargar la carta PDF, debe haber al menos una tarea agregada con actividad real.`
        this.mensajeTipo = 'success'
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
        const { data: estadoPdf } = await axios.get(`${API_URL}/expulsiones/${expulsionId}/puede-generar-pdf`)
        if (!estadoPdf?.puedeGenerar) {
          this.mensaje = 'Aún no se puede generar la carta PDF: debe existir al menos una tarea agregada con actividad real.'
          this.mensajeTipo = 'error'
          return
        }

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
    }
  }
}
</script>

<style scoped>
.filtros { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 0.75rem; margin: 1rem 0 1.5rem; }
input, select { padding: 0.6rem; border: 1px solid #d1d5db; border-radius: 6px; }
.expulsion-panel { margin: 1rem 0; padding: 1rem; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 8px; }
.expulsion-form { display: grid; grid-template-columns: 1fr 1fr auto; gap: 0.75rem; margin-top: 0.75rem; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th, td { padding: 0.75rem; border-bottom: 1px solid #e5e7eb; text-align: left; }
.sin-datos { color: #666; margin-top: 1rem; }
.alert { margin: 1rem 0; padding: 0.75rem 1rem; border-radius: 6px; }
.alert-success { background: #dcfce7; color: #166534; }
.alert-error { background: #fee2e2; color: #991b1b; }
.pdf-actions { margin: 0.5rem 0 1rem; }
.pdf-actions-header { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; }
.pill { padding: 0.2rem 0.6rem; border-radius: 999px; font-weight: 600; font-size: 0.8rem; }
.pill-pending { background: #fef3c7; color: #92400e; }
.pill-ok { background: #dcfce7; color: #166534; }
</style>
