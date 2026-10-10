<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Power, Copy, Check } from 'lucide-vue-next'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseTable from '@/components/base/BaseTable.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ConfirmDialog from '@/components/base/ConfirmDialog.vue'
import ModalNovaChave from '@/components/configuracoes/ModalNovaChave.vue'
import ModalChaveGerada from '@/components/configuracoes/ModalChaveGerada.vue'
import integracaoService from '@/services/integracao'
import { useToast } from '@/composables/useToast'

const { mostrar } = useToast()

const ABAS = [
  { key: 'chaves', label: 'Chaves' },
  { key: 'negadas', label: 'Tentativas recusadas' },
  { key: 'docs', label: 'Como usar' }
]

const RECURSOS = [
  { key: 'ativos', label: 'Ativos', escopo: 'ATIVOS' },
  { key: 'usuarios', label: 'Usuários', escopo: 'USUARIOS' },
  { key: 'chamados', label: 'Chamados', escopo: 'CHAMADOS' },
  { key: 'dashboard', label: 'Painel', escopo: 'DASHBOARD' }
]

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

const aba = ref('chaves')
const chaves = ref([])
const carregando = ref(true)

const modalNova = ref(false)
const modalGerada = ref(false)
const chaveGerada = ref({ valor: '', nome: '' })

const confirmacao = ref({ open: false, id: null, nome: '' })

const negadas = ref([])
const negadasCarregando = ref(false)
const negadasCarregadas = ref(false)

const recursoDoc = ref('ativos')
const previews = ref({})
const copiado = ref('')

const baseUrl = window.location.origin
const urlDoc = computed(() => `${baseUrl}/api/integracao/${recursoDoc.value}`)
const curlDoc = computed(() => `curl -H "X-API-Key: SUA_CHAVE_AQUI" ${urlDoc.value}`)

const temChaves = computed(() => chaves.value.length > 0)

async function carregarChaves() {
  carregando.value = true
  try {
    const { data } = await integracaoService.listar()
    chaves.value = data
  } catch {
    mostrar('Não foi possível carregar as chaves.')
  } finally {
    carregando.value = false
  }
}

async function carregarNegadas() {
  negadasCarregando.value = true
  try {
    const { data } = await integracaoService.tentativasNegadas(0, 100)
    negadas.value = data.content || []
    negadasCarregadas.value = true
  } catch {
    mostrar('Não foi possível carregar as tentativas recusadas.')
  } finally {
    negadasCarregando.value = false
  }
}

async function carregarPreview(recurso) {
  if (previews.value[recurso]) return
  previews.value = { ...previews.value, [recurso]: 'Carregando...' }
  try {
    const { data } = await integracaoService.preview(recurso)
    const amostra = Array.isArray(data) ? data.slice(0, 3) : data
    previews.value = { ...previews.value, [recurso]: JSON.stringify(amostra, null, 2) }
  } catch {
    previews.value = { ...previews.value, [recurso]: 'Erro ao carregar prévia.' }
  }
}

function mudarAba(key) {
  aba.value = key
  if (key === 'negadas' && !negadasCarregadas.value) carregarNegadas()
  if (key === 'docs') carregarPreview(recursoDoc.value)
}

function mudarRecursoDoc(key) {
  recursoDoc.value = key
  carregarPreview(key)
}

function aoCriar(resposta) {
  modalNova.value = false
  chaveGerada.value = { valor: resposta.valor, nome: resposta.chave.nome }
  modalGerada.value = true
  carregarChaves()
}

function pedirDesligar(c) {
  confirmacao.value = { open: true, id: c.id, nome: c.nome }
}

async function confirmarDesligar() {
  const { id } = confirmacao.value
  confirmacao.value = { open: false, id: null, nome: '' }
  try {
    await integracaoService.desligar(id)
    mostrar('Chave desligada.')
    carregarChaves()
  } catch (e) {
    mostrar(e?.response?.data?.erro || 'Não foi possível desligar a chave.')
  }
}

async function copiar(texto, label) {
  try {
    await navigator.clipboard.writeText(texto)
    copiado.value = label
    setTimeout(() => { if (copiado.value === label) copiado.value = '' }, 1500)
  } catch {}
}

function dataHora(valor) {
  if (!valor) return '—'
  return new Date(valor).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

function dataCurta(valor) {
  if (!valor) return 'Sem validade'
  return new Date(valor).toLocaleDateString('pt-BR')
}

function variantSituacao(situacao) {
  if (situacao === 'ATIVA') return 'ativo'
  if (situacao === 'EXPIRADA') return 'date'
  return 'bloq'
}

function rotuloSituacao(situacao) {
  if (situacao === 'ATIVA') return 'Ativa'
  if (situacao === 'EXPIRADA') return 'Expirada'
  return 'Desligada'
}

onMounted(carregarChaves)
</script>

<template>
  <LayoutSidebar>
    <div class="flex items-start justify-between gap-4 mb-6">
      <div>
        <h1 class="text-4xl font-semibold text-texto">Integração via API</h1>
        <div class="text-base text-texto-sub mt-1">
          Chaves de acesso para sistemas externos lerem os dados do GTI
        </div>
      </div>
      <BaseButton variant="primary" @click="modalNova = true">
        <Plus :size="16" /> Gerar nova chave
      </BaseButton>
    </div>

    <div class="flex gap-1 border-b border-borda mb-5">
      <button
        v-for="a in ABAS"
        :key="a.key"
        type="button"
        class="px-4 py-2 text-base font-medium bg-transparent cursor-pointer border-x-0 border-t-0 border-b-2 border-solid -mb-px"
        :class="aba === a.key ? 'border-primaria text-primaria-text' : 'border-transparent text-texto-sub hover:text-texto'"
        @click="mudarAba(a.key)"
      >
        {{ a.label }}
      </button>
    </div>

    <!-- ABA: CHAVES -->
    <div v-if="aba === 'chaves'">
      <div v-if="carregando" class="text-base text-texto-sub py-8 text-center">Carregando...</div>

      <EmptyState
        v-else-if="!temChaves"
        texto="Nenhuma chave criada ainda. Gere a primeira para liberar o acesso de um sistema externo."
      />

      <BaseCard v-else>
        <BaseTable
          :colunas="[
            { key: 'nome', label: 'Nome' },
            { key: 'prefixo', label: 'Chave', width: '170px' },
            { key: 'escopos', label: 'Dados liberados' },
            { key: 'uso', label: 'Último uso', width: '190px' },
            { key: 'validade', label: 'Validade', width: '120px' },
            { key: 'situacao', label: 'Situação', width: '110px' },
            { key: 'acoes', label: '', width: '120px', align: 'right' }
          ]"
        >
          <tr v-for="c in chaves" :key="c.id" class="border-b border-borda last:border-0">
            <td class="px-4 py-3">
              <div class="text-base text-texto font-medium">{{ c.nome }}</div>
              <div class="text-xs text-texto-sub">Criada por {{ c.criadoPorNome || '—' }} em {{ dataCurta(c.criadoEm) }}</div>
            </td>
            <td class="px-4 py-3">
              <code class="font-mono text-xs text-texto-sub">{{ c.prefixo }}••••</code>
            </td>
            <td class="px-4 py-3">
              <div class="flex flex-wrap gap-1">
                <BaseBadge v-for="e in c.escopos" :key="e" variant="varchar">
                  {{ ROTULO_ESCOPO[e] || e }}
                </BaseBadge>
              </div>
              <div v-if="c.ipsPermitidos.length" class="text-xs text-texto-sub mt-1">
                Restrita a {{ c.ipsPermitidos.join(', ') }}
              </div>
            </td>
            <td class="px-4 py-3 text-sm text-texto-sub">
              <div>{{ dataHora(c.ultimoUsoEm) }}</div>
              <div v-if="c.ultimoUsoIp" class="text-xs">{{ c.ultimoUsoIp }}</div>
            </td>
            <td class="px-4 py-3 text-sm text-texto-sub">{{ dataCurta(c.expiraEm) }}</td>
            <td class="px-4 py-3">
              <BaseBadge :variant="variantSituacao(c.situacao)">{{ rotuloSituacao(c.situacao) }}</BaseBadge>
            </td>
            <td class="px-4 py-3 text-right">
              <BaseButton
                v-if="c.situacao === 'ATIVA'"
                variant="danger"
                size="sm"
                @click="pedirDesligar(c)"
              >
                <Power :size="14" /> Desligar
              </BaseButton>
              <span v-else class="text-xs text-texto-fraco">{{ dataCurta(c.desligadaEm) }}</span>
            </td>
          </tr>
        </BaseTable>
      </BaseCard>
    </div>

    <!-- ABA: TENTATIVAS RECUSADAS -->
    <div v-else-if="aba === 'negadas'">
      <p class="text-sm text-texto-sub mb-4 max-w-[720px]">
        Toda chamada recusada aparece aqui. Repetições do mesmo endereço indicam sistema mal configurado
        ou tentativa de acesso indevido.
      </p>

      <div v-if="negadasCarregando" class="text-base text-texto-sub py-8 text-center">Carregando...</div>

      <EmptyState v-else-if="!negadas.length" texto="Nenhuma tentativa recusada registrada." />

      <BaseCard v-else>
        <BaseTable
          :colunas="[
            { key: 'quando', label: 'Quando', width: '170px' },
            { key: 'motivo', label: 'Motivo' },
            { key: 'chave', label: 'Chave usada' },
            { key: 'endpoint', label: 'Endereço chamado' },
            { key: 'ip', label: 'Origem', width: '150px' }
          ]"
        >
          <tr v-for="n in negadas" :key="n.id" class="border-b border-borda last:border-0">
            <td class="px-4 py-3 text-sm text-texto-sub">{{ dataHora(n.ocorridoEm) }}</td>
            <td class="px-4 py-3">
              <BaseBadge variant="obrig">{{ MOTIVOS[n.motivoNegacao] || n.motivoNegacao || 'Recusada' }}</BaseBadge>
            </td>
            <td class="px-4 py-3 text-sm text-texto">
              <span v-if="n.chaveNome">{{ n.chaveNome }}</span>
              <code v-else-if="n.prefixoTentado" class="font-mono text-xs text-texto-sub">{{ n.prefixoTentado }}••••</code>
              <span v-else class="text-texto-fraco">—</span>
            </td>
            <td class="px-4 py-3 text-sm text-texto-sub font-mono text-xs">{{ n.endpoint }}</td>
            <td class="px-4 py-3 text-sm text-texto-sub">{{ n.ip || '—' }}</td>
          </tr>
        </BaseTable>
      </BaseCard>
    </div>

    <!-- ABA: COMO USAR -->
    <div v-else>
      <p class="text-sm text-texto-sub mb-5 max-w-[720px]">
        O sistema externo envia a chave no cabeçalho
        <code class="font-mono text-xs bg-surface-alt border border-borda rounded-raio-sm px-1">X-API-Key</code>
        em toda chamada. Não há login: a chave é o que identifica e autoriza o acesso. Cada endereço abaixo
        só responde se a chave tiver aquele conjunto de dados liberado.
      </p>

      <div class="flex gap-1 border-b border-borda mb-4">
        <button
          v-for="r in RECURSOS"
          :key="r.key"
          type="button"
          class="px-4 py-2 text-base font-medium bg-transparent cursor-pointer border-x-0 border-t-0 border-b-2 border-solid -mb-px"
          :class="recursoDoc === r.key ? 'border-primaria text-primaria-text' : 'border-transparent text-texto-sub hover:text-texto'"
          @click="mudarRecursoDoc(r.key)"
        >
          {{ r.label }}
        </button>
      </div>

      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Endereço</div>
      <div class="flex items-center gap-2 mb-4 max-w-[680px]">
        <code class="flex-1 font-mono text-sm bg-surface-alt border border-borda rounded-raio-sm px-3 py-2 truncate">{{ urlDoc }}</code>
        <button
          type="button"
          class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt"
          title="Copiar endereço"
          @click="copiar(urlDoc, 'url')"
        >
          <Check v-if="copiado === 'url'" :size="16" class="text-sucesso-text" />
          <Copy v-else :size="16" />
        </button>
      </div>

      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Exemplo pronto para usar</div>
      <div class="flex items-start gap-2 mb-6 max-w-[680px]">
        <pre class="flex-1 font-mono text-xs bg-surface-alt border border-borda rounded-raio-sm px-3 py-2 overflow-x-auto whitespace-pre-wrap break-all">{{ curlDoc }}</pre>
        <button
          type="button"
          class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt shrink-0"
          title="Copiar comando"
          @click="copiar(curlDoc, 'curl')"
        >
          <Check v-if="copiado === 'curl'" :size="16" class="text-sucesso-text" />
          <Copy v-else :size="16" />
        </button>
      </div>

      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Exemplo de resposta</div>
      <pre class="bg-surface border border-borda rounded-raio p-4 text-sm font-mono text-texto overflow-x-auto max-h-[420px] overflow-y-auto whitespace-pre">{{ previews[recursoDoc] || 'Carregando...' }}</pre>
    </div>

    <ModalNovaChave :open="modalNova" @close="modalNova = false" @criada="aoCriar" />

    <ModalChaveGerada
      :open="modalGerada"
      :valor="chaveGerada.valor"
      :nome="chaveGerada.nome"
      @close="modalGerada = false"
    />

    <ConfirmDialog
      :open="confirmacao.open"
      titulo="Desligar chave"
      :texto="`A chave ${confirmacao.nome} para de funcionar imediatamente e o sistema que a usa perde o acesso. O histórico de uso é mantido. Esta ação não pode ser desfeita.`"
      confirmar-label="Desligar"
      @confirm="confirmarDesligar"
      @cancel="confirmacao = { open: false, id: null, nome: '' }"
    />
  </LayoutSidebar>
</template>