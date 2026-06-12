import { getToken, logout } from './auth.js';

const BASE = '/coovalluna/api';

export async function api(path, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...(options.headers || {})
    };

    const res = await fetch(`${BASE}${path}`, { ...options, headers });

    if (res.status === 401) { logout(); return; }

    if (!res.ok) {
        const err = await res.json().catch(() => ({ mensaje: `Error ${res.status}` }));
        throw new Error(err.mensaje || `Error ${res.status}`);
    }

    if (res.status === 204) return null;
    return res.json();
}

export const get    = (path)         => api(path);
export const post   = (path, body)   => api(path, { method: 'POST',   body: JSON.stringify(body) });
export const put    = (path, body)   => api(path, { method: 'PUT',    body: JSON.stringify(body) });
export const del    = (path)         => api(path, { method: 'DELETE' });

export function buildQuery(params) {
    const q = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) {
        if (v !== null && v !== undefined && v !== '') q.append(k, v);
    }
    const s = q.toString();
    return s ? `?${s}` : '';
}