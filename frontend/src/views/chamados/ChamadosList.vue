<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Eye } from 'lucide-vue-next'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import UsuarioLabel from '@/components/UsuarioLabel.vue'
import chamadosService from '@/services/chamados'
import usuariosService from '@/services/usuarios'
import { STATUS_BADGE, STATUS_LABEL, PRIORIDADE_BADGE, PRIORIDADE_LABEL, formatDataHora } from '@/utils/chamados'

const router = useRouter()

const todos = ref([])
const carregando = ref(true)
const erro = ref(false)
const tecnicos = ref([])

const fBusca = ref('')
const fStatus = ref('')
const fPrioridade = ref('')
const fTecnico = ref('')
const fDataIni = ref('')
const fDataFim = ref('')

let timer = null

const colunas = [
  { key: 'acao', label: 'Ação', width: '60px' },
  { key: 'id', label: 'ID', width: '60px' },
  { key: 'titulo', label: 'Título' },
  { key: 'solicitante', label: 'Solicitante' },
  { key: 'tecnico', label: 'Técnico' },
  { key: 'prioridade', label: 'Prioridade' },
  { key: 'status', label: 'Status' },
  { key: 'aberto', label: 'Aberto em' }
]

const filtrados = computed(() => {
  let lista = todos.value
  const busca = fBusca.value.toLowerCase().trim()

  if (fStatus.value) lista = lista.filter((c) => c.status === fStatus.value)
  if (fPrioridade.value) lista = lista.filter((c) => c.prioridade === fPrioridade.value)

  if (fTecnico.value === 'SEM_TECNICO') lista = lista.filter((c) => !c.tecnico)
  else if (fTecnico.value) lista = lista.filter((c) => c.tecnico && String(c.tecnico.id) === fTecnico.value)

  if (busca) {
    lista = lista.filter((c) => {
      const sol = (c.solicitante?.nomeCompleto || c.solicitante?.nomeUsuario || '').toLowerCase()
      return (c.titulo || '').toLowerCase().includes(busca) || sol.includes(busca) || String(c.id) === busca
    })
  }

  if (fDataIni.value) lista = lista.filter((c) => c.dataAbertura && c.dataAbertura.substring(0, 10) >= fDataIni.value)
  if (fDataFim.value) lista = lista.filter((c) => c.dataAbertura && c.dataAbertura.substring(0, 10) <= fDataFim.value)

  return lista
})

const cards = computed(() => ({
  total: filtrados.value.length,
  semAtend: filtrados.value.filter((c) => c.status === 'ABERTO').length,
  emAtend: filtrados.value.filter((c) => c.status === 'EM_ANDAMENTO').length,
  concluidos: filtrados.value.filter((c) => c.status === 'CONCLUIDO').length
}))

async function carregar() {
  try {
    const { data } = await chamadosService.listar()
    todos.value = data
    erro.value = false
  } catch {
    erro.value = true
  } finally {
    carregando.value = false
  }
}

function limparFiltros() {
  fBusca.value = ''
  fStatus.value = ''
  fPrioridade.value = ''
  fTecnico.value = ''
  fDataIni.value = ''
  fDataFim.value = ''
}

onMounted(async () => {
  try {
    const { data } = await usuariosService.listar()
    tecnicos.value = data.filter((u) => u.tipoAcesso === 'TI')
  } catch {}
  await carregar()
  timer = setInterval(carregar, 15000)
})

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <LayoutSidebar>
    <div class="mb-6">
      <h1 class="text-4xl font-semibold text-texto">Chamados</h1>
      <div class="text-base text-texto-sub mt-1">Visão geral e gerenciamento dos chamados</div>
    </div>

    <div class="grid grid-cols-4 gap-4 mb-5">
      <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
        <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Total de chamados</div>
        <div class="text-4xl font-semibold text-texto">{{ cards.total }}</div>
      </div>
      <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
        <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Sem atendimento</div>
        <div class="text-4xl font-semibold text-perigo-text">{{ cards.semAtend }}</div>
      </div>
      <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
        <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Em atendimento</div>
        <div class="text-4xl font-semibold text-aviso-text">{{ cards.emAtend }}</div>
      </div>
      <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
        <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Atendidos</div>
        <div class="text-4xl font-semibold text-sucesso-text">{{ cards.concluidos }}</div>
      </div>
    </div>

    <div class="flex items-end gap-3 mb-4 flex-wrap">
      <div class="min-w-[200px]">
        <label class="field-label">Buscar</label>
        <input v-model="fBusca" class="field-input" placeholder="Título, solicitante..." />
      </div>
      <div>
        <label class="field-label">Status</label>
        <select v-model="fStatus" class="field-input">
          <option value="">Todos</option>
          <option value="ABERTO">Aberto</option>
          <option value="EM_ANDAMENTO">Em andamento</option>
          <option value="CONCLUIDO">Concluído</option>
        </select>
      </div>
      <div>
        <label class="field-label">Prioridade</label>
        <select v-model="fPrioridade" class="field-input">
          <option value="">Todas</option>
          <option value="MUITO_ALTA">Muito alta</option>
          <option value="ALTA">Alta</option>
          <option value="MEDIA">Média</option>
          <option value="BAIXA">Baixa</option>
        </select>
      </div>
      <div>
        <label class="field-label">Técnico</label>
        <select v-model="fTecnico" class="field-input">
          <option value="">Todos</option>
          <option value="SEM_TECNICO">Sem técnico atribuído</option>
          <option v-for="t in tecnicos" :key="t.id" :value="String(t.id)">
            {{ t.nomeCompleto || t.nomeUsuario }}
          </option>
        </select>
      </div>
      <div>
        <label class="field-label">Aberto de</label>
        <input v-model="fDataIni" type="date" class="field-input" />
      </div>
      <div>
        <label class="field-label">Aberto até</label>
        <input v-model="fDataFim" type="date" class="field-input" />
      </div>
      <BaseButton size="sm" @click="limparFiltros">Limpar</BaseButton>
    </div>

    <BaseCard>
      <EmptyState v-if="carregando" texto="Carregando..." />
      <EmptyState v-else-if="erro" texto="Erro ao conectar." />
      <EmptyState v-else-if="!filtrados.length" texto="Nenhum chamado encontrado." />
      <BaseTable v-else :colunas="colunas">
        <tr v-for="c in filtrados" :key="c.id" class="border-b border-borda last:border-b-0 hover:bg-surface-alt">
          <td class="px-4 py-2.5">
            <BaseButton size="sm" variant="ghost" @click="router.push(`/chamados/${c.id}`)">
              <Eye :size="14" />
            </BaseButton>
          </td>
          <td class="px-4 py-2.5 font-semibold text-texto">#{{ c.id }}</td>
          <td class="px-4 py-2.5">{{ c.titulo }}</td>
          <td class="px-4 py-2.5"><UsuarioLabel :valor="c.solicitante" /></td>
          <td class="px-4 py-2.5">
            <UsuarioLabel v-if="c.tecnico" :valor="c.tecnico" />
            <span v-else class="text-texto-fraco">-</span>
          </td>
          <td class="px-4 py-2.5">
            <BaseBadge :variant="PRIORIDADE_BADGE[c.prioridade]">{{ PRIORIDADE_LABEL[c.prioridade] || c.prioridade || '-' }}</BaseBadge>
          </td>
          <td class="px-4 py-2.5">
            <BaseBadge :variant="STATUS_BADGE[c.status]">{{ STATUS_LABEL[c.status] || c.status }}</BaseBadge>
          </td>
          <td class="px-4 py-2.5 whitespace-nowrap">{{ formatDataHora(c.dataAbertura) }}</td>
        </tr>
      </BaseTable>
    </BaseCard>
  </LayoutSidebar>
</template>