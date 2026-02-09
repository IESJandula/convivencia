// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getAnalytics } from "firebase/analytics";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyCV3uXa89o5UNucAkjEv2j0ZIUt9msmkA4",
  authDomain: "jandula-convivencia.firebaseapp.com",
  projectId: "jandula-convivencia",
  storageBucket: "jandula-convivencia.firebasestorage.app",
  messagingSenderId: "165263920680",
  appId: "1:165263920680:web:5c0110964e7593236caf1d",
  measurementId: "G-3S6ND84SF5"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
export const auth = getAuth(app);