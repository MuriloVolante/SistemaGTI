<script setup>
import { ref, computed, watch } from 'vue'
import { Copy, Eye, EyeOff, Check, TriangleAlert } from 'lucide-vue-next'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  valor: { type: String, default: '' },
  nome: { type: String, default: '' }
})
const emit = defineEmits(['close'])

const visivel = ref(false)
const copiado = ref(false)

const mascarada = computed(() => '•'.repeat(Math.min(props.valor.length || 32, 44)))

watch(() => props.open, (aberto) => {
  if (aberto) {
    visivel.value = false
    copiado.value = false
  }
})

async function copiar() {
  try {
    await navigator.clipboard.writeText(props.valor)
    copiado.value = true
    setTimeout(() => { copiado.value = false }, 2000)
  } catch {}
}
</script>

<template>
  <BaseModal :open="open" title="Chave criada" max-width="max-w-[600px]">
    <div class="flex gap-3 items-start bg-aviso-bg rounded-raio-sm px-3 py-3 mb-5">
      <TriangleAlert :size="18" class="text-aviso-text shrink-0 mt-0.5" />
      <p class="text-sm text-aviso-text">
        Esta é a única vez que a chave será exibida. Copie e guarde agora em local seguro.
        Depois de fechar esta janela não será possível vê-la novamente.
      </p>
    </div>

    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">
      Chave de {{ nome }}
    </div>
    <div class="flex items-center gap-2 mb-5">
      <code class="flex-1 font-mono text-sm bg-surface-alt border border-borda rounded-raio-sm px-3 py-2 truncate">
        {{ visivel ? valor : mascarada }}
      </code>
      <button
        type="button"
        class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt"
        :title="visivel ? 'Ocultar chave' : 'Mostrar chave'"
        @click="visivel = !visivel"
      >
        <EyeOff v-if="visivel" :size="16" />
        <Eye v-else :size="16" />
      </button>
      <button
        type="button"
        class="p-2 rounded-raio-sm border border-borda bg-surface hover:bg-surface-alt"
        title="Copiar chave"
        @click="copiar"
      >
        <Check v-if="copiado" :size="16" class="text-sucesso-text" />
        <Copy v-else :size="16" />
      </button>
    </div>

    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Como o sistema externo usa</div>
    <pre class="font-mono text-xs bg-surface-alt border border-borda rounded-raio-sm px-3 py-2 overflow-x-auto whitespace-pre-wrap break-all">X-API-Key: {{ visivel ? valor : mascarada }}</pre>

    <template #footer>
      <BaseButton variant="primary" @click="emit('close')">Continuar</BaseButton>
    </template>
  </BaseModal>
</template>