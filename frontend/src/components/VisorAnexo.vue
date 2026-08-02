<script setup>
defineProps({
  open: { type: Boolean, required: true },
  url: { type: String, default: '' },
  nome: { type: String, default: 'Anexo' }
})
defineEmits(['close'])
</script>

<template>
  <div v-if="open" class="fixed inset-0 bg-[rgba(9,30,66,.54)] z-[100] flex items-center justify-center p-6" @click.self="$emit('close')">
    <div class="bg-surface rounded-raio shadow-sombra-md p-5 w-full max-w-[900px] h-[85vh] flex flex-col">
      <div class="text-xl font-semibold mb-3 text-texto truncate">{{ nome }}</div>
      <iframe :src="url" title="Visualização do anexo" class="flex-1 w-full border border-borda rounded-raio-sm bg-surface-alt"></iframe>
      <div class="flex justify-end mt-3">
        <button class="btn" @click="$emit('close')">Fechar</button>
      </div>
    </div>
  </div>
</template>