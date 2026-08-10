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

axios.interceptors.request.use((config) => {
  const result = riflClient.headersFor(config.url ?? '', config.method ?? '');
  if (result) {
    Object.assign(config.headers, result.headers);
    config.riflSeq = result.seq;
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
