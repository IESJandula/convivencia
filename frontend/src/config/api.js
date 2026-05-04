const DEFAULT_REMOTE_API = 'http://api-convivencia.51.210.104.106.sslip.io/api'
const LOCAL_API = 'http://localhost:8080/api'

const isLocalhost = typeof window !== 'undefined' && window.location.hostname === 'localhost'

export const API_URL = import.meta.env.VITE_API_URL || (isLocalhost ? LOCAL_API : DEFAULT_REMOTE_API)
