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
          <button @click="cerrarSesion" class="btn-logout-jandula" title="Cerrar Sesión">
            <svg class="logout-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
            </svg>
          </button>
        </div>
      </div>

      <div class="nav-links">
        <router-link to="/nuevo" class="nav-button">📝 Nuevo Parte</router-link>
        <router-link v-if="rolUsuario === 'PROFESOR' && esGuardiaUsuario" to="/aula-convivencia" class="nav-button">🏫 Aula Convivencia</router-link>
        <router-link v-if="rolUsuario === 'PROFESOR'" to="/tareas-expulsion" class="nav-button">📚 Tareas Expulsión</router-link>
        <router-link v-if="rolUsuario === 'JEFATURA'" to="/validar-sanciones" class="nav-button">⚖️ Validar Sanciones</router-link>
        <router-link v-if="rolUsuario === 'JEFATURA'" to="/expulsiones" class="nav-button">📄 Expulsiones</router-link>
        <router-link v-if="rolUsuario === 'PROFESOR' || rolUsuario === 'TUTOR'" to="/mis-alumnos" class="nav-button">👥 Mi Tutoría</router-link>
        <router-link to="/historial" class="nav-button">📋 Historial</router-link>
      </div>
    </header>

    <div class="container">
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
export default {
  name: 'App',
  data() {
    return {
      nombreProfesor: '',
      rolUsuario: 'PROFESOR',
      esGuardiaUsuario: false
    }
  },
  computed: {
    mostrarNavbar() {
      const usuario = JSON.parse(localStorage.getItem('profesor') || 'null')
      const sesionValida = Boolean(usuario?.email)
      return this.$route.path !== '/login' && sesionValida
    }
  },
  mounted() { this.cargarDatosUsuario() },
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
        this.esGuardiaUsuario = usuario.esGuardia === true || usuario.esGuardia === 1 || usuario.esGuardia === '1'
        // Lógica de detección inteligente para el Sprint 1
        if (usuario.rol) {
          this.rolUsuario = usuario.rol
        } else {
          if (usuario.email.includes('jefe')) this.rolUsuario = 'JEFATURA'
          else if (usuario.email.includes('tutor')) this.rolUsuario = 'TUTOR'
          else this.rolUsuario = 'PROFESOR'
        }
      } else {
        this.esGuardiaUsuario = false
        this.nombreProfesor = ''
        if (this.$route.path !== '/login') {
          this.$router.push('/login')
        }
      }
    },
    cerrarSesion() {
      if (confirm('¿Cerrar sesión?')) {
        localStorage.removeItem('profesor');
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
.btn-logout-jandula { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.2); border-radius: 4px; cursor: pointer; color: white; }
.logout-icon-svg { width: 22px; height: 22px; }
.nav-links { background: #0b4e6b; display: flex; padding: 0 2rem; }
.nav-button { padding: 1rem 1.5rem; color: rgba(255,255,255,0.8); text-decoration: none; font-size: 0.95rem; }
.nav-button.router-link-active { background: white !important; color: #1a3a5a !important; font-weight: bold; }
.container { max-width: 1000px; margin: 2rem auto; padding: 0 2rem; }
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif; background: #f0f2f5; }
</style>