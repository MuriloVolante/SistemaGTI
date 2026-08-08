<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: { type: [Number, String, null], default: null },
  nomeInicial: { type: String, default: '' },
  usuarios: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])

const texto = ref(props.nomeInicial)
const aberto = ref(false)
const raiz = ref(null)

const filtrados = computed(() => {
  const termo = texto.value.toLowerCase().trim()
  if (!termo) return props.usuarios
  return props.usuarios.filter(
    (u) =>
      u.nomeCompleto.toLowerCase().includes(termo) ||
      u.nomeUsuario.toLowerCase().includes(termo)
  )
})

function aoDigitar() {
  if (!texto.value.trim()) emit('update:modelValue', null)
  aberto.value = true
}

function selecionar(u) {
  texto.value = u.nomeCompleto
  emit('update:modelValue', u.id)
  aberto.value = false
}

function foraDoClique(e) {
  if (raiz.value && !raiz.value.contains(e.target)) aberto.value = false
}

function definirTexto(v) { texto.value = v }
defineExpose({ definirTexto })

onMounted(() => document.addEventListener('click', foraDoClique))
onUnmounted(() => document.removeEventListener('click', foraDoClique))
</script>

<template>
  <div ref="raiz" class="relative">
    <input
      v-model="texto"
      type="text"
      autocomplete="off"
      placeholder="Digite o nome..."
      class="field-input"
      @input="aoDigitar"
      @focus="aberto = true"
    />
    <div
      v-if="aberto && filtrados.length"
      class="absolute left-0 right-0 top-full mt-1 bg-surface border border-borda rounded-raio-sm shadow-sombra-md max-h-52 overflow-y-auto z-50"
    >
      <div
        v-for="u in filtrados"
        :key="u.id"
        class="px-3 py-2 text-base text-texto cursor-pointer hover:bg-surface-alt flex justify-between gap-3"
        @click="selecionar(u)"
      >
        <span>{{ u.nomeCompleto }}</span>
        <span class="text-texto-fraco text-sm">{{ u.nomeUsuario }}</span>
      </div>
    </div>
  </div>
</template>