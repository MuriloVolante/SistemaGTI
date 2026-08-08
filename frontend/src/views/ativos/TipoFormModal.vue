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

const nome = ref('')
const vidaUtilMeses = ref('')
const percentualDepreciacao = ref('')
const salvando = ref(false)

const editando = computed(() => !!props.tipo?.id)

watch(() => props.open, (v) => {
  if (!v) return
  nome.value = props.tipo?.nome || ''
  vidaUtilMeses.value = props.tipo?.vidaUtilMeses ?? ''
  percentualDepreciacao.value = props.tipo?.percentualDepreciacao ?? ''
})

async function salvar() {
  if (!nome.value.trim()) { mostrar('Nome obrigatório.'); return }
  const payload = {
    nome: nome.value.trim(),
    vidaUtilMeses: vidaUtilMeses.value || null,
    percentualDepreciacao: percentualDepreciacao.value || null
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
    <BaseField v-model="nome" label="Nome *" placeholder="ex: Notebook, Switch, Nobreak" />

    <hr class="border-borda my-5" />
    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-3">Depreciação (opcional)</div>

    <div class="grid grid-cols-2 gap-3">
      <BaseField v-model="vidaUtilMeses" label="Vida útil (meses)" type="number" placeholder="ex: 60" />
      <BaseField v-model="percentualDepreciacao" label="% de depreciação" type="number" placeholder="ex: 20" />
    </div>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="salvando" @click="salvar">
        {{ editando ? 'Salvar' : 'Criar' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>