<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import LayoutUsuario from '@/layouts/LayoutUsuario.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseBadge from '@/components/base/BaseBadge.vue'
import BaseField from '@/components/base/BaseField.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import UsuarioLabel from '@/components/UsuarioLabel.vue'
import chamadosService from '@/services/chamados'
import { STATUS_BADGE, STATUS_LABEL, formatDataHora } from '@/utils/chamados'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { mostrar } = useToast()

const ABAS = [
  { key: 'abrir', label: 'Abrir chamado' },
  { key: 'acompanhar', label: 'Acompanhar chamados' }
]

const aba = ref('abrir')

const titulo = ref('')
const descricao = ref('')
const criando = ref(false)

const chamados = ref([])
const carregando = ref(true)
const erro = ref(false)

let timer = null

async function criarChamado() {
  if (!titulo.value.trim()) { mostrar('Informe o título.'); return }
  if (!descricao.value.trim()) { mostrar('Informe a descrição.'); return }

  criando.value = true
  try {
    await chamadosService.criar({ titulo: titulo.value.trim(), descricao: descricao.value.trim() })
    titulo.value = ''
    descricao.value = ''
    mostrar('Chamado criado com sucesso.')
    mudarAba('acompanhar')
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  } finally {
    criando.value = false
  }
}

async function listar() {
  try {
    const { data } = await chamadosService.listar()
    chamados.value = data
    erro.value = false
  } catch {
    erro.value = true
  } finally {
    carregando.value = false
  }
}

function mudarAba(key) {
  aba.value = key
  if (key === 'acompanhar') listar()
}

onMounted(() => {
  listar()
  timer = setInterval(listar, 15000)
})

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <LayoutUsuario :abas="ABAS" :ativa="aba" @change="mudarAba">
    <div v-if="aba === 'abrir'" class="bg-surface border border-borda rounded-raio shadow-sombra p-7">
      <h2 class="text-3xl font-semibold text-texto">Abrir novo chamado</h2>
      <div class="text-base text-texto-sub mt-1 mb-6">Descreva o problema. A equipe de TI será notificada.</div>

      <BaseField v-model="titulo" label="Título *" placeholder="ex: Computador não liga" maxlength="200" />

      <div class="mb-5">
        <label class="field-label">Descrição *</label>
        <textarea
          v-model="descricao"
          rows="6"
          class="field-input resize-y"
          placeholder="Descreva o problema com o máximo de detalhes possível..."
        ></textarea>
      </div>

      <BaseButton variant="primary" block :disabled="criando" @click="criarChamado">
        {{ criando ? 'Criando...' : 'Criar chamado' }}
      </BaseButton>
    </div>

    <div v-else>
      <h2 class="text-3xl font-semibold text-texto mb-5">Meus chamados</h2>

      <EmptyState v-if="carregando" texto="Carregando..." />
      <EmptyState v-else-if="erro" texto="Erro ao carregar chamados." />
      <EmptyState v-else-if="!chamados.length" texto="Você ainda não abriu nenhum chamado." />

      <div v-else class="space-y-3">
        <div
          v-for="c in chamados"
          :key="c.id"
          class="bg-surface border border-borda rounded-raio shadow-sombra p-4 cursor-pointer transition-all hover:border-primaria hover:shadow-sombra-md"
          @click="router.push(`/meus-chamados/${c.id}`)"
        >
          <div class="flex items-start justify-between gap-3 mb-2">
            <div class="text-lg font-semibold text-texto">#{{ c.id }} - {{ c.titulo }}</div>
            <BaseBadge :variant="STATUS_BADGE[c.status]">{{ STATUS_LABEL[c.status] || c.status }}</BaseBadge>
          </div>
          <div class="text-base text-texto-sub line-clamp-2 mb-3">{{ c.descricao }}</div>
          <div class="flex gap-4 text-sm text-texto-fraco">
            <span>Aberto em {{ formatDataHora(c.dataAbertura) }}</span>
            <span v-if="c.tecnico">Técnico: <UsuarioLabel :valor="c.tecnico" /></span>
            <span v-else>Aguardando atendimento</span>
          </div>
        </div>
      </div>
    </div>
  </LayoutUsuario>
</template>