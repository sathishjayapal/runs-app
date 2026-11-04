import React from 'react';
import ReactDOM from 'react-dom/client';
import { initReactI18next } from 'react-i18next';
import i18n from 'i18next';
import axios from 'axios';
import translation from './translation.json';
import AppRoutes from './app/routes';
import { riflClient } from 'app/common/rifl-client';
import './index.css';


i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: translation },
    },
    lng: 'en',
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false
    }
  });

axios.defaults.baseURL = process.env.API_PATH;
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

riflClient.open().catch(() => { /* server not yet ready; lease will be absent until retry */ });

// Helper function to get CSRF token from cookie
function getCsrfToken(): string | null {
  const name = 'XSRF-TOKEN';
  let cookieValue: string | null = null;
  if (document.cookie && document.cookie !== '') {
    const cookies = document.cookie.split(';');
    for (let i = 0; i < cookies.length; i++) {
      const cookie = (cookies[i] ?? '').trim();
      if (cookie.substring(0, name.length + 1) === (name + '=')) {
        cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
        break;
      }
    }
  }
  return cookieValue;
}

// Fetch CSRF token on app initialization
axios.get('/api/current-user').catch(() => {
  // Even if this fails, the response will set the CSRF token cookie
});

axios.interceptors.request.use(async (config) => {
  if (riflClient.appliesTo(config.url ?? '', config.method ?? '')) {
    try {
      await riflClient.ensureOpen();
    } catch (e) {
      console.error('RIFL lease could not be opened; the request will be rejected with 400', e);
    }
  }
  const result = riflClient.headersFor(config.url ?? '', config.method ?? '');
  if (result) {
    Object.assign(config.headers, result.headers);
    config.riflSeq = result.seq;
  }

  // Add CSRF token to state-changing requests
  const method = (config.method ?? '').toLowerCase();
  if (['post', 'put', 'delete', 'patch'].includes(method)) {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      config.headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  return config;
});

axios.interceptors.response.use(
  (response) => {
    if (response.config.riflSeq != null) {
      riflClient.ack(response.config.riflSeq);
    }

    const contentType = String(response.headers['content-type'] ?? '');
    if (response.config.url?.startsWith('/api') && contentType.includes('text/html')) {
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      return Promise.reject(new Error('Session expired'));
    }
    return response;
  },
  (error) => {
    if (error.config?.riflSeq != null) {
      riflClient.ack(error.config.riflSeq);
    }
    if (error.response?.status === 410) {
      // Lease expired or unknown (e.g. the server restarted); open a new one for the next save.
      riflClient.reset();
    }
    if (error.response?.status >= 400) {
      // The /error page only shows a status code; keep the server's reason in the browser console.
      console.error(`${error.config?.method?.toUpperCase()} ${error.config?.url} -> ${error.response.status}`,
          error.response.data, 'correlationId=' + error.response.headers?.['x-correlation-id']);
    }
    if (error.response?.status === 401 && window.location.pathname !== '/login') {
      window.location.href = '/login';
      return Promise.reject(error);
    }
    return Promise.reject(error);
  }
);

const root = document.getElementById('root')!!;
ReactDOM.createRoot(root).render(
  <AppRoutes />
);
