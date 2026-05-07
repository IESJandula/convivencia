const DEFAULT_REMOTE_API = 'http://api-convivencia.51.210.104.106.sslip.io/api';
const LOCAL_API = 'http://localhost:8080/api';

const isLocalhost = typeof window !== 'undefined'
	&& (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');

// Eliminamos la lectura de la variable de entorno para evitar que tome la URL incorrecta del frontend
export const API_URL = isLocalhost ? LOCAL_API : DEFAULT_REMOTE_API;