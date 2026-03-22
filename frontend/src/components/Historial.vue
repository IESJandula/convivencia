<template>
  <div class="card">
    <h2>📋 Historial de Partes</h2>

    <div class="filtros">
      <div class="form-group">
        <label>Filtrar por fecha</label>
        <input type="date" v-model="filtroFecha" @change="cargarPartes" />
      </div>
      <div class="form-group">
        <label>Filtrar por profesor (email)</label>
        <input type="email" v-model="filtroProfesor" @input="cargarPartes" placeholder="profesor@instituto.es" />
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
          <th>ID</th>
          <th>Fecha</th>
          <th>Profesor</th>
          <th>Alumno</th>
          <th>Curso</th>
          <th>Gravedad</th>
          <th>Medida</th>
          <th>Estado</th>
          <th>Cómputo</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="parte in partes" :key="parte.id">
          <td>{{ parte.id }}</td>
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
            <span :class="['badge', parte.estado === 'EVALUADO' ? 'badge-success' : 'badge-pending']">
              {{ parte.estado }}
            </span>
          </td>
          <td>
            <span :class="['badge', parte.estadoComputo === 'COMPUTADO' ? 'badge-info' : 'badge-success']">
              {{ parte.estadoComputo || 'ACTIVO' }}
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
      filtroFecha: '',
      filtroProfesor: '',
      parteSeleccionado: null
    }
  },
  mounted() {
    this.cargarPartes()
  },
  methods: {
    async cargarPartes() {
      try {
        let url = `${API_URL}/partes`
        
        if (this.filtroFecha) {
          url = `${API_URL}/partes/fecha/${this.filtroFecha}`
        } else if (this.filtroProfesor) {
          url = `${API_URL}/partes/profesor/${this.filtroProfesor}`
        }
        
        const response = await axios.get(url)
        this.partes = response.data
        console.log('Partes cargados:', this.partes)
      } catch (error) {
        console.error('Error al cargar partes:', error)
      }
    },
    
    limpiarFiltros() {
      this.filtroFecha = ''
      this.filtroProfesor = ''
      this.cargarPartes()
    },
    
    verDetalle(parte) {
      console.log('Parte seleccionado:', parte)
      console.log('Archivo URL:', parte.archivoUrl)
      this.parteSeleccionado = parte
    },
    
    cerrarModal() {
      this.parteSeleccionado = null
    },
    
    formatearFecha(fecha) {
      if (!fecha) return ''
      return new Date(fecha).toLocaleDateString('es-ES')
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
</style>