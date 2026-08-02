<script setup>
import { ref, onMounted } from 'vue'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import integracaoService from '@/services/integracao'

const RECURSOS = [
  { key: 'ativos', label: 'Ativos' },
  { key: 'usuarios', label: 'Usuarios' },
  { key: 'chamados', label: 'Chamados' },
  { key: 'dashboard', label: 'Dashboard' }
]

const aba = ref('ativos')
const previews = ref({})

async function carregar(recurso) {
  if (previews.value[recurso]) return
  previews.value = { ...previews.value, [recurso]: 'Carregando...' }
  try {
    const { data } = await integracaoService.preview(recurso)
    const amostra = Array.isArray(data) ? data.slice(0, 3) : data
    previews.value = { ...previews.value, [recurso]: JSON.stringify(amostra, null, 2) }
  } catch {
    previews.value = { ...previews.value, [recurso]: 'Erro ao carregar previa.' }
  }
}

function mudarAba(key) {
  aba.value = key
  carregar(key)
}

onMounted(() => carregar('ativos'))
</script>

<template>
  <LayoutSidebar>
    <div class="mb-6">
      <h1 class="text-4xl font-semibold text-texto">Integracao via API</h1>
      <div class="text-base text-texto-sub mt-1">Endpoints somente leitura para sistemas externos</div>
    </div>

    <div class="flex gap-1 border-b border-borda mb-4">
      <button
        v-for="r in RECURSOS"
        :key="r.key"
        type="button"
        class="px-4 py-2 text-base font-medium border-none bg-transparent cursor-pointer border-b-2 -mb-px"
        :class="aba === r.key ? 'border-primaria text-primaria-text' : 'border-transparent text-texto-sub hover:text-texto'"
        @click="mudarAba(r.key)"
      >
        {{ r.label }}
      </button>
    </div>

    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Como autenticar</div>
    <p class="text-base text-texto-sub leading-relaxed max-w-[640px] mb-6">
      Envie o header
      <code class="font-mono text-sm bg-surface-alt border border-borda rounded-raio-sm px-1">X-API-Key</code>
      com a chave configurada em
      <code class="font-mono text-sm bg-surface-alt border border-borda rounded-raio-sm px-1">application.properties</code>
      (propriedade
      <code class="font-mono text-sm bg-surface-alt border border-borda rounded-raio-sm px-1">integracao.api-key</code>).
      Endpoints somente leitura (GET).
    </p>

    <div v-for="r in RECURSOS" :key="r.key" v-show="aba === r.key">
      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">
        GET /api/integracao/{{ r.key }}
      </div>
      <pre class="bg-surface border border-borda rounded-raio p-4 text-sm font-mono text-texto overflow-x-auto max-h-[480px] overflow-y-auto">{{ previews[r.key] || 'Carregando...' }}</pre>
    </div>
  </LayoutSidebar>
</template>