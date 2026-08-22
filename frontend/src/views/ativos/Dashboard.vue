<script setup>
import { ref, computed, onMounted } from 'vue'
import { Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS, BarElement, LineElement, PointElement, ArcElement, Filler,
  CategoryScale, LinearScale, Tooltip, Legend
} from 'chart.js'
import { Boxes, Gauge, Wrench, Hourglass, Wallet, Calculator, TrendingDown } from 'lucide-vue-next'

import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import DashCard from '@/components/ativos/DashCard.vue'
import ChartBox from '@/components/ativos/ChartBox.vue'
import DonutChart from '@/components/ativos/DonutChart.vue'

import dashboardService from '@/services/dashboard'
import tiposService from '@/services/tipos'
import relatoriosService from '@/services/relatorios'
import {
  fmt, fmtCompacto, pctFmt, STATUS_LABEL, STATUS_COR, ORDEM_STATUS, DONUT_AZUIS,
  ordenarEntries, somar, alturaBarras, carregarIconesRank,
  centroValor, rankLabels,
  barrasData, barrasOpcoes,
  linhaMensal, opcoesLinhaMensal
} from '@/utils/graficos'

ChartJS.register(BarElement, LineElement, PointElement, ArcElement, Filler, CategoryScale, LinearScale, Tooltip, Legend)

const aba = ref('operacional')
const dados = ref(null)
const erro = ref(false)
const versaoIcones = ref(0)

const tipos = ref([])
const centros = ref([])

const fTipo = ref('')
const fStatus = ref('')
const fCentro = ref('')

const op = computed(() => {
  const d = dados.value
  if (!d) return null
  const st = d.porStatus || {}
  const total = Number(d.total || 0)
  const ativo = Number(st.ATIVO || 0)
  return {
    total,
    ativo,
    manut: Number(st.MANUTENCAO || 0),
    taxa: total > 0 ? (ativo / total) * 100 : 0,
    proximosFimVida: Number(d.proximosFimVida || 0)
  }
})

const fin = computed(() => {
  const d = dados.value
  if (!d) return null
  const custoTotal = Number(d.valorTotal || 0)
  const manut12m = Number(d.custoManutencao12m || 0)
  const total = Number(d.total || 0)
  return {
    custoTotal,
    manut12m,
    depreciado: Number(d.valorTotalDepreciado || 0),
    pct: custoTotal > 0 ? (manut12m / custoTotal) * 100 : 0,
    medio: total > 0 ? custoTotal / total : 0
  }
})

const itensStatus = computed(() =>
  ORDEM_STATUS.map((k) => ({
    label: STATUS_LABEL[k],
    valor: Number(dados.value?.porStatus?.[k] || 0),
    cor: STATUS_COR[k]
  }))
)
const totalStatus = computed(() => itensStatus.value.reduce((a, i) => a + i.valor, 0))

const entriesValorCentro = computed(() => ordenarEntries(dados.value?.valorPorCentroCusto))
const totalValorCentro = computed(() => somar(entriesValorCentro.value))
const itensValorCentro = computed(() =>
  entriesValorCentro.value.map((e, i) => ({
    label: e[0],
    valor: Number(e[1]),
    cor: DONUT_AZUIS[i % DONUT_AZUIS.length]
  }))
)

const entriesTipo = computed(() => ordenarEntries(dados.value?.porTipo))
const entriesCentro = computed(() => ordenarEntries(dados.value?.porCentroCusto))
const entriesValorTipo = computed(() => ordenarEntries(dados.value?.valorPorTipo))

function barras(entries, opts) {
  versaoIcones.value
  const valores = entries.map((e) => Number(e[1]))
  return {
    data: barrasData(entries.map((e) => e[0]), valores, opts),
    options: barrasOpcoes(valores, opts)
  }
}

const grafTipo = computed(() => barras(entriesTipo.value, { ranking: true }))
const grafCentro = computed(() => barras(entriesCentro.value, {}))
const grafValorTipo = computed(() => barras(entriesValorTipo.value, { ranking: true, moeda: true }))

const alturaCentro = computed(() => alturaBarras(entriesCentro.value.length))
const dadosMensal = computed(() => linhaMensal(dados.value?.custoManutencaoMensal))

function tooltipValorCentro(v) {
  return `R$ ${fmt(v)} (${pctFmt(v, totalValorCentro.value)})`
}

async function carregar() {
  erro.value = false
  try {
    const params = {}
    if (fTipo.value) params.tipo = fTipo.value
    if (fStatus.value) params.status = fStatus.value
    if (fCentro.value) params.centroCusto = fCentro.value
    const { data } = await dashboardService.buscar(params)
    dados.value = data
  } catch {
    erro.value = true
  }
}

function limparFiltros() {
  fTipo.value = ''
  fStatus.value = ''
  fCentro.value = ''
  carregar()
}

onMounted(async () => {
  carregarIconesRank(() => versaoIcones.value++)
  try {
    const { data } = await tiposService.listar()
    tipos.value = data
  } catch {}
  try {
    const { data } = await relatoriosService.ativos()
    centros.value = [...new Set(data.map((a) => a.centroCusto).filter(Boolean))].sort()
  } catch {}
  carregar()
})
</script>

<template>
  <LayoutSidebar>
    <div class="mb-6">
      <h1 class="text-4xl font-semibold text-texto">Dashboard</h1>
      <div class="text-base text-texto-sub mt-1">Visão geral dos ativos de TI</div>
    </div>

    <div class="flex items-end gap-3 mb-5 flex-wrap">
      <div class="flex-1 min-w-[140px]">
        <label class="field-label">Tipo</label>
        <select v-model="fTipo" class="field-input" :class="fTipo && 'border-primaria'" @change="carregar">
          <option value="">Todos os tipos</option>
          <option v-for="t in tipos" :key="t.id" :value="t.nome">{{ t.nome }}</option>
        </select>
      </div>
      <div class="flex-1 min-w-[140px]">
        <label class="field-label">Status</label>
        <select v-model="fStatus" class="field-input" :class="fStatus && 'border-primaria'" @change="carregar">
          <option value="">Todos</option>
          <option value="ATIVO">Ativo</option>
          <option value="ESTOQUE">Estoque</option>
          <option value="MANUTENCAO">Manutenção</option>
          <option value="DESCARTADO">Descartado</option>
        </select>
      </div>
      <div class="flex-1 min-w-[140px]">
        <label class="field-label">Centro de custo</label>
        <select v-model="fCentro" class="field-input" :class="fCentro && 'border-primaria'" @change="carregar">
          <option value="">Todos os centros</option>
          <option v-for="c in centros" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>
      <BaseButton size="sm" @click="limparFiltros">Limpar</BaseButton>
    </div>

    <div class="flex gap-1 border-b border-borda mb-5">
      <button
        v-for="t in [{ k: 'operacional', l: 'Operacional' }, { k: 'financeiro', l: 'Financeiro' }]"
        :key="t.k"
        type="button"
        class="px-4 py-2 text-base font-medium bg-transparent cursor-pointer border-x-0 border-t-0 border-b-2 border-solid -mb-px"
        :class="aba === t.k ? 'border-primaria text-primaria-text' : 'border-transparent text-texto-sub hover:text-texto'"
        @click="aba = t.k"
      >
        {{ t.l }}
      </button>
    </div>

    <div v-if="erro" class="bg-surface border border-borda rounded-raio p-6 text-center text-perigo-forte">
      Erro ao carregar.
    </div>

    <template v-else-if="dados">
      <div v-show="aba === 'operacional'">
        <div class="grid grid-cols-4 gap-4 mb-5">
          <DashCard :icone="Boxes" label="Total de ativos" :valor="String(op.total)"
            tip="Quantidade total de ativos, considerando os filtros aplicados." />
          <DashCard :icone="Gauge" label="Taxa de operação"
            :valor="op.taxa.toLocaleString('pt-BR', { maximumFractionDigits: 0 }) + '%'"
            :sub="`${op.ativo} de ${op.total} com status Ativo`"
            tip="Percentual de ativos com status Ativo (em operação) sobre o total de ativos." />
          <DashCard :icone="Wrench" label="Em manutenção" :valor="String(op.manut)" cor="text-aviso-text"
            tip="Ativos atualmente com status Manutenção." />
          <DashCard :icone="Hourglass" label="Próximos do fim da vida útil" :valor="String(op.proximosFimVida)" cor="text-aviso-text"
            tip="Ativos cujo fim da vida útil (data de compra + vida útil em meses) ocorre nos próximos 90 dias." />
        </div>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <ChartBox titulo="Ativos por status" :altura="200"
            tip="Distribuição percentual dos ativos por status. A legenda mostra a quantidade e o percentual de cada status sobre o total.">
            <DonutChart :itens="itensStatus" :total="totalStatus" vazio="Nenhum ativo encontrado." />
          </ChartBox>
          <ChartBox titulo="Ativos por tipo" :altura="200"
            tip="Eixo horizontal: quantidade de ativos. Eixo vertical: tipo. Cada barra conta os ativos daquele tipo. Os 3 maiores recebem medalha de ranking.">
            <Bar :data="grafTipo.data" :options="grafTipo.options" :plugins="[rankLabels]" />
          </ChartBox>
        </div>

        <ChartBox titulo="Ativos por centro de custo" :altura="alturaCentro"
          tip="Eixo horizontal: quantidade de ativos. Eixo vertical: centro de custo. Cada barra conta os ativos daquele centro.">
          <Bar :data="grafCentro.data" :options="grafCentro.options" />
        </ChartBox>
      </div>

      <div v-show="aba === 'financeiro'">
        <div class="grid grid-cols-4 gap-4 mb-5">
          <DashCard :icone="Wallet" label="Custo total" :valor="'R$ ' + fmt(fin.custoTotal)" pequeno
            tip="Soma do valor de aquisição de todos os ativos." />
          <DashCard :icone="Wrench" label="Manutenção 12m" :valor="'R$ ' + fmt(fin.manut12m)" pequeno
            :sub="fin.pct.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + '% do custo total'"
            tip="Soma do custo das manutenções dos últimos 12 meses. O percentual é esse valor dividido pelo custo total." />
          <DashCard :icone="Calculator" label="Valor médio / ativo" :valor="'R$ ' + fmt(fin.medio)" pequeno
            tip="Custo total dividido pela quantidade de ativos." />
          <DashCard :icone="TrendingDown" label="Valor total" :valor="'R$ ' + fmt(fin.depreciado)" pequeno
            tip="Quanto os ativos valem hoje, após depreciação linear (% anual do tipo, capada na vida útil em meses). Ativos sem valor ou sem depreciação configurada no tipo entram pelo valor cheio." />
        </div>

        <div class="grid grid-cols-2 gap-4 mb-4">
          <ChartBox titulo="Valor por centro de custo" :altura="200"
            tip="Distribuição percentual do valor de aquisição dos ativos por centro de custo. A legenda mostra o valor e o percentual de cada centro sobre o total.">
            <DonutChart
              :itens="itensValorCentro"
              :total="totalValorCentro"
              :linhas="centroValor(totalValorCentro)"
              :valor-fmt="fmtCompacto"
              :tooltip-fmt="tooltipValorCentro"
              vazio="Nenhum valor encontrado."
            />
          </ChartBox>
          <ChartBox titulo="Valor por tipo" :altura="200"
            tip="Eixo horizontal: valor em R$. Eixo vertical: tipo de ativo. Cada barra soma o valor de aquisição dos ativos daquele tipo. Os 3 maiores recebem medalha de ranking.">
            <Bar :data="grafValorTipo.data" :options="grafValorTipo.options" :plugins="[rankLabels]" />
          </ChartBox>
        </div>

        <ChartBox titulo="Custo de manutenção mensal (12m)"
          tip="Eixo horizontal: últimos 12 meses. Eixo vertical: valor em R$ gasto em manutenção. Cada ponto soma as manutenções daquele mês.">
          <Line :data="dadosMensal" :options="opcoesLinhaMensal" />
        </ChartBox>
      </div>
    </template>
  </LayoutSidebar>
</template>