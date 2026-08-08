<script setup>
import InfoIcon from '@/components/base/InfoIcon.vue'

defineProps({
  titulo: { type: String, required: true },
  tip: { type: String, required: true },
  altura: { type: [String, Number], default: 280 }
})
</script>

<template>
  <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4">
    <div class="flex items-start justify-between gap-2 mb-3">
      <div class="text-base font-semibold text-texto">{{ titulo }}</div>
      <InfoIcon :texto="tip" />
    </div>
    <div class="relative" :style="{ height: typeof altura === 'number' ? altura + 'px' : altura }">
      <slot />
    </div>
  </div>
</template>