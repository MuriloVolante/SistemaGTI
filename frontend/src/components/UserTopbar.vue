<script setup>
defineProps({
  abas: { type: Array, required: true }, // [{ key, label }]
  ativa: { type: String, required: true }
})
defineEmits(['change', 'logout'])
</script>

<template>
  <div class="flex items-center justify-between py-[13px] px-6 bg-surface border-b border-borda shadow-sombra">
    <div class="text-xl font-bold text-texto">GTI</div>
    <div class="flex gap-1">
      <button
        v-for="a in abas"
        :key="a.key"
        type="button"
        class="px-3.5 py-[7px] text-base font-medium border-none rounded-raio-sm cursor-pointer bg-transparent"
        :class="a.key === ativa ? 'bg-primaria-light text-primaria-text' : 'text-texto-sub hover:bg-surface-alt hover:text-texto'"
        @click="$emit('change', a.key)"
      >
        {{ a.label }}
      </button>
    </div>
    <a href="#" class="text-base text-texto-sub no-underline hover:text-texto" @click.prevent="$emit('logout')">
      Sair
    </a>
  </div>
</template>