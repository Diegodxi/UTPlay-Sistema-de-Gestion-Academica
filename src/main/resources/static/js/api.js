/* Cliente HTTP y utilidades compartidas de UTPlay. */
window.UTPlay = (() => {
    const TOKEN_KEY = 'utplay_token';
    const USER_KEY = 'utplay_usuario';

    function token() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function user() {
        try {
            return JSON.parse(localStorage.getItem(USER_KEY));
        } catch (_) {
            return null;
        }
    }

    function setAuth(loginResponse) {
        localStorage.setItem(TOKEN_KEY, loginResponse.token);
        localStorage.setItem(USER_KEY, JSON.stringify(loginResponse.usuario));
    }

    function updateUser(usuario) {
        localStorage.setItem(USER_KEY, JSON.stringify(usuario));
        fillUserChrome(usuario);
    }

    function clearAuth() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        sessionStorage.removeItem('utplay_ultimo_resultado');
    }

    function logout() {
        clearAuth();
        window.location.replace('/login.html');
    }

    function requireAuth() {
        if (!token() || !user()) {
            logout();
            return false;
        }
        return true;
    }

    async function api(path, options = {}) {
        const { publicRequest = false, ...fetchOptions } = options;
        const headers = new Headers(fetchOptions.headers || {});
        if (fetchOptions.body && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }
        if (!publicRequest && token()) {
            headers.set('Authorization', `Bearer ${token()}`);
        }

        let response;
        try {
            response = await fetch(path, { ...fetchOptions, headers });
        } catch (_) {
            throw new Error('No se pudo conectar con UTPlay. Verifica que el servidor este encendido.');
        }

        const contentType = response.headers.get('content-type') || '';
        const data = contentType.includes('application/json') ? await response.json() : null;

        if (!response.ok) {
            if (response.status === 401 && !publicRequest) {
                clearAuth();
                window.location.replace('/login.html?sesion=expirada');
            }
            const validation = data?.erroresValidacion
                ? Object.values(data.erroresValidacion).join(' ')
                : '';
            throw new Error(validation || data?.mensaje || `Error ${response.status}`);
        }
        return data;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function firstName(name) {
        return String(name || 'Estudiante').trim().split(/\s+/)[0];
    }

    function formatDate(value, includeTime = false) {
        if (!value) return '—';
        const date = new Date(value);
        const options = includeTime
            ? { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }
            : { day: '2-digit', month: 'short', year: 'numeric' };
        return new Intl.DateTimeFormat('es-PE', options).format(date);
    }

    function setLoading(button, loading) {
        if (!button) return;
        button.disabled = loading;
        const text = button.querySelector('.button-text');
        const loader = button.querySelector('.button-loader');
        if (text) text.hidden = loading;
        if (loader) loader.hidden = !loading;
    }

    function showAlert(element, message, success = false) {
        if (!element) return;
        element.textContent = message;
        element.classList.toggle('success', success);
        element.hidden = false;
        element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function hideAlert(element) {
        if (element) element.hidden = true;
    }

    function toast(message) {
        const element = document.getElementById('toast');
        if (!element) return;
        element.textContent = message;
        element.hidden = false;
        window.setTimeout(() => { element.hidden = true; }, 3200);
    }

    function fillUserChrome(usuario = user()) {
        if (!usuario) return;
        document.querySelectorAll('.js-user-name').forEach(el => { el.textContent = firstName(usuario.nombre); });
        document.querySelectorAll('.js-user-initial').forEach(el => { el.textContent = firstName(usuario.nombre).charAt(0).toUpperCase(); });
        document.querySelectorAll('.js-user-cycle').forEach(el => { el.textContent = `${usuario.cicloActual}.° ciclo`; });
    }

    return {
        api, token, user, setAuth, updateUser, clearAuth, logout, requireAuth,
        escapeHtml, firstName, formatDate, setLoading, showAlert, hideAlert,
        toast, fillUserChrome
    };
})();
