<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import UsuarioLabel from '@/components/UsuarioLabel.vue'
import UserTopbar from '@/components/UserTopbar.vue'
import ChatBox from '@/components/chamados/ChatBox.vue'
import chamadosService from '@/services/chamados'
import { STATUS_BADGE, STATUS_LABEL, formatDataHora } from '@/utils/chamados'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { mostrar } = useToast()

const chamadoId = route.params.id

const chamado = ref(null)
const mensagens = ref([])
const naoEncontrado = ref(false)

let timerMensagens = null
let timerChamado = null

const meuId = computed(() => auth.usuario?.id)
const estadoChat = computed(() => (chamado.value?.status === 'CONCLUIDO' ? 'concluido' : 'input'))

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

function sair() {
  auth.logout()
  router.push('/login')
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
  <div>
    <UserTopbar :abas="[]" ativa="" @logout="sair" />

    <div class="max-w-[860px] mx-auto p-6">
      <div v-if="naoEncontrado" class="text-3xl font-semibold text-texto">Chamado não encontrado</div>

      <template v-else-if="chamado">
        <div class="flex items-center gap-4 mb-6">
          <BaseButton size="sm" @click="router.push('/meus-chamados')">← Voltar</BaseButton>
          <div>
            <div class="text-2xl font-semibold text-texto">#{{ chamado.id }} - {{ chamado.titulo }}</div>
            <div class="text-sm text-texto-sub mt-0.5">
              Solicitante: <UsuarioLabel :valor="chamado.solicitante" />
            </div>
          </div>
        </div>

        <div class="bg-surface border border-borda rounded-raio shadow-sombra p-5 mb-6">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-fraco mb-3">Informações do chamado</div>

          <div class="grid grid-cols-4 gap-4">
            <div>
              <div class="text-xs uppercase tracking-wide text-texto-fraco mb-1">Status</div>
              <BaseBadge :variant="STATUS_BADGE[chamado.status]">{{ STATUS_LABEL[chamado.status] || chamado.status }}</BaseBadge>
            </div>
            <div>
              <div class="text-xs uppercase tracking-wide text-texto-fraco mb-1">Aberto em</div>
              <div class="text-base font-medium text-texto">{{ formatDataHora(chamado.dataAbertura) }}</div>
            </div>
            <div>
              <div class="text-xs uppercase tracking-wide text-texto-fraco mb-1">Técnico</div>
              <div class="text-base font-medium text-texto">
                <UsuarioLabel v-if="chamado.tecnico" :valor="chamado.tecnico" />
                <span v-else class="text-texto-sub font-normal">Aguardando atendimento</span>
              </div>
            </div>
            <div>
              <div class="text-xs uppercase tracking-wide text-texto-fraco mb-1">Concluído em</div>
              <div class="text-base font-medium text-texto">
                {{ chamado.dataFechamento ? formatDataHora(chamado.dataFechamento) : '-' }}
              </div>
            </div>
          </div>

          <div class="mt-5">
            <div class="text-xs uppercase tracking-wide text-texto-fraco mb-1.5">Descrição</div>
            <div class="text-base text-texto bg-surface-alt rounded-raio-sm px-4 py-3 whitespace-pre-wrap">
              {{ chamado.descricao }}
            </div>
          </div>
        </div>

        <ChatBox
          :mensagens="mensagens"
          :meu-id="meuId"
          :estado="estadoChat"
          :mostrar-concluir="false"
          texto-concluido="Este chamado foi concluído. A conversa não aceita mais mensagens."
          @enviar="enviarMensagem"
        />
      </template>

      <EmptyState v-else texto="Carregando..." />
    </div>
  </div>
</template>