<template>
  <div class="jandula-page-wrapper">
    <transition name="toast-fade">
      <div v-if="toastVisible" :class="['creation-toast', toastTipo === 'success' ? 'ok' : 'err']">
        {{ toastMensaje }}
      </div>
    </transition>

    <div class="section-header">
      <span class="section-kicker">Convivencia</span>
      <h2>Registro de incidencia disciplinaria</h2>
      <p>Complete el formulario con la información del parte y, si procede, la derivación al Aula de Convivencia.</p>
    </div>

    <div class="card-jandula">
      <div v-if="mensaje" :class="['alert-jandula', mensajeTipo === 'success' ? 'ok' : 'err']">
        {{ mensaje }}
      </div>

      <form class="parte-form" @submit.prevent="crearParte">
        <div class="block-title">Datos de identificación</div>
        <div class="form-grid">
          <div class="field-group">
            <label>Profesor (Email):</label>
            <input 
              type="email" 
              v-model="parte.profesorEmail" 
              class="jandula-input"
              readonly
            />
          </div>

          <div class="field-group">
            <label>Fecha de Incidencia:</label>
            <input 
              type="date" 
              v-model="parte.fecha" 
              class="jandula-input"
              required
            />
          </div>
        </div>

        <div class="block-title">Alumno y conducta</div>
        <div class="form-grid">
          <div class="field-group">
            <label>Seleccionar Curso:</label>
            <select v-model="parte.curso" @change="cargarAlumnos" class="jandula-input" required>
              <option value="">Buscar curso...</option>
              <option value="1º ESO">1º ESO</option>
              <option value="2º ESO">2º ESO</option>
              <option value="3º ESO">3º ESO</option>
              <option value="4º ESO">4º ESO</option>
              <option value="1º BIC">1º BIC</option>
              <option value="2º BIC">2º BIC</option>
              <option value="OS1B">OS1B</option>
              <option value="OS2B">OS2B</option>
            </select>
          </div>

          <div class="field-group">
            <label>Seleccionar Alumno/a:</label>
            <select v-model="parte.alumnoId" class="jandula-input" :disabled="!alumnos.length" required>
              <option value="">{{ alumnos.length ? 'Buscar por nombre...' : 'Primero seleccione curso' }}</option>
              <option v-for="alumno in alumnos" :key="alumno.id" :value="alumno.id">
                {{ alumno.apellidos }}, {{ alumno.nombre }} ({{ alumno.grupo }})
              </option>
            </select>
          </div>
        </div>

        <div class="form-grid">
          <div class="field-group">
            <label>Tipo de conducta:</label>
            <select v-model="parte.gravedad" class="jandula-input" required>
              <option value="LEVE">LEVE</option>
              <option value="GRAVE">GRAVE</option>
            </select>
          </div>

          <div class="field-group conducta-wide">
            <label>Conducta detectada:</label>
            <button
              type="button"
              class="jandula-input conducta-trigger"
              :disabled="!conductasFiltradas.length"
              @click="abrirConductaModal"
            >
              <span v-if="conductaSeleccionada">
                {{ conductaSeleccionada.codigo }} - {{ conductaSeleccionada.descripcion }}
              </span>
              <span v-else>
                {{ parte.gravedad ? 'Seleccione la conducta...' : 'Seleccione el tipo de conducta' }}
              </span>
            </button>
            <input type="hidden" v-model="parte.conductaId" required />
          </div>
        </div>

        <div class="field-group">
          <label>Descripción detallada de los hechos:</label>
          <textarea 
            v-model="parte.descripcion" 
            class="jandula-input"
            rows="3"
            placeholder="Describa lo ocurrido..."
            required
          ></textarea>
        </div>

        <div class="block-title">Medida aplicada</div>
        <div class="field-group">
          <label>¿Se deriva al Aula de Convivencia?</label>
          <div class="toggle-jandula-wrapper">
            <button 
              type="button" 
              :class="{ active: parte.medidaTomada === 'AULA_CONVIVENCIA' }"
              @click="parte.medidaTomada = 'AULA_CONVIVENCIA'"
            >SÍ</button>
            <button 
              type="button" 
              :class="{ active: parte.medidaTomada === 'SE_QUEDA_EN_CLASE' }"
              @click="parte.medidaTomada = 'SE_QUEDA_EN_CLASE'"
            >NO</button>
          </div>
        </div>

        <div v-if="parte.medidaTomada === 'AULA_CONVIVENCIA'" class="aula-detail-box">
          <div class="field-group">
            <label>Tareas para el Aula:</label>
            <textarea v-model="parte.tareas" class="jandula-input" rows="2" placeholder="Ej: Ejercicios 1 al 10..."></textarea>
          </div>

          <div class="field-group">
            <label>Evidencia (Foto/Documento):</label>
            <div v-if="archivoPreview || parte.archivoUrl" class="preview-zone">
               <img v-if="esImagen" :src="archivoPreview || parte.archivoUrl" class="img-preview-min" />
               <span v-else>📄 {{ archivoNombre }}</span>
               <button type="button" @click="eliminarArchivo" class="btn-clean">Eliminar</button>
            </div>
            <div v-else class="upload-zone">
              <input type="file" @change="onFileChange" id="file-up" hidden />
              <label for="file-up" class="btn-upload">📎 Adjuntar archivo</label>
            </div>
          </div>
        </div>

        <div class="form-footer">
          <button type="button" class="btn-j-secondary" @click="limpiarFormulario">CANCELAR</button>
          <button type="submit" class="btn-j-primary" :disabled="guardando">
            {{ guardando ? 'GUARDANDO...' : 'CREAR PARTE' }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="mostrarConductaModal" class="conducta-modal-overlay" @click.self="cerrarConductaModal">
      <div class="conducta-modal">
        <div class="conducta-modal-header">
          <h4>Selecciona la conducta</h4>
          <button type="button" class="btn-close" @click="cerrarConductaModal">×</button>
        </div>
        <input
          v-model="filtroConducta"
          class="jandula-input"
          type="text"
          placeholder="Buscar por codigo o descripcion..."
        />
        <div class="conducta-modal-list">
          <button
            v-for="c in conductasFiltradasModal"
            :key="c.id"
            type="button"
            class="conducta-item"
            @click="seleccionarConducta(c)"
          >
            <span class="conducta-code">{{ c.codigo }}</span>
            <span class="conducta-text">{{ c.descripcion }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
/* Mantenemos exactamente tu lógica que ya te funcionaba */
import axios from 'axios'
import { API_URL } from '@/config/api'

export default {
  name: 'NuevoParte',
  data() {
    return {
      parte: {
        profesorEmail: '',
        fecha: new Date().toISOString().split('T')[0],
        curso: '',
        alumnoId: '',
        descripcion: '',
        gravedad: 'LEVE',
        medidaTomada: 'AULA_CONVIVENCIA',
        tareas: '',
        archivoUrl: '',
        conductaId: ''
      },
      alumnos: [],
      conductas: [],
      mensaje: '',
      mensajeTipo: '',
      toastVisible: false,
      toastMensaje: '',
      toastTipo: 'success',
      toastTimer: null,
      guardando: false,
      cargandoAlumnos: false,
      archivoSeleccionado: null,
      archivoPreview: null,
      archivoNombre: '',
      subiendoArchivo: false,
      progresoSubida: 0,
      mostrarConductaModal: false,
      filtroConducta: ''
    }
  },
  mounted() {
    this.cargarConductas()
    this.cargarEmailProfesor()
  },
  computed: {
    esImagen() {
      if (!this.archivoSeleccionado && !this.parte.archivoUrl) return false
      const nombre = this.archivoNombre || this.parte.archivoUrl || ''
      return /\.(jpg|jpeg|png|gif|webp)$/i.test(nombre)
    },
    rolProfesor() {
      const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
      if (!profesor) return 'PROFESOR'
      if (profesor.rol) return profesor.rol
      const email = (profesor.email || '').toLowerCase()
      if (email.includes('jefe')) return 'JEFATURA'
      if (email.includes('tutor')) return 'TUTOR'
      return 'PROFESOR'
    },
    conductasFiltradas() {
      const tipo = this.parte.gravedad
      const esJefatura = this.rolProfesor === 'JEFATURA'
      const codigosRD327 = [
        '1a', '1b', '1c', '1d', '1e', '1f', '1g',
        '2a', '2b', '2c', '2d', '2e', '2f', '2g', '2h', '2i', '2j', '2k'
      ]
      return this.conductas
        .filter(c => codigosRD327.includes(String(c.codigo || '').toLowerCase()))
        .filter(c => !tipo || String(c.gravedad || '').toUpperCase() === tipo)
        .filter(c => esJefatura || String(c.codigo || '').toLowerCase() !== '2i')
        .sort((a, b) => (a.codigo || '').localeCompare(b.codigo || '', 'es', { numeric: true }))
    },
    conductaSeleccionada() {
      return this.conductas.find(c => String(c.id) === String(this.parte.conductaId)) || null
    },
    conductasFiltradasModal() {
      const filtro = this.filtroConducta.trim().toLowerCase()
      if (!filtro) return this.conductasFiltradas
      return this.conductasFiltradas.filter(c => {
        const codigo = String(c.codigo || '').toLowerCase()
        const descripcion = String(c.descripcion || '').toLowerCase()
        return codigo.includes(filtro) || descripcion.includes(filtro)
      })
    }
  },
  methods: {
    cargarEmailProfesor() {
      const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
      if (profesor) this.parte.profesorEmail = profesor.email
    },
    async cargarConductas() {
      try {
        const incluir2i = this.rolProfesor === 'JEFATURA'
        const response = await axios.get(`${API_URL}/conductas`, {
          params: { incluir2i }
        })
        this.conductas = response.data
      } catch (e) { console.error(e) }
    },
    abrirConductaModal() {
      if (!this.conductasFiltradas.length) return
      this.filtroConducta = ''
      this.mostrarConductaModal = true
    },
    cerrarConductaModal() {
      this.mostrarConductaModal = false
    },
    seleccionarConducta(conducta) {
      this.parte.conductaId = conducta?.id || ''
      this.mostrarConductaModal = false
    },
    async cargarAlumnos() {
      if (!this.parte.curso) return
      this.cargandoAlumnos = true
      try {
        const response = await axios.get(`${API_URL}/alumnos/curso/${this.parte.curso}`)
        this.alumnos = response.data
      } catch (e) { console.error(e) }
      finally { this.cargandoAlumnos = false }
    },
    async crearParte() {
      this.guardando = true
      try {
        if (this.archivoSeleccionado) await this.subirArchivo()
        await axios.post(`${API_URL}/partes`, this.parte)
        this.mensaje = ''
        this.mensajeTipo = ''
        this.mostrarToast('Parte creado con exito', 'success')
        
        // Despachar evento para refrescar avisos inmediatamente
        window.dispatchEvent(new CustomEvent('parte-creado'))
        
        this.limpiarFormulario()
      } catch (e) {
        this.mensaje = '❌ Error al guardar'
        this.mensajeTipo = 'error'
        this.mostrarToast('Error al crear el parte', 'error')
      } finally { this.guardando = false }
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
    onFileChange(e) {
      const file = e.target.files[0]
      if (!file) return
      this.archivoSeleccionado = file
      this.archivoNombre = file.name
      if (file.type.startsWith('image/')) {
        const reader = new FileReader()
        reader.onload = (ev) => this.archivoPreview = ev.target.result
        reader.readAsDataURL(file)
      }
    },
    async subirArchivo() {
      const formData = new FormData()
      formData.append('file', this.archivoSeleccionado)
      const res = await axios.post(`${API_URL}/archivos/upload`, formData)
      this.parte.archivoUrl = res.data.url
    },
    eliminarArchivo() {
      this.archivoSeleccionado = null; this.archivoPreview = null; this.parte.archivoUrl = ''
    },
    limpiarFormulario() {
      this.parte = { profesorEmail: this.parte.profesorEmail, fecha: new Date().toISOString().split('T')[0], curso: '', alumnoId: '', descripcion: '', gravedad: 'LEVE', medidaTomada: 'AULA_CONVIVENCIA', tareas: '', archivoUrl: '', conductaId: '' }
      this.alumnos = []
      this.mostrarConductaModal = false
    }
  },
  watch: {
    'parte.gravedad'() {
      this.parte.conductaId = ''
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
.jandula-page-wrapper {
  --j-primary: #0f4c5c;
  --j-primary-soft: #e6f1f4;
  --j-accent: #1f7a8c;
  --j-ink: #1f2937;
  --j-muted: #64748b;
  --j-border: #d8e1e8;

  /* Contenedor principal para que el fondo gris se vea */
  background:
    radial-gradient(circle at 0% 0%, rgba(15, 76, 92, 0.1), transparent 34%),
    linear-gradient(180deg, #f5f8fb 0%, #eff3f7 100%);
  min-height: 100vh;
  padding: 32px 20px 46px;
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

.section-header {
  text-align: center;
  margin-bottom: 22px;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--j-primary);
  background: var(--j-primary-soft);
  border: 1px solid rgba(15, 76, 92, 0.18);
}

.section-header h2 {
  color: var(--j-ink);
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  margin-top: 10px;
}

.section-header p {
  color: var(--j-muted);
  max-width: 760px;
  margin: 10px auto 0;
  line-height: 1.5;
}

/* Card Blanca con Borde Azul */
.card-jandula {
  background: white;
  max-width: 1200px;
  margin: 0 auto 0 24px;
  padding: 30px;
  border-radius: 14px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
  border: 1px solid #dce5ee;
  border-top: 5px solid var(--j-primary);
}

@media (max-width: 1100px) {
  .card-jandula {
    margin: 0 auto;
  }
}

.parte-form {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.block-title {
  margin: 10px 0 8px;
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--j-primary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field-group {
  margin-bottom: 16px;
}

.conducta-wide {
  grid-column: 1 / -1;
  overflow: visible;
}

.conducta-select {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
}

.conducta-trigger {
  text-align: left;
  cursor: pointer;
}

.conducta-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.conducta-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1500;
  padding: 16px;
}

.conducta-modal {
  background: #ffffff;
  width: min(900px, 92vw);
  max-height: 80vh;
  border-radius: 12px;
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.25);
  border: 1px solid #dce5ee;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.conducta-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conducta-modal-header h4 {
  margin: 0;
  font-size: 1.1rem;
  color: #0f4c5c;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 1.4rem;
  cursor: pointer;
  color: #64748b;
}

.conducta-modal-list {
  display: grid;
  gap: 10px;
  overflow: auto;
  padding-right: 6px;
}

.conducta-item {
  text-align: left;
  border: 1px solid #dce5ee;
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 10px;
  display: grid;
  gap: 6px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.conducta-item:hover {
  border-color: rgba(31, 122, 140, 0.7);
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.08);
}

.conducta-code {
  font-weight: 800;
  color: #0f4c5c;
}

.conducta-text {
  color: #1f2937;
  line-height: 1.4;
}

.field-group label {
  display: block;
  font-weight: 700;
  color: #334155;
  margin-bottom: 7px;
  font-size: 0.85rem;
}

.jandula-input {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid var(--j-border);
  border-radius: 8px;
  background-color: #fff;
  color: #1f2937;
  box-sizing: border-box;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.jandula-input:focus {
  outline: none;
  border-color: rgba(31, 122, 140, 0.7);
  box-shadow: 0 0 0 3px rgba(31, 122, 140, 0.16);
}

/* Toggle Buttons SÍ/NO */
.toggle-jandula-wrapper {
  display: flex;
  background: #edf2f7;
  padding: 4px;
  border-radius: 9px;
  gap: 4px;
  border: 1px solid #d7e1ea;
}

.toggle-jandula-wrapper button {
  flex: 1;
  padding: 9px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-weight: 700;
  color: #64748b;
  border-radius: 7px;
  transition: 0.2s;
}

.toggle-jandula-wrapper button.active {
  background: white;
  color: var(--j-primary);
  box-shadow: 0 2px 8px rgba(17, 24, 39, 0.14);
}

.aula-detail-box {
  background: linear-gradient(180deg, #f7fbfc 0%, #f4f8fa 100%);
  padding: 18px;
  border-radius: 10px;
  border: 1px solid #d3e2e8;
  border-left: 4px solid var(--j-accent);
  margin-bottom: 18px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 0 4px;
  border-top: 1px solid #e6edf3;
}

.btn-j-primary {
  background-color: #0f4c5c;
  background: linear-gradient(135deg, var(--j-primary) 0%, var(--j-accent) 100%);
  color: white;
  padding: 12px 24px;
  border: 1px solid #0f4c5c;
  border-radius: 9px;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  min-width: 170px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-j-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 76, 92, 0.3);
}

.btn-j-secondary {
  background: #eef2f6;
  color: #475569;
  padding: 12px 24px;
  border: 1px solid #d5dee8;
  border-radius: 9px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease;
  min-width: 130px;
}

.btn-j-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-j-secondary:hover {
  background: #e4ebf3;
}

.btn-upload {
  display: inline-block;
  padding: 10px 18px;
  border: 2px dashed #c6d4df;
  color: #4b6074;
  border-radius: 8px;
  cursor: pointer;
  background: #f7fafc;
}

.alert-jandula {
  padding: 12px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
  text-align: center;
  font-weight: 700;
}
.ok { background: #d1fae5; color: #065f46; }
.err { background: #fee2e2; color: #991b1b; }

.img-preview-min { height: 66px; border-radius: 6px; margin-right: 10px; border: 1px solid #d9e2ea; }

.preview-zone {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-clean {
  border: 1px solid #f2b8b5;
  background: #fff5f5;
  color: #b42318;
  border-radius: 6px;
  padding: 6px 10px;
  cursor: pointer;
}

@media (max-width: 600px) {
  .jandula-page-wrapper {
    padding: 20px 12px 28px;
  }
  .card-jandula {
    padding: 18px;
    border-radius: 10px;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-footer {
    flex-direction: column-reverse;
    align-items: stretch;
  }
}
</style>