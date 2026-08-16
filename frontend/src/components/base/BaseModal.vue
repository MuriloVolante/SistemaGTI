<script setup>
defineProps({
  open: { type: Boolean, required: true },
  title: { type: String, default: '' },
  maxWidth: { type: String, default: 'max-w-[480px]' }
})
defineEmits(['close'])
</script>

<template>
  <div
    v-if="open"
    class="fixed inset-0 bg-[rgba(9,30,66,.54)] z-[100] flex items-center justify-center p-4"
  >
    <div class="bg-surface rounded-raio shadow-sombra-md p-6 w-full max-h-[90vh] overflow-y-auto" :class="maxWidth">
      <div v-if="title" class="text-xl font-semibold mb-5 text-texto">{{ title }}</div>
      <slot />
      <div v-if="$slots.footer" class="flex justify-end gap-2 mt-5 pt-4 border-t border-borda">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>