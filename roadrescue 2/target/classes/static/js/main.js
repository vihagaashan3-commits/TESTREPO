/* ===========================
   RoadRescue - main.js
   =========================== */

// ── CSRF helper for fetch/XHR ──────────────────────────────────────────────
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

function csrfHeaders(extra = {}) {
    const h = { 'Content-Type': 'application/json', ...extra };
    if (csrfToken && csrfHeader) h[csrfHeader] = csrfToken;
    return h;
}

// ── Auto-dismiss alerts after 4 s ─────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.alert.alert-success, .alert.alert-info').forEach(el => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(el);
            bsAlert?.close();
        }, 4000);
    });
});

// ── Confirm delete forms ───────────────────────────────────────────────────
document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', e => {
        if (!confirm(form.dataset.confirm)) e.preventDefault();
    });
});

// ── WebSocket / STOMP connection (shared) ─────────────────────────────────
let stompClient = null;

function connectWebSocket(onMessage) {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // suppress console spam

    stompClient.connect({}, frame => {
        console.log('WS connected');

        // Per-user queue for request status updates
        stompClient.subscribe('/user/queue/request-update', msg => {
            if (typeof onMessage === 'function') onMessage(msg.body);
            showToast('Update', msg.body.replace(':', ': '), 'info');
        });

        // Per-user queue for new notifications
        stompClient.subscribe('/user/queue/notifications', msg => {
            updateNotificationBadge();
        });
    }, err => {
        console.warn('WS error, retrying in 5 s…', err);
        setTimeout(() => connectWebSocket(onMessage), 5000);
    });
}

// ── Toast helper ──────────────────────────────────────────────────────────
function showToast(title, message, type = 'info') {
    const container = getOrCreateToastContainer();
    const id = 'toast-' + Date.now();
    const colorMap = { info: 'text-bg-info', success: 'text-bg-success',
                       warning: 'text-bg-warning', danger: 'text-bg-danger' };
    const bg = colorMap[type] || 'text-bg-secondary';

    container.insertAdjacentHTML('beforeend', `
        <div id="${id}" class="toast align-items-center ${bg} border-0" role="alert" aria-live="assertive">
            <div class="d-flex">
                <div class="toast-body"><strong>${title}:</strong> ${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto"
                        data-bs-dismiss="toast"></button>
            </div>
        </div>`);

    const el = document.getElementById(id);
    const toast = new bootstrap.Toast(el, { delay: 5000 });
    toast.show();
    el.addEventListener('hidden.bs.toast', () => el.remove());
}

function getOrCreateToastContainer() {
    let c = document.getElementById('toast-container');
    if (!c) {
        c = document.createElement('div');
        c.id = 'toast-container';
        c.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        c.style.zIndex = '9999';
        document.body.appendChild(c);
    }
    return c;
}

// ── Update notification badge dynamically ─────────────────────────────────
function updateNotificationBadge() {
    fetch('/notifications/count', { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then(r => r.json())
        .then(data => {
            const badge = document.querySelector('.navbar .badge.bg-warning');
            if (badge) {
                badge.textContent = data.count;
                badge.classList.toggle('d-none', data.count === 0);
            }
        }).catch(() => {});
}

// ── Plate number uppercase ─────────────────────────────────────────────────
document.querySelectorAll('input[placeholder*="plate"], input[placeholder*="Plate"]')
    .forEach(el => el.addEventListener('input', () => { el.value = el.value.toUpperCase(); }));

// ── Initialize WS on pages that need it ───────────────────────────────────
if (document.getElementById('wsAlert')) {
    connectWebSocket(body => {
        const alert = document.getElementById('wsAlert');
        const msg   = document.getElementById('wsMessage');
        if (alert && msg) {
            alert.classList.remove('d-none');
            msg.textContent = 'Status updated to: ' + body.split(':')[0];
            setTimeout(() => location.reload(), 2000);
        }
    });
}
