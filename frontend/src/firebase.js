import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";

const firebaseConfig = {
  apiKey: "AIzaSyCV3uXa89o5UNucAkjEv2j0ZIUt9msmkA4",
  authDomain: "jandula-convivencia.firebaseapp.com",
  projectId: "jandula-convivencia",
  storageBucket: "jandula-convivencia.firebasestorage.app",
  messagingSenderId: "165263920680",
  appId: "1:165263920680:web:5c0110964e7593236caf1d",
  measurementId: "G-3S6ND84SF5"
};

// Inicialización
const app = initializeApp(firebaseConfig);

// Exportación de servicios
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);

// Configuración de Google
export const googleProvider = new GoogleAuthProvider();
// Esto fuerza a que siempre pregunte qué cuenta usar (útil si tienen varias)
googleProvider.setCustomParameters({
  prompt: 'select_account'
});