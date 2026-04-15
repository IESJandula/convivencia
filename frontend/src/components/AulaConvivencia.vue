<template>
  <div class="card">
    <transition name="toast-fade">
      <div v-if="toastVisible" :class="['creation-toast', toastTipo === 'success' ? 'ok' : 'err']">
        {{ toastMensaje }}
      </div>
    </transition>

    <div class="header-guardia">
      <h2>🏫 Aula de Convivencia</h2>
      <p class="subtitulo-guardia">Seguimiento del alumnado derivado por tramo horario</p>
      <div class="header-meta">
        <span class="meta-pill">
          <strong>Profesor:</strong> {{ profesorNombre || 'Sin identificar' }}
        </span>
        <span class="meta-pill">
          <strong>Tramo:</strong> {{ etiquetaTramo(tramoActual) }}
        </span>
      </div>
    </div>

    <div class="filtros">
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
      <div class="filtros-info">
        <span class="filtros-info-item">👥 Alumnos en aula: <strong>{{ partesAula.length }}</strong></span>
        <span class="filtros-info-item">📝 Recuerda guardar tras evaluar.</span>
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
        <colgroup>
          <col class="col-n" />
          <col class="col-alumno" />
          <col class="col-grupo" />
          <col class="col-doc" />
          <col class="col-tareas" />
          <col class="col-trabaja" />
          <col class="col-comportamiento" />
          <col class="col-observaciones" />
        </colgroup>
        <thead>
          <tr>
            <th>N</th>
            <th>Alumno</th>
            <th>Grupo</th>
            <th>Doc.</th>
            <th>Tareas</th>
            <th>Trabaja</th>
            <th>Comportamiento</th>
            <th>Observaciones</th>
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
                Ver
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
            <td>
              <textarea
                v-model="parte.observaciones"
                class="textarea-small"
                rows="2"
                placeholder="Añadir observaciones del tramo horario"
              ></textarea>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="form-actions">
        <button @click="guardarSesiones" class="btn btn-success btn-guardar-evaluaciones" :disabled="guardando">
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

const API_URL = 'http://http://api-convivencia.51.210.104.106.sslip.io/api'

export default {
  name: 'AulaConvivencia',
  data() {
    return {
      partesAula: [],
      fecha: new Date().toISOString().split('T')[0],
      tramoActual: this.obtenerTramoActual(),
      profesorGuardia: 'antonio.oliver@g.educaand.es',
      profesorNombre: '',
      modalTareas: null,
      mensaje: '',
      mensajeTipo: '',
      toastVisible: false,
      toastMensaje: '',
      toastTipo: 'success',
      toastTimer: null,
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
        this.profesorNombre = profesor.nombre || ''
      }
    },

    etiquetaTramo(tramo) {
      const tramos = {
        '1': '1ª (8:15 - 9:15)',
        '2': '2ª (9:15 - 10:15)',
        '3': '3ª (10:15 - 11:15)',
        '4': '4ª (11:45 - 12:45)',
        '5': '5ª (12:45 - 13:45)',
        '6': '6ª (13:45 - 14:45)'
      }
      return tramos[tramo] || `${tramo}ª`
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
            comportamiento: sesionGuardada?.comportamiento || '',
            observaciones: sesionGuardada?.observaciones || ''
          }
        })

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
        url = `http://http://api-convivencia.51.210.104.106.sslip.io${url}`
      }
      window.open(url, '_blank')
    },
    
    async guardarSesiones() {
      // Validar que al menos un alumno tenga evaluación
      const partesConEvaluacion = this.partesAula.filter(
        parte => parte.comportamiento || parte.trabaja !== null || (parte.observaciones && parte.observaciones.trim() !== '')
      )
      
      if (partesConEvaluacion.length === 0) {
        this.mensaje = '⚠️ Por favor, evalúe al menos a un alumno antes de guardar'
        this.mensajeTipo = 'error'
        this.mostrarToast('Evalua al menos a un alumno antes de guardar', 'error')
        return
      }
      
      if (!this.profesorGuardia) {
        this.mensaje = '⚠️ Por favor, ingrese el email del profesor'
        this.mensajeTipo = 'error'
        this.mostrarToast('No se ha identificado el profesor', 'error')
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
              observaciones: parte.observaciones ? parte.observaciones.trim() : ''
            })
            guardados++
          } catch (error) {
            console.error('Error al guardar sesión:', error)
            errores++
            ultimoError = this.extraerMensajeError(error)
          }
        }
        
        if (errores === 0) {
          this.mensaje = ''
          this.mensajeTipo = ''
          this.mostrarToast('Guardado con exito', 'success')
          this.cargarPartes()
        } else {
          this.mensaje = `⚠️ Se guardaron ${guardados} evaluación(es), pero ${errores} tuvieron errores${ultimoError ? `: ${ultimoError}` : ''}`
          this.mensajeTipo = 'error'
          this.mostrarToast(`Guardadas ${guardados} evaluaciones con ${errores} error(es)`, 'error')
        }
      } catch (error) {
        console.error('Error general:', error)
        this.mensaje = `❌ ${this.extraerMensajeError(error, 'Error al guardar las evaluaciones')}`
        this.mensajeTipo = 'error'
        this.mostrarToast('Error al guardar las evaluaciones', 'error')
      } finally {
        this.guardando = false
      }
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
  z-index: 2500;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.22);
}

.creation-toast.ok {
  background: linear-gradient(135deg, #0c9b58 0%, #0d7a4a 100%);
}

.creation-toast.err {
  background: linear-gradient(135deg, #d43d2f 0%, #b42318 100%);
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

.header-guardia {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.45rem;
  margin-bottom: 2rem;
  padding-bottom: 1.2rem;
  border-bottom: 3px solid #667eea;
}

.subtitulo-guardia {
  color: #5f6b7a;
  font-size: 0.96rem;
  margin: 0;
}

.header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.meta-pill {
  background: #f4f7ff;
  border: 1px solid #d7e0ff;
  color: #334155;
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  font-size: 0.88rem;
}

.meta-pill strong {
  color: #1f2937;
}

.filtros {
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 1rem;
  margin-bottom: 2rem;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 1.3rem;
  border-radius: 12px;
  border: 2px solid #e0e0e0;
}

.filtros .form-group {
  margin-bottom: 0;
}

.filtros-info {
  background: #ffffff;
  border: 1px solid #d8e1ef;
  border-radius: 10px;
  padding: 0.65rem 0.8rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  justify-content: center;
}

.filtros-info-item {
  font-size: 0.9rem;
  color: #334155;
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
  overflow-x: hidden;
}

table {
  width: 100%;
  table-layout: fixed;
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
  padding: 0.6rem 0.45rem;
  text-align: left;
  font-weight: 600;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

table th + th {
  border-left: 1px solid rgba(255,255,255,0.28);
}

table td {
  padding: 0.55rem 0.45rem;
  border-bottom: 1px solid #e0e0e0;
  vertical-align: top;
  overflow-wrap: anywhere;
}

.col-n { width: 4%; }
.col-alumno { width: 20%; }
.col-grupo { width: 10%; }
.col-doc { width: 6%; }
.col-tareas { width: 8%; }
.col-trabaja { width: 12%; }
.col-comportamiento { width: 16%; }
.col-observaciones { width: 24%; }

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
  font-size: 0.9rem;
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

.textarea-small {
  width: 100%;
  min-width: 0;
  padding: 0.4rem;
  border: 1px solid #d9dee8;
  border-radius: 6px;
  font-size: 0.8rem;
  resize: vertical;
  font-family: inherit;
  line-height: 1.3;
}

.alumno-nombre strong {
  display: block;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  transition: transform 0.2s;
}

.btn-icon:hover {
  transform: scale(1.2);
}

.btn-small {
  padding: 0.35rem 0.55rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.78rem;
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
  padding: 0.38rem;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  width: 100%;
  font-size: 0.8rem;
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

.btn-guardar-evaluaciones {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 45%, #1e40af 100%);
  color: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.18);
  border-radius: 12px;
  padding: 0.75rem 1.3rem;
  font-size: 0.92rem;
  font-weight: 700;
  letter-spacing: 0.2px;
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.25);
  transition: transform 0.16s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.btn-guardar-evaluaciones:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.04);
  box-shadow: 0 14px 24px rgba(29, 78, 216, 0.35);
}

.btn-guardar-evaluaciones:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 6px 12px rgba(29, 78, 216, 0.25);
}

.btn-guardar-evaluaciones:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.35), 0 10px 18px rgba(37, 99, 235, 0.25);
}

.btn-guardar-evaluaciones:disabled {
  opacity: 0.72;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
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
    align-items: flex-start;
    gap: 0.65rem;
  }
  
  .filtros {
    grid-template-columns: 1fr;
  }
  
  table {
    font-size: 0.72rem;
  }

  table th,
  table td {
    padding: 0.4rem 0.3rem;
  }

  .btn-small,
  .select-small,
  .textarea-small {
    font-size: 0.72rem;
  }

  .btn-icon {
    font-size: 1rem;
  }
  
  .modal-content {
    width: 95%;
  }
}
</style>