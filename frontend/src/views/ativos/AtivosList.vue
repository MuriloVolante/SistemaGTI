<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import AtivoFormModal from './AtivoFormModal.vue'
import ImpressaoQrModal from '@/components/ativos/ImpressaoQrModal.vue'
import ativosService from '@/services/ativos'
import tiposService from '@/services/tipos'
import { useToast } from '@/composables/useToast'
import QRCode from 'qrcode'

const route = useRoute()
const POR_PAGINA = 20

const ativos = ref([])
const tipos = ref([])
const carregando = ref(true)
const erroCarga = ref(false)
const pagina = ref(1)

const fBusca = ref('')
const fTipo = ref('')
const fStatus = ref('')
const fCentroCusto = ref('')

const formAberto = ref(false)
const ativoEditandoId = ref(null)

const impressaoAberta = ref(false)
const itensImpressao = ref([])
const { mostrar } = useToast()

const MAX_ETIQUETAS = 300
const gerandoEtiquetas = ref(false)

const centrosCusto = computed(() => [...new Set(ativos.value.map((a) => a.centroCusto).filter(Boolean))].sort())

const filtrados = computed(() => {
  const busca = fBusca.value.toLowerCase().trim()
  return ativos.value.filter((a) => {
    if (fTipo.value && a.tipo?.id !== Number(fTipo.value)) return false
    if (fStatus.value && a.status !== fStatus.value) return false
    if (fCentroCusto.value && a.centroCusto !== fCentroCusto.value) return false
    if (busca) {
      const patrimonio = (a.patrimonio || '').toLowerCase()
      const marca = (a.marcaModelo || '').toLowerCase()
      const resp = (a.responsavel?.nomeCompleto || '').toLowerCase()
      if (!patrimonio.includes(busca) && !marca.includes(busca) && !resp.includes(busca)) return false
    }
    return true
  })
})

const totalPaginas = computed(() => Math.max(1, Math.ceil(filtrados.value.length / POR_PAGINA)))
const visiveis = computed(() => {
  const p = Math.min(pagina.value, totalPaginas.value)
  return filtrados.value.slice((p - 1) * POR_PAGINA, p * POR_PAGINA)
})

function limparFiltros() {
  fBusca.value = ''
  fTipo.value = ''
  fStatus.value = ''
   fCentroCusto.value = ''
   pagina.value = 1
}

function filtrarParaImpressao({ ativoId, tipoId, centroCusto, incluirDescartados }) {
  if (ativoId !== null && ativoId !== undefined) {
    const a = ativos.value.find((x) => x.id === ativoId)
    return a ? [a] : []
  }
  return ativos.value.filter((a) => {
    if (!incluirDescartados && a.status === 'DESCARTADO') return false
    if (tipoId && a.tipo?.id !== tipoId) return false
    if (centroCusto && a.centroCusto !== centroCusto) return false
    return true
  })
}

async function gerarItensImpressao(selecionados) {
  const itens = []
  for (const a of selecionados) {
    const url = `${window.location.origin}/consulta?patrimonio=${encodeURIComponent(a.patrimonio)}`
    const qr = await QRCode.toDataURL(url, { width: 160, margin: 1 })
    itens.push({ patrimonio: a.patrimonio, marcaModelo: a.marcaModelo, qr })
  }
  itensImpressao.value = itens
}

async function aoGerarImpressao(filtros) {
  if (gerandoEtiquetas.value) return

  const selecionados = filtrarParaImpressao(filtros)
  if (!selecionados.length) { mostrar('Nenhum ativo encontrado com esse filtro.'); return }
  if (selecionados.length > MAX_ETIQUETAS) {
    mostrar(`${selecionados.length} etiquetas excedem o limite de ${MAX_ETIQUETAS}. Refine o filtro por tipo ou centro de custo.`)
    return
  }

  gerandoEtiquetas.value = true
  mostrar(`Gerando ${selecionados.length} etiqueta(s)...`)
  try {
    await gerarItensImpressao(selecionados)
    await nextTick()
    window.print()
  } catch {
    mostrar('Erro ao gerar as etiquetas.')
  } finally {
    gerandoEtiquetas.value = false
  }
}

const colunas = [
  { key: 'patrimonio', label: 'Patrimônio', width: '12%' },
  { key: 'tipo', label: 'Tipo', width: '15%' },
  { key: 'marca', label: 'Marca / Modelo', width: '20%' },
  { key: 'resp', label: 'Responsável', width: '15%' },
  { key: 'centro', label: 'Centro de custo', width: '13%' },
  { key: 'status', label: 'Status', width: '10%' },
  { key: 'acoes', label: 'Ações', width: '15%', align: 'right' }
]

const BADGE = { ATIVO: 'ativo', ESTOQUE: 'boolean', MANUTENCAO: 'date', DESCARTADO: 'bloq' }
const LABEL = { ATIVO: 'Ativo', ESTOQUE: 'Estoque', MANUTENCAO: 'Manutenção', DESCARTADO: 'Descartado' }

async function listar() {
  carregando.value = true
  erroCarga.value = false
  try {
    const { data } = await ativosService.listar()
    ativos.value = data
  } catch {
    erroCarga.value = true
  } finally {
    carregando.value = false
  }
}

function abrirCriar() {
  ativoEditandoId.value = null
  formAberto.value = true
}

function abrirEditar(id) {
  ativoEditandoId.value = id
  formAberto.value = true
}

onMounted(async () => {
  await listar()
  try {
    const { data } = await tiposService.listar()
    tipos.value = data
  } catch {}
  if (route.query.editar) abrirEditar(Number(route.query.editar))
})
</script>

<template>
  <LayoutSidebar class="print:hidden">
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-4xl font-semibold text-texto">Ativos</h1>
        <div class="text-base text-texto-sub mt-1">Gerencie os ativos de TI da empresa</div>
      </div>
      <div class="flex gap-2">
        <BaseButton @click="impressaoAberta = true">Imprimir QR Codes</BaseButton>
        <BaseButton variant="primary" @click="abrirCriar">+ Novo ativo</BaseButton>
      </div>
    </div>

    <div class="flex items-end gap-3 mb-4 flex-wrap">
      <div class="flex-1 min-w-[220px]">
        <label class="field-label">Buscar</label>
        <input v-model="fBusca" class="field-input" placeholder="Patrimônio, marca/modelo, responsável..." @input="pagina = 1" />
      </div>
      <div class="flex-1 min-w-[140px]">
        <label class="field-label">Tipo</label>
        <select v-model="fTipo" class="field-input" @change="pagina = 1">
          <option value="">Todos</option>
          <option v-for="t in tipos" :key="t.id" :value="String(t.id)">{{ t.nome }}</option>
        </select>
      </div>
      <div class="flex-1 min-w-[140px]">
        <label class="field-label">Status</label>
        <select v-model="fStatus" class="field-input" @change="pagina = 1">
          <option value="">Todos</option>
          <option v-for="(label, key) in LABEL" :key="key" :value="key">{{ label }}</option>
        </select>
      </div>
      <div class="flex-1 min-w-[160px]">
        <label class="field-label">Centro de custo</label>
        <select v-model="fCentroCusto" class="field-input" @change="pagina = 1">
          <option value="">Todos</option>
          <option v-for="c in centrosCusto" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>
      <BaseButton size="sm" @click="limparFiltros">Limpar</BaseButton>
    </div>

    <BaseCard>
      <EmptyState v-if="carregando" texto="Carregando..." />
      <EmptyState v-else-if="erroCarga" texto="Erro ao conectar." />
      <EmptyState v-else-if="!filtrados.length" texto="Nenhum ativo cadastrado." />
      <BaseTable v-else :colunas="colunas">
        <tr v-for="a in visiveis" :key="a.id" class="border-b border-borda last:border-b-0 hover:bg-surface-alt">
          <td class="px-4 py-2.5 font-medium text-texto">{{ a.patrimonio }}</td>
          <td class="px-4 py-2.5">{{ a.tipo?.nome || '-' }}</td>
          <td class="px-4 py-2.5">{{ a.marcaModelo }}</td>
          <td class="px-4 py-2.5">{{ a.responsavel?.nomeCompleto || '-' }}</td>
          <td class="px-4 py-2.5">{{ a.centroCusto }}</td>
          <td class="px-4 py-2.5"><BaseBadge :variant="BADGE[a.status]">{{ LABEL[a.status] || a.status }}</BaseBadge></td>
          <td class="px-4 py-2.5 text-right">
            <div class="flex gap-1.5 justify-end">
              <BaseButton size="sm" variant="ghost" @click="$router.push(`/ativos/${a.id}`)">Detalhes</BaseButton>
              <BaseButton size="sm" variant="ghost" @click="abrirEditar(a.id)">Editar</BaseButton>
            </div>
          </td>
        </tr>
      </BaseTable>
    </BaseCard>

    <div v-if="filtrados.length" class="flex items-center gap-3 mt-4">
      <BaseButton size="sm" :disabled="pagina <= 1" @click="pagina--">← Anterior</BaseButton>
      <span class="text-base text-texto-sub">
        {{ (pagina - 1) * POR_PAGINA + 1 }}–{{ Math.min(pagina * POR_PAGINA, filtrados.length) }}
        de {{ filtrados.length }} · pág. {{ pagina }}/{{ totalPaginas }}
      </span>
      <BaseButton size="sm" :disabled="pagina >= totalPaginas" @click="pagina++">Próxima →</BaseButton>
    </div>

    <AtivoFormModal
      :open="formAberto"
      :ativo-id="ativoEditandoId"
      @close="formAberto = false"
      @salvo="formAberto = false; listar()"
    />

    <ImpressaoQrModal
      :open="impressaoAberta"
      @close="impressaoAberta = false"
      @gerar="aoGerarImpressao"
    />
  </LayoutSidebar>

  <div v-if="itensImpressao.length" class="hidden print:block">
    <div class="grid grid-cols-3 gap-4">
      <div v-for="(item, i) in itensImpressao" :key="i" class="flex flex-col items-center border border-black p-2 break-inside-avoid">
        <img :src="item.qr" class="w-24 h-24" />
        <div class="text-xs font-semibold mt-1">{{ item.patrimonio }}</div>
        <div class="text-[10px] text-center">{{ item.marcaModelo }}</div>
      </div>
    </div>
  </div>
</template>

<style>
@media print {
  @page { size: A4; margin: 10mm; }
}
</style>