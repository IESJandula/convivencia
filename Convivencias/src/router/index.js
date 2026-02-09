import { createRouter, createWebHistory } from 'vue-router';
import Dashboard from '../views/Dashboard.vue';
import Login from '../views/Login.vue';

const routes = [
  { path: '/', component: Dashboard, meta: { auth: true } },
  { path: '/login', component: Login },
  { 
    path: '/jefatura', 
    component: () => import('../views/Jefatura.vue'),
    meta: { auth: true, role: 'JEFATURA' } 
  },
  { 
    path: '/mis-tutorados', 
    component: () => import('../views/VistaTutor.vue'),
    meta: { auth: true, role: 'TUTOR' } 
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user'));

  if (to.meta.auth && !user) {
    next('/login');
  } else if (to.meta.role && user.role !== to.meta.role && user.role !== 'JEFATURA') {
    // Si no tiene el rol y NO es jefatura (jefatura puede verlo todo)
    next('/');
  } else {
    next();
  }
});

export default router;