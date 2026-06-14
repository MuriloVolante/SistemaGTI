// ─── Autenticação JWT ─────────────────────────────────────────────────────────

function getToken() {
    return sessionStorage.getItem('gti_token');
}

function getUsuario() {
    const u = sessionStorage.getItem('gti_usuario');
    return u ? JSON.parse(u) : null;
}

function logout() {
    sessionStorage.removeItem('gti_token');
    sessionStorage.removeItem('gti_usuario');
    window.location.href = '/login.html';
}

// Verifica se está logado. Se não, redireciona para login.
function lerClaims() {
    const t = getToken();
    if (!t) return null;
    try {
        return JSON.parse(atob(t.split('.')[1]));
    } catch (e) {
        return null;
    }
}

function checarAuth() {
    const claims = lerClaims();
    if (!claims) {
        window.location.href = '/login.html';
        return false;
    }
    if (claims.precisaTrocarSenha) {
        // troca de senha pendente — volta ao login para concluir
        window.location.href = '/login.html';
        return false;
    }
    return true;
}

// Substitui o fetch normal — sempre envia o Bearer token
async function apiFetch(url, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...(options.headers || {})
    };

    const res = await fetch(url, { ...options, headers });

    if (res.status === 401 || res.status === 403) {
        console.warn('[GTI] Sessão expirada ou sem permissão. Redirecionando para login.');
        logout();
        throw new Error('Sessão expirada.');
    }

    return res;
}