<script setup>
import { ref, computed, onMounted } from 'vue'
import { Copy, Eye, EyeOff, Check } from 'lucide-vue-next'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import integracaoService from '@/services/integracao'

const RECURSOS = [
  { key: 'ativos', label: 'Ativos' },
  { key: 'usuarios', label: 'Usuários' },
  { key: 'chamados', label: 'Chamados' },
  { key: 'dashboard', label: 'Dashboard' }
]

const aba = ref('ativos')
const previews = ref({})
const chave = ref('')
const chaveVisivel = ref(false)
const copiado = ref('')

const baseUrl = window.location.origin
const urlAtual = computed(() => `${baseUrl}/api/integracao/${aba.value}`)
const chaveMascarada = computed(() => (chave.value ? '•'.repeat(Math.min(chave.value.length, 24)) : ''))
const curlAtual = computed(
  () => `curl -H "X-API-Key: ${chave.value || 'SUA_CHAVE_AQUI'}" ${urlAtual.value}`
)

async function carregar(recurso) {
  if (previews.value[recurso]) return
  previews.value = { ...previews.value, [recurso]: 'Carregando...' }
  try {
    const { data } = await integracaoService.preview(recurso)
    const amostra = Array.isArray(data) ? data.slice(0, 3) : data
    previews.value = { ...previews.value, [recurso]: JSON.stringify(amostra, null, 2) }
  } catch {
    previews.value = { ...previews.value, [recurso]: 'Erro ao carregar prévia.' }
  }
}

function mudarAba(key) {
  aba.value = key
  carregar(key)
}

async function copiar(texto, label) {
  try {
    await navigator.clipboard.writeText(texto)
    copiado.value = label
    setTimeout(() => { if (copiado.value === label) copiado.value = '' }, 1500)
  } catch {}
}

onMounted(async () => {
  carregar('ativos')
  try {
    const { data } = await integracaoService.chave()
    chave.value = data.apiKey
  } catch {}
})
</script>

<template>
  <LayoutSidebar>
    <div class="mb-6">
      <h1 class="text-4xl font-semibold text-texto">Integração via API</h1>
      <div class="text-base text-texto-sub mt-1">Endpoints somente leitura para sistemas externos</div>
    </div>

    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Chave de acesso (X-API-Key)</div>
    <div class="flex items-center gap-2 mb-1 max-w-[640px]">
      <code class="flex-1 font-mono text-sm bg-surface-alt border border-borda rounded-raio-sm px-3 py-2 truncate">
        {{ chaveVisivel ? (chave || 'Carregando...') : chaveMascarada || 'Carregando...' }}
      </code>
      <button
        type="button"
        class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt"
        title="Mostrar/ocultar chave"
        @click="chaveVisivel = !chaveVisivel"
      >
        <EyeOff v-if="chaveVisivel" :size="16" />
        <Eye v-else :size="16" />
      </button>
      <button
        type="button"
        class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt"
        title="Copiar chave"
        @click="copiar(chave, 'chave')"
      >
        <Check v-if="copiado === 'chave'" :size="16" class="text-sucesso-text" />
        <Copy v-else :size="16" />
      </button>
    </div>
    <p class="text-sm text-texto-sub max-w-[640px] mb-6">
      Envie esta chave no header <code class="font-mono text-xs bg-surface-alt border border-borda rounded-raio-sm px-1">X-API-Key</code>
      em toda chamada aos endpoints abaixo. Não é preciso login — é essa chave que autentica sistemas externos.
    </p>

    <div class="flex gap-1 border-b border-borda mb-4">
      <button
        v-for="r in RECURSOS"
        :key="r.key"
        type="button"
        class="px-4 py-2 text-base font-medium bg-transparent cursor-pointer border-x-0 border-t-0 border-b-2 border-solid -mb-px"
        :class="aba === r.key ? 'border-primaria text-primaria-text' : 'border-transparent text-texto-sub hover:text-texto'"
        @click="mudarAba(r.key)"
      >
        {{ r.label }}
      </button>
    </div>

    <div :key="aba">
      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Endereço deste endpoint</div>
      <div class="flex items-center gap-2 mb-4 max-w-[640px]">
        <code class="flex-1 font-mono text-sm bg-surface-alt border border-borda rounded-raio-sm px-3 py-2 truncate">{{ urlAtual }}</code>
        <button
          type="button"
          class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt"
          title="Copiar endereço"
          @click="copiar(urlAtual, 'url')"
        >
          <Check v-if="copiado === 'url'" :size="16" class="text-sucesso-text" />
          <Copy v-else :size="16" />
        </button>
      </div>

      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Exemplo pronto para usar (terminal)</div>
      <div class="flex items-start gap-2 mb-6 max-w-[640px]">
        <pre class="flex-1 font-mono text-xs bg-surface-alt border border-borda rounded-raio-sm px-3 py-2 overflow-x-auto whitespace-pre-wrap break-all">{{ curlAtual }}</pre>
        <button
          type="button"
          class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt shrink-0"
          title="Copiar comando"
          @click="copiar(curlAtual, 'curl')"
        >
          <Check v-if="copiado === 'curl'" :size="16" class="text-sucesso-text" />
          <Copy v-else :size="16" />
        </button>
      </div>

      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Prévia da resposta (3 primeiros registros)</div>
      <pre class="bg-surface border border-borda rounded-raio p-4 text-sm font-mono text-texto overflow-x-auto max-h-[480px] overflow-y-auto whitespace-pre">{{ previews[aba] || 'Carregando...' }}</pre>
    </div>
  </LayoutSidebar>
</template>