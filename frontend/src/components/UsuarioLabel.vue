<script setup>
import { computed } from 'vue'

const props = defineProps({
  valor: { type: [Object, String, null], default: null },
  fallback: { type: String, default: '-' }
})

const nomeUsuario = computed(() =>
  typeof props.valor === 'string' ? props.valor : props.valor?.nomeUsuario ?? ''
)

const exibicao = computed(() => {
  if (!props.valor) return props.fallback
  if (typeof props.valor === 'string') return props.valor
  return props.valor.nomeCompleto || props.valor.nomeUsuario || props.fallback
})

const removido = computed(() => nomeUsuario.value.startsWith('removido_'))
</script>

<template>
  <span :class="removido && 'italic text-texto-fraco'">
    {{ removido ? 'Usuário removido' : exibicao }}
  </span>
</template>