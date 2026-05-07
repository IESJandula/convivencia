<template>
  <div id="app">
    <header v-if="mostrarNavbar" class="jandula-header">
      <div class="header-top">
        <div class="header-left">
          <div class="logo-white-box">
            <img src="@/assets/logo-jandula.png" alt="Logo" class="img-logo">
          </div>
          
          <div class="header-user-info">
            <h1 class="text-role">{{ rolUsuario }}</h1>
            <span class="user-name-pill">{{ nombreProfesor }}</span>
          </div>
        </div>
        
        <div class="navbar-right">
          <!-- Botón Avisos Jefatura -->
          <div v-if="rolUsuario === 'JEFATURA'" class="notifications-wrapper" @click="toggleAvisos">
            <button class="btn-avisos-jandula" :class="{'has-alerts': avisosNoLeidos.length > 0}" title="Avisos">
              <svg class="bell-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <span v-if="avisosNoLeidos.length > 0" class="badge-avisos">{{ avisosNoLeidos.length }}</span>
            </button>
            
            <!-- Dropdown Avisos -->
            <transition name="dropdown">
              <div v-if="mostrarAvisos" class="avisos-dropdown" @click.stop>
                <div class="avisos-header">
                  <h3>Notificaciones</h3>
                  <button v-if="avisosNoLeidos.length > 0" class="btn-close-dropdown" @click="mostrarAvisos = false">✖</button>
                </div>
                <div v-if="avisosNoLeidos.length === 0" class="avisos-empty">
                  No tienes avisos nuevos
                </div>
                <div v-else class="avisos-list">
                  <div v-for="aviso in avisosNoLeidos" :key="aviso.id" class="aviso-item">
                    <div class="aviso-content">
                      <span class="aviso-icon">⚠️</span>
                      <p class="aviso-text">{{ aviso.mensaje }}</p>
                    </div>
                    <div class="aviso-actions">
                      <button class="btn-aviso-action view" @click="verHistorial(aviso)">Ver historial</button>
                      <button class="btn-aviso-action dismiss" @click="marcarLeido(aviso.id)">Marcar leído</button>
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>

          <button @click="cerrarSesion" class="btn-logout-jandula" title="Cerrar Sesión">
            <svg class="logout-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
            </svg>
          </button>
        </div>
      </div>

      <div class="nav-links">
        <router-link to="/nuevo" class="nav-button">📝 Nuevo Parte</router-link>
        <router-link v-if="rolUsuario === 'PROFESOR'" to="/aula-convivencia" class="nav-button">🏫 Aula Convivencia</router-link>
        <router-link
          v-if="rolUsuario === 'PROFESOR'"
          to="/tareas-expulsion"
          active-class=""
          exact-active-class=""
          :class="['nav-button', 'nav-with-indicator', isTareasExpulsionNavActive ? 'nav-forced-active' : 'nav-force-inactive']"
        >
          📚 Tareas Expulsión
          <span v-if="tareasPendientes > 0" class="pending-dot" :title="`Tienes ${tareasPendientes} tarea(s) pendiente(s)`"></span>
        </router-link>
        <router-link
          v-if="rolUsuario === 'PROFESOR'"
          to="/tareas-expulsion?tab=enviadas"
          active-class=""
          exact-active-class=""
          :class="['nav-button', isTareasEnviadasNavActive ? 'nav-forced-active' : 'nav-force-inactive']"
        >
          📤 Tareas Enviadas
        </router-link>
        <router-link v-if="rolUsuario === 'JEFATURA'" to="/validar-sanciones" class="nav-button">⚖️ Validar Sanciones</router-link>
        <router-link v-if="rolUsuario === 'JEFATURA'" to="/expulsiones" class="nav-button">📄 Expulsiones</router-link>
        <router-link v-if="rolUsuario === 'PROFESOR' || rolUsuario === 'TUTOR'" to="/mis-alumnos" class="nav-button">👥 Mi Tutoría</router-link>
        <router-link to="/historial" class="nav-button">📋 Historial</router-link>
      </div>
    </header>

    <div :class="['container', {
      'container-wide-aula': esRutaAulaConvivencia,
      'container-wide-expulsiones': esRutaExpulsiones
    }]">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <keep-alive include="ValidarSanciones">
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { API_URL } from './config/api.js'

export default {
  name: 'App',
  data() {
    return {
      nombreProfesor: '',
      rolUsuario: 'PROFESOR',
      esGuardiaUsuario: false,
      profesorEmail: '',
      tareasPendientes: 0,
      tareasRefreshTimer: null,
      avisosNoLeidos: [],
      mostrarAvisos: false,
      avisosRefreshTimer: null
    }
  },
  computed: {
    mostrarNavbar() {
      const usuario = JSON.parse(localStorage.getItem('profesor') || 'null')
      const sesionValida = Boolean(usuario?.email)
      return this.$route.path !== '/login' && sesionValida
    },
    esRutaAulaConvivencia() {
      return this.$route.path === '/aula-convivencia'
    },
    esRutaExpulsiones() {
      return this.$route.path === '/expulsiones'
    }
    ,
    isTareasEnviadasNavActive() {
      return this.$route.path === '/tareas-expulsion' && this.$route.query.tab === 'enviadas'
    },
    isTareasExpulsionNavActive() {
      return this.$route.path === '/tareas-expulsion' && this.$route.query.tab !== 'enviadas'
    }
  },
  mounted() {
    window.addEventListener('tareas-expulsion-pendientes', this.actualizarPendientesDesdeEvento)
    window.addEventListener('parte-creado', this.cargarAvisos)
    this.cargarDatosUsuario()
  },
  beforeUnmount() {
    window.removeEventListener('tareas-expulsion-pendientes', this.actualizarPendientesDesdeEvento)
    window.removeEventListener('parte-creado', this.cargarAvisos)
    this.limpiarTemporizadorPendientes()
    this.limpiarTemporizadorAvisos()
    document.removeEventListener('click', this.cerrarAvisosClickFuera)
  },
  watch: { '$route'() { this.cargarDatosUsuario() } },
  methods: {
    cargarDatosUsuario() {
      const usuario = JSON.parse(localStorage.getItem('profesor') || 'null')
      if (usuario) {
        if (!usuario.email) {
          localStorage.removeItem('profesor')
          if (this.$route.path !== '/login') {
            this.$router.push('/login')
          }
          return
        }

        this.nombreProfesor = usuario.nombre
        this.profesorEmail = usuario.email
        this.esGuardiaUsuario = usuario.esGuardia === true || usuario.esGuardia === 1 || usuario.esGuardia === '1'
        // Lógica de detección inteligente para el Sprint 1
        if (usuario.rol) {
          this.rolUsuario = usuario.rol
        } else {
          if (usuario.email.includes('jefe')) this.rolUsuario = 'JEFATURA'
          else if (usuario.email.includes('tutor')) this.rolUsuario = 'TUTOR'
          else this.rolUsuario = 'PROFESOR'
        }

        this.configurarIndicadorPendientes()
      } else {
        this.esGuardiaUsuario = false
        this.nombreProfesor = ''
        this.profesorEmail = ''
        this.tareasPendientes = 0
        this.limpiarTemporizadorPendientes()
        if (this.$route.path !== '/login') {
          this.$router.push('/login')
        }
      }
    },
    configurarIndicadorPendientes() {
      this.limpiarTemporizadorPendientes()
      this.limpiarTemporizadorAvisos()

      if (this.rolUsuario === 'PROFESOR' && this.profesorEmail) {
        this.actualizarIndicadorTareas()
        this.tareasRefreshTimer = setInterval(() => {
          this.actualizarIndicadorTareas()
        }, 30000)
      }

      if (this.rolUsuario === 'JEFATURA') {
        this.cargarAvisos()
        this.avisosRefreshTimer = setInterval(() => {
          this.cargarAvisos()
        }, 30000)
        document.addEventListener('click', this.cerrarAvisosClickFuera)
      }
    },
    limpiarTemporizadorPendientes() {
      if (this.tareasRefreshTimer) {
        clearInterval(this.tareasRefreshTimer)
        this.tareasRefreshTimer = null
      }
    },
    limpiarTemporizadorAvisos() {
      if (this.avisosRefreshTimer) {
        clearInterval(this.avisosRefreshTimer)
        this.avisosRefreshTimer = null
      }
    },
    async actualizarIndicadorTareas() {
      if (!this.profesorEmail || this.rolUsuario !== 'PROFESOR') {
        this.tareasPendientes = 0
        return
      }

      try {
        const { data } = await axios.get(`${API_URL}/tareas-expulsion/profesor/${this.profesorEmail}/resumen`)
        this.tareasPendientes = Number(data?.pendientes ?? 0)
      } catch (e) {
        this.tareasPendientes = 0
      }
    },
    actualizarPendientesDesdeEvento(event) {
      const count = Number(event?.detail?.count)
      if (!Number.isNaN(count)) {
        this.tareasPendientes = Math.max(0, count)
      }
    },
    async cargarAvisos() {
      try {
        const { data } = await axios.get(`${API_URL}/avisos?soloNoLeidos=true`)
        this.avisosNoLeidos = Array.isArray(data) ? data : []
      } catch (error) {
        console.error('Error cargando avisos:', error)
      }
    },
    toggleAvisos() {
      this.mostrarAvisos = !this.mostrarAvisos
    },
    cerrarAvisosClickFuera(event) {
      const dropdown = this.$el.querySelector('.avisos-dropdown')
      const btn = this.$el.querySelector('.btn-avisos-jandula')
      if (this.mostrarAvisos && dropdown && !dropdown.contains(event.target) && btn && !btn.contains(event.target)) {
        this.mostrarAvisos = false
      }
    },
    async marcarLeido(id) {
      try {
        await axios.put(`${API_URL}/avisos/${id}/leer`)
        this.avisosNoLeidos = this.avisosNoLeidos.filter(a => a.id !== id)
        if (this.avisosNoLeidos.length === 0) {
          this.mostrarAvisos = false
        }
      } catch (error) {
        console.error('Error marcando aviso:', error)
      }
    },
    async verHistorial(aviso) {
      this.mostrarAvisos = false
      this.$router.push('/historial?alumno_id=' + aviso.alumno.id)
    },
    cerrarSesion() {
      if (confirm('¿Cerrar sesión?')) {
        localStorage.removeItem('profesor');
        this.tareasPendientes = 0
        this.limpiarTemporizadorPendientes()
        this.$router.push('/login');
      }
    }
  }
}
</script>

<style>
/* --- ESTILOS DE CABECERA AJUSTADOS --- */
.jandula-header { display: flex; flex-direction: column; }
.header-top { background-color: #1c314a; height: 80px; display: flex; justify-content: space-between; align-items: center; padding: 0 2rem; color: white; }
.header-left { display: flex; align-items: center; }
.logo-white-box { background: white; width: 45px; height: 45px; display: flex; align-items: center; justify-content: center; border-radius: 6px; }
.img-logo { max-width: 85%; }

/* Nueva agrupación estética */
.header-user-info { 
  display: flex; 
  align-items: center; 
  gap: 15px; 
  margin-left: 15px;
  border-left: 1px solid rgba(255,255,255,0.2); /* Separador elegante */
  padding-left: 15px;
}

.text-role { 
  font-size: 1.4rem !important; 
  font-weight: 800 !important; 
  color: white !important; 
  margin: 0 !important; 
  text-transform: uppercase;
  letter-spacing: 1px;
}

.user-name-pill { 
  background-color: #ff8c00; 
  color: white; 
  font-size: 12px; 
  padding: 4px 12px; 
  border-radius: 20px; /* Forma de píldora más estética */
  font-weight: 800; 
  text-transform: uppercase;
}

/* --- RESTO DE ESTILOS MANTENIDOS --- */
.navbar-right { display: flex; align-items: center; gap: 15px; }
.btn-logout-jandula { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.2); border-radius: 4px; cursor: pointer; color: white; transition: background 0.2s; }
.btn-logout-jandula:hover { background: rgba(255,255,255,0.3); }
.logout-icon-svg { width: 22px; height: 22px; }

/* Estilos de Avisos */
.notifications-wrapper { position: relative; }
.btn-avisos-jandula {
  width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.2);
  border-radius: 4px; cursor: pointer; color: white; position: relative;
  transition: all 0.3s ease;
}
.btn-avisos-jandula:hover { background: rgba(255,255,255,0.3); }
.bell-icon-svg { width: 22px; height: 22px; transition: transform 0.3s; }
.btn-avisos-jandula.has-alerts .bell-icon-svg {
  animation: bell-ring 2s infinite cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}
.badge-avisos {
  position: absolute; top: -5px; right: -5px;
  background-color: #ef4444; color: white;
  font-size: 11px; font-weight: bold;
  width: 18px; height: 18px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 0 2px #1c314a;
  animation: badge-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}
.avisos-dropdown {
  position: absolute; top: 55px; right: 0;
  width: 320px; background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.2);
  border: 1px solid rgba(255,255,255,0.5);
  overflow: hidden; z-index: 1000;
  transform-origin: top right;
}
.avisos-header {
  padding: 12px 15px; background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex; justify-content: space-between; align-items: center;
}
.avisos-header h3 { margin: 0; font-size: 14px; color: #1e293b; font-weight: 700; }
.btn-close-dropdown { background: none; border: none; cursor: pointer; color: #64748b; }
.avisos-empty { padding: 20px; text-align: center; color: #64748b; font-size: 14px; }
.avisos-list { max-height: 350px; overflow-y: auto; }
.aviso-item { padding: 15px; border-bottom: 1px solid #f1f5f9; transition: background 0.2s; }
.aviso-item:hover { background: #f8fafc; }
.aviso-item:last-child { border-bottom: none; }
.aviso-content { display: flex; gap: 10px; margin-bottom: 10px; }
.aviso-icon { font-size: 18px; }
.aviso-text { margin: 0; font-size: 13px; color: #334155; line-height: 1.4; }
.aviso-actions { display: flex; gap: 8px; justify-content: flex-end; }
.btn-aviso-action {
  padding: 5px 10px; font-size: 12px; font-weight: 600; border-radius: 4px; cursor: pointer; border: none;
}
.btn-aviso-action.view { background: #0b4e6b; color: white; }
.btn-aviso-action.view:hover { background: #083b52; }
.btn-aviso-action.dismiss { background: #e2e8f0; color: #475569; }
.btn-aviso-action.dismiss:hover { background: #cbd5e1; }

@keyframes bell-ring {
  0% { transform: rotate(0); }
  5% { transform: rotate(15deg); }
  10% { transform: rotate(-15deg); }
  15% { transform: rotate(10deg); }
  20% { transform: rotate(-10deg); }
  25% { transform: rotate(5deg); }
  30% { transform: rotate(-5deg); }
  35% { transform: rotate(0); }
  100% { transform: rotate(0); }
}
@keyframes badge-pop {
  0% { transform: scale(0); }
  100% { transform: scale(1); }
}
.dropdown-enter-active, .dropdown-leave-active { transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: scale(0.9) translateY(-10px); }

.nav-links { background: #0b4e6b; display: flex; padding: 0 2rem; }
.nav-button {
  padding: 1rem 1.5rem;
  color: rgba(255,255,255,0.82);
  text-decoration: none;
  font-size: 0.95rem;
  position: relative;
  transition: background-color 0.18s ease, color 0.18s ease, transform 0.12s ease;
}

.nav-button::after {
  content: '';
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 8px;
  height: 2px;
  background: #ffffff;
  border-radius: 999px;
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.18s ease;
}

.nav-button:hover {
  background: rgba(255,255,255,0.14);
  color: #ffffff;
}

.nav-button:hover::after,
.nav-button:focus-visible::after {
  transform: scaleX(1);
}

.nav-button:focus-visible {
  outline: none;
  box-shadow: inset 0 0 0 2px rgba(255,255,255,0.45);
}
.nav-with-indicator { display: inline-flex; align-items: center; gap: 0.45rem; }
.pending-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #facc15;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.2);
  position: relative;
  animation: pending-blink 1s infinite steps(1, end);
}

.pending-dot::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(250, 204, 21, 0.55);
  transform: scale(1);
  opacity: 0.8;
  animation: pending-ripple 1.2s infinite ease-out;
}

@keyframes pending-blink {
  0%, 49% {
    background: #facc15;
  }
  50%, 100% {
    background: #f59e0b;
  }
}

@keyframes pending-ripple {
  0% {
    transform: scale(1);
    opacity: 0.7;
  }
  100% {
    transform: scale(2.2);
    opacity: 0;
  }
}
.nav-button.router-link-active { background: white !important; color: #1a3a5a !important; font-weight: bold; }
.nav-button.router-link-active::after { transform: scaleX(0); }

.nav-button.nav-force-inactive.router-link-active {
  background: rgba(255,255,255,0.14) !important;
  color: rgba(255,255,255,0.82) !important;
  font-weight: normal !important;
}

.nav-button.nav-force-inactive.router-link-active::after {
  transform: scaleX(0);
}

.nav-button.nav-forced-active {
  background: white !important;
  color: #1a3a5a !important;
  font-weight: bold;
}

.nav-button.nav-forced-active::after {
  transform: scaleX(0);
}
.container { max-width: 1000px; margin: 2rem auto; padding: 0 2rem; }
.container.container-wide-aula { max-width: 1320px; padding: 0 1rem; }
.container.container-wide-expulsiones { max-width: 1240px; padding: 0 1rem; }
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif; background: #f0f2f5; }
</style>