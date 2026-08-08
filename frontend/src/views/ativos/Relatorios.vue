<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Eye } from 'lucide-vue-next'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import UsuarioLabel from '@/components/UsuarioLabel.vue'
import relatoriosService from '@/services/relatorios'
import tiposService from '@/services/tipos'
import { formatValor } from '@/utils/formato'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { mostrar } = useToast()

const BADGE = { ATIVO: 'ativo', ESTOQUE: 'boolean', MANUTENCAO: 'date', DESCARTADO: 'bloq' }
const LABEL = { ATIVO: 'Ativo', ESTOQUE: 'Estoque', MANUTENCAO: 'Manutenção', DESCARTADO: 'Descartado' }

const ativos = ref([])
const carregando = ref(true)
const erro = ref(false)

const tipos = ref([])
const centros = ref([])
const responsaveis = ref([])
const camposDinamicosTipo = ref([])

const fTipo = ref('')
const fStatus = ref('')
const fCentro = ref('')
const fResponsavel = ref('')

const filtros = computed(() => {
  const p = {}
  if (fTipo.value) p.tipo = fTipo.value
  if (fStatus.value) p.status = fStatus.value
  if (fCentro.value) p.centroCusto = fCentro.value
  if (fResponsavel.value) p.responsavel = fResponsavel.value
  return p
})

async function carregarTabela() {
  carregando.value = true
  erro.value = false
  try {
    const { data } = await relatoriosService.ativos(filtros.value)
    ativos.value = data
  } catch {
    erro.value = true
  } finally {
    carregando.value = false
  }
}

async function aoMudarTipo() {
  camposDinamicosTipo.value = []
  const tipo = tipos.value.find((t) => t.nome === fTipo.value)
  if (tipo) {
    try {
      const { data } = await tiposService.campos.listar(tipo.id)
      camposDinamicosTipo.value = data
    } catch {}
  }
  carregarTabela()
}

function limparFiltros() {
  fTipo.value = ''
  fStatus.value = ''
  fCentro.value = ''
  fResponsavel.value = ''
  camposDinamicosTipo.value = []
  carregarTabela()
}

async function exportarCSV() {
  try {
    const { data } = await relatoriosService.exportar(filtros.value)
    const link = document.createElement('a')
    link.href = URL.createObjectURL(data)
    link.download = `ativos_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    mostrar('CSV exportado com sucesso.')
  } catch {
    mostrar('Erro ao exportar CSV.')
  }
}

onMounted(async () => {
  try {
    const { data } = await tiposService.listar()
    tipos.value = data
  } catch {}
  try {
    const { data } = await relatoriosService.ativos()
    centros.value = [...new Set(data.map((a) => a.centroCusto).filter(Boolean))].sort()
    responsaveis.value = [...new Set(data.map((a) => a.responsavel).filter(Boolean))].sort()
  } catch {}
  carregarTabela()
})
</script>

<template>
  <LayoutSidebar>
    <div class="mb-6">
      <h1 class="text-4xl font-semibold text-texto">Relatórios</h1>
      <div class="text-base text-texto-sub mt-1">Visão geral e exportação dos ativos</div>
    </div>

    <div class="flex items-center justify-between mb-4">
      <div class="text-base font-semibold text-texto">Todos os ativos</div>
      <BaseButton size="sm" variant="primary" @click="exportarCSV">↓ Exportar CSV</BaseButton>
    </div>

    <div class="flex items-end gap-3 mb-4 flex-wrap">
      <div>
        <label class="field-label">Tipo</label>
        <select v-model="fTipo" class="field-input" @change="aoMudarTipo">
          <option value="">Todos</option>
          <option v-for="t in tipos" :key="t.id" :value="t.nome">{{ t.nome }}</option>
        </select>
      </div>
      <div>
        <label class="field-label">Status</label>
        <select v-model="fStatus" class="field-input" @change="carregarTabela">
          <option value="">Todos</option>
          <option value="ATIVO">Ativo</option>
          <option value="ESTOQUE">Estoque</option>
          <option value="MANUTENCAO">Manutenção</option>
          <option value="DESCARTADO">Descartado</option>
        </select>
      </div>
      <div>
        <label class="field-label">Centro de custo</label>
        <select v-model="fCentro" class="field-input" @change="carregarTabela">
          <option value="">Todos</option>
          <option v-for="c in centros" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>
      <div>
        <label class="field-label">Responsável</label>
        <select v-model="fResponsavel" class="field-input" @change="carregarTabela">
          <option value="">Todos</option>
          <option v-for="r in responsaveis" :key="r" :value="r">{{ r }}</option>
        </select>
      </div>
      <BaseButton size="sm" @click="limparFiltros">Limpar</BaseButton>
    </div>

    <BaseCard>
      <EmptyState v-if="carregando" texto="Carregando..." />
      <EmptyState v-else-if="erro" texto="Erro ao conectar." />
      <EmptyState v-else-if="!ativos.length" texto="Nenhum ativo encontrado." />
      <div v-else class="overflow-x-auto">
        <table class="w-full border-collapse text-base">
          <thead>
            <tr>
              <th v-for="h in ['Ação','Patrimônio','Tipo','Marca / Modelo','Responsável','Centro de custo','Status','Data compra','Valor (R$)']"
                  :key="h"
                  class="px-4 py-[9px] text-left text-xs font-semibold uppercase tracking-wide text-texto-sub bg-surface-alt border-b border-borda whitespace-nowrap">
                {{ h }}
              </th>
              <th v-for="c in camposDinamicosTipo" :key="c.id"
                  class="px-4 py-[9px] text-left text-xs font-semibold uppercase tracking-wide text-texto-sub bg-surface-alt border-b border-borda whitespace-nowrap">
                {{ c.nomeDoCampo }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in ativos" :key="a.id" class="border-b border-borda last:border-b-0 hover:bg-surface-alt">
              <td class="px-4 py-2.5">
                <BaseButton size="sm" variant="ghost" @click="router.push(`/ativos/${a.id}`)">
                  <Eye :size="14" />
                </BaseButton>
              </td>
              <td class="px-4 py-2.5 font-medium text-texto">{{ a.patrimonio }}</td>
              <td class="px-4 py-2.5">{{ a.tipo || '-' }}</td>
              <td class="px-4 py-2.5">{{ a.marcaModelo }}</td>
              <td class="px-4 py-2.5"><UsuarioLabel :valor="a.responsavel" /></td>
              <td class="px-4 py-2.5">{{ a.centroCusto }}</td>
              <td class="px-4 py-2.5"><BaseBadge :variant="BADGE[a.status]">{{ LABEL[a.status] || a.status }}</BaseBadge></td>
              <td class="px-4 py-2.5">{{ a.dataCompra || '-' }}</td>
              <td class="px-4 py-2.5">{{ a.valorAquisicao ? formatValor(a.valorAquisicao) : '-' }}</td>
              <td v-for="c in camposDinamicosTipo" :key="c.id" class="px-4 py-2.5">
                {{ a.camposDinamicos?.[c.nomeDoCampo] || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </BaseCard>
  </LayoutSidebar>
</template>