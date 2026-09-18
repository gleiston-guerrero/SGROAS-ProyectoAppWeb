import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// Corrige el problema real senalado en una auditoria externa: el contraste
// frio/caliente anterior comparaba cold.js (1 VU, 1 iteracion) contra
// script.js (50 VUs, 30s) -- perfiles de carga distintos, no comparables, y
// ademas las corridas del 6-sep se hicieron antes del fix de @Cacheable
// (self-invocation), asi que ninguna midio caché real.
//
// Este script usa el MISMO usuario y el MISMO perfil de carga (1 VU,
// secuencial) para las dos condiciones: la iteracion 1 pega contra una
// clave de cache que garantizadamente nunca se pidio antes (query param con
// timestamp unico) -> cache MISS real. Las iteraciones 2..N piden la MISMA
// clave ya cacheada -> cache HIT real. Frio y caliente quedan medidos con
// el mismo usuario, la misma maquina, la misma corrida.

export let duracionFria = new Trend('duracion_fria');
export let duracionCaliente = new Trend('duracion_caliente');

const ITERACIONES_CALIENTES = Number(__ENV.WARM_ITERATIONS || 10);

export const options = {
  vus: 1,
  iterations: 1 + ITERACIONES_CALIENTES,
  thresholds: {
    http_req_failed: [{ threshold: 'rate<1', abortOnFail: false }],
  },
  summaryTrendStats: ['avg', 'med', 'min', 'max', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'https://sgroas-backend.onrender.com';
// Tamano de pagina fijo y distinto del que usan otras corridas (k6/script.js,
// k6/cold.js), para no compartir la clave de cache con ellas y garantizar
// que la primera peticion de ESTA corrida sea un miss real.
const PAGE_SIZE = Number(__ENV.CACHE_CONTRAST_PAGE_SIZE || 7);

export function setup() {
  const loginPayload = JSON.stringify({
    email: __ENV.K6_LOGIN_EMAIL || 'admin@sgroas.com',
    password: __ENV.K6_LOGIN_PASSWORD,
  });

  if (!__ENV.K6_LOGIN_PASSWORD) {
    throw new Error(
      'K6_LOGIN_PASSWORD env var is required (no credentials are hardcoded here). ' +
      'Run e.g.: k6 run -e K6_LOGIN_PASSWORD=*** -e BASE_URL=... cache-contrast.js'
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
  const url = `${BASE_URL}/api/conductores?page=0&size=${PAGE_SIZE}`;
  const res = http.get(url, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.token}`,
    },
  });

  const esFria = __ITER === 0;
  if (esFria) {
    duracionFria.add(res.timings.duration);
  } else {
    duracionCaliente.add(res.timings.duration);
  }

  check(res, { 'status es 200': (r) => r.status === 200 });

  sleep(1);
}
