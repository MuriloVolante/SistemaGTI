<script setup>
defineProps({ variant: { type: String, default: 'default' } })

const map = {
  ativo: 'bg-sucesso-bg text-sucesso-text',
  bloq: 'bg-perigo-bg text-perigo-text',
  varchar: 'bg-info-bg text-info-text',
  int: 'bg-sucesso-bg text-sucesso-text',
  date: 'bg-aviso-bg text-aviso-text',
  boolean: 'bg-roxo-bg text-roxo-text',
  obrig: 'bg-perigo-bg text-perigo-text',
  default: 'bg-surface-alt text-texto-sub'
}
</script>

<template>
  <span class="inline-flex items-center px-2 py-[3px] rounded-full text-xs font-semibold" :class="map[variant] || map.default">
    <slot />
  </span>
</template>