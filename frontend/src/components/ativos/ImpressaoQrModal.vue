<script setup>
import { ref, computed, watch } from 'vue'
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
const incluirDescartados = ref(false)
const erro = ref('')

const tipos = ref([])
const centros = ref([])

const idPreenchido = computed(() => String(ativoId.value).trim() !== '')
const outrosFiltrosAtivos = computed(() => !!tipoId.value || !!centroCusto.value)

watch(() => props.open, async (v) => {
  if (!v) return
  ativoId.value = ''
  tipoId.value = ''
  centroCusto.value = ''
  incluirDescartados.value = false
  erro.value = ''
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

function limparErroQr() {
  if (erro.value) erro.value = ''
}

function gerar() {
  erro.value = ''

  let id = null
  if (idPreenchido.value) {
    const bruto = String(ativoId.value).trim()
    if (!/^\d+$/.test(bruto)) {
      erro.value = 'O ID do ativo deve ser um numero inteiro.'
      return
    }
    id = Number(bruto)
    if (id <= 0) {
      erro.value = 'O ID do ativo deve ser maior que zero.'
      return
    }
  }

  emit('gerar', {
    ativoId: id,
    tipoId: tipoId.value ? Number(tipoId.value) : null,
    centroCusto: centroCusto.value || null,
    incluirDescartados: incluirDescartados.value
  })
  emit('close')
}
</script>

<template>
  <BaseModal :open="open" title="Imprimir QR Codes" @close="emit('close')">
    <BaseField
      v-model="ativoId"
      label="ID do ativo"
      type="number"
      min="1"
      step="1"
      placeholder="Deixe em branco pra ignorar"
      :erro="erro"
      opcional
      @input="limparErroQr"
    />

    <div
      v-if="idPreenchido && outrosFiltrosAtivos"
      class="text-sm text-texto-sub -mt-2 mb-4 leading-relaxed"
    >
      Com o ID preenchido, os filtros de tipo e centro de custo são ignorados.
    </div>

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

    <label class="flex items-center gap-1.5 text-base text-texto cursor-pointer mb-3">
      <input v-model="incluirDescartados" type="checkbox" />
      Incluir ativos descartados
    </label>

    <div class="text-sm text-texto-sub mb-2">Preencha o ID do ativo pra imprimir só um. Deixe tudo em branco pra imprimir todos.</div>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="!!erro" @click="gerar">Gerar</BaseButton>
    </template>
  </BaseModal>
</template>