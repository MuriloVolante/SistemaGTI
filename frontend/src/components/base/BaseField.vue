<script setup>
import { ref, computed } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  as: { type: String, default: 'input' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  opcional: { type: Boolean, default: false },
  erro: { type: String, default: '' },
  rows: { type: [String, Number], default: 3 }
})
defineEmits(['update:modelValue'])

const mostrarSenha = ref(false)
const ehSenha = computed(() => props.as === 'input' && props.type === 'password')
const tipoReal = computed(() => {
  if (!ehSenha.value) return props.type
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
      <select
        v-if="as === 'select'"
        :value="modelValue"
        v-bind="$attrs"
        class="field-input"
        :class="erro && 'field-input-erro'"
        @change="$emit('update:modelValue', $event.target.value)"
      >
        <slot />
      </select>

      <textarea
        v-else-if="as === 'textarea'"
        :placeholder="placeholder"
        :rows="rows"
        :value="modelValue"
        v-bind="$attrs"
        class="field-input"
        :class="erro && 'field-input-erro'"
        @input="$emit('update:modelValue', $event.target.value)"
      ></textarea>

      <input
        v-else
        :type="tipoReal"
        :placeholder="placeholder"
        :value="modelValue"
        v-bind="$attrs"
        class="field-input"
        :class="[erro && 'field-input-erro', ehSenha && 'pr-9']"
        @input="$emit('update:modelValue', $event.target.value)"
      />

      <button
        v-if="ehSenha"
        type="button"
        tabindex="-1"
        class="absolute right-2.5 top-1/2 -translate-y-1/2 text-texto-fraco hover:text-texto-sub"
        :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
        @click="mostrarSenha = !mostrarSenha"
      >
        <component :is="mostrarSenha ? EyeOff : Eye" :size="16" />
      </button>
    </div>

    <div v-if="erro" class="text-sm text-perigo-forte mt-1">{{ erro }}</div>
  </div>
</template>