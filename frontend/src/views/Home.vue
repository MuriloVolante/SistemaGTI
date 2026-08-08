<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Users, Monitor, Ticket } from 'lucide-vue-next'

const router = useRouter()
const auth = useAuthStore()

const modulos = [
  { rota: '/usuarios', label: 'Gestão de usuários', icone: Users },
  { rota: '/ativos/dashboard', label: 'Gestão de Ativos', icone: Monitor },
  { rota: '/chamados', label: 'Gestão de Chamados', icone: Ticket }
]

function sair() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div>
    <div class="fixed top-5 right-6">
      <a href="#" class="text-base text-texto-sub no-underline hover:text-texto" @click.prevent="sair">
        Logout →
      </a>
    </div>

    <div class="flex flex-col items-center justify-center min-h-screen p-8">
      <h1 class="text-4xl font-semibold text-center mb-12 text-texto">
        Selecione o módulo que deseja navegar:
      </h1>

      <div class="flex gap-8 flex-wrap justify-center">
        <div
          v-for="m in modulos"
          :key="m.rota"
          class="bg-surface border border-borda rounded-raio shadow-sombra px-8 py-11 w-[280px] min-h-[220px] flex flex-col items-center justify-center gap-5 cursor-pointer transition-all hover:border-primaria hover:shadow-sombra-md hover:-translate-y-0.5"
          @click="router.push(m.rota)"
        >
          <div class="w-[72px] h-[72px] rounded-raio bg-primaria-light border border-borda flex items-center justify-center text-primaria">
            <component :is="m.icone" :size="34" />
          </div>
          <div class="text-lg font-semibold text-center text-texto">{{ m.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>