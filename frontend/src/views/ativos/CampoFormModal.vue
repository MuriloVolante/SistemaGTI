<script setup>
import { ref, computed, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import tiposService from '@/services/tipos'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  open: { type: Boolean, required: true },
  tipoId: { type: Number, required: true }
})
const emit = defineEmits(['close', 'salvo'])
const { mostrar } = useToast()

const MAX_NOME_CAMPO = 100
const TIPOS_DADO = ['VARCHAR', 'INT', 'DATE', 'BOOLEAN']
const RE_NOME_CAMPO = /^[\p{L}0-9][\p{L}0-9 ._/()-]*$/u

const nomeDoCampo = ref('')
const tipoDado = ref('VARCHAR')
const obrigatorio = ref(false)
const salvando = ref(false)
const erro = ref('')

const podeSalvar = computed(() => !salvando.value && !!nomeDoCampo.value.trim())

watch(() => props.open, (v) => {
  if (!v) return
  nomeDoCampo.value = ''
  tipoDado.value = 'VARCHAR'
  obrigatorio.value = false
  erro.value = ''
})

function limparErroCampo() {
  if (erro.value) erro.value = ''
}

function validarCampo() {
  const n = nomeDoCampo.value.trim()
  if (!n) return 'Nome do campo obrigatorio.'
  if (n.length < 2) return 'O nome do campo deve ter ao menos 2 caracteres.'
  if (n.length > MAX_NOME_CAMPO) return 'O nome do campo deve ter no maximo ' + MAX_NOME_CAMPO + ' caracteres.'
  if (!RE_NOME_CAMPO.test(n)) return 'Use apenas letras, numeros, espaco e os simbolos . _ / ( ) -'
  if (!TIPOS_DADO.includes(tipoDado.value)) return 'Tipo de dado invalido.'
  if (!props.tipoId) return 'Tipo de ativo nao identificado.'
  return ''
}

async function salvar() {
  if (salvando.value) return

  const problema = validarCampo()
  if (problema) {
    erro.value = problema
    mostrar(problema)
    return
  }

  erro.value = ''
  salvando.value = true
  try {
    await tiposService.campos.criar(props.tipoId, {
      nomeDoCampo: nomeDoCampo.value.trim(),
      tipoDado: tipoDado.value,
      obrigatorio: obrigatorio.value
    })
    mostrar('Campo adicionado.')
    emit('salvo')
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" title="Adicionar campo" @close="emit('close')">
    <BaseField
      v-model="nomeDoCampo"
      label="Nome do campo *"
      placeholder="ex: Endereco IP, Numero de Serie"
      :maxlength="MAX_NOME_CAMPO"
      :erro="erro"
      @input="limparErroCampo"
    />

    <div class="mb-4">
      <label class="field-label">Tipo de dado *</label>
      <select v-model="tipoDado" class="field-input">
        <option value="VARCHAR">Texto (VARCHAR)</option>
        <option value="INT">Número inteiro (INT)</option>
        <option value="DATE">Data (DATE)</option>
        <option value="BOOLEAN">Sim/Não (BOOLEAN)</option>
      </select>
    </div>

    <label class="flex items-center gap-1.5 text-base text-texto cursor-pointer">
      <input v-model="obrigatorio" type="checkbox" />
      Campo obrigatório
    </label>

    <div v-if="obrigatorio" class="text-sm text-texto-sub mt-2 leading-relaxed">
      Ativos já cadastrados neste tipo ficarão sem valor neste campo. Ao editar qualquer um deles, o preenchimento passará a ser exigido.
    </div>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="!podeSalvar" @click="salvar">
        {{ salvando ? 'Adicionando...' : 'Adicionar' }}
      </BaseButton>
    </template>
    </template>
  </BaseModal>
</template>