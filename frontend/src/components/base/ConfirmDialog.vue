<script setup>
defineProps({
  open: { type: Boolean, required: true },
  titulo: { type: String, required: true },
  texto: { type: String, required: true },
  confirmarLabel: { type: String, default: 'Confirmar' }
})
defineEmits(['confirm', 'cancel'])
</script>

<template>
  <div v-if="open" class="fixed inset-0 bg-[rgba(9,30,66,.54)] z-[100] flex items-center justify-center p-4">
    <div class="bg-surface rounded-raio shadow-sombra-md p-6 max-w-[360px] w-full">
      <h4 class="text-lg font-semibold mb-2 text-texto">{{ titulo }}</h4>
      <p class="text-base text-texto-sub leading-relaxed">{{ texto }}</p>
      <div class="flex justify-end gap-2 mt-5">
        <BaseButton @click="$emit('cancel')">Cancelar</BaseButton>
        <BaseButton variant="danger" @click="$emit('confirm')">{{ confirmarLabel }}</BaseButton>
      </div>
    </div>
  </div>
</template>

<script>
import BaseButton from './BaseButton.vue'
export default { components: { BaseButton } }
</script>