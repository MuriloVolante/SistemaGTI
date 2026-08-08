<script setup>
import { useToast } from '@/composables/useToast'
const { mensagem, visivel } = useToast()
</script>

<template>
  <div
    class="fixed bottom-6 left-1/2 -translate-x-1/2 bg-texto text-white rounded-raio-sm px-[18px] py-2.5 text-base font-medium z-[200] whitespace-nowrap shadow-sombra-md transition-opacity pointer-events-none"
    :class="visivel ? 'opacity-100' : 'opacity-0'"
  >
    {{ mensagem }}
  </div>
</template>