let toastContainer;
function getToastContainer() {
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container';
        document.body.appendChild(toastContainer);
    }
    return toastContainer;
}

export function toast(msg, type = 'success', duration = 3500) {
    const icons = { success: '✓', error: '✕', warning: '⚠' };
    const el = document.createElement('div');
    el.className = `toast${type !== 'success' ? ' ' + type : ''}`;
    el.innerHTML = `<span>${icons[type] || '✓'}</span><span>${msg}</span>`;
    getToastContainer().appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity .3s'; setTimeout(() => el.remove(), 300); }, duration);
}


let loadEl;
export function showLoading(msg = 'Cargando…') {
    if (loadEl) return;
    loadEl = document.createElement('div');
    loadEl.className = 'loading-overlay';
    loadEl.innerHTML = `<div class="spinner spinner-lg"></div><p>${msg}</p>`;
    document.body.appendChild(loadEl);
}
export function hideLoading() {
    if (loadEl) { loadEl.remove(); loadEl = null; }
}


export function confirm(msg) {
    return new Promise(resolve => {
        const ov = document.createElement('div');
        ov.className = 'modal-overlay';
        ov.innerHTML = `
      <div class="modal" style="max-width:400px">
        <div class="modal-header"><span class="modal-title">⚠ Confirmar</span></div>
        <div class="modal-body"><p>${msg}</p></div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="cfn">Cancelar</button>
          <button class="btn btn-danger" id="cfy">Confirmar</button>
        </div>
      </div>`;
        document.body.appendChild(ov);
        ov.querySelector('#cfy').onclick = () => { ov.remove(); resolve(true); };
        ov.querySelector('#cfn').onclick = () => { ov.remove(); resolve(false); };
    });
}

export function openModal(id) {
    document.getElementById(id)?.classList.remove('hidden');
}
export function closeModal(id) {
    document.getElementById(id)?.classList.add('hidden');
}
export function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.add('hidden'));
}


export function fmtMoneda(n) {
    if (n == null) return '—';
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(n);
}
export function fmtFecha(s) {
    if (!s) return '—';
    const d = new Date(s + (s.includes('T') ? '' : 'T00:00:00'));
    return d.toLocaleDateString('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
export function fmtFechaHora(s) {
    if (!s) return '—';
    const d = new Date(s);
    return d.toLocaleString('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function badge(val) {
    const cls = `badge badge-${(val||'').replace(/\s/g,'_').toLowerCase()}`;
    return `<span class="${cls}">${val || '—'}</span>`;
}


export function buildTable(cols, rows, actionsFn) {
    if (!rows || rows.length === 0) {
        return `<div class="empty-state"><div class="empty-icon">📋</div><h3>Sin resultados</h3><p>No hay datos para mostrar con los filtros actuales.</p></div>`;
    }
    const head = cols.map(c => `<th>${c.label}</th>`).join('');
    const body = rows.map(r => {
        const cells = cols.map(c => `<td>${c.render ? c.render(r[c.key], r) : (r[c.key] ?? '—')}</td>`).join('');
        const actions = actionsFn ? `<td class="table-actions">${actionsFn(r)}</td>` : '';
        return `<tr>${cells}${actions}</tr>`;
    }).join('');
    const actHead = actionsFn ? '<th>Acciones</th>' : '';
    return `<div class="table-wrap"><table class="table"><thead><tr>${head}${actHead}</tr></thead><tbody>${body}</tbody></table></div>`;
}


export function setActiveNav(id) {
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById(id)?.classList.add('active');
}


export function renderUserInfo(user) {
    document.querySelectorAll('[data-username]').forEach(el => el.textContent = user.username || '');
    document.querySelectorAll('[data-rol]').forEach(el => el.textContent = user.rol || '');
    document.querySelectorAll('[data-avatar]').forEach(el => el.textContent = (user.username || '?')[0].toUpperCase());
}