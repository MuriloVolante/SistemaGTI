<script setup>
import { ref, computed, watch } from 'vue'
import { X, Power, RefreshCw, Download, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import BaseTable from '@/components/base/BaseTable.vue'
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

const POR_PAGINA = 10
const pagina = ref(1)

const totalChamadas = computed(() => detalhe.value?.ultimasChamadas?.length || 0)
const totalPaginas = computed(() => Math.max(1, Math.ceil(totalChamadas.value / POR_PAGINA)))

const chamadasPagina = computed(() => {
  const inicio = (pagina.value - 1) * POR_PAGINA
  return (detalhe.value?.ultimasChamadas || []).slice(inicio, inicio + POR_PAGINA)
})

const faixaExibida = computed(() => {
  if (!totalChamadas.value) return '0'
  const inicio = (pagina.value - 1) * POR_PAGINA + 1
  const fim = Math.min(pagina.value * POR_PAGINA, totalChamadas.value)
  return `${inicio}-${fim} de ${totalChamadas.value}`
})

watch(() => detalhe.value, () => { pagina.value = 1 })

function resultadoDe(u) {
  return u.sucesso ? 'Autorizada' : (MOTIVOS[u.motivoNegacao] || 'Recusada')
}

function exportarCsv() {
  if (!detalhe.value) return
  const sep = ';'
  const campo = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const linhas = [
    ['Data e hora', 'Endereço chamado', 'Método', 'Origem', 'Resultado', 'Status HTTP']
      .map(campo).join(sep)
  ]
  detalhe.value.ultimasChamadas.forEach((u) => {
    linhas.push([
      dataHora(u.ocorridoEm),
      u.endpoint,
      u.metodo,
      u.ip,
      resultadoDe(u),
      u.statusHttp
    ].map(campo).join(sep))
  })

  const conteudo = '\uFEFF' + linhas.join('\r\n')
  const blob = new Blob([conteudo], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  const nome = (detalhe.value.chave.nome || 'chave').replace(/[^\w\-]+/g, '_')
  a.href = url
  a.download = `chamadas_${nome}_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
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

          <div class="flex items-center justify-between gap-3 mb-2">
            <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub">Últimas chamadas</div>
            <BaseButton
              v-if="detalhe.ultimasChamadas.length"
              size="sm"
              @click="exportarCsv"
            >
              <Download :size="14" /> Exportar CSV
            </BaseButton>
          </div>

          <div v-if="!detalhe.ultimasChamadas.length" class="text-base text-texto-fraco py-6 text-center border border-borda rounded-raio">
            Esta chave ainda não foi usada.
          </div>

          <div v-else class="border border-borda rounded-raio overflow-hidden">
            <BaseTable
              :colunas="[
                { key: 'quando', label: 'Data e hora', width: '140px' },
                { key: 'endpoint', label: 'Endereço chamado' },
                { key: 'ip', label: 'Origem', width: '150px' },
                { key: 'resultado', label: 'Resultado', width: '150px', align: 'right' }
              ]"
            >
              <tr v-for="u in chamadasPagina" :key="u.id" class="border-b border-borda last:border-0">
                <td class="px-4 py-2 text-sm text-texto-sub whitespace-nowrap">{{ dataHora(u.ocorridoEm) }}</td>
                <td class="px-4 py-2">
                  <code class="font-mono text-xs text-texto">{{ u.endpoint }}</code>
                </td>
                <td class="px-4 py-2 text-sm text-texto-sub font-mono text-xs truncate">{{ u.ip || '—' }}</td>
                <td class="px-4 py-2 text-right">
                  <BaseBadge :variant="u.sucesso ? 'ativo' : 'obrig'">{{ resultadoDe(u) }}</BaseBadge>
                </td>
              </tr>
            </BaseTable>

            <div
              v-if="totalPaginas > 1"
              class="flex items-center justify-between px-4 py-2 bg-surface-alt border-t border-borda"
            >
              <span class="text-xs text-texto-sub">{{ faixaExibida }}</span>
              <div class="flex items-center gap-2">
                <BaseButton size="sm" :disabled="pagina === 1" @click="pagina--">
                  <ChevronLeft :size="14" />
                </BaseButton>
                <span class="text-xs text-texto-sub tabular-nums">{{ pagina }} / {{ totalPaginas }}</span>
                <BaseButton size="sm" :disabled="pagina === totalPaginas" @click="pagina++">
                  <ChevronRight :size="14" />
                </BaseButton>
              </div>
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