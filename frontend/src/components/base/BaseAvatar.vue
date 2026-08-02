<script setup>
defineProps({ nome: { type: String, required: true } })

const iniciais = (nome) => nome.trim().split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase()
</script>

<template>
  <div class="w-8 h-8 rounded-full bg-primaria-light text-primaria-text text-sm font-semibold flex items-center justify-center flex-shrink-0">
    {{ iniciais(nome) }}
  </div>
</template>