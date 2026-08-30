<script setup>
import { ref, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import tiposService from '@/services/tipos'
import centrosCustoService from '@/services/centrosCusto'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  open: { type: Boolean, required: true }
})
const emit = defineEmits(['close', 'gerar'])
const { mostrar } = useToast()

const ativoId = ref('')
const tipoId = ref('')
const centroCusto = ref('')

const tipos = ref([])
const centros = ref([])

watch(() => props.open, async (v) => {
  if (!v) return
  ativoId.value = ''
  tipoId.value = ''
  centroCusto.value = ''
  try {
    const [{ data: tiposData }, { data: centrosData }] = await Promise.all([
      tiposService.listar(),
      centrosCustoService.listar()
    ])
    tipos.value = tiposData
    centros.value = centrosData
  } catch {
    mostrar('Erro ao carregar filtros.')
  }
})

function gerar() {
  emit('gerar', {
    ativoId: ativoId.value ? Number(ativoId.value) : null,
    tipoId: tipoId.value ? Number(tipoId.value) : null,
    centroCusto: centroCusto.value || null
  })
  emit('close')
}
</script>

<template>
  <BaseModal :open="open" title="Imprimir QR Codes" @close="emit('close')">
    <BaseField v-model="ativoId" label="ID do ativo" type="number" placeholder="Deixe em branco pra ignorar" opcional />

    <div class="mb-4">
      <label class="field-label">Tipo <span class="font-normal normal-case tracking-normal">(opcional)</span></label>
      <select v-model="tipoId" class="field-input">
        <option value="">Todos</option>
        <option v-for="t in tipos" :key="t.id" :value="t.id">{{ t.nome }}</option>
      </select>
    </div>

    <div class="mb-4">
      <label class="field-label">Centro de custo <span class="font-normal normal-case tracking-normal">(opcional)</span></label>
      <select v-model="centroCusto" class="field-input">
        <option value="">Todos</option>
        <option v-for="c in centros" :key="c.id" :value="c.nome">{{ c.nome }}</option>
      </select>
    </div>

    <div class="text-sm text-texto-sub mb-2">Preencha o ID do ativo pra imprimir só um. Deixe tudo em branco pra imprimir todos.</div>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" @click="gerar">Gerar</BaseButton>
    </template>
  </BaseModal>
</template>