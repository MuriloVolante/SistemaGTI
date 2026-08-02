<script setup>
import { ref, watch } from 'vue'
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

const nomeDoCampo = ref('')
const tipoDado = ref('VARCHAR')
const obrigatorio = ref(false)
const salvando = ref(false)

watch(() => props.open, (v) => {
  if (!v) return
  nomeDoCampo.value = ''
  tipoDado.value = 'VARCHAR'
  obrigatorio.value = false
})

async function salvar() {
  if (!nomeDoCampo.value.trim()) { mostrar('Nome do campo obrigatório.'); return }
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
    <BaseField v-model="nomeDoCampo" label="Nome do campo *" placeholder="ex: Endereco IP, Numero de Serie" />

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

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="salvando" @click="salvar">Adicionar</BaseButton>
    </template>
  </BaseModal>
</template>