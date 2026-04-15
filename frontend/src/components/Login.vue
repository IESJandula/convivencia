<template>
  <div class="login-wrapper">
    <div class="login-card-jandula">
      <div class="login-brand">
        <img src="@/assets/logo-jandula.png" alt="IES Jándula" class="main-logo">
        <h2>Sistema de Partes</h2>
        <p class="subtitle">Aula de Convivencia</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div v-if="mensaje" :class="['alert-jandula', mensajeTipo === 'error' ? 'err' : 'ok']">
          {{ mensaje }}
        </div>

        <div class="input-group-jandula">
          <label for="email">Email Institucional (@g.educaand.es)</label>
          <input
            id="email"
            type="email"
            v-model="email"
            placeholder="usuario@g.educaand.es"
            required
            :disabled="cargando"
            @input="limpiarMensaje"
          />
          <small class="help-text">Use su cuenta corporativa para acceder</small>
        </div>

        <button type="submit" class="btn-jandula-primary" :disabled="cargando || !email">
          <span v-if="cargando">🔄 VERIFICANDO...</span>
          <span v-else>🔐 INICIAR SESIÓN</span>
        </button>

        <div class="info-box-jandula">
          <p class="info-title">ℹ️ Aviso de acceso</p>
          <p class="info-text">
            Acceso restringido a personal docente. Si presenta problemas técnicos, 
            contacte con la coordinación de TIC.
          </p>
        </div>
      </form>

      <div class="login-footer-jandula">
        <p>© 2026 IES Jándula - Gestión Educativa</p>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
const API_URL = 'http://http://api-convivencia.51.210.104.106.sslip.io/api'

export default {
  name: 'Login',
  data() {
    return {
      email: '',
      cargando: false,
      mensaje: '',
      mensajeTipo: ''
    }
  },
  methods: {
    async handleLogin() {
      if (!this.email?.trim()) return
      
      this.cargando = true
      this.limpiarMensaje()

      try {
        const response = await axios.post(`${API_URL}/auth/login`, {
          email: this.email.trim()
        })

        if (response.data.success) {
          // GUARDADO DE DATOS (Incluye el 'rol' para el Sprint 1)
          localStorage.setItem('profesor', JSON.stringify(response.data))
          
          this.mostrarMensaje(`¡Bienvenido/a, ${response.data.nombre}!`, 'success')
          
          setTimeout(() => {
            // REDIRECCIÓN SEGURA: Enviamos a /nuevo para evitar errores de rol al entrar
            this.$router.push('/nuevo') 
          }, 1000)
        }
      } catch (error) {
        this.mostrarMensaje(
          error.response?.status === 401 
            ? 'Usuario no registrado o dominio incorrecto.' 
            : 'Error de conexión con el servidor.',
          'error'
        )
      } finally {
        this.cargando = false
      }
    },
    mostrarMensaje(texto, tipo) {
      this.mensaje = texto
      this.mensajeTipo = tipo
    },
    limpiarMensaje() {
      this.mensaje = ''
      this.mensajeTipo = ''
    }
  }
}
</script>

<style scoped>
/* SE MANTIENEN TUS ESTILOS ORIGINALES INTACTOS */
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f4f7f6;
  padding: 20px;
}

.login-card-jandula {
  background: white;
  width: 100%;
  max-width: 420px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border-top: 5px solid #1a3a5a;
  overflow: hidden;
}

.login-brand {
  text-align: center;
  padding: 40px 20px 20px;
}

.main-logo {
  max-width: 150px;
  margin-bottom: 15px;
}

.login-brand h2 {
  color: #1a3a5a;
  font-size: 1.5rem;
  font-weight: 700;
}

.subtitle {
  color: #666;
  font-size: 0.9rem;
}

.login-form {
  padding: 0 40px 30px;
}

.input-group-jandula {
  margin-bottom: 20px;
}

.input-group-jandula label {
  display: block;
  font-weight: 600;
  color: #444;
  margin-bottom: 8px;
}

.input-group-jandula input {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-sizing: border-box;
}

.btn-jandula-primary {
  width: 100%;
  padding: 14px;
  background-color: #1a3a5a;
  color: white;
  border: none;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
}

.alert-jandula {
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 20px;
  text-align: center;
}
.err { background: #fee2e2; color: #991b1b; }
.ok { background: #d1fae5; color: #065f46; }

.login-footer-jandula {
  background: #f1f5f9;
  padding: 15px;
  text-align: center;
  font-size: 0.75rem;
  color: #94a3b8;
}
</style>