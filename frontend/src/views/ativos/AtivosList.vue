<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import AtivoFormModal from './AtivoFormModal.vue'
import ativosService from '@/services/ativos'

const route = useRoute()

const ativos = ref([])
const carregando = ref(true)
const erroCarga = ref(false)

const formAberto = ref(false)
const ativoEditandoId = ref(null)

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
  if (route.query.editar) abrirEditar(Number(route.query.editar))
})
</script>

<template>
  <LayoutSidebar>
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-4xl font-semibold text-texto">Ativos</h1>
        <div class="text-base text-texto-sub mt-1">Gerencie os ativos de TI da empresa</div>
      </div>
      <BaseButton variant="primary" @click="abrirCriar">+ Novo ativo</BaseButton>
    </div>

    <BaseCard>
      <EmptyState v-if="carregando" texto="Carregando..." />
      <EmptyState v-else-if="erroCarga" texto="Erro ao conectar." />
      <EmptyState v-else-if="!ativos.length" texto="Nenhum ativo cadastrado." />
      <BaseTable v-else :colunas="colunas">
        <tr v-for="a in ativos" :key="a.id" class="border-b border-borda last:border-b-0 hover:bg-surface-alt">
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

    <AtivoFormModal
      :open="formAberto"
      :ativo-id="ativoEditandoId"
      @close="formAberto = false"
      @salvo="formAberto = false; listar()"
    />
  </LayoutSidebar>
</template>