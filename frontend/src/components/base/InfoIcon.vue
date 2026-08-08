<script setup>
defineProps({ texto: { type: String, required: true } })
</script>

<template>
  <span
    tabindex="0"
    class="group relative flex-shrink-0 w-[15px] h-[15px] rounded-full border border-borda text-texto-fraco text-2xs font-semibold leading-[13px] text-center cursor-help hover:text-texto-sub hover:border-borda-forte"
  >
    i
    <span class="hidden group-hover:block group-focus:block absolute top-5 right-0 w-52 px-2.5 py-2 bg-texto text-white text-xs font-normal leading-relaxed rounded-raio-sm shadow-sombra-md z-30 text-left">
      {{ texto }}
    </span>
  </span>
</template>