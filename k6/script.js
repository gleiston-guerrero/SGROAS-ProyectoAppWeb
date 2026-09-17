import http from 'k6/http';
import { check, sleep } from 'k6';
import { opciones } from './opts.js';
import { Rate, Trend } from 'k6/metrics';

export let errorRate = new Rate('errores');
export let duracionListado = new Trend('duracion_listado');

export const options = opciones;

const BASE_URL = __ENV.BASE_URL || 'https://sgroas-backend.onrender.com';

let authToken = null;

export function setup() {
  const loginPayload = JSON.stringify({
    email: __ENV.K6_LOGIN_EMAIL || 'admin@sgroas.com',
    password: __ENV.K6_LOGIN_PASSWORD,
  });

  if (!loginPayload.includes('"password":"') || !__ENV.K6_LOGIN_PASSWORD) {
    throw new Error(
      'K6_LOGIN_PASSWORD env var is required (no credentials are hardcoded here). ' +
      'Run e.g.: k6 run -e K6_LOGIN_PASSWORD=*** -e BASE_URL=... script.js'
    );
  }

  const res = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const cookies = res.cookies;
  let token = null;

  if (cookies.access_token && cookies.access_token.length > 0) {
    token = cookies.access_token[0].value;
  }

  if (!token) {
    const body = res.json();
    token = body.accessToken;
  }

  if (!token) {
    throw new Error('No se pudo obtener el token de autenticacion');
  }

  return { token };
}

export default function (data) {
  const res = http.get(`${BASE_URL}/api/conductores`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.token}`,
    },
  });

  duracionListado.add(res.timings.duration);

  const exitoso = check(res, {
    'status es 200': (r) => r.status === 200,
    'tiempo respuesta < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!exitoso);

  sleep(1);
}
