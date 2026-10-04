<script setup>
import { ref, computed, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import chamadosService from '@/services/chamados'
import usuariosService from '@/services/usuarios'
import ativosService from '@/services/ativos'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  open: { type: Boolean, required: true },
  chamado: { type: Object, default: null }
})
const emit = defineEmits(['close', 'salvo'])
const { mostrar } = useToast()

const CATEGORIAS = [
  { v: '', l: 'Sem categoria' },
  { v: 'HARDWARE', l: 'Hardware' },
  { v: 'SOFTWARE', l: 'Software' },
  { v: 'REDE', l: 'Rede' },
  { v: 'ACESSO', l: 'Acesso / Senha' },
  { v: 'EMAIL', l: 'E-mail' },
  { v: 'IMPRESSAO', l: 'Impressão' },
  { v: 'TELEFONIA', l: 'Telefonia' },
  { v: 'OUTRO', l: 'Outro' }
]

const MIN_TITULO = 5
const MAX_TITULO = 200
const MIN_DESCRICAO = 10
const MAX_DESCRICAO = 5000

const form = ref({})
const tecnicos = ref([])
const ativos = ref([])
const salvando = ref(false)
const erros = ref({})
const falhaListas = ref('')

const podeSalvar = computed(
  () =>
    !salvando.value &&
    !!(form.value.titulo || '').trim() &&
    !!(form.value.descricao || '').trim()
)  try {
     const { data } = await usuariosService.listar()
     tecnicos.value = data.filter((u) => u.tipoAcesso === 'TI')
   } catch {}

   try {
     const { data } = await ativosService.listar()

watch(() => props.open, async (v) => {
  if (!v || !props.chamado) return
  const c = props.chamado
  form.value = {
    titulo: c.titulo || '',
    descricao: c.descricao || '',
    categoria: c.categoria || '',
    prioridade: c.prioridade || 'MEDIA',
    ativoId: c.ativo?.id || '',
    tecnicoId: c.tecnico?.id || ''
  }

  erros.value = {}
  falhaListas.value = ''

  try {
    const { data } = await usuariosService.listar()
    tecnicos.value = data.filter((u) => u.tipoAcesso === 'TI' && !u.bloqueado)
  } catch {
    tecnicos.value = []
    falhaListas.value = 'Nao foi possivel carregar a lista de tecnicos.'
  }

  try {
    const { data } = await ativosService.listar()
    const solId = c.solicitante?.id
    let filtrados = data.filter((a) => a.responsavel && typeof a.responsavel === 'object' && a.responsavel.id === solId)
    const atualId = c.ativo?.id
    if (atualId && !filtrados.some((a) => a.id === atualId)) {
      const atual = data.find((a) => a.id === atualId)
      if (atual) filtrados = [atual, ...filtrados]
    }
    ativos.value = filtrados
  } catch {
    ativos.value = []
    falhaListas.value = falhaListas.value
      ? 'Nao foi possivel carregar as listas de tecnicos e ativos.'
      : 'Nao foi possivel carregar a lista de ativos.'
  }
})

function validarEdicao() {
  const e = {}
  const t = (form.value.titulo || '').trim()
  const d = (form.value.descricao || '').trim()

  if (!t) e.titulo = 'Titulo obrigatorio.'
  else if (t.length < MIN_TITULO) e.titulo = 'O titulo deve ter ao menos ' + MIN_TITULO + ' caracteres.'
  else if (t.length > MAX_TITULO) e.titulo = 'O titulo deve ter no maximo ' + MAX_TITULO + ' caracteres.'

  if (!d) e.descricao = 'Descricao obrigatoria.'
  else if (d.length < MIN_DESCRICAO) e.descricao = 'A descricao deve ter ao menos ' + MIN_DESCRICAO + ' caracteres.'
  else if (d.length > MAX_DESCRICAO) e.descricao = 'A descricao deve ter no maximo ' + MAX_DESCRICAO + ' caracteres.'

  erros.value = e
  return Object.keys(e).length === 0
}

function limparErroEdicao() {
  if (Object.keys(erros.value).length) erros.value = {}
}

async function salvar() {
  if (salvando.value) return

  if (!validarEdicao()) {
    mostrar(erros.value.titulo || erros.value.descricao)
    return
  }

  salvando.value = true
  try {
    await chamadosService.atualizar(props.chamado.id, {
      titulo: form.value.titulo.trim(),
      descricao: form.value.descricao.trim(),
      categoria: form.value.categoria,
      prioridade: form.value.prioridade,
      ativoId: form.value.ativoId || null,
      tecnicoId: form.value.tecnicoId || null
    })
    mostrar('Chamado atualizado.')
    emit('salvo')
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" title="Editar chamado" max-width="max-w-[620px]" @close="emit('close')">
    <div
      v-if="falhaListas"
      class="text-sm text-aviso-text bg-aviso-bg border border-aviso-borda rounded-raio-sm px-3 py-2 mb-4"
    >
      {{ falhaListas }} Salve somente se nao precisar alterar esses campos.
    </div>

    <BaseField
      v-model="form.titulo"
      label="Título *"
      :maxlength="MAX_TITULO"
      :erro="erros.titulo"
      @input="limparErroEdicao"
    />

    <div class="mb-4">
      <label class="field-label">Descrição *</label>
      <textarea
        v-model="form.descricao"
        rows="4"
        :maxlength="MAX_DESCRICAO"
        class="field-input resize-y"
        @input="limparErroEdicao"
      ></textarea>
      <div v-if="erros.descricao" class="text-sm text-perigo-forte mt-1">{{ erros.descricao }}</div>
    </div>

    <div class="grid grid-cols-2 gap-3">
      <div class="mb-4">
        <label class="field-label">Categoria</label>
        <select v-model="form.categoria" class="field-input">
          <option v-for="c in CATEGORIAS" :key="c.v" :value="c.v">{{ c.l }}</option>
        </select>
      </div>
      <div class="mb-4">
        <label class="field-label">Prioridade</label>
        <select v-model="form.prioridade" class="field-input">
          <option value="BAIXA">Baixa</option>
          <option value="MEDIA">Média</option>
          <option value="ALTA">Alta</option>
          <option value="MUITO_ALTA">Muito alta</option>
        </select>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-3">
      <div class="mb-4">
        <label class="field-label">Ativo vinculado</label>
        <select v-model="form.ativoId" class="field-input">
          <option value="">Sem ativo vinculado</option>
          <option v-for="a in ativos" :key="a.id" :value="a.id">{{ a.patrimonio }} — {{ a.marcaModelo }}</option>
        </select>
      </div>
      <div class="mb-4">
        <label class="field-label">Técnico responsável</label>
        <select v-model="form.tecnicoId" class="field-input">
          <option value="">Sem técnico</option>
          <option v-for="t in tecnicos" :key="t.id" :value="t.id">{{ t.nomeCompleto || t.nomeUsuario }}</option>
        </select>
      </div>
    </div>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="!podeSalvar" @click="salvar">Salvar</BaseButton>
    </template>
  </BaseModal>
</template>