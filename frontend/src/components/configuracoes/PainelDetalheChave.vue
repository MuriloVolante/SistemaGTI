<script setup>
import { ref, computed, watch } from 'vue'
import { X, Power, RefreshCw } from 'lucide-vue-next'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import integracaoService from '@/services/integracao'

const props = defineProps({
  open: { type: Boolean, required: true },
  chaveId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'desligar', 'girada'])

const ROTULO_ESCOPO = {
  ATIVOS: 'Ativos',
  USUARIOS: 'Usuários',
  CHAMADOS: 'Chamados',
  DASHBOARD: 'Painel'
}

const MOTIVOS = {
  SEM_CHAVE: 'Chave não enviada',
  CHAVE_INVALIDA: 'Chave inexistente',
  CHAVE_DESLIGADA: 'Chave desligada',
  CHAVE_EXPIRADA: 'Chave expirada',
  IP_NAO_AUTORIZADO: 'Endereço não autorizado',
  ESCOPO_NEGADO: 'Sem acesso ao conjunto',
  LIMITE_EXCEDIDO: 'Limite por minuto excedido'
}

const detalhe = ref(null)
const carregando = ref(false)
const girando = ref(false)
const erro = ref('')

const maiorDia = computed(() => {
  if (!detalhe.value?.serie?.length) return 0
  return Math.max(...detalhe.value.serie.map((p) => p.total))
})

const serieCompleta = computed(() => {
  const mapa = new Map((detalhe.value?.serie || []).map((p) => [p.dia, p]))
  const dias = []
  for (let i = 29; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const chave = d.toISOString().slice(0, 10)
    const p = mapa.get(chave)
    dias.push({
      dia: chave,
      total: p ? p.total : 0,
      negadas: p ? p.negadas : 0,
      rotulo: d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
    })
  }
  return dias
})

watch(
  () => [props.open, props.chaveId],
  async () => {
    if (!props.open || !props.chaveId) return
    carregando.value = true
    erro.value = ''
    detalhe.value = null
    try {
      const { data } = await integracaoService.detalhar(props.chaveId)
      detalhe.value = data
    } catch (e) {
      erro.value = e?.response?.data?.erro || 'Não foi possível carregar os detalhes.'
    } finally {
      carregando.value = false
    }
  },
  { immediate: true }
)

async function girar() {
  if (!detalhe.value) return
  girando.value = true
  try {
    const { data } = await integracaoService.girar(props.chaveId, 7)
    emit('girada', data)
  } catch (e) {
    erro.value = e?.response?.data?.erro || 'Não foi possível girar a chave.'
  } finally {
    girando.value = false
  }
}

function dataHora(valor) {
  if (!valor) return '—'
  return new Date(valor).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

function dataCurta(valor) {
  if (!valor) return 'Sem validade'
  return new Date(valor).toLocaleDateString('pt-BR')
}

function rotuloSituacao(s) {
  if (s === 'ATIVA') return 'Ativa'
  if (s === 'EXPIRADA') return 'Expirada'
  return 'Desligada'
}

function variantSituacao(s) {
  if (s === 'ATIVA') return 'ativo'
  if (s === 'EXPIRADA') return 'date'
  return 'bloq'
}

function altura(total) {
  if (!maiorDia.value) return 2
  return Math.max(2, Math.round((total / maiorDia.value) * 100))
}
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-[90] flex justify-end">
    <div class="absolute inset-0 bg-[rgba(9,30,66,.54)]" @click="emit('close')"></div>

    <aside class="relative bg-surface w-full max-w-[720px] h-full overflow-y-auto shadow-sombra-md">
      <header class="sticky top-0 bg-surface border-b border-borda px-6 py-4 flex items-start justify-between gap-4 z-10">
        <div class="min-w-0">
          <div class="text-xl font-semibold text-texto truncate">
            {{ detalhe?.chave?.nome || 'Detalhes da chave' }}
          </div>
          <div v-if="detalhe" class="flex items-center gap-2 mt-1">
            <BaseBadge :variant="variantSituacao(detalhe.chave.situacao)">
              {{ rotuloSituacao(detalhe.chave.situacao) }}
            </BaseBadge>
            <code class="font-mono text-xs text-texto-sub">{{ detalhe.chave.prefixo }}••••</code>
          </div>
        </div>
        <button
          type="button"
          class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt shrink-0"
          title="Fechar"
          @click="emit('close')"
        >
          <X :size="16" />
        </button>
      </header>

      <div class="px-6 py-5">
        <div v-if="carregando" class="text-base text-texto-sub py-8 text-center">Carregando...</div>
        <p v-else-if="erro" class="text-sm text-perigo-text bg-perigo-bg rounded-raio-sm px-3 py-2">{{ erro }}</p>

        <template v-else-if="detalhe">
          <div class="grid grid-cols-2 gap-x-6 gap-y-4 mb-6">
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Dados liberados</div>
              <div class="flex flex-wrap gap-1">
                <BaseBadge v-for="e in detalhe.chave.escopos" :key="e" variant="varchar">
                  {{ ROTULO_ESCOPO[e] || e }}
                </BaseBadge>
              </div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Endereços permitidos</div>
              <div class="text-base text-texto">
                {{ detalhe.chave.ipsPermitidos.length ? detalhe.chave.ipsPermitidos.join(', ') : 'Qualquer endereço' }}
              </div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Criada por</div>
              <div class="text-base text-texto">{{ detalhe.chave.criadoPorNome || '—' }}</div>
              <div class="text-xs text-texto-sub">{{ dataHora(detalhe.chave.criadoEm) }}</div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Validade</div>
              <div class="text-base text-texto">{{ dataCurta(detalhe.chave.expiraEm) }}</div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Último uso</div>
              <div class="text-base text-texto">{{ dataHora(detalhe.chave.ultimoUsoEm) }}</div>
              <div v-if="detalhe.chave.ultimoUsoIp" class="text-xs text-texto-sub">{{ detalhe.chave.ultimoUsoIp }}</div>
            </div>
            <div v-if="detalhe.chave.desligadaEm">
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Desligada</div>
              <div class="text-base text-texto">{{ detalhe.chave.desligadaPorNome || '—' }}</div>
              <div class="text-xs text-texto-sub">{{ dataHora(detalhe.chave.desligadaEm) }}</div>
            </div>
          </div>

          <div class="flex gap-4 mb-5">
            <div class="flex-1 bg-surface-alt border border-borda rounded-raio px-4 py-3">
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub">Chamadas em 30 dias</div>
              <div class="text-2xl font-semibold text-texto tabular-nums">{{ detalhe.totalUltimos30Dias }}</div>
            </div>
            <div class="flex-1 bg-surface-alt border border-borda rounded-raio px-4 py-3">
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub">Recusadas em 30 dias</div>
              <div class="text-2xl font-semibold tabular-nums" :class="detalhe.negadasUltimos30Dias ? 'text-perigo-text' : 'text-texto'">
                {{ detalhe.negadasUltimos30Dias }}
              </div>
            </div>
          </div>

          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Uso nos últimos 30 dias</div>
          <div class="border border-borda rounded-raio p-4 mb-6">
            <div class="flex items-end gap-[3px] h-[110px]">
              <div
                v-for="d in serieCompleta"
                :key="d.dia"
                class="flex-1 flex flex-col justify-end h-full group relative"
                :title="`${d.rotulo}: ${d.total} chamadas, ${d.negadas} recusadas`"
              >
                <div
                  class="w-full rounded-t-sm transition-colors"
                  :class="d.negadas > 0 ? 'bg-perigo-borda' : 'bg-primaria'"
                  :style="{ height: altura(d.total) + '%' }"
                ></div>
              </div>
            </div>
            <div class="flex justify-between text-xs text-texto-fraco mt-2">
              <span>{{ serieCompleta[0]?.rotulo }}</span>
              <span>{{ serieCompleta[serieCompleta.length - 1]?.rotulo }}</span>
            </div>
          </div>

          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Últimas chamadas</div>
          <div v-if="!detalhe.ultimasChamadas.length" class="text-base text-texto-fraco py-6 text-center border border-borda rounded-raio">
            Esta chave ainda não foi usada.
          </div>
          <div v-else class="border border-borda rounded-raio overflow-hidden">
            <div
              v-for="u in detalhe.ultimasChamadas"
              :key="u.id"
              class="flex items-center gap-3 px-4 py-2 border-b border-borda last:border-0 text-sm"
            >
              <span class="text-texto-sub w-[115px] shrink-0">{{ dataHora(u.ocorridoEm) }}</span>
              <code class="font-mono text-xs text-texto flex-1 min-w-0 truncate">{{ u.endpoint }}</code>
              <span class="text-texto-sub w-[110px] shrink-0 truncate">{{ u.ip || '—' }}</span>
              <BaseBadge :variant="u.sucesso ? 'ativo' : 'obrig'">
                {{ u.sucesso ? 'OK' : (MOTIVOS[u.motivoNegacao] || 'Recusada') }}
              </BaseBadge>
            </div>
          </div>

          <div v-if="detalhe.chave.situacao === 'ATIVA'" class="flex gap-2 mt-6 pt-5 border-t border-borda">
            <BaseButton :disabled="girando" @click="girar">
              <RefreshCw :size="14" /> {{ girando ? 'Girando...' : 'Girar chave' }}
            </BaseButton>
            <BaseButton variant="danger" @click="emit('desligar', detalhe.chave)">
              <Power :size="14" /> Desligar chave
            </BaseButton>
          </div>
          <p v-if="detalhe.chave.situacao === 'ATIVA'" class="text-xs text-texto-sub mt-2">
            Girar cria uma chave nova com as mesmas permissões e mantém esta funcionando por mais 7 dias,
            para o sistema externo trocar sem parar.
          </p>
        </template>
      </div>
    </aside>
  </div>
</template>