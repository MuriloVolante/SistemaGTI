<script setup>
import { ref, onMounted } from 'vue'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ConfirmDialog from '@/components/base/ConfirmDialog.vue'
import ExclusaoModal from '@/components/ExclusaoModal.vue'
import TipoFormModal from './TipoFormModal.vue'
import CampoFormModal from './CampoFormModal.vue'
import tiposService from '@/services/tipos'
import { useToast } from '@/composables/useToast'

const { mostrar } = useToast()

const tipos = ref([])
const carregandoTipos = ref(true)
const selecionado = ref(null)

const campos = ref([])
const carregandoCampos = ref(false)

const tipoFormAberto = ref(false)
const tipoEditando = ref(null)

const campoFormAberto = ref(false)

const exclusaoTipo = ref(null)
const confirmCampo = ref(null)

const BADGE = { VARCHAR: 'varchar', INT: 'int', DATE: 'date', BOOLEAN: 'boolean' }

async function listarTipos() {
  carregandoTipos.value = true
  try {
    const { data } = await tiposService.listar()
    tipos.value = data
  } catch {
    mostrar('Erro ao conectar.')
  } finally {
    carregandoTipos.value = false
  }
}

async function selecionar(t) {
  selecionado.value = t
  carregandoCampos.value = true
  try {
    const { data } = await tiposService.campos.listar(t.id)
    campos.value = data
  } catch {
    mostrar('Erro ao carregar campos.')
  } finally {
    carregandoCampos.value = false
  }
}

function abrirCriarTipo() {
  tipoEditando.value = null
  tipoFormAberto.value = true
}

function abrirEditarTipo(t) {
  tipoEditando.value = t
  tipoFormAberto.value = true
}

function aoSalvarTipo() {
  tipoFormAberto.value = false
  listarTipos()
}

function aoSalvarCampo() {
  campoFormAberto.value = false
  selecionar(selecionado.value)
}

function aoExcluirTipo() {
  if (selecionado.value?.id === exclusaoTipo.value.id) {
    selecionado.value = null
    campos.value = []
  }
  exclusaoTipo.value = null
  listarTipos()
}

async function excluirCampo() {
  try {
    await tiposService.campos.excluir(confirmCampo.value.id)
    mostrar('Campo excluído.')
    selecionar(selecionado.value)
  } catch {
    mostrar('Erro ao excluir campo.')
  } finally {
    confirmCampo.value = null
  }
}

onMounted(listarTipos)
</script>

<template>
  <LayoutSidebar>
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-4xl font-semibold text-texto">Tipos de ativo</h1>
        <div class="text-base text-texto-sub mt-1">Crie os tipos e defina os campos de cada um</div>
      </div>
      <BaseButton variant="primary" @click="abrirCriarTipo">+ Novo tipo</BaseButton>
    </div>

    <div class="grid grid-cols-2 gap-5 items-start">
      <BaseCard title="Tipos cadastrados">
        <EmptyState v-if="carregandoTipos" texto="Carregando..." />
        <EmptyState v-else-if="!tipos.length" texto="Nenhum tipo cadastrado." />
        <table v-else class="w-full border-collapse text-base">
          <tbody>
            <tr
              v-for="t in tipos"
              :key="t.id"
              class="border-b border-borda last:border-b-0 cursor-pointer hover:bg-surface-alt"
              :class="selecionado?.id === t.id && 'bg-primaria-light'"
              @click="selecionar(t)"
            >
              <td class="px-4 py-2.5 font-medium text-texto">{{ t.nome }}</td>
              <td class="px-4 py-2.5 text-right">
                <div class="flex gap-1.5 justify-end">
                  <BaseButton size="sm" variant="ghost" @click.stop="abrirEditarTipo(t)">Editar</BaseButton>
                  <BaseButton size="sm" variant="danger" @click.stop="exclusaoTipo = t">Excluir</BaseButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </BaseCard>

      <BaseCard title="Campos do tipo">
        <template #header>
          <BaseButton v-if="selecionado" size="sm" variant="primary" @click="campoFormAberto = true">
            + Adicionar campo
          </BaseButton>
        </template>

        <EmptyState v-if="!selecionado" texto="Selecione um tipo para ver os campos." />
        <EmptyState v-else-if="carregandoCampos" texto="Carregando..." />
        <EmptyState v-else-if="!campos.length" :texto="`Nenhum campo em ${selecionado.nome}. Clique em Adicionar campo.`" />
        <table v-else class="w-full border-collapse text-base">
          <thead>
            <tr>
              <th class="px-4 py-[9px] text-left text-xs font-semibold uppercase tracking-wide text-texto-sub bg-surface-alt border-b border-borda">Nome do campo</th>
              <th class="px-4 py-[9px] text-left text-xs font-semibold uppercase tracking-wide text-texto-sub bg-surface-alt border-b border-borda">Tipo</th>
              <th class="px-4 py-[9px] text-left text-xs font-semibold uppercase tracking-wide text-texto-sub bg-surface-alt border-b border-borda">Obrigatório</th>
              <th class="px-4 py-[9px] text-right text-xs font-semibold uppercase tracking-wide text-texto-sub bg-surface-alt border-b border-borda">Ação</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in campos" :key="c.id" class="border-b border-borda last:border-b-0">
              <td class="px-4 py-2.5 font-medium text-texto">{{ c.nomeDoCampo }}</td>
              <td class="px-4 py-2.5"><BaseBadge :variant="BADGE[c.tipoDado]">{{ c.tipoDado }}</BaseBadge></td>
              <td class="px-4 py-2.5">
                <BaseBadge v-if="c.obrigatorio" variant="obrig">Sim</BaseBadge>
                <span v-else class="text-texto-sub">Não</span>
              </td>
              <td class="px-4 py-2.5 text-right">
                <BaseButton size="sm" variant="danger" @click="confirmCampo = c">Excluir</BaseButton>
              </td>
            </tr>
          </tbody>
        </table>
      </BaseCard>
    </div>

    <TipoFormModal :open="tipoFormAberto" :tipo="tipoEditando" @close="tipoFormAberto = false" @salvo="aoSalvarTipo" />

    <CampoFormModal
      v-if="selecionado"
      :open="campoFormAberto"
      :tipo-id="selecionado.id"
      @close="campoFormAberto = false"
      @salvo="aoSalvarCampo"
    />

    <ExclusaoModal
      v-if="exclusaoTipo"
      :open="!!exclusaoTipo"
      :titulo="`Excluir tipo &quot;${exclusaoTipo.nome}&quot;`"
      :impacto-url="`/tipos-ativo/${exclusaoTipo.id}/impacto-exclusao`"
      :delete-url="`/tipos-ativo/${exclusaoTipo.id}`"
      :palavra-confirmacao="exclusaoTipo.nome"
      @close="exclusaoTipo = null"
      @sucesso="aoExcluirTipo"
    />

    <ConfirmDialog
      v-if="confirmCampo"
      :open="!!confirmCampo"
      titulo="Excluir campo"
      :texto="`Excluir o campo &quot;${confirmCampo.nomeDoCampo}&quot;? Os valores preenchidos nesse campo em todos os ativos serão perdidos.`"
      confirmar-label="Excluir"
      @confirm="excluirCampo"
      @cancel="confirmCampo = null"
    />
  </LayoutSidebar>
</template>