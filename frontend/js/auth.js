const BASE = '/coovalluna/api';

export function getToken() {
    return sessionStorage.getItem('jwt');
}
export function getUser() {
    const raw = sessionStorage.getItem('usuario');
    return raw ? JSON.parse(raw) : null;
}
export function setSession(token, usuario) {
    sessionStorage.setItem('jwt', token);
    sessionStorage.setItem('usuario', JSON.stringify(usuario));
}
export function clearSession() {
    sessionStorage.removeItem('jwt');
    sessionStorage.removeItem('usuario');
}
export function isLoggedIn() {
    return !!getToken();
}
export function getRol() {
    const u = getUser();
    return u ? u.rol : null;
}

export async function login(username, password) {
    const res = await fetch(`${BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ mensaje: 'Error de conexión' }));
        throw new Error(err.mensaje || 'Credenciales incorrectas');
    }
    return res.json();
}

export function logout() {
    clearSession();
    window.location.href = '/coovalluna/pages/login.html';
}


export function redirectByRole() {
    const rol = getRol();
    if (!rol) { logout(); return; }
    if (rol === 'ADMIN')    window.location.href = '/coovalluna/pages/admin.html';
    else if (rol === 'ASESOR') window.location.href = '/coovalluna/pages/asesor.html';
    else if (rol === 'ASOCIADO') window.location.href = '/coovalluna/pages/asociado.html';
}