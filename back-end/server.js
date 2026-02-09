const express = require('express');
const mysql = require('mysql2');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const db = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: '1234', 
  database: 'convivencia_ies_jandula'
});

// NUEVA RUTA: Login mediante UID de Firebase
app.post('/api/login-firebase', (req, res) => {
  const { uid } = req.body; // Recibimos el código único de Firebase

  // Buscamos al usuario que tenga ese UID y traemos su grupo (si es tutor)
  const query = `
    SELECT u.id, u.first_name, u.role, g.name as group_name 
    FROM users u 
    LEFT JOIN class_groups g ON u.id = g.tutor_id 
    WHERE u.firebase_uid = ?
  `;
  
  db.query(query, [uid], (err, results) => {
    if (err) {
      console.error("Error en DB:", err);
      return res.status(500).json({ error: "Error interno del servidor" });
    }

    if (results.length > 0) {
      // Si el UID existe en nuestra tabla 'users', devolvemos su perfil
      res.json(results[0]); 
    } else {
      // Si Firebase lo autenticó pero NO está en nuestra tabla de profes...
      res.status(404).json({ message: "Usuario autenticado pero no registrado en la base de datos local" });
    }
  });
});

app.listen(3000, () => {
  console.log('✅ Servidor de Convivencia (Firebase Mode) en puerto 3000');
});