<script setup>
import { ref, computed, onMounted } from 'vue'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import BaseAvatar from '@/components/base/BaseAvatar.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ConfirmDialog from '@/components/base/ConfirmDialog.vue'
import UsuarioFormModal from './UsuarioFormModal.vue'
import usuariosService from '@/services/usuarios'
import { useToast } from '@/composables/useToast'

const { mostrar } = useToast()
const POR_PAGINA = 20

const todos = ref([])
const carregando = ref(true)
const erroCarga = ref(false)
const pagina = ref(1)

const fBusca = ref('')
const fAcesso = ref('')
const fStatus = ref('')

const formAberto = ref(false)
const usuarioEditando = ref(null)

const confirmAberto = ref(false)
const alvoToggle = ref(null)

const colunas = [
  { key: 'usuario', label: 'Usuário' },
  { key: 'nome', label: 'Nome completo' },
  { key: 'desc', label: 'Descrição' },
  { key: 'acesso', label: 'Acesso' },
  { key: 'status', label: 'Status' },
  { key: 'acoes', label: 'Ações', width: '160px' }
]

const filtrados = computed(() => {
  const busca = fBusca.value.toLowerCase().trim()
  return todos.value
    .filter((u) => {
      if (fAcesso.value && (u.tipoAcesso || 'COMUM') !== fAcesso.value) return false
      if (fStatus.value === 'ATIVO' && u.bloqueado) return false
      if (fStatus.value === 'INATIVO' && !u.bloqueado) return false
      if (busca) {
        const nu = (u.nomeUsuario || '').toLowerCase()
        const nc = (u.nomeCompleto || '').toLowerCase()
        if (!nu.includes(busca) && !nc.includes(busca)) return false
      }
      return true
    })
    .sort((a, b) => (a.bloqueado ? 1 : 0) - (b.bloqueado ? 1 : 0))
})

const totalPaginas = computed(() => Math.max(1, Math.ceil(filtrados.value.length / POR_PAGINA)))
const visiveis = computed(() => {
  const p = Math.min(pagina.value, totalPaginas.value)
  return filtrados.value.slice((p - 1) * POR_PAGINA, p * POR_PAGINA)
})

async function listar() {
  carregando.value = true
  erroCarga.value = false
  try {
    const { data } = await usuariosService.listar()
    todos.value = data
  } catch {
    erroCarga.value = true
  } finally {
    carregando.value = false
  }
}

function limparFiltros() {
  fBusca.value = ''
  fAcesso.value = ''
  fStatus.value = ''
  pagina.value = 1
}

function abrirCriar() {
  usuarioEditando.value = null
  formAberto.value = true
}

async function abrirEditar(id) {
  try {
    const { data } = await usuariosService.buscar(id)
    usuarioEditando.value = data
    formAberto.value = true
  } catch {
    mostrar('Erro ao carregar usuário.')
  }
}

function pedirToggle(u, desativar) {
  alvoToggle.value = { id: u.id, nome: u.nomeUsuario, desativar }
  confirmAberto.value = true
}

async function aplicarToggle() {
  const { id, desativar } = alvoToggle.value
  try {
    await usuariosService.bloqueio(id, desativar)
    mostrar(desativar ? 'Usuário desativado.' : 'Usuário reativado.')
    listar()
  } catch (e) {
    mostrar(e.response?.data?.erro || 'Erro ao alterar status.')
  } finally {
    confirmAberto.value = false
  }
}

onMounted(listar)
</script>

<template>
  <LayoutSidebar>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-4xl font-semibold text-texto">Gestão de usuários</h1>
      <BaseButton variant="primary" @click="abrirCriar">+ Novo usuário</BaseButton>
    </div>

    <div class="flex items-end gap-3 mb-4 flex-wrap">
      <div class="flex-1 min-w-[220px]">
        <label class="field-label">Buscar</label>
        <input v-model="fBusca" class="field-input" placeholder="Usuário, nome completo..." @input="pagina = 1" />
      </div>
      <div class="flex-1 min-w-[140px]">
        <label class="field-label">Acesso</label>
        <select v-model="fAcesso" class="field-input" @change="pagina = 1">
          <option value="">Todos</option>
          <option value="COMUM">Comum</option>
          <option value="TI">TI</option>
        </select>
      </div>
      <div class="flex-1 min-w-[140px]">
        <label class="field-label">Status</label>
        <select v-model="fStatus" class="field-input" @change="pagina = 1">
          <option value="">Todos</option>
          <option value="ATIVO">Ativo</option>
          <option value="INATIVO">Inativo</option>
        </select>
      </div>
      <BaseButton size="sm" @click="limparFiltros">Limpar</BaseButton>
    </div>

    <BaseCard>
      <EmptyState v-if="carregando" texto="Carregando..." />
      <EmptyState v-else-if="erroCarga" texto="Erro ao conectar." />
      <EmptyState v-else-if="!visiveis.length" texto="Nenhum usuário cadastrado." />
      <BaseTable v-else :colunas="colunas">
        <tr v-for="u in visiveis" :key="u.id" class="border-b border-borda last:border-b-0 hover:bg-surface-alt">
          <td class="px-4 py-2.5">
            <div class="flex items-center gap-2.5">
              <BaseAvatar :nome="u.nomeCompleto || u.nomeUsuario" />
              <div>
                <div class="font-medium text-texto">{{ u.nomeUsuario }}</div>
                <div class="text-sm text-texto-sub">{{ u.email || '' }}</div>
              </div>
            </div>
          </td>
          <td class="px-4 py-2.5">{{ u.nomeCompleto }}</td>
          <td class="px-4 py-2.5 text-texto-sub">{{ u.descricao || '-' }}</td>
          <td class="px-4 py-2.5">
            <BaseBadge :variant="u.tipoAcesso === 'TI' ? 'varchar' : 'boolean'">{{ u.tipoAcesso || 'COMUM' }}</BaseBadge>
          </td>
          <td class="px-4 py-2.5">
            <BaseBadge :variant="u.bloqueado ? 'bloq' : 'ativo'">{{ u.bloqueado ? 'Inativo' : 'Ativo' }}</BaseBadge>
          </td>
          <td class="px-4 py-2.5">
            <div class="flex gap-1.5">
              <BaseButton size="sm" variant="ghost" @click="abrirEditar(u.id)">Editar</BaseButton>
              <BaseButton v-if="u.bloqueado" size="sm" @click="pedirToggle(u, false)">Reativar</BaseButton>
              <BaseButton v-else size="sm" variant="danger" @click="pedirToggle(u, true)">Desativar</BaseButton>
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

    <UsuarioFormModal
      :open="formAberto"
      :usuario="usuarioEditando"
      @close="formAberto = false"
      @salvo="formAberto = false; listar()"
    />

    <ConfirmDialog
      v-if="alvoToggle"
      :open="confirmAberto"
      :titulo="alvoToggle.desativar ? 'Desativar usuário' : 'Reativar usuário'"
      :texto="alvoToggle.desativar
        ? `Desativar \u0022${alvoToggle.nome}\u0022? Não poderá logar nem receber novas atribuições. Vínculos atuais são mantidos.`
        : `Reativar \u0022${alvoToggle.nome}\u0022?`"
      :confirmar-label="alvoToggle.desativar ? 'Desativar' : 'Reativar'"
      @confirm="aplicarToggle"
      @cancel="confirmAberto = false"
    />
  </LayoutSidebar>
</template>