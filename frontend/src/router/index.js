import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/Login.vue'
import NuevoParte from '../components/NuevoParte.vue'
import AulaConvivencia from '../components/AulaConvivencia.vue'
import Historial from '../components/Historial.vue'
import MisAlumnos from '../components/MisAlumnos.vue'
import ValidarSanciones from '../components/ValidarSanciones.vue'
import TareasExpulsion from '../components/TareasExpulsion.vue'

// Definición de rutas del Sprint 1
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: 'Iniciar Sesión', requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/nuevo'
  },
  {
    path: '/nuevo',
    name: 'NuevoParte',
    component: NuevoParte,
    meta: { title: 'Nuevo Parte', requiresAuth: true }
  },
  {
    path: '/aula-convivencia',
    name: 'AulaConvivencia',
    component: AulaConvivencia,
    meta: { 
      title: 'Aula de Convivencia', 
      requiresAuth: true, 
      role: 'PROFESOR' // Solo accesible para rol PROFESOR
    }
  },
  {
    path: '/mis-alumnos',
    name: 'MisAlumnos',
    component: MisAlumnos,
    meta: { 
      title: 'Mi Tutoría', 
      requiresAuth: true, 
      role: 'TUTOR' // Solo accesible para rol TUTOR
    }
  },
  {
    path: '/validar-sanciones',
    name: 'ValidarSanciones',
    component: ValidarSanciones,
    meta: { 
      title: 'Validar Sanciones', 
      requiresAuth: true, 
      role: 'JEFATURA' // Solo accesible para rol JEFATURA
    }
  },
  {
    path: '/expulsiones',
    name: 'Expulsiones',
    component: ValidarSanciones,
    meta: {
      title: 'Expulsiones',
      requiresAuth: true,
      role: 'JEFATURA'
    }
  },
  {
    path: '/tareas-expulsion',
    name: 'TareasExpulsion',
    component: TareasExpulsion,
    meta: {
      title: 'Tareas de Expulsión',
      requiresAuth: true,
      role: 'PROFESOR'
    }
  },
  {
    path: '/historial',
    name: 'Historial',
    component: Historial,
    meta: { title: 'Historial de Partes', requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// MIDDLEWARE DE SEGURIDAD (GUARD) - REFORZADO SPRINT 1
router.beforeEach((to, from, next) => {
  // 1. Actualizar el título de la pestaña del navegador
  document.title = to.meta.title 
    ? `${to.meta.title} - Sistema de Partes` 
    : 'Sistema de Partes Disciplinarios'
  
  const profesor = JSON.parse(localStorage.getItem('profesor') || 'null')
  const sesionValida = Boolean(profesor?.email)
  const requiresAuth = to.meta.requiresAuth
  
  // 2. Protección de rutas que requieren estar logueado
  if (requiresAuth && !sesionValida) {
    return next('/login')
  } 

  // 3. Evitar volver al login si ya estás dentro
  if (to.path === '/login' && sesionValida) {
    return next('/nuevo')
  }

  // 4. LÓGICA DE ROLES INTELIGENTE (Para evitar bloqueos injustificados)
  const requiredRole = to.meta.role
  if (requiredRole && profesor) {
    // Intentamos sacar el rol del objeto, si no está, lo deducimos por el email
    let userRole = profesor.rol;
    
    if (!userRole) {
      if (profesor.email.includes('jefe')) userRole = 'JEFATURA';
      else if (profesor.email.includes('tutor')) userRole = 'TUTOR';
      else userRole = 'PROFESOR'; // Fallback para maestros como María García o Ana Martínez
    }

    // Comprobación final de permiso
    if (userRole !== requiredRole) {
      alert(`Acceso denegado: Se requiere rol de ${requiredRole}`)
      return next('/nuevo') // Te devuelve a la zona común
    }
  }

  // 5. Aula de convivencia solo para profesorado de guardia
  if (to.path === '/aula-convivencia' && profesor) {
    const esGuardia = profesor.esGuardia === true || profesor.esGuardia === 1 || profesor.esGuardia === '1'
    if (!esGuardia) {
      alert('Acceso denegado: solo el profesorado de guardia puede evaluar en aula de convivencia')
      return next('/nuevo')
    }
  }

  next()
})

export default router