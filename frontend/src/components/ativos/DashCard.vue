<script setup>
import InfoIcon from '@/components/base/InfoIcon.vue'

defineProps({
  icone: { type: [Object, Function], required: true },
  label: { type: String, required: true },
  tip: { type: String, required: true },
  valor: { type: String, default: '' },
  sub: { type: String, default: '' },
  cor: { type: String, default: '' },
  pequeno: { type: Boolean, default: false },
  emDesenvolvimento: { type: Boolean, default: false }
})
</script>

<template>
  <div class="bg-surface border border-borda rounded-raio shadow-sombra p-4 flex gap-3.5">
    <div class="w-20 h-20 rounded-raio bg-primaria-light text-primaria flex items-center justify-center flex-shrink-0">
      <component :is="icone" :size="34" />
    </div>
    <div class="min-w-0 flex-1">
      <div class="flex items-start justify-between gap-2 mb-1">
        <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub">{{ label }}</div>
        <InfoIcon :texto="tip" />
      </div>
      <div v-if="emDesenvolvimento" class="text-base text-texto-fraco italic">Em desenvolvimento</div>
      <div v-else class="font-semibold text-texto truncate" :class="[pequeno ? 'text-2xl' : 'text-4xl', cor]">{{ valor }}</div>
      <div v-if="sub" class="text-sm text-texto-sub mt-0.5">{{ sub }}</div>
    </div>
  </div>
</template>