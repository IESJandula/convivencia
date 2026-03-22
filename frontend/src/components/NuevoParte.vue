<template>
  <div class="jandula-page-wrapper">
    <div class="section-header">
      <h2>Registro de Incidencia</h2>
      <p>Complete el formulario para notificar una conducta contraria a las normas</p>
    </div>

    <div class="card-jandula">
      <div v-if="mensaje" :class="['alert-jandula', mensajeTipo === 'success' ? 'ok' : 'err']">
        {{ mensaje }}
      </div>

      <form @submit.prevent="crearParte">
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

        <div class="field-group">
          <label>Conducta detectada:</label>
          <select v-model="parte.conductaId" class="jandula-input" required>
            <option value="">Seleccione el tipo de falta...</option>
            <option v-for="c in conductas" :key="c.id" :value="c.id">
              {{ c.codigo }} - {{ c.descripcion }}
            </option>
          </select>
        </div>

        <div class="field-group">
          <label>Gravedad del parte:</label>
          <select v-model="parte.gravedad" class="jandula-input" required>
            <option value="LEVE">LEVE</option>
            <option value="GRAVE">GRAVE</option>
          </select>
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
            {{ guardando ? 'GUARDANDO...' : 'REGISTRAR PARTE' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
/* Mantenemos exactamente tu lógica que ya te funcionaba */
import axios from 'axios'
const API_URL = 'http://localhost:8080/api'

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
      guardando: false,
      cargandoAlumnos: false,
      archivoSeleccionado: null,
      archivoPreview: null,
      archivoNombre: '',
      subiendoArchivo: false,
      progresoSubida: 0
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
    }
  },
  methods: {
    cargarEmailProfesor() {
      const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
      if (profesor) this.parte.profesorEmail = profesor.email
    },
    async cargarConductas() {
      try {
        const response = await axios.get(`${API_URL}/conductas`)
        this.conductas = response.data
      } catch (e) { console.error(e) }
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
        this.mensaje = '✅ Parte creado con éxito'
        this.mensajeTipo = 'success'
        this.limpiarFormulario()
      } catch (e) {
        this.mensaje = '❌ Error al guardar'
        this.mensajeTipo = 'error'
      } finally { this.guardando = false }
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
    }
  }
}
</script>

<style scoped>
/* Contenedor principal para que el fondo gris se vea */
.jandula-page-wrapper {
  background-color: #f4f7f6;
  min-height: 100vh;
  padding: 40px 20px;
}

.section-header {
  text-align: center;
  margin-bottom: 30px;
}

.section-header h2 {
  color: #1a3a5a;
  font-size: 24px;
  font-weight: 600;
}

/* Card Blanca con Borde Azul */
.card-jandula {
  background: white;
  max-width: 800px;
  margin: 0 auto;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border-top: 4px solid #1a3a5a;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.field-group {
  margin-bottom: 25px;
}

.field-group label {
  display: block;
  font-weight: 700;
  color: #33475b;
  margin-bottom: 8px;
  font-size: 14px;
}

.jandula-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #33475b;
  box-sizing: border-box;
}

/* Toggle Buttons SÍ/NO */
.toggle-jandula-wrapper {
  display: flex;
  background: #f1f5f9;
  padding: 5px;
  border-radius: 6px;
  gap: 5px;
}

.toggle-jandula-wrapper button {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-weight: bold;
  color: #64748b;
  border-radius: 4px;
  transition: 0.2s;
}

.toggle-jandula-wrapper button.active {
  background: white;
  color: #1a3a5a;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.aula-detail-box {
  background: #f8fafc;
  padding: 20px;
  border-radius: 8px;
  border-left: 4px solid #336699;
  margin-bottom: 25px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.btn-j-primary {
  background: #1a3a5a;
  color: white;
  padding: 14px 30px;
  border: none;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
}

.btn-j-secondary {
  background: #e2e8f0;
  color: #475569;
  padding: 14px 30px;
  border: none;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
}

.btn-upload {
  display: inline-block;
  padding: 10px 20px;
  border: 2px dashed #cbd5e1;
  color: #64748b;
  border-radius: 6px;
  cursor: pointer;
}

.alert-jandula {
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 25px;
  text-align: center;
}
.ok { background: #d1fae5; color: #065f46; }
.err { background: #fee2e2; color: #991b1b; }

.img-preview-min { height: 60px; border-radius: 4px; margin-right: 10px; }

@media (max-width: 600px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>