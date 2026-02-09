// main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // El archivo que creamos antes

const app = createApp(App)
app.use(router) // <--- ESTA LÍNEA ES VITAL
app.mount('#app')