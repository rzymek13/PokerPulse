const productionApiBase = 'https://pokerpulse-cya3hygchtejgzhc.polandcentral-01.azurewebsites.net';

export const API_BASE = import.meta.env.VITE_API_BASE
  ?? (import.meta.env.DEV ? 'http://localhost:8080' : productionApiBase);

export const SOCKJS_URL = `${API_BASE}/ws`;
