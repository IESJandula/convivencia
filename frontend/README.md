Frontend Vue 3 - Partes Disciplinarios (iPasen style)
Carpeta: /mnt/data/frontend-partes-ipasen

Instrucciones:
1. cd /mnt/data/frontend-partes-ipasen
2. npm install
3. npm run dev

Endpoints esperados en backend (http://api-convivencia.51.210.104.106.sslip.io):
- GET /api/partes
- POST /api/partes
- GET /api/partes/conductas
- GET /api/partes/alumnos?curso=...

Ajusta src/services/api.js para usar autenticación real (header X-User-Email).
