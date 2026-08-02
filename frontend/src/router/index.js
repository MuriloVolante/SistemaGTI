import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/home', name: 'home', component: () => import('@/views/Home.vue') },

  { path: '/usuarios', name: 'usuarios', component: () => import('@/views/usuarios/UsuariosList.vue'), meta: { requiresTI: true, modulo: 'usuarios' } },

  { path: '/ativos/dashboard', name: 'ativos-dashboard', component: () => import('@/views/ativos/Dashboard.vue'), meta: { requiresTI: true, modulo: 'ativos' } },
  { path: '/ativos', name: 'ativos-list', component: () => import('@/views/ativos/AtivosList.vue'), meta: { modulo: 'ativos' } },
  { path: '/ativos/:id', name: 'ativos-detalhe', component: () => import('@/views/ativos/AtivoDetalhe.vue'), meta: { modulo: 'ativos' } },
  { path: '/ativos/tipos', name: 'ativos-tipos', component: () => import('@/views/ativos/Tipos.vue'), meta: { requiresTI: true, modulo: 'ativos' } },
  { path: '/ativos/relatorios', name: 'ativos-relatorios', component: () => import('@/views/ativos/Relatorios.vue'), meta: { requiresTI: true, modulo: 'ativos' } },

  { path: '/chamados', name: 'chamados', component: () => import('@/views/chamados/ChamadosList.vue'), meta: { requiresTI: true, modulo: 'chamados' } },
    { path: '/chamados/:id', name: 'chamados-detalhe', component: () => import('@/views/chamados/ChamadoDetalhe.vue'), meta: { requiresTI: true, modulo: 'chamados' } },

    { path: '/meus-chamados', name: 'meus-chamados', component: () => import('@/views/chamados/ChamadosUsuario.vue') },
    { path: '/meus-chamados/:id', name: 'meus-chamados-detalhe', component: () => import('@/views/chamados/ChamadoDetalheUsuario.vue') },

  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.autenticado) return auth.isTI ? '/home' : '/meus-chamados'
    return true
  }

  if (!auth.autenticado) return '/login'
  if (auth.precisaTrocarSenha) return '/login'
  if (to.meta.requiresTI && !auth.isTI) return '/meus-chamados'

  return true
})

export default router