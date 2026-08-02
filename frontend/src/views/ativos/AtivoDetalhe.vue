<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ExclusaoModal from '@/components/ExclusaoModal.vue'
import UsuarioLabel from '@/components/UsuarioLabel.vue'
import AnexosPanel from '@/components/ativos/AnexosPanel.vue'
import HistoricoPanel from '@/components/ativos/HistoricoPanel.vue'
import ManutencaoFormModal from '@/components/ativos/ManutencaoFormModal.vue'
import { useAuthStore } from '@/stores/auth'
import ativosService from '@/services/ativos'
import chamadosService from '@/services/chamados'
import { formatData, formatValor } from '@/utils/formato'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { mostrar } = useToast()

const ativoId = route.params.id

const ativo = ref(null)
const camposDinamicos = ref({})
const naoEncontrado = ref(false)

const aba = ref('historico')
const chaveHistorico = ref(0)
const chaveAnexos = ref(0)

const manutencoes = ref([])
const manutencoesCarregadas = ref(false)
const chamados = ref([])
const chamadosCarregados = ref(false)

const manutencaoAberta = ref(false)
const exclusaoAberta = ref(false)

const BADGE_ATIVO = { ATIVO: 'ativo', ESTOQUE: 'boolean', MANUTENCAO: 'date', DESCARTADO: 'bloq' }
const LABEL_ATIVO = { ATIVO: 'Ativo', ESTOQUE: 'Estoque', MANUTENCAO: 'Manutenção', DESCARTADO: 'Descartado' }
const BADGE_CHAMADO = { ABERTO: 'bloq', EM_ANDAMENTO: 'date', CONCLUIDO: 'ativo' }
const LABEL_CHAMADO = { ABERTO: 'Aberto', EM_ANDAMENTO: 'Em andamento', CONCLUIDO: 'Concluído' }

const ABAS = [
  { key: 'historico', label: 'Histórico de Responsável' },
  { key: 'manutencoes', label: 'Manutenções' },
  { key: 'chamados', label: 'Chamados Vinculados' },
  { key: 'anexos', label: 'Anexos' }
]

const nomeTipo = computed(() => {
  const t = ativo.value?.tipo
  return t?.nome || (typeof t === 'string' ? t : '-')
})

const nomeResponsavel = computed(() => {
  const r = ativo.value?.responsavel
  return r?.nomeCompleto || r?.nomeUsuario || (typeof r === 'string' ? r : '-')
})

const infoCells = computed(() => {
  const a = ativo.value
  if (!a) return []
  const fixos = [
    { label: 'Patrimônio', value: a.patrimonio },
    { label: 'Tipo', value: nomeTipo.value },
    { label: 'Marca / Modelo', value: a.marcaModelo },
    { label: 'Responsável', value: nomeResponsavel.value },
    { label: 'Centro de custo', value: a.centroCusto },
    { label: 'Status', value: a.status },
    { label: 'Data de compra', value: formatData(a.dataCompra) },
    { label: 'Garantia até', value: formatData(a.garantia ?? a.garantiaAte) },
    { label: 'Valor de aquis.', value: formatValor(a.valorAquisicao) },
    { label: 'Cadastrado em', value: formatData(a.cadastradoEm) }
  ]
  const dinamicos = Object.entries(camposDinamicos.value).map(([k, v]) => ({ label: k, value: v || '-' }))
  return [...fixos, ...dinamicos]
})

async function carregarAtivo() {
  try {
    const { data } = await ativosService.buscar(ativoId)
    ativo.value = data
    try {
      const { data: valores } = await ativosService.valores(ativoId)
      const mapa = {}
      valores.forEach((v) => { mapa[v.campo.nomeDoCampo] = v.valor })
      camposDinamicos.value = mapa
    } catch {
      camposDinamicos.value = {}
    }
  } catch {
    naoEncontrado.value = true
  }
}

async function carregarManutencoes() {
  try {
    const { data } = await ativosService.manutencoes.listar(ativoId)
    manutencoes.value = data
  } catch {
    mostrar('Erro ao carregar manutenções.')
  } finally {
    manutencoesCarregadas.value = true
  }
}

async function carregarChamados() {
  try {
    const { data } = await chamadosService.porAtivo(ativoId)
    chamados.value = data
  } catch {
    mostrar('Erro ao carregar chamados.')
  } finally {
    chamadosCarregados.value = true
  }
}

function mudarAba(key) {
  aba.value = key
  if (key === 'manutencoes' && !manutencoesCarregadas.value) carregarManutencoes()
  if (key === 'chamados' && !chamadosCarregados.value) carregarChamados()
}

async function aoRegistrarManutencao() {
  manutencaoAberta.value = false
  await carregarAtivo()
  manutencoesCarregadas.value = false
  if (aba.value === 'manutencoes') carregarManutencoes()
}

onMounted(carregarAtivo)
</script>

<template>
  <LayoutSidebar>
    <div v-if="naoEncontrado" class="text-4xl font-semibold text-texto">Ativo não encontrado</div>

    <template v-else-if="ativo">
      <div class="flex items-start justify-between mb-6 gap-4">
        <div class="flex items-center gap-4">
          <BaseButton size="sm" @click="router.back()">← Voltar</BaseButton>
          <div>
            <div class="text-4xl font-semibold text-texto">{{ ativo.marcaModelo || '-' }}</div>
            <div class="text-base text-texto-sub mt-1">Patrimônio: {{ ativo.patrimonio }}</div>
          </div>
        </div>
        <div class="flex gap-2 flex-shrink-0">
          <BaseButton size="sm" @click="router.push({ path: '/ativos', query: { editar: ativoId } })">Editar</BaseButton>
          <BaseButton size="sm" variant="primary" @click="manutencaoAberta = true">Manutenção</BaseButton>
          <BaseButton v-if="auth.isTI" size="sm" variant="danger" @click="exclusaoAberta = true">Excluir</BaseButton>
        </div>
      </div>

      <div class="grid grid-cols-4 gap-4 mb-6">
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Tipo de ativo</div>
          <div class="text-lg font-semibold text-texto">{{ nomeTipo }}</div>
        </div>
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Status</div>
          <BaseBadge :variant="BADGE_ATIVO[ativo.status]">{{ LABEL_ATIVO[ativo.status] || ativo.status }}</BaseBadge>
        </div>
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Responsável</div>
          <div class="text-lg font-semibold text-texto">
            <UsuarioLabel :valor="ativo.responsavel" />
          </div>
        </div>
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Centro de custo</div>
          <div class="text-lg font-semibold text-texto">{{ ativo.centroCusto || '-' }}</div>
        </div>
      </div>

      <div class="bg-surface border border-borda rounded-raio shadow-sombra p-5 mb-6">
        <div class="text-base font-semibold text-texto mb-4">Informações do ativo</div>
        <div class="grid grid-cols-4 gap-x-6 gap-y-4">
          <div v-for="(c, i) in infoCells" :key="i">
            <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">{{ c.label }}</div>
            <div class="text-base text-texto break-words">{{ c.value ?? '-' }}</div>
          </div>
        </div>
      </div>

      <div class="bg-surface border border-borda rounded-raio shadow-sombra overflow-hidden">
        <div class="flex border-b border-borda bg-surface-alt">
          <button
            v-for="t in ABAS"
            :key="t.key"
            type="button"
            class="px-4 py-3 text-base font-medium border-none bg-transparent cursor-pointer border-b-2 -mb-px"
            :class="aba === t.key ? 'border-primaria text-primaria-text bg-surface' : 'border-transparent text-texto-sub hover:text-texto'"
            @click="mudarAba(t.key)"
          >
            {{ t.label }}
          </button>
        </div>

        <HistoricoPanel v-if="aba === 'historico'" :key="chaveHistorico" :ativo-id="ativoId" />

        <div v-else-if="aba === 'manutencoes'" class="p-5">
          <EmptyState v-if="!manutencoesCarregadas" texto="Carregando..." />
          <EmptyState v-else-if="!manutencoes.length" texto="Nenhuma manutenção registrada." />
          <table v-else class="w-full border-collapse text-base">
            <thead>
              <tr>
                <th v-for="h in ['Data','Tipo','Descrição','Técnico','Custo','Garantia']" :key="h"
                    class="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-texto-sub border-b border-borda">
                  {{ h }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(m, i) in manutencoes" :key="i" class="border-b border-borda last:border-b-0">
                <td class="px-3 py-2.5">{{ formatData(m.dataManutencao) }}</td>
                <td class="px-3 py-2.5">{{ m.tipo || '-' }}</td>
                <td class="px-3 py-2.5">{{ m.descricao }}</td>
                <td class="px-3 py-2.5">{{ m.tecnico || '-' }}</td>
                <td class="px-3 py-2.5">{{ m.custo != null ? formatValor(m.custo) : '-' }}</td>
                <td class="px-3 py-2.5">
                  <BaseBadge :variant="m.garantia ? 'ativo' : 'bloq'">{{ m.garantia ? 'Sim' : 'Não' }}</BaseBadge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else-if="aba === 'chamados'" class="p-5">
          <EmptyState v-if="!chamadosCarregados" texto="Carregando..." />
          <EmptyState v-else-if="!chamados.length" texto="Nenhum chamado vinculado." />
          <table v-else class="w-full border-collapse text-base">
            <thead>
              <tr>
                <th v-for="h in ['ID','Título','Descrição','Solicitante','Status','Data']" :key="h"
                    class="px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-texto-sub border-b border-borda">
                  {{ h }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in chamados" :key="c.id" class="border-b border-borda last:border-b-0">
                <td class="px-3 py-2.5 font-semibold text-texto">#{{ c.id }}</td>
                <td class="px-3 py-2.5">{{ c.titulo }}</td>
                <td class="px-3 py-2.5 max-w-[260px] truncate" :title="c.descricao">{{ c.descricao }}</td>
                <td class="px-3 py-2.5"><UsuarioLabel :valor="c.solicitante" /></td>
                <td class="px-3 py-2.5">
                  <BaseBadge :variant="BADGE_CHAMADO[c.status]">{{ LABEL_CHAMADO[c.status] || c.status }}</BaseBadge>
                </td>
                <td class="px-3 py-2.5">{{ formatData(c.dataAbertura) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <AnexosPanel v-else-if="aba === 'anexos'" :key="chaveAnexos" :ativo-id="ativoId" />
      </div>

      <ManutencaoFormModal
        :open="manutencaoAberta"
        :ativo-id="ativoId"
        @close="manutencaoAberta = false"
        @salvo="aoRegistrarManutencao"
      />

      <ExclusaoModal
        :open="exclusaoAberta"
        :titulo="`Excluir ativo &quot;${ativo.patrimonio}&quot;`"
        :impacto-url="`/ativos/${ativoId}/impacto-exclusao`"
        :delete-url="`/ativos/${ativoId}`"
        :palavra-confirmacao="ativo.patrimonio"
        @close="exclusaoAberta = false"
        @sucesso="router.push('/ativos')"
      />
    </template>

    <EmptyState v-else texto="Carregando..." />
  </LayoutSidebar>
</template>