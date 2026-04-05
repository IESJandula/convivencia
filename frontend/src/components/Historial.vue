<template>
  <div class="card">
    <h2>📋 Historial de Partes</h2>

    <div class="filtros">
      <div class="form-group">
        <label>Fecha desde</label>
        <input type="date" v-model="filtroFechaDesde" @change="cargarPartes" />
      </div>
      <div class="form-group">
        <label>Fecha hasta</label>
        <input type="date" v-model="filtroFechaHasta" @change="cargarPartes" />
      </div>
      <button @click="limpiarFiltros" class="btn btn-secondary">
        🔄 Limpiar filtros
      </button>
    </div>

    <div v-if="partes.length === 0" class="sin-datos">
      <p>📭 No hay partes registrados</p>
    </div>

    <table v-else>
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
          <td>{{ parte.alumno.nombre }} {{ parte.alumno.apellidos }}</td>
          <td>{{ parte.curso }}</td>
          <td>
            <span :class="['badge', parte.gravedad === 'GRAVE' ? 'badge-danger' : 'badge-warning']">
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
              👁️ Ver
            </button>
          </td>
        </tr>
      </tbody>
    </table>

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
          <div v-if="parteSeleccionado.valoracionConvivencia" class="detalle-item full-width valoracion-section">
            <strong>Valoración del profesor en aula de convivencia:</strong>
            <div class="valoracion-grid">
              <div class="valoracion-campo">
                <span class="valoracion-label">Profesor de guardia</span>
                <span class="valoracion-value">{{ obtenerNombreProfesorGuardia(parteSeleccionado.valoracionConvivencia) }}</span>
              </div>
              <div class="valoracion-campo">
                <span class="valoracion-label">Comportamiento</span>
                <span class="badge" :class="claseComportamiento(parteSeleccionado.valoracionConvivencia)">
                  {{ textoComportamiento(parteSeleccionado.valoracionConvivencia) }}
                </span>
              </div>
              <div class="valoracion-campo">
                <span class="valoracion-label">Trabaja</span>
                <span class="badge" :class="claseTrabaja(parteSeleccionado.valoracionConvivencia)">
                  {{ textoTrabaja(parteSeleccionado.valoracionConvivencia) }}
                </span>
              </div>
              <div class="valoracion-campo full-row">
                <span class="valoracion-label">Observaciones</span>
                <p class="valoracion-observaciones">
                  {{ textoObservaciones(parteSeleccionado.valoracionConvivencia) }}
                </p>
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

const API_URL = 'http://localhost:8080/api'

export default {
  name: 'Historial',
  data() {
    return {
      partes: [],
      filtroFechaDesde: '',
      filtroFechaHasta: '',
      emailSesion: '',
      rolUsuario: 'PROFESOR',
      parteSeleccionado: null
    }
  },
  computed: {
    esVistaProfesor() {
      return this.rolUsuario === 'PROFESOR' && Boolean(this.emailSesion)
    }
  },
  mounted() {
    this.inicializarContextoUsuario()
    this.cargarPartes()
  },
  methods: {
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

    async cargarPartes() {
      try {
        if (this.esVistaProfesor) {
          const response = await axios.get(`${API_URL}/partes/profesor/${this.emailSesion}`)
          const partesProfesor = Array.isArray(response.data) ? response.data : []
          this.partes = partesProfesor.filter(parte => this.coincideConRangoFechas(parte.fecha))
          console.log('Partes del profesor cargados:', this.partes)
          return
        }

        const response = await axios.get(`${API_URL}/partes`)
        const partes = Array.isArray(response.data) ? response.data : []
        this.partes = partes.filter(parte => this.coincideConRangoFechas(parte.fecha))
        console.log('Partes cargados:', this.partes)
      } catch (error) {
        console.error('Error al cargar partes:', error)
      }
    },
    
    limpiarFiltros() {
      this.filtroFechaDesde = ''
      this.filtroFechaHasta = ''
      this.cargarPartes()
    },
    
    async verDetalle(parte) {
      console.log('Parte seleccionado:', parte)
      console.log('Archivo URL:', parte.archivoUrl)

      this.parteSeleccionado = {
        ...parte,
        valoracionConvivencia: null
      }

      if (parte.medidaTomada !== 'AULA_CONVIVENCIA') {
        return
      }

      try {
        const response = await axios.get(`${API_URL}/sesiones/parte/${parte.id}`)
        if (response?.status === 200 && response.data) {
          this.parteSeleccionado = {
            ...this.parteSeleccionado,
            valoracionConvivencia: response.data
          }
        }
      } catch (error) {
        if (error?.response?.status !== 204) {
          console.error('Error al cargar valoración de convivencia:', error)
        }
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
        const urlCompleta = `http://localhost:8080${url}`
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
.filtros {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  margin-bottom: 2rem;
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 8px;
}

.filtros .form-group {
  flex: 1;
  margin-bottom: 0;
  border: 1px solid #d9dee8;
  border-radius: 8px;
  padding: 0.75rem;
  background: #fff;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid #d9dee8;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

thead th {
  border-bottom: 1px solid #d9dee8;
  border-right: 1px solid #e8ecf3;
  padding: 0.8rem;
  background: #f4f6fb;
}

thead th:last-child {
  border-right: none;
}

tbody td {
  border-bottom: 1px solid #e8ecf3;
  border-right: 1px solid #eef1f6;
  padding: 0.8rem;
}

tbody td:last-child {
  border-right: none;
}

tbody tr:last-child td {
  border-bottom: none;
}

.sin-datos {
  text-align: center;
  padding: 3rem;
  color: #666;
  font-size: 1.1rem;
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
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
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
  color: #667eea;
}

.detalle-item p {
  margin: 0;
  color: #333;
}

.archivo-section {
  border: 2px solid #667eea;
}

.valoracion-section {
  border: 2px solid #b9d3ff;
  background: #f4f8ff;
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
  background: #667eea;
  color: white;
  border: none;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s;
  cursor: pointer;
  margin-top: 1rem;
}

.link-documento:hover {
  background: #5568d3;
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
  .valoracion-grid {
    grid-template-columns: 1fr;
  }
}
</style>