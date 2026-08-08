<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LayoutSidebar from '@/layouts/LayoutSidebar.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ExclusaoModal from '@/components/ExclusaoModal.vue'
import UsuarioLabel from '@/components/UsuarioLabel.vue'
import ChatBox from '@/components/chamados/ChatBox.vue'
import ChamadoEditModal from '@/components/chamados/ChamadoEditModal.vue'
import { useAuthStore } from '@/stores/auth'
import chamadosService from '@/services/chamados'
import { STATUS_BADGE, STATUS_LABEL, PRIORIDADE_BADGE, PRIORIDADE_LABEL, formatDataHora } from '@/utils/chamados'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { mostrar } = useToast()

const chamadoId = route.params.id

const chamado = ref(null)
const mensagens = ref([])
const naoEncontrado = ref(false)

const edicaoAberta = ref(false)
const exclusaoAberta = ref(false)

let timerMensagens = null
let timerChamado = null

const meuId = computed(() => auth.usuario?.id)
const souTecnico = computed(() => !!chamado.value?.tecnico && chamado.value.tecnico.id === meuId.value)
const concluido = computed(() => chamado.value?.status === 'CONCLUIDO')

const estadoChat = computed(() => {
  if (concluido.value) return 'concluido'
  if (!souTecnico.value) return 'bloqueado'
  return 'input'
})

const textoBloqueio = computed(() =>
  chamado.value?.tecnico
    ? 'Apenas o técnico que assumiu este chamado pode enviar mensagens.'
    : 'Assuma o chamado para iniciar a conversa.'
)

async function carregarChamado() {
  try {
    const { data } = await chamadosService.buscar(chamadoId)
    chamado.value = data
  } catch {
    naoEncontrado.value = true
  }
}

async function carregarMensagens() {
  try {
    const { data } = await chamadosService.mensagens.listar(chamadoId)
    mensagens.value = data
  } catch {}
}

async function enviarMensagem(texto) {
  try {
    await chamadosService.mensagens.enviar(chamadoId, texto)
    await carregarMensagens()
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  }
}

async function concluirChamado(parecer) {
  try {
    await chamadosService.mudarStatus(chamadoId, 'CONCLUIDO', parecer)
    mostrar('Chamado concluído com sucesso.')
    await carregarChamado()
    await carregarMensagens()
  } catch (e) {
    mostrar('Erro ao concluir: ' + (e.response?.data?.erro || ''))
  }
}

async function assumir() {
  try {
    await chamadosService.assumir(chamadoId)
    mostrar('Chamado assumido.')
    await carregarChamado()
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || ''))
  }
}

async function reabrir() {
  try {
    await chamadosService.reabrir(chamadoId)
    mostrar('Chamado reaberto.')
    await carregarChamado()
    await carregarMensagens()
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || ''))
  }
}

function aoSalvarEdicao() {
  edicaoAberta.value = false
  carregarChamado()
}

onMounted(async () => {
  await carregarChamado()
  await carregarMensagens()
  timerMensagens = setInterval(carregarMensagens, 5000)
  timerChamado = setInterval(carregarChamado, 30000)
})

onUnmounted(() => {
  clearInterval(timerMensagens)
  clearInterval(timerChamado)
})
</script>

<template>
  <LayoutSidebar>
    <div v-if="naoEncontrado" class="text-4xl font-semibold text-texto">Chamado não encontrado</div>

    <template v-else-if="chamado">
      <div class="flex items-start justify-between mb-6 gap-4">
        <div class="flex items-center gap-4">
          <BaseButton size="sm" @click="router.push('/chamados')">← Voltar</BaseButton>
          <div>
            <div class="text-4xl font-semibold text-texto">#{{ chamado.id }} - {{ chamado.titulo }}</div>
            <div class="text-base text-texto-sub mt-1">
              Solicitante: <UsuarioLabel :valor="chamado.solicitante" />
            </div>
          </div>
        </div>
        <div class="flex gap-2 flex-shrink-0">
          <BaseButton v-if="!chamado.tecnico && !concluido" size="sm" variant="primary" @click="assumir">
            Assumir chamado
          </BaseButton>
          <template v-if="souTecnico && !concluido">
            <BaseButton size="sm" @click="edicaoAberta = true">Editar</BaseButton>
            <BaseButton size="sm" variant="danger" @click="exclusaoAberta = true">Excluir</BaseButton>
          </template>
          <BaseButton v-if="concluido" size="sm" variant="primary" @click="reabrir">Reabrir chamado</BaseButton>
        </div>
      </div>

      <div class="grid grid-cols-4 gap-4 mb-6">
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Status</div>
          <BaseBadge :variant="STATUS_BADGE[chamado.status]">{{ STATUS_LABEL[chamado.status] || chamado.status }}</BaseBadge>
        </div>
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Prioridade</div>
          <BaseBadge :variant="PRIORIDADE_BADGE[chamado.prioridade]">
            {{ PRIORIDADE_LABEL[chamado.prioridade] || chamado.prioridade || '-' }}
          </BaseBadge>
        </div>
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Solicitante</div>
          <div class="text-lg font-semibold text-texto"><UsuarioLabel :valor="chamado.solicitante" /></div>
        </div>
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1.5">Técnico</div>
          <div class="text-lg font-semibold text-texto">
            <UsuarioLabel v-if="chamado.tecnico" :valor="chamado.tecnico" />
            <span v-else class="text-texto-fraco font-normal">Não atribuído</span>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-[1fr_420px] gap-5 items-start">
        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-5">
          <div class="text-base font-semibold text-texto mb-4">Informações do chamado</div>
          <div class="grid grid-cols-2 gap-x-6 gap-y-4">
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Categoria</div>
              <div class="text-base text-texto">{{ chamado.categoria || '-' }}</div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Ativo vinculado</div>
              <div class="text-base text-texto">
                {{ chamado.ativo ? `${chamado.ativo.patrimonio} - ${chamado.ativo.marcaModelo}` : '-' }}
              </div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Aberto em</div>
              <div class="text-base text-texto">{{ formatDataHora(chamado.dataAbertura) }}</div>
            </div>
            <div>
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Concluído em</div>
              <div class="text-base text-texto">
                {{ chamado.dataFechamento ? formatDataHora(chamado.dataFechamento) : '-' }}
              </div>
            </div>
            <div class="col-span-2">
              <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-1">Descrição</div>
              <div class="text-base text-texto whitespace-pre-wrap">{{ chamado.descricao }}</div>
            </div>
          </div>
        </div>

        <ChatBox
          :mensagens="mensagens"
          :meu-id="meuId"
          :estado="estadoChat"
          :texto-bloqueio="textoBloqueio"
          @enviar="enviarMensagem"
          @concluir="concluirChamado"
        />
      </div>

      <ChamadoEditModal
        :open="edicaoAberta"
        :chamado="chamado"
        @close="edicaoAberta = false"
        @salvo="aoSalvarEdicao"
      />

      <ExclusaoModal
        :open="exclusaoAberta"
        :titulo="`Excluir chamado #${chamado.id}`"
        :impacto-url="`/chamados/${chamado.id}/impacto-exclusao`"
        :delete-url="`/chamados/${chamado.id}`"
        :palavra-confirmacao="`#${chamado.id}`"
        @close="exclusaoAberta = false"
        @sucesso="router.push('/chamados')"
      />
    </template>

    <EmptyState v-else texto="Carregando..." />
  </LayoutSidebar>
</template>