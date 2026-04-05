<template>
  <div class="card">
    <div class="header-guardia">
      <div>
        <h2>🏫 Aula de Convivencia</h2>
        <p class="info-guardia">
          <strong>Profesor:</strong> {{ profesorGuardia }} | 
          <strong>Fecha:</strong> {{ formatearFecha(fecha) }} | 
          <strong>Hora:</strong> {{ tramoActual }}ª
        </p>
      </div>
      <button @click="cargarPartes" class="btn btn-primary">
        🔄 Actualizar
      </button>
    </div>

    <div class="filtros">
      <div class="form-group">
        <label>Fecha</label>
        <input type="date" v-model="fecha" @change="cargarPartes" />
      </div>
      <div class="form-group">
        <label>Tramo Horario</label>
        <select v-model="tramoActual" @change="cargarPartes">
          <option value="1">1ª (8:15 - 9:15)</option>
          <option value="2">2ª (9:15 - 10:15)</option>
          <option value="3">3ª (10:15 - 11:15)</option>
          <option value="4">4ª (11:45 - 12:45)</option>
          <option value="5">5ª (12:45 - 13:45)</option>
          <option value="6">6ª (13:45 - 14:45)</option>
        </select>
      </div>
      <div class="form-group">
        <label>Email Profesor</label>
        <input 
          type="email" 
          v-model="profesorGuardia" 
          placeholder="profesor@instituto.es"
          :readonly="true"
          :disabled="true"
        />
      </div>
    </div>

    <div v-if="mensaje" :class="['alert', mensajeTipo === 'success' ? 'alert-success' : 'alert-error']">
      {{ mensaje }}
    </div>

    <div v-if="cargando" class="loading">
      <p>⏳ Cargando alumnos del aula de convivencia...</p>
    </div>

    <div v-else-if="partesAula.length === 0" class="sin-alumnos">
      <div class="empty-state">
        <span class="empty-icon">📭</span>
        <h3>No hay alumnos en el aula de convivencia</h3>
        <p>No hay partes pendientes para este día y tramo horario.</p>
      </div>
    </div>

    <div v-else class="tabla-container">
      <table>
        <thead>
          <tr>
            <th>N</th>
            <th>Alumno</th>
            <th>Grupo</th>
            <th>Doc.</th>
            <th>Tareas</th>
            <th>Trabaja</th>
            <th>Comportamiento</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(parte, index) in partesAula" :key="parte.id">
            <td class="numero">{{ index + 1 }}</td>
            <td class="alumno-nombre">
              <strong>{{ parte.alumnoApellidos }}, {{ parte.alumnoNombre }}</strong>
            </td>
            <td>{{ parte.curso }} {{ parte.grupo }}</td>
            <td class="centro">
              <button 
                v-if="parte.archivoUrl" 
                @click="abrirDocumento(parte.archivoUrl)"
                class="btn-icon"
                title="Ver documento adjunto"
              >
                📎
              </button>
              <span v-else class="no-doc">-</span>
            </td>
            <td>
              <button 
                @click="verTareas(parte)" 
                class="btn-small btn-info"
              >
                Ver tareas [+]
              </button>
            </td>
            <td>
              <select v-model="parte.trabaja" class="select-small">
                <option :value="null">Seleccione</option>
                <option :value="true">✅ Sí</option>
                <option :value="false">❌ No</option>
              </select>
            </td>
            <td>
              <select v-model="parte.comportamiento" class="select-small">
                <option value="">Seleccione</option>
                <option value="BIEN">😊 Bien</option>
                <option value="REGULAR">😐 Regular</option>
                <option value="MAL">😠 Mal</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="form-actions">
        <button @click="guardarSesiones" class="btn btn-success" :disabled="guardando">
          {{ guardando ? '⏳ Guardando...' : '💾 Guardar Evaluaciones' }}
        </button>
      </div>
    </div>

    <!-- Modal para ver tareas -->
    <div v-if="modalTareas" class="modal" @click="cerrarModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>📝 Tareas Asignadas</h3>
          <button @click="cerrarModal" class="btn-close">✕</button>
        </div>
        
        <div class="tareas-content">
          <div class="info-alumno">
            <p><strong>Alumno:</strong> {{ modalTareas.alumnoNombre }} {{ modalTareas.alumnoApellidos }}</p>
            <p><strong>Grupo:</strong> {{ modalTareas.curso }} {{ modalTareas.grupo }}</p>
          </div>
          
          <div class="seccion">
            <h4>📋 Descripción del Incidente:</h4>
            <p class="descripcion">{{ modalTareas.descripcion }}</p>
          </div>
          
          <div class="seccion">
            <h4>✏️ Tareas Asignadas:</h4>
            <p class="tareas">{{ modalTareas.tareas || 'No se especificaron tareas concretas' }}</p>
          </div>
          
          <div v-if="modalTareas.archivoUrl" class="seccion">
            <h4>📎 Documento Adjunto:</h4>
            <a :href="modalTareas.archivoUrl" target="_blank" class="link-documento">
              🔗 Abrir documento en nueva pestaña
            </a>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="cerrarModal" class="btn btn-secondary">Cerrar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

const API_URL = 'http://localhost:8080/api'

export default {
  name: 'AulaConvivencia',
  data() {
    return {
      partesAula: [],
      fecha: new Date().toISOString().split('T')[0],
      tramoActual: this.obtenerTramoActual(),
      profesorGuardia: 'antonio.oliver@g.educaand.es',
      modalTareas: null,
      mensaje: '',
      mensajeTipo: '',
      cargando: false,
      guardando: false
    }
  },
  mounted() {
    this.inicializarProfesorGuardia()
    this.cargarPartes()
  },
  methods: {
    inicializarProfesorGuardia() {
      const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
      if (profesor?.email) {
        this.profesorGuardia = profesor.email
      }
    },

    extraerMensajeError(error, mensajeDefecto = 'Error al guardar evaluación') {
      const status = error?.response?.status
      const detalle = error?.response?.data?.message
        || error?.response?.data?.error
      if (detalle) {
        return detalle
      }

      if (status === 400) {
        return 'Solicitud inválida al guardar la evaluación'
      }

      if (status === 403) {
        return 'No autorizado para evaluar en aula de convivencia'
      }

      if (status === 404) {
        return 'No se encontró el parte o el profesor'
      }

      if (status >= 500) {
        return 'Error interno del servidor al guardar la evaluación'
      }

      return error?.message || mensajeDefecto
    },

    obtenerTramoActual() {
      const hora = new Date().getHours()
      if (hora >= 8 && hora < 9) return '1'
      if (hora >= 9 && hora < 10) return '2'
      if (hora >= 10 && hora < 11) return '3'
      if (hora >= 11 && hora < 12) return '4'
      if (hora >= 12 && hora < 13) return '5'
      if (hora >= 13 && hora < 14) return '6'
      return '3' // Por defecto 3ª hora
    },
    
    async cargarPartes() {
      this.cargando = true
      this.mensaje = ''
      
      try {
        const [partesResponse, sesionesResponse] = await Promise.all([
          axios.get(`${API_URL}/partes/aula-convivencia`, {
            params: { fecha: this.fecha }
          }),
          axios.get(`${API_URL}/sesiones/fecha/${this.fecha}/tramo/${this.tramoActual}`)
        ])

        const sesionesPorParte = new Map(
          (sesionesResponse.data || [])
            .filter(sesion => sesion?.parte?.id)
            .map(sesion => [sesion.parte.id, sesion])
        )

        this.partesAula = (partesResponse.data || []).map(parte => {
          const sesionGuardada = sesionesPorParte.get(parte.id)
          return {
            ...parte,
            trabaja: sesionGuardada?.trabaja ?? null,
            comportamiento: sesionGuardada?.comportamiento || ''
          }
        })
        
        if (this.partesAula.length > 0) {
          this.mensaje = `✅ Se encontraron ${this.partesAula.length} alumno(s) en el aula de convivencia`
          this.mensajeTipo = 'success'
        }
      } catch (error) {
        console.error('Error al cargar partes:', error)
        this.mensaje = '❌ Error al cargar los partes del aula de convivencia'
        this.mensajeTipo = 'error'
      } finally {
        this.cargando = false
      }
    },
    
    verTareas(parte) {
      this.modalTareas = parte
    },
    
    cerrarModal() {
      this.modalTareas = null
    },
    
    abrirDocumento(url) {
      // Si es una URL relativa del backend, construir la URL completa
      if (url.startsWith('/api/')) {
        url = `http://localhost:8080${url}`
      }
      window.open(url, '_blank')
    },
    
    async guardarSesiones() {
      // Validar que al menos un alumno tenga evaluación
      const partesConEvaluacion = this.partesAula.filter(
        parte => parte.comportamiento || parte.trabaja !== null
      )
      
      if (partesConEvaluacion.length === 0) {
        this.mensaje = '⚠️ Por favor, evalúe al menos a un alumno antes de guardar'
        this.mensajeTipo = 'error'
        return
      }
      
      if (!this.profesorGuardia) {
        this.mensaje = '⚠️ Por favor, ingrese el email del profesor'
        this.mensajeTipo = 'error'
        return
      }
      
      this.guardando = true
      let guardados = 0
      let errores = 0
      let ultimoError = ''
      
      try {
        for (const parte of partesConEvaluacion) {
          try {
            await axios.post(`${API_URL}/sesiones`, {
              parteId: parte.id,
              profesorGuardiaEmail: this.profesorGuardia,
              fecha: this.fecha,
              tramoHorario: this.tramoActual,
              comportamiento: parte.comportamiento || null,
              trabaja: parte.trabaja,
              observaciones: ''
            })
            guardados++
          } catch (error) {
            console.error('Error al guardar sesión:', error)
            errores++
            ultimoError = this.extraerMensajeError(error)
          }
        }
        
        if (errores === 0) {
          this.mensaje = `✅ Guardado con éxito (${guardados} evaluación(es))`
          this.mensajeTipo = 'success'
          
          setTimeout(() => {
            this.mensaje = ''
            this.cargarPartes()
          }, 3000)
        } else {
          this.mensaje = `⚠️ Se guardaron ${guardados} evaluación(es), pero ${errores} tuvieron errores${ultimoError ? `: ${ultimoError}` : ''}`
          this.mensajeTipo = 'error'
        }
      } catch (error) {
        console.error('Error general:', error)
        this.mensaje = `❌ ${this.extraerMensajeError(error, 'Error al guardar las evaluaciones')}`
        this.mensajeTipo = 'error'
      } finally {
        this.guardando = false
      }
    },
    
    formatearFecha(fecha) {
      if (!fecha) return ''
      return new Date(fecha + 'T00:00:00').toLocaleDateString('es-ES', {
        weekday: 'long',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    }
  }
}
</script>

<style scoped>
.header-guardia {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 3px solid #667eea;
}

.info-guardia {
  color: #666;
  margin-top: 0.75rem;
  font-size: 1.05rem;
}

.info-guardia strong {
  color: #333;
}

.filtros {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 2rem;
  border-radius: 12px;
  border: 2px solid #e0e0e0;
}

.filtros .form-group {
  margin-bottom: 0;
}

.loading {
  text-align: center;
  padding: 3rem;
  color: #667eea;
  font-size: 1.1rem;
}

.sin-alumnos {
  padding: 3rem;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  background: #f8f9fa;
  border-radius: 12px;
  border: 2px dashed #dee2e6;
}

.empty-icon {
  font-size: 4rem;
  display: block;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #495057;
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: #6c757d;
}

.tabla-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border-radius: 8px;
  overflow: hidden;
}

table thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

table th {
  padding: 1.25rem;
  text-align: left;
  font-weight: 600;
  font-size: 0.95rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

table td {
  padding: 1.25rem;
  border-bottom: 1px solid #e0e0e0;
}

table tbody tr:hover {
  background: #f8f9fa;
}

table tbody tr:last-child td {
  border-bottom: none;
}

.numero {
  text-align: center;
  font-weight: 600;
  color: #667eea;
  font-size: 1.1rem;
}

.alumno-nombre {
  color: #333;
}

.centro {
  text-align: center;
}

.no-doc {
  color: #ccc;
  font-size: 1.2rem;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 1.8rem;
  cursor: pointer;
  transition: transform 0.2s;
}

.btn-icon:hover {
  transform: scale(1.2);
}

.btn-small {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-info:hover {
  background: #138496;
  transform: translateY(-2px);
}

.select-small {
  padding: 0.6rem;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  width: 100%;
  font-size: 0.95rem;
  transition: border-color 0.3s;
  cursor: pointer;
}

.select-small:focus {
  outline: none;
  border-color: #667eea;
}

.form-actions {
  margin-top: 2rem;
  text-align: right;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 16px;
  max-width: 700px;
  width: 90%;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0,0,0,0.3);
  animation: slideUp 0.3s;
}

@keyframes slideUp {
  from {
    transform: translateY(50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 2rem;
  border-bottom: 2px solid #e0e0e0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px 16px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
}

.btn-close {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  font-size: 1.5rem;
  width: 35px;
  height: 35px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-close:hover {
  background: rgba(255,255,255,0.3);
  transform: rotate(90deg);
}

.tareas-content {
  padding: 2rem;
}

.info-alumno {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
}

.info-alumno p {
  margin: 0.5rem 0;
}

.seccion {
  margin-bottom: 2rem;
}

.seccion h4 {
  color: #667eea;
  margin-bottom: 0.75rem;
  font-size: 1.1rem;
}

.descripcion,
.tareas {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 8px;
  line-height: 1.6;
  color: #333;
  border-left: 4px solid #667eea;
}

.link-documento {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  background: #667eea;
  color: white;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s;
}

.link-documento:hover {
  background: #764ba2;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.modal-footer {
  padding: 1.5rem 2rem;
  border-top: 2px solid #e0e0e0;
  text-align: right;
}

@media (max-width: 768px) {
  .header-guardia {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .filtros {
    grid-template-columns: 1fr;
  }
  
  table {
    font-size: 0.85rem;
  }
  
  table th,
  table td {
    padding: 0.75rem 0.5rem;
  }
  
  .modal-content {
    width: 95%;
  }
}
</style>