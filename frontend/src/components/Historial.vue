<template>
  <div class="historial-page">
    <div class="section-header">
      <span class="section-kicker">Convivencia</span>
      <h2>Historial de Partes</h2>
      <p>Consulta y seguimiento de partes disciplinarios con filtros por fecha y detalle completo.</p>
    </div>

    <div class="card-jandula">
      <div class="resumen-grid">
        <article class="metric-card principal">
          <span class="metric-label">Total registros</span>
          <strong class="metric-value">{{ totalElements }}</strong>
          <small>Partes en la vista actual</small>
        </article>

        <article class="metric-card">
          <span class="metric-label">Pendientes</span>
          <strong class="metric-value">{{ totalPendientes }}</strong>
          <small>Sin cómputo de expulsión</small>
        </article>

        <article class="metric-card">
          <span class="metric-label">Computados</span>
          <strong class="metric-value">{{ totalComputados }}</strong>
          <small>Expulsados o computados</small>
        </article>
      </div>

      <div class="filtros-panel">
        <div class="filtros">
          <div class="form-group">
            <label>Fecha desde</label>
            <input type="date" v-model="filtroFechaDesde" />
          </div>
          <div class="form-group">
            <label>Fecha hasta</label>
            <input type="date" v-model="filtroFechaHasta" />
          </div>
          <div class="form-group">
            <label>Alumno o Grupo</label>
            <input type="text" v-model="filtroTextoAlumno" placeholder="Buscar..." />
          </div>
          <div class="form-group" v-if="!esVistaProfesor">
            <label>Profesor</label>
            <input type="text" v-model="filtroProfesor" placeholder="Nombre de profesor" />
          </div>
          <div class="form-group">
            <label>Gravedad</label>
            <select v-model="filtroGravedad">
              <option value="">Todas</option>
              <option value="LEVE">Leve</option>
              <option value="GRAVE">Grave</option>
            </select>
          </div>
          <div class="form-group">
            <label>Conducta</label>
            <input type="text" v-model="filtroConducta" placeholder="Ej: Faltas de respeto" />
          </div>
          <div class="form-group">
            <label>Estado</label>
            <select v-model="filtroEstadoComputo">
              <option value="">Todos</option>
              <option value="PENDIENTE">Pendiente</option>
              <option value="COMPUTADO">Computado/Expulsado</option>
            </select>
          </div>
        </div>
      </div>

      <div v-if="$route.query.alumno_id && nombreAlumnoFiltrado" class="filtro-alumno-banner">
        <span>Mostrando partes de: <strong>{{ nombreAlumnoFiltrado }}</strong></span>
        <button @click="limpiarFiltroAlumno" class="btn-clear">Ver todos</button>
      </div>

      <div v-if="partes.length === 0" class="sin-datos">
        <h3>Sin resultados</h3>
        <p>No hay partes registrados para los filtros seleccionados.</p>
      </div>

      <div class="table-shell" v-else>
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Profesor</th>
              <th>Alumno</th>
              <th>Curso</th>
              <th>Gravedad</th>
              <th>Medida</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="parte in partes" :key="parte.id">
              <td>{{ formatearFecha(parte.fecha) }}</td>
              <td>{{ parte.profesor.nombre }}</td>
              <td>
                <div class="alumno-col">
                  <strong>{{ parte.alumno.nombre }} {{ parte.alumno.apellidos }}</strong>
                </div>
              </td>
              <td>{{ parte.curso }}</td>
              <td>
                <span :class="['gravedad-tag', parte.gravedad === 'GRAVE' ? 'grave' : 'leve']">
                  {{ parte.gravedad || 'LEVE' }}
                </span>
              </td>
              <td>
                <span :class="['badge', parte.medidaTomada === 'AULA_CONVIVENCIA' ? 'badge-warning' : 'badge-info']">
                  {{ parte.medidaTomada === 'AULA_CONVIVENCIA' ? 'Aula' : 'Clase' }}
                </span>
              </td>
              <td>
                <span :class="['badge', estaComputado(parte) ? 'badge-info' : 'badge-pending']">
                  {{ estaComputado(parte) ? 'EXPULSADO' : 'PENDIENTE' }}
                </span>
              </td>
              <td>
                <button @click="verDetalle(parte)" class="btn-small btn-primary">
                  Ver
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button class="pagination-btn" :disabled="page === 0" @click="cambiarPagina(-1)">Anterior</button>
        <span class="pagination-info">Pagina {{ page + 1 }} de {{ totalPages }}</span>
        <button class="pagination-btn" :disabled="page >= totalPages - 1" @click="cambiarPagina(1)">Siguiente</button>
      </div>
    </div>

    <!-- Modal detalle -->
    <div v-if="parteSeleccionado" class="modal" @click="cerrarModal">
      <div class="modal-content" @click.stop>
        <h3>📄 Detalle del Parte #{{ parteSeleccionado.id }}</h3>
        
        <div class="detalle-grid">
          <div class="detalle-item">
            <strong>Fecha:</strong> {{ formatearFecha(parteSeleccionado.fecha) }}
          </div>
          <div class="detalle-item">
            <strong>Profesor:</strong> {{ parteSeleccionado.profesor.nombre }}
          </div>
          <div class="detalle-item">
            <strong>Alumno:</strong> {{ parteSeleccionado.alumno.nombre }} {{ parteSeleccionado.alumno.apellidos }}
          </div>
          <div class="detalle-item">
            <strong>Curso:</strong> {{ parteSeleccionado.curso }}
          </div>
          <div class="detalle-item">
            <strong>Conducta:</strong> {{ parteSeleccionado.conducta.codigo }} - {{ parteSeleccionado.conducta.descripcion }}
          </div>
          <div class="detalle-item full-width">
            <strong>Descripción:</strong>
            <p>{{ parteSeleccionado.descripcion }}</p>
          </div>
          <div class="detalle-item">
            <strong>Gravedad:</strong> {{ parteSeleccionado.gravedad || 'LEVE' }}
          </div>
          <div class="detalle-item">
            <strong>Medida:</strong> {{ parteSeleccionado.medidaTomada === 'AULA_CONVIVENCIA' ? 'Aula de Convivencia' : 'Se queda en clase' }}
          </div>
          <div class="detalle-item">
            <strong>Estado:</strong> {{ parteSeleccionado.estado }}
          </div>
          <div class="detalle-item">
            <strong>Estado cómputo:</strong> {{ parteSeleccionado.estadoComputo || 'ACTIVO' }}
          </div>
          <div v-if="tieneValoracionesConvivencia" class="detalle-item full-width valoracion-section">
            <strong>Evaluaciones en aula de convivencia:</strong>
            <div class="valoracion-lista">
              <div
                v-for="(valoracion, index) in parteSeleccionado.valoracionesConvivencia"
                :key="valoracion.id || `${valoracion.fecha || 'sin-fecha'}-${valoracion.tramoHorario || 'sin-tramo'}-${index}`"
                class="valoracion-card"
              >
                <div class="valoracion-card-header">Evaluación {{ index + 1 }}</div>
                <div class="valoracion-grid">
                  <div class="valoracion-campo">
                    <span class="valoracion-label">Profesor</span>
                    <span class="valoracion-value">{{ obtenerNombreProfesorGuardia(valoracion) }}</span>
                  </div>
                  <div class="valoracion-campo">
                    <span class="valoracion-label">Tramo horario</span>
                    <span class="valoracion-value">{{ textoTramoHorario(valoracion) }}</span>
                  </div>
                  <div class="valoracion-campo">
                    <span class="valoracion-label">Fecha</span>
                    <span class="valoracion-value">{{ formatearFecha(valoracion.fecha) || 'No indicada' }}</span>
                  </div>
                  <div class="valoracion-campo">
                    <span class="valoracion-label">Comportamiento</span>
                    <span class="badge" :class="claseComportamiento(valoracion)">
                      {{ textoComportamiento(valoracion) }}
                    </span>
                  </div>
                  <div class="valoracion-campo">
                    <span class="valoracion-label">Trabaja</span>
                    <span class="badge" :class="claseTrabaja(valoracion)">
                      {{ textoTrabaja(valoracion) }}
                    </span>
                  </div>
                  <div class="valoracion-campo full-row">
                    <span class="valoracion-label">Observaciones</span>
                    <p class="valoracion-observaciones">
                      {{ textoObservaciones(valoracion) }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="parteSeleccionado.tareas" class="detalle-item full-width">
            <strong>Tareas:</strong>
            <p>{{ parteSeleccionado.tareas }}</p>
          </div>
          <div v-if="parteSeleccionado.archivoUrl" class="detalle-item full-width archivo-section">
            <strong>📎 Documento Adjunto:</strong>
            
            <!-- Preview de imagen si es imagen -->
            <div v-if="esImagen(parteSeleccionado.archivoUrl)" class="preview-imagen-container">
              <img 
                :src="obtenerUrlCompleta(parteSeleccionado.archivoUrl)" 
                alt="Documento adjunto" 
                class="imagen-adjunta"
                @error="errorCargaImagen"
              />
            </div>
            
            <!-- Botón para abrir -->
            <button @click="abrirDocumento(parteSeleccionado.archivoUrl)" class="link-documento">
              🔗 {{ esImagen(parteSeleccionado.archivoUrl) ? 'Ver imagen en tamaño completo' : 'Abrir documento' }}
            </button>
            
            <!-- Mostrar URL para debug -->
            <small class="url-debug">{{ obtenerUrlCompleta(parteSeleccionado.archivoUrl) }}</small>
          </div>
        </div>

        <button @click="cerrarModal" class="btn btn-secondary">
          Cerrar
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { API_URL } from '@/config/api'

export default {
  name: 'Historial',
  data() {
    return {
      partesOriginales: [],
      filtroFechaDesde: '',
      filtroFechaHasta: '',
      filtroTextoAlumno: '',
      filtroProfesor: '',
      filtroGravedad: '',
      filtroConducta: '',
      filtroEstadoComputo: '',
      emailSesion: '',
      rolUsuario: 'PROFESOR',
      parteSeleccionado: null,
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      cargaTimer: null
    }
  },
  computed: {
    partes() {
      return this.partesOriginales
    },
    esVistaProfesor() {
      return this.rolUsuario === 'PROFESOR' && Boolean(this.emailSesion)
    },
    totalComputados() {
      return this.partes.filter(parte => this.estaComputado(parte)).length
    },
    totalPendientes() {
      return this.partes.filter(parte => !this.estaComputado(parte)).length
    },
    tieneValoracionesConvivencia() {
      return Array.isArray(this.parteSeleccionado?.valoracionesConvivencia)
        && this.parteSeleccionado.valoracionesConvivencia.length > 0
    },
    nombreAlumnoFiltrado() {
      const id = this.$route.query.alumno_id
      if (!id || this.partesOriginales.length === 0) return ''
      const parte = this.partesOriginales.find(p => p.alumno.id === parseInt(id))
      return parte ? `${parte.alumno.nombre} ${parte.alumno.apellidos}` : ''
    }
  },
  mounted() {
    this.inicializarContextoUsuario()
    this.cargarPartes()
  },
  beforeUnmount() {
    if (this.cargaTimer) {
      clearTimeout(this.cargaTimer)
      this.cargaTimer = null
    }
  },
  watch: {
    filtroFechaDesde() {
      this.programarCarga()
    },
    filtroFechaHasta() {
      this.programarCarga()
    },
    filtroTextoAlumno() {
      this.programarCarga()
    },
    filtroProfesor() {
      this.programarCarga()
    },
    filtroGravedad() {
      this.programarCarga()
    },
    filtroConducta() {
      this.programarCarga()
    },
    filtroEstadoComputo() {
      this.programarCarga()
    },
    '$route.query.alumno_id'() {
      this.programarCarga()
    }
  },
  methods: {
    quitarTildes(texto) {
      if (!texto) return ''
      return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    },
    
    inicializarContextoUsuario() {
      const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
      this.emailSesion = profesor?.email || ''
      this.rolUsuario = profesor?.rol || this.inferirRolPorEmail(this.emailSesion)
    },

    inferirRolPorEmail(email) {
      if (!email) return 'PROFESOR'
      const emailNormalizado = email.toLowerCase()
      if (emailNormalizado.includes('jefe')) return 'JEFATURA'
      if (emailNormalizado.includes('tutor')) return 'TUTOR'
      return 'PROFESOR'
    },

    coincideConRangoFechas(fechaParte) {
      if (!fechaParte) return false
      const fechaNormalizada = String(fechaParte).split('T')[0]

      if (this.filtroFechaDesde && fechaNormalizada < this.filtroFechaDesde) {
        return false
      }

      if (this.filtroFechaHasta && fechaNormalizada > this.filtroFechaHasta) {
        return false
      }

      return true
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

    programarCarga() {
      if (this.cargaTimer) {
        clearTimeout(this.cargaTimer)
      }
      this.cargaTimer = setTimeout(() => {
        this.cargaTimer = null
        this.page = 0
        this.cargarPartes()
      }, 350)
    },

    cambiarPagina(delta) {
      const nuevaPagina = this.page + delta
      if (nuevaPagina < 0 || nuevaPagina >= this.totalPages) {
        return
      }
      this.page = nuevaPagina
      this.cargarPartes(true)
    },

    construirParams() {
      return {
        page: this.page,
        size: this.size,
        fechaDesde: this.filtroFechaDesde || null,
        fechaHasta: this.filtroFechaHasta || null,
        alumnoTexto: this.filtroTextoAlumno || null,
        profesorTexto: this.esVistaProfesor ? null : (this.filtroProfesor || null),
        gravedad: this.filtroGravedad || null,
        conductaTexto: this.filtroConducta || null,
        estadoComputo: this.filtroEstadoComputo || null,
        alumnoId: this.$route.query.alumno_id || null
      }
    },

    async cargarPartes(preservarPagina = false) {
      try {
        let datos = []
        const params = this.construirParams()
        if (!preservarPagina) {
          this.page = 0
          params.page = 0
        }
        if (this.esVistaProfesor) {
          const response = await axios.get(`${API_URL}/partes/profesor/${this.emailSesion}`, { params })
          const payload = response.data || {}
          datos = Array.isArray(payload.content) ? payload.content : (Array.isArray(payload) ? payload : [])
          this.totalElements = Number(payload.totalElements || datos.length)
          this.totalPages = Number(payload.totalPages || 1)
        } else {
          const response = await axios.get(`${API_URL}/partes`, { params })
          const payload = response.data || {}
          datos = Array.isArray(payload.content) ? payload.content : (Array.isArray(payload) ? payload : [])
          this.totalElements = Number(payload.totalElements || datos.length)
          this.totalPages = Number(payload.totalPages || 1)
        }
        
        this.partesOriginales = datos
      } catch (error) {
        console.error('Error al cargar partes:', error)
        this.partesOriginales = []
        this.totalElements = 0
        this.totalPages = 0
      }
    },
    
    limpiarFiltros() {
      this.filtroFechaDesde = ''
      this.filtroFechaHasta = ''
      this.filtroTextoAlumno = ''
      this.filtroProfesor = ''
      this.filtroGravedad = ''
      this.filtroConducta = ''
      this.filtroEstadoComputo = ''
      this.page = 0
      this.cargarPartes()
    },
    
    limpiarFiltroAlumno() {
      this.$router.push('/historial')
      this.page = 0
      this.cargarPartes()
    },
    
    async verDetalle(parte) {
      console.log('Parte seleccionado:', parte)
      console.log('Archivo URL:', parte.archivoUrl)

      this.parteSeleccionado = {
        ...parte,
        valoracionesConvivencia: []
      }

      if (parte.medidaTomada !== 'AULA_CONVIVENCIA') {
        return
      }

      try {
        const response = await axios.get(`${API_URL}/sesiones/parte/${parte.id}`)
        if (response?.status === 200) {
          const valoraciones = Array.isArray(response.data) ? response.data : []
          this.parteSeleccionado = {
            ...this.parteSeleccionado,
            valoracionesConvivencia: valoraciones
          }
        }
      } catch (error) {
        console.error('Error al cargar valoraciones de convivencia:', error)
      }
    },
    
    cerrarModal() {
      this.parteSeleccionado = null
    },
    
    formatearFecha(fecha) {
      if (!fecha) return ''
      return new Date(fecha).toLocaleDateString('es-ES')
    },

    obtenerNombreProfesorGuardia(valoracion) {
      if (!valoracion) return 'No indicado'
      return valoracion.profesorGuardia?.nombre || 'No indicado'
    },

    textoTramoHorario(valoracion) {
      const tramo = valoracion?.tramoHorario
      if (!tramo) return 'No indicado'
      return `${tramo}ª hora`
    },

    textoComportamiento(valoracion) {
      if (!valoracion?.comportamiento) return 'No indicado'

      const comportamiento = valoracion.comportamiento.toUpperCase()
      if (comportamiento === 'BIEN') return 'Bien'
      if (comportamiento === 'REGULAR') return 'Regular'
      if (comportamiento === 'MAL') return 'Mal'

      return valoracion.comportamiento
    },

    claseComportamiento(valoracion) {
      const comportamiento = valoracion?.comportamiento?.toUpperCase()
      if (comportamiento === 'BIEN') return 'badge-success'
      if (comportamiento === 'REGULAR') return 'badge-warning'
      if (comportamiento === 'MAL') return 'badge-danger'
      return 'badge-info'
    },

    textoTrabaja(valoracion) {
      if (valoracion?.trabaja === true) return 'Sí'
      if (valoracion?.trabaja === false) return 'No'
      return 'No indicado'
    },

    claseTrabaja(valoracion) {
      if (valoracion?.trabaja === true) return 'badge-success'
      if (valoracion?.trabaja === false) return 'badge-danger'
      return 'badge-info'
    },

    textoObservaciones(valoracion) {
      return valoracion?.observaciones || 'Sin observaciones'
    },
    
    obtenerUrlCompleta(url) {
      if (!url) return ''
      console.log('URL original:', url)
      
      // Si es una URL relativa del backend, construir la URL completa
      if (url.startsWith('/api/')) {
        const urlCompleta = `http://api-convivencia.51.210.104.106.sslip.io${url}`
        console.log('URL completa:', urlCompleta)
        return urlCompleta
      }
      
      // Si ya es una URL completa, devolverla tal cual
      console.log('URL completa (ya era completa):', url)
      return url
    },
    
    esImagen(url) {
      if (!url) return false
      const extension = url.toLowerCase()
      const esImg = extension.includes('.jpg') || 
             extension.includes('.jpeg') || 
             extension.includes('.png') || 
             extension.includes('.gif') ||
             extension.includes('.webp')
      console.log('Es imagen:', esImg, 'URL:', url)
      return esImg
    },
    
    abrirDocumento(url) {
      const urlCompleta = this.obtenerUrlCompleta(url)
      console.log('Abriendo documento:', urlCompleta)
      window.open(urlCompleta, '_blank')
    },
    
    errorCargaImagen(event) {
      console.error('Error al cargar imagen:', event)
      console.error('URL que falló:', event.target.src)
    }
  }
}
</script>

<style scoped>

.historial-page {
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

.filtros-panel {
  background: #f7fafc;
  border: 1px solid #dbe6ef;
  border-radius: 10px;
  padding: 0.8rem;
  margin-bottom: 1rem;
}

.filtro-alumno-banner {
  background: #fff3cd;
  color: #856404;
  padding: 0.8rem 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #ffeeba;
}

.btn-clear {
  background: #e0a800;
  color: white;
  border: none;
  padding: 0.3rem 0.7rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 600;
}

.btn-clear:hover {
  background: #c69500;
}

.filtros {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  align-items: end;
}

.filtros .form-group {
  margin-bottom: 0;
  border: 1px solid #d9dee8;
  border-radius: 8px;
  padding: 0.5rem;
  background: #fff;
  flex: 0 1 auto;
  min-width: 130px;
  max-width: 200px;
}

.btn-filtro {
  height: 40px;
}

.filtros input, .filtros select {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  padding: 0.2rem;
  font-size: 0.9rem;
  color: #333;
}

.filtros label {
  display: block;
  font-size: 0.75rem;
  font-weight: 600;
  color: #5f7285;
  margin-bottom: 0.2rem;
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
  position: sticky;
  top: 0;
  z-index: 2;
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

.sin-datos {
  text-align: center;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 1.2rem;
  color: #5f7285;
  background: #f8fafc;
}

.sin-datos h3 {
  margin: 0 0 0.35rem;
  color: #2d4b66;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.8rem;
  margin-top: 0.85rem;
}

.pagination-btn {
  background: #0f4c5c;
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
  color: #2d4b66;
}

.badge {
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 600;
}

.badge-warning {
  background: #fff3cd;
  color: #856404;
}

.badge-info {
  background: #d1ecf1;
  color: #0c5460;
}

.badge-success {
  background: #d4edda;
  color: #155724;
}

.badge-pending {
  background: #f8d7da;
  color: #721c24;
}

.badge-danger {
  background: #f8d7da;
  color: #721c24;
}

.btn-small {
  padding: 0.4rem 0.8rem;
  font-size: 0.85rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #1d5f86;
  color: white;
}

.btn-primary:hover {
  background: #164967;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  max-width: 800px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.detalle-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
  margin: 2rem 0;
}

.detalle-item {
  padding: 1rem;
  background: #f8f9fa;
  border: 1px solid #d9dee8;
  border-radius: 8px;
}

.detalle-item.full-width {
  grid-column: 1 / -1;
}

.detalle-item strong {
  display: block;
  margin-bottom: 0.5rem;
  color: #1b4f77;
}

.detalle-item p {
  margin: 0;
  color: #333;
}

.archivo-section {
  border: 2px solid #1d5f86;
}

.valoracion-section {
  border: 2px solid #b9d3ff;
  background: #f4f8ff;
}

.valoracion-lista {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.valoracion-card {
  background: #ffffff;
  border: 1px solid #d9e6ff;
  border-radius: 10px;
  padding: 0.8rem;
}

.valoracion-card-header {
  font-weight: 700;
  color: #2f466e;
  margin-bottom: 0.65rem;
}

.valoracion-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 0.5rem;
}

.valoracion-campo {
  background: #fff;
  border: 1px solid #d9e6ff;
  border-radius: 8px;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.valoracion-campo.full-row {
  grid-column: 1 / -1;
}

.valoracion-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: #4f5d79;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.valoracion-value {
  font-size: 1rem;
  color: #20324d;
  font-weight: 600;
}

.valoracion-observaciones {
  margin: 0;
  color: #20324d;
  line-height: 1.45;
}

.preview-imagen-container {
  margin: 1rem 0;
  text-align: center;
  background: white;
  padding: 1rem;
  border-radius: 8px;
}

.imagen-adjunta {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  cursor: pointer;
  transition: transform 0.3s;
}

.imagen-adjunta:hover {
  transform: scale(1.02);
}

.link-documento {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  background: #1d5f86;
  color: white;
  border: none;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s;
  cursor: pointer;
  margin-top: 1rem;
}

.link-documento:hover {
  background: #164967;
  transform: translateY(-2px);
}

.url-debug {
  display: block;
  margin-top: 0.5rem;
  color: #666;
  font-size: 0.8rem;
  word-break: break-all;
}

@media (max-width: 640px) {
  .resumen-grid {
    grid-template-columns: 1fr;
  }

  .filtros {
    grid-template-columns: 1fr;
  }

  .table-shell {
    overflow-x: auto;
  }

  table {
    min-width: 840px;
  }

  .valoracion-grid {
    grid-template-columns: 1fr;
  }
}
</style>