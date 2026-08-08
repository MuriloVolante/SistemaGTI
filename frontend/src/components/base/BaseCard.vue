<script setup>
defineProps({ title: { type: String, default: '' } })
</script>

<template>
  <div class="bg-surface border border-borda rounded-raio shadow-sombra overflow-hidden">
    <div v-if="title || $slots.header" class="px-4 py-3 bg-surface-alt border-b border-borda flex items-center justify-between">
      <span v-if="title" class="text-base font-semibold text-texto">{{ title }}</span>
      <slot name="header" />
    </div>
    <slot />
  </div>
</template>