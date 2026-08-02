import { defineStore } from 'pinia'

function lerClaims(token) {
  try {
    return JSON.parse(atob(token.split('.')[1]))
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: sessionStorage.getItem('gti_token') || null,
    usuario: JSON.parse(sessionStorage.getItem('gti_usuario') || 'null')
  }),

  getters: {
    isTI: (state) => state.usuario?.tipoAcesso === 'TI',
    claims: (state) => (state.token ? lerClaims(state.token) : null),
    precisaTrocarSenha: (state) => lerClaims(state.token || '')?.precisaTrocarSenha ?? false,
    autenticado: (state) => !!state.token
  },

  actions: {
    setSessao(token, usuario) {
      this.token = token
      this.usuario = usuario
      sessionStorage.setItem('gti_token', token)
      sessionStorage.setItem('gti_usuario', JSON.stringify(usuario))
    },
    logout() {
      this.token = null
      this.usuario = null
      sessionStorage.removeItem('gti_token')
      sessionStorage.removeItem('gti_usuario')
    }
  }
})