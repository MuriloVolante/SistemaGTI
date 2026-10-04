<script setup>
import { ref, computed, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import tiposService from '@/services/tipos'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  open: { type: Boolean, required: true },
  tipo: { type: Object, default: null }
})
const emit = defineEmits(['close', 'salvo'])
const { mostrar } = useToast()

const MAX_NOME = 100
const MAX_VIDA_UTIL = 600
const MAX_PERCENTUAL = 100

const nome = ref('')
const vidaUtilMeses = ref('')
const percentualDepreciacao = ref('')
const salvando = ref(false)
const erros = ref({})

const editando = computed(() => !!props.tipo?.id)
const podeSalvar = computed(() => !salvando.value && !!nome.value.trim())

watch(() => props.open, (v) => {
  if (!v) return
  erros.value = {}
  nome.value = props.tipo?.nome || ''
  vidaUtilMeses.value = props.tipo?.vidaUtilMeses ?? ''
  percentualDepreciacao.value = props.tipo?.percentualDepreciacao ?? ''
})

function vazio(v) {
  return v === '' || v === null || v === undefined
}

function validarTipo() {
  const e = {}
  const n = nome.value.trim()

  if (!n) e.nome = 'Nome obrigatorio.'
  else if (n.length < 2) e.nome = 'O nome deve ter ao menos 2 caracteres.'
  else if (n.length > MAX_NOME) e.nome = 'O nome deve ter no maximo ' + MAX_NOME + ' caracteres.'

  if (!vazio(vidaUtilMeses.value)) {
    const meses = Number(vidaUtilMeses.value)
    if (!Number.isInteger(meses)) e.vidaUtil = 'Vida util deve ser um numero inteiro de meses.'
    else if (meses <= 0) e.vidaUtil = 'Vida util deve ser maior que zero.'
    else if (meses > MAX_VIDA_UTIL) e.vidaUtil = 'Vida util deve ser no maximo ' + MAX_VIDA_UTIL + ' meses.'
  }

  if (!vazio(percentualDepreciacao.value)) {
    const p = Number(String(percentualDepreciacao.value).replace(',', '.'))
    if (!Number.isFinite(p)) e.percentual = 'Percentual invalido.'
    else if (p <= 0) e.percentual = 'Percentual deve ser maior que zero.'
    else if (p > MAX_PERCENTUAL) e.percentual = 'Percentual nao pode ser maior que 100.'
    else if (!/^\d+([.,]\d{1,2})?$/.test(String(percentualDepreciacao.value))) {
      e.percentual = 'Percentual deve ter no maximo 2 casas decimais.'
    }
  }

  erros.value = e
  return Object.keys(e).length === 0
}

function limparErroTipo() {
  if (Object.keys(erros.value).length) erros.value = {}
}

async function salvar() {
  if (salvando.value) return
  if (!validarTipo()) {
    mostrar(erros.value.nome || erros.value.vidaUtil || erros.value.percentual)
    return
  }

  const payload = {
    nome: nome.value.trim(),
    vidaUtilMeses: vazio(vidaUtilMeses.value) ? null : String(vidaUtilMeses.value).trim(),
    percentualDepreciacao: vazio(percentualDepreciacao.value) ? null : String(percentualDepreciacao.value).trim()
  }
  salvando.value = true
  try {
    if (editando.value) await tiposService.atualizar(props.tipo.id, payload)
    else await tiposService.criar(payload)
    mostrar(editando.value ? 'Tipo atualizado.' : 'Tipo criado.')
    emit('salvo')
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" :title="editando ? 'Editar tipo' : 'Novo tipo de ativo'" @close="emit('close')">
    <BaseField
      v-model="nome"
      label="Nome *"
      placeholder="ex: Notebook, Switch, Nobreak"
      :maxlength="MAX_NOME"
      :erro="erros.nome"
      @input="limparErroTipo"
    />

    <hr class="border-borda my-5" />
    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-3">Depreciação (opcional)</div>

    <div class="grid grid-cols-2 gap-3">
      <BaseField
        v-model="vidaUtilMeses"
        label="Vida útil (meses)"
        type="number"
        min="1"
        :max="MAX_VIDA_UTIL"
        step="1"
        placeholder="ex: 60"
        :erro="erros.vidaUtil"
        @input="limparErroTipo"
      />
      <BaseField
        v-model="percentualDepreciacao"
        label="% de depreciação"
        type="number"
        min="0"
        :max="MAX_PERCENTUAL"
        step="0.01"
        placeholder="ex: 20"
        :erro="erros.percentual"
        @input="limparErroTipo"
      />
    </div>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="!podeSalvar" @click="salvar">
        {{ salvando ? 'Salvando...' : (editando ? 'Salvar' : 'Criar') }}
      </BaseButton>
    </template>
  </BaseModal>
</template>