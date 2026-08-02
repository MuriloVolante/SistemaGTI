<script setup>
defineProps({
  variant: { type: String, default: 'default' },
  size: { type: String, default: 'md' },
  block: { type: Boolean, default: false }
})
</script>

<template>
  <button
    class="inline-flex items-center gap-1.5 rounded-raio-sm border font-medium cursor-pointer transition-colors font-sans"
    :class="[
      variant === 'primary' && 'bg-primaria border-primaria text-white hover:bg-primaria-hover hover:border-primaria-hover active:bg-primaria-active',
      variant === 'danger' && 'bg-white border-perigo-borda text-perigo-forte hover:bg-red-50 hover:border-red-300',
      variant === 'ghost' && 'border-transparent bg-transparent hover:bg-surface-alt',
      variant === 'default' && 'bg-surface border-borda text-texto hover:bg-surface-alt hover:border-borda-forte',
      size === 'sm' ? 'px-2.5 py-1 text-sm' : 'px-3.5 py-[7px] text-base',
      block && 'w-full justify-center'
    ]"
  >
    <slot />
  </button>
</template>