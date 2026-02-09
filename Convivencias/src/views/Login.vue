<template>
  <div class="login-container">
    <img src="../assets/logo-jandula.png" alt="Logo" width="100">
    <h2>Gestión de Convivencia</h2>
    <form @submit.prevent="handleLogin">
      <input v-model="form.email" type="email" placeholder="Email (@jandula.es)" required>
      <input v-model="form.password" type="password" placeholder="Contraseña" required>
      <button type="submit">Entrar al Sistema</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { auth } from '../firebase'; // <--- Asegúrate de tener src/firebase.js configurado
import { signInWithEmailAndPassword } from "firebase/auth";

const router = useRouter();
const form = ref({ email: '', password: '' });
const error = ref(null);

const handleLogin = async () => {
  try {
    error.value = null;

    // 1. PRIMERO: Autenticamos con Firebase
    const userCredential = await signInWithEmailAndPassword(auth, form.value.email, form.value.password);
    const uid = userCredential.user.uid;

    // 2. SEGUNDO: Enviamos el UID a nuestro Backend para obtener el ROL
    const res = await fetch('http://localhost:3000/api/login-firebase', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uid: uid }) // Solo enviamos el UID, no la contraseña
    });

    if (!res.ok) {
      const errorData = await res.json();
      throw new Error(errorData.message || "Usuario no registrado en el centro");
    }

    const user = await res.json();
    
    // 3. TERCERO: Guardamos el perfil (nombre, rol, grupo) en el navegador
    localStorage.setItem('user', JSON.stringify(user));
    
    window.dispatchEvent(new Event('user-logged-in'));
    router.push('/');
    
  } catch (err) {
    // Manejo de errores de Firebase o del Servidor
    console.error(err);
    error.value = "Email o contraseña incorrectos o usuario no autorizado";
  }
};
</script>

<style scoped>
.login-container { max-width: 300px; margin: 100px auto; text-align: center; }
input { display: block; width: 100%; margin-bottom: 10px; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
button { width: 100%; background: #42b983; color: white; border: none; padding: 10px; cursor: pointer; border-radius: 4px; }
.error { color: red; font-size: 0.8rem; margin-top: 10px; }
</style>