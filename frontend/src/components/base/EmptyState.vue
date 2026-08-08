<script setup>
defineProps({ texto: { type: String, default: 'Nenhum registro encontrado.' } })
</script>

<template>
  <div class="text-center py-12 px-8 text-texto-fraco text-base">
    {{ texto }}
  </div>
</template>