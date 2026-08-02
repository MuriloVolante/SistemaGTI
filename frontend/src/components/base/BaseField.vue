<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  opcional: { type: Boolean, default: false },
  erro: { type: String, default: '' }
})
defineEmits(['update:modelValue'])

const mostrarSenha = ref(false)
const tipoReal = computed(() => {
  if (props.type !== 'password') return props.type
  return mostrarSenha.value ? 'text' : 'password'
})
</script>

<template>
  <div class="mb-4">
    <label v-if="label" class="field-label">
      {{ label }}
      <span v-if="opcional" class="font-normal normal-case tracking-normal">(opcional)</span>
    </label>
    <div class="relative">
      <input
        :type="tipoReal"
        :placeholder="placeholder"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        class="field-input"
        :class="type === 'password' && 'pr-9'"
      />
      <button
        v-if="type === 'password'"
        type="button"
        tabindex="-1"
        @click="mostrarSenha = !mostrarSenha"
        class="absolute right-2.5 top-1/2 -translate-y-1/2 text-texto-fraco hover:text-texto-sub"
      >
        {{ mostrarSenha ? '🙈' : '👁' }}
      </button>
    </div>
    <div v-if="erro" class="text-sm text-perigo-forte mt-1">{{ erro }}</div>
  </div>
</template>